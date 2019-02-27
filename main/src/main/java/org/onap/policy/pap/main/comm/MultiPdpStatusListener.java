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

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.TypedMessageListener;
import org.onap.policy.pdp.common.models.PdpStatus;

/**
 * Listener for PDP Status messages expected to be received from multiple PDPs. The
 * listener "completes" once a message has been seen from all of the PDPs.
 */
public class MultiPdpStatusListener implements TypedMessageListener<PdpStatus> {

    /**
     * This is decremented once a message has been received from every PDP.
     */
    private final CountDownLatch allSeen = new CountDownLatch(1);

    /**
     * PDPs from which no message has been received yet.
     */
    private final Set<String> unseenPdpNames = ConcurrentHashMap.newKeySet();

    /**
     * Constructs the object.
     *
     * @param pdpName name of the PDP for which to wait
     */
    public MultiPdpStatusListener(String pdpName) {
        unseenPdpNames.add(pdpName);
    }

    /**
     * Constructs the object.
     *
     * @param pdpNames names of the PDP for which to wait
     */
    public MultiPdpStatusListener(Collection<String> pdpNames) {
        if (pdpNames.isEmpty()) {
            allSeen.countDown();

        } else {
            unseenPdpNames.addAll(pdpNames);
        }
    }

    /**
     * Gets the set of names for which messages have not yet been received.
     *
     * @return the names of the PDPs that have not been seen yet
     */
    public SortedSet<String> getUnseenPdpNames() {
        return new TreeSet<>(unseenPdpNames);
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
     * Indicates that a message was received for a PDP. Triggers completion of
     * {@link #await(long, TimeUnit)} if all PDPs have received a message. Threads may
     * override this method to process a message. However, they should still invoke this
     * method so that PDPs can be properly tracked.
     */
    @Override
    public void onTopicEvent(CommInfrastructure infra, String topic, PdpStatus message) {
        unseenPdpNames.remove(message.getName());

        if (unseenPdpNames.isEmpty()) {
            allSeen.countDown();
        }
    }
}
