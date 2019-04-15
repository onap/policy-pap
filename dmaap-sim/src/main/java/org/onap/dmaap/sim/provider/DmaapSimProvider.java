/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.dmaap.sim.provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.Response;

import org.onap.dmaap.sim.DmaapSimRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider to simulate DMaaP.
 *
 * @author Liam Fallon (liam.fallon@est.tech)
 */
public class DmaapSimProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DmaapSimProvider.class);

    // Time for a get to wait before checking of a message has come
    private static final long DMAAP_SIM_WAIT_TIME = 50;

    // recurring constants
    private static final String WITH_TIMEOUT = " with timeout ";

    // The map of outstanding GET requests
    private static final Map<String, Map<String, Object>> topicMap = new LinkedHashMap<>();

    // Integer used to ensure that GET requests are unique
    private static AtomicInteger nextGetId = new AtomicInteger();

    /**
     * Process a DMaaP message.
     *
     * @param topicName The topic name
     * @param dmaapMessage the message to process
     * @return a response to the message
     */
    public Response processDmaapMessage(final String topicName, final Object dmaapMessage) {
        LOGGER.debug("Received DMaaP message: " + dmaapMessage);

        synchronized (topicMap) {
            Map<String, Object> subscriberMap = topicMap.get(topicName);
            if (subscriberMap == null) {
                LOGGER.debug("Dropped DMaaP message: " + dmaapMessage);
                return Response.status(Response.Status.OK).build();
            }

            for (Entry<String, Object> subscriberMapEntry : subscriberMap.entrySet()) {
                subscriberMapEntry.setValue(dmaapMessage);
            }
        }

        LOGGER.debug("Processed DMaaP message: " + dmaapMessage);
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Wait for and return a DMaaP message.
     *
     * @param topicName The topic to wait on
     * @param consumerGroup the consumer group that is waiting
     * @param consumerId the consumer ID that is waiting
     * @param timeout the length of time to wait for
     * @return the DMaaP message or
     */
    public Response getDmaapMessage(final String topicName, final String consumerGroup, final String consumerId,
            final int timeout) {

        String requestId = consumerGroup + ":" + consumerId + ':' + nextGetId.getAndIncrement();

        LOGGER.debug("Request for DMaaP message: " + requestId + WITH_TIMEOUT + timeout);

        synchronized (topicMap) {
            Map<String, Object> subscriberMap = topicMap.get(topicName);
            if (subscriberMap == null) {
                subscriberMap = new LinkedHashMap<>();
                topicMap.put(topicName, subscriberMap);
                LOGGER.trace("Created topic map entry for topic: " + topicName);
            }

            subscriberMap.put(requestId, null);
            LOGGER.trace("Added request ID to topic map: " + requestId + WITH_TIMEOUT + timeout);
        }

        long timeOfTimeout = System.currentTimeMillis() + timeout;

        do {
            try {
                Thread.sleep(DMAAP_SIM_WAIT_TIME);
            } catch (InterruptedException ie) {
                String errorMessage = "Interrupt on wait on simulation of DMaaP topic " + topicName + " for request ID "
                        + requestId + WITH_TIMEOUT + timeout;
                LOGGER.warn(errorMessage, ie);
                throw new DmaapSimRuntimeException(errorMessage, ie);
            }

            synchronized (topicMap) {
                Map<String, Object> subscriberMap = topicMap.get(topicName);

                if (subscriberMap == null) {
                    String errorMessage = "internal error on simulation of DMaaP topic " + topicName
                            + " for request ID " + requestId + WITH_TIMEOUT + timeout;
                    LOGGER.warn(errorMessage);
                    throw new DmaapSimRuntimeException(errorMessage);
                }

                Object dmaapMessage = subscriberMap.get(requestId);
                if (dmaapMessage != null) {
                    topicMap.get(topicName).remove(requestId);
                    LOGGER.trace("Removed request ID from topic map: " + requestId + WITH_TIMEOUT + timeout);

                    if (topicMap.get(topicName).isEmpty()) {
                        topicMap.remove(topicName);
                        LOGGER.trace("Removed topic map entry for topic: " + topicName);
                    }

                    subscriberMap.remove(requestId);

                    LOGGER.debug("Responded to request ID : " + requestId + WITH_TIMEOUT + timeout + " with message : "
                            + dmaapMessage);
                    return Response.status(Response.Status.OK).entity(dmaapMessage).build();
                }
            }

        }
        while (timeOfTimeout > System.currentTimeMillis());

        synchronized (topicMap) {
            topicMap.get(topicName).remove(requestId);
            LOGGER.trace("Removed request ID from topic map: " + requestId + WITH_TIMEOUT + timeout);

            if (topicMap.get(topicName).isEmpty()) {
                topicMap.remove(topicName);
                LOGGER.trace("Removed topic map entry for topic: " + topicName);
            }
        }

        LOGGER.debug("Request ID : " + requestId + WITH_TIMEOUT + timeout + " timed out");
        return Response.status(Response.Status.REQUEST_TIMEOUT).build();
    }
}
