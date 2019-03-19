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

import org.onap.policy.common.endpoints.listeners.TypedMessageListener;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.onap.policy.pdp.common.models.PdpMessage;
import org.onap.policy.pdp.common.models.PdpStatus;

/**
 * Objects used when communicating with PDPs.
 */
public interface PdpCommObjects {

    String POLICY_PDP_PAP = "POLICY-PDP-PAP";

    /**
     * Gets the configuration parameters.
     *
     * @return the configuration parameters
     */
    PdpParameters getConfig();

    /**
     * Gets the publisher with which to publish requests to the PDPs.
     *
     * @return the PDP publisher
     */
    Publisher getPublisher();

    /**
     * Gets the timers for UPDATE requests.
     *
     * @return the timers for UPDATE requests
     */
    TimerManager getUpdateTimers();

    /**
     * Gets the timers for STATE-CHANGE requests.
     *
     * @return the timers for STATE-CHANGE requests
     */
    TimerManager getStateChangeTimers();

    /**
     * Gets the lock used to prevent simultaneous updates to PDP data structures.
     * @return the lock used to prevent simultaneous updates to PDP data structures
     */
    Object getPdpLock();

    /**
     * Adds a token to the publisher queue.
     * @param ref token to be added to the queue
     */
    void enqueue(QueueToken<PdpMessage> ref);

    /**
     * Registers a listener with the response dispatcher.
     * @param reqid request ID for which to listen
     * @param listener the listener that will handle the response
     */
    void registerListener(String reqid, TypedMessageListener<PdpStatus> listener);

    /**
     * Unregisters the listener associated with a particular request ID.
     * @param reqid request ID whose listener is to be unregistered
     */
    void unregisterListener(String reqid);

    /**
     * Indicates that a PDP should be taken offline.
     *
     * @param name name of the PDP to take offline
     * @param reason reason the PDP is being taken offline
     */
    void stopPdp(String name, String reason);
}
