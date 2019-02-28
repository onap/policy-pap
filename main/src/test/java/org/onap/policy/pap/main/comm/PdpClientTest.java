/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.comm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.TopicSink;

public class PdpClientTest {
    private static final String SINK_FIELD_NAME = "sink";
    private static final String TOPIC = "my-topic";

    private PdpClient client;
    private TopicSink sink;
    private List<TopicSink> sinks;

    /**
     * Creates mocks and an initial client object.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        sink = mock(TopicSink.class);
        when(sink.send(anyString())).thenReturn(true);

        sinks = Arrays.asList(sink, null);

        client = new PdpClient2(TOPIC);
    }

    /**
     * Uses a real NO-OP topic sink.
     */
    @Test
    public void testGetTopicSinks() throws Exception {
        // clear all topics and then configure one topic
        TopicEndpoint.manager.shutdown();

        Properties props = new Properties();
        props.setProperty("noop.sink.topics", TOPIC);
        TopicEndpoint.manager.addTopicSinks(props);

        sink = TopicEndpoint.manager.getNoopTopicSink(TOPIC);
        assertNotNull(sink);

        AtomicReference<String> evref = new AtomicReference<>(null);

        sink.register(new TopicListener() {
            @Override
            public void onTopicEvent(CommInfrastructure infra, String topic, String event) {
                evref.set(event);
            }
        });

        sink.start();

        client = new PdpClient(TOPIC);
        client.send(100);

        assertEquals("100", evref.get());
    }

    @Test
    public void testPdpClient_testGetTopic() {
        assertEquals(TOPIC, client.getTopic());
        assertSame(sink, Whitebox.getInternalState(client, SINK_FIELD_NAME));

        // unknown topic -> should throw exception
        sinks = new LinkedList<>();
        assertThatThrownBy(() -> new PdpClient2(TOPIC)).isInstanceOf(PdpClientException.class)
                        .hasMessage("no sinks for topic: my-topic");
    }

    @Test
    public void testSend() throws Exception {
        client.send(Arrays.asList("abc", "def"));
        verify(sink).send("['abc','def']".replace('\'', '"'));

        // sink send fails
        when(sink.send(anyString())).thenReturn(false);
        assertFalse(client.send("ghi"));

        // sink send throws an exception
        RuntimeException ex = new RuntimeException("expected exception");
        when(sink.send(anyString())).thenThrow(ex);
        assertFalse(client.send("jkl"));
    }

    /**
     * PdpClient with some overrides.
     */
    private class PdpClient2 extends PdpClient {

        public PdpClient2(String topic) throws PdpClientException {
            super(topic);
        }

        @Override
        protected List<TopicSink> getTopicSinks(String topic) {
            return sinks;
        }
    }
}
