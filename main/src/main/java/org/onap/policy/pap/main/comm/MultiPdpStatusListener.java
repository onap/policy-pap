/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.onap.policy.common.endpoints.listeners.TypedMessageListener;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for PDP Status messages expected to be received from multiple PDPs. The
 * listener "completes" once a message has been seen from all PDPs.
 */
public abstract class MultiPdpStatusListener implements TypedMessageListener<PdpStatus> {
    private static final Logger logger = LoggerFactory.getLogger(MultiPdpStatusListener.class);

    /**
     * This is decremented once a message has been received from every PDP.
     */
    private final CountDownLatch allSeen = new CountDownLatch(1);

    /**
     * IDs for which no message has been received yet.
     */
    private final Set<String> unseenIds = ConcurrentHashMap.newKeySet();

    /**
     * Constructs the object.
     *
     * @param id ID for which to wait
     */
    protected MultiPdpStatusListener(String id) {
        unseenIds.add(id);
    }

    /**
     * Constructs the object.
     *
     * @param ids IDs for which to wait
     */
    protected MultiPdpStatusListener(Collection<String> ids) {
        if (ids.isEmpty()) {
            allSeen.countDown();

        } else {
            unseenIds.addAll(ids);
        }
    }

    /**
     * Gets the set of IDs for which messages have not yet been received.
     *
     * @return the IDs that have not been seen yet
     */
    public SortedSet<String> getUnseenIds() {
        return new TreeSet<>(unseenIds);
    }

    /**
     * Waits for messages to be received for all PDPs, or until a timeout is reached.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return {@code true} if messages were received for all PDPs, {@code false} if the
     *         timeout was reached first
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return allSeen.await(timeout, unit);
    }

    /**
     * After giving the event to the subclass via
     * {@link #handleEvent(CommInfrastructure, String, PdpStatus)}, this triggers
     * completion of {@link #await(long, TimeUnit)}, if all PDPs have received a message.
     */
    @Override
    public final void onTopicEvent(CommInfrastructure infra, String topic, PdpStatus message) {
        String id = null;
        try {
            id = handleEvent(infra, topic, message);
        } catch (RuntimeException e) {
            logger.warn("handleEvent failed due to: {}", e.getMessage(), e);
        }

        if (id == null) {
            return;
        }

        unseenIds.remove(id);

        if (unseenIds.isEmpty()) {
            allSeen.countDown();
        }
    }

    /**
     * Indicates that a message was received for a PDP.
     *
     * @param infra infrastructure with which the message was received
     * @param topic topic on which the message was received
     * @param message message that was received
     * @return the ID extracted from the message, or {@code null} if it cannot be extracted
     */
    protected abstract String handleEvent(CommInfrastructure infra, String topic, PdpStatus message);
}
