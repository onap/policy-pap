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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.PapParameterGroup;

public class PublisherTest extends Threaded {

    // these messages will have different request IDs
    private static final PdpStateChange MSG1 = new PdpStateChange();
    private static final PdpStateChange MSG2 = new PdpStateChange();

    // MSG1 & MSG2, respectively, encoded as JSON
    private static final String JSON1;
    private static final String JSON2;

    static {
        try {
            Coder coder = new StandardCoder();
            JSON1 = coder.encode(MSG1);
            JSON2 = coder.encode(MSG2);

        } catch (CoderException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Max time to wait, in milliseconds, for a thread to terminate or for a message to be
     * published.
     */
    private static final long MAX_WAIT_MS = 5000;

    private Publisher<PdpMessage> pub;
    private MyListener listener;

    /**
     * Configures the topic and attaches a listener.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        //final String[] papConfigParameters = {"-c", "parameters/PapConfigParameters.json"};
        final PapParameterGroup parameterGroup = new CommonTestData().getPapParameterGroup(6969);
        TopicEndpointManager.getManager().shutdown();

        TopicEndpointManager.getManager().addTopics(parameterGroup.getTopicParameterGroup());
        TopicEndpointManager.getManager().start();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        TopicEndpointManager.getManager().shutdown();
    }

    /**
     * Set up.
     *
     * @throws Exception if an error occurs
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        pub = new Publisher<>(PapConstants.TOPIC_POLICY_PDP_PAP);

        listener = new MyListener();
        TopicEndpointManager.getManager().getNoopTopicSink(PapConstants.TOPIC_POLICY_PDP_PAP).register(listener);
    }

    /**
     * Tear down.
     *
     * @throws Exception if an error occurs
     */
    @Override
    @After
    public void tearDown() throws Exception {
        TopicEndpointManager.getManager().getNoopTopicSink(PapConstants.TOPIC_POLICY_PDP_PAP).unregister(listener);

        super.tearDown();
    }

    @Override
    protected void stopThread() {
        if (pub != null) {
            pub.stop();
        }
    }

    @Test
    public void testPublisher_testStop() throws Exception {
        startThread(pub);
        pub.stop();

        assertTrue(waitStop());

        // ensure we can call "stop" a second time
        pub.stop();
    }

    @Test
    public void testPublisher_Ex() throws Exception {
        assertThatThrownBy(() -> new Publisher<>("unknwon-topic")).isInstanceOf(PolicyPapException.class);
    }

    @Test
    public void testEnqueue() throws Exception {
        // enqueue before running
        pub.enqueue(new QueueToken<>(MSG1));

        // enqueue another after running
        startThread(pub);
        pub.enqueue(new QueueToken<>(MSG2));

        String json = listener.await(MAX_WAIT_MS);
        assertEquals(JSON1, json);

        json = listener.await(MAX_WAIT_MS);
        assertEquals(JSON2, json);
    }

    @Test
    public void testRun_StopBeforeProcess() throws Exception {
        // enqueue before running
        QueueToken<PdpMessage> token = new QueueToken<>(MSG1);
        pub.enqueue(token);

        // stop before running
        pub.stop();

        // start the thread and then wait for it to stop
        startThread(pub);
        assertTrue(waitStop());

        // message should not have been processed
        assertTrue(listener.isEmpty());
        assertNotNull(token.get());
    }

    @Test
    public void testRun() throws Exception {
        startThread(pub);

        // should skip token with null message
        QueueToken<PdpMessage> token1 = new QueueToken<>(null);
        pub.enqueue(token1);

        QueueToken<PdpMessage> token2 = new QueueToken<>(MSG2);
        pub.enqueue(token2);

        // only the second message should have been processed
        String json = listener.await(MAX_WAIT_MS);
        assertEquals(JSON2, json);
        assertNull(token2.get());

        pub.stop();
        assertTrue(waitStop());

        // no more messages
        assertTrue(listener.isEmpty());
    }

    @Test
    public void testGetNext() throws Exception {
        startThread(pub);

        // wait for a message to be processed
        pub.enqueue(new QueueToken<>(MSG1));
        assertNotNull(listener.await(MAX_WAIT_MS));

        // now interrupt
        interruptThread();

        assertTrue(waitStop());
    }

    /**
     * Listener for messages published to the topic.
     */
    private static class MyListener implements TopicListener {

        /**
         * Released every time a message is added to the queue.
         */
        private final Semaphore sem = new Semaphore(0);

        private final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

        public boolean isEmpty() {
            return messages.isEmpty();
        }

        /**
         * Waits for a message to be published to the topic.
         *
         * @param waitMs time to wait, in milli-seconds
         * @return the next message in the queue, or {@code null} if there are no messages
         *         or if the timeout was reached
         * @throws InterruptedException if this thread was interrupted while waiting
         */
        public String await(long waitMs) throws InterruptedException {
            if (sem.tryAcquire(waitMs, TimeUnit.MILLISECONDS)) {
                return messages.poll();
            }

            return null;
        }

        @Override
        public void onTopicEvent(CommInfrastructure commType, String topic, String event) {
            messages.add(event);
            sem.release();
        }
    }
}
