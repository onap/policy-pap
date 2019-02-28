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

import java.util.List;
import lombok.Getter;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for communication with PDPs.
 */
public class PdpClient {
    private static final Logger logger = LoggerFactory.getLogger(PdpClient.class);

    /**
     * Coder used to encode messages being sent to PDPs.
     */
    private static final Coder CODER = new StandardCoder();

    /**
     * Topic to which messages are published.
     */
    @Getter
    private final String topic;

    /**
     * Where messages are published.
     */
    private final TopicSink sink;

    /**
     * Constructs the object.
     *
     * @param topic topic to which messages should be published
     * @throws PdpClientException if the topic does not exist
     */
    public PdpClient(String topic) throws PdpClientException {
        this.topic = topic;

        List<TopicSink> lst = getTopicSinks(topic);
        if (lst.isEmpty()) {
            throw new PdpClientException("no sinks for topic: " + topic);
        }

        this.sink = lst.get(0);
    }

    /**
     * Sends a message to the PDPs via the topic, after encoding the message as json.
     *
     * @param message message to be encoded and sent
     * @return {@code true} if the message was successfully sent/enqueued, {@code false}
     *         otherwise
     */
    public boolean send(Object message) {
        try {
            String json = CODER.encode(message);
            return sink.send(json);

        } catch (RuntimeException | CoderException e) {
            logger.warn("send to {} failed because of {}", topic, e.getMessage(), e);
            return false;
        }
    }

    // the remaining methods are wrappers that can be overridden by junit tests

    /**
     * Gets the sinks for a given topic.
     *
     * @param topic the topic of interest
     * @return the sinks for the topic
     */
    protected List<TopicSink> getTopicSinks(String topic) {
        return TopicEndpoint.manager.getTopicSinks(topic);
    }
}
