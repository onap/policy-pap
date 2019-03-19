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

import lombok.Getter;
import org.onap.policy.common.capabilities.Startable;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.common.endpoints.listeners.TypedMessageListener;
import org.onap.policy.common.utils.services.ServiceManager;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.onap.policy.pdp.common.models.PdpMessage;
import org.onap.policy.pdp.common.models.PdpStatus;

/**
 * Objects used when communicating with PDPs.
 */
public class PdpCommObjectsImpl implements Startable, PdpCommObjects {

    /**
     * Configuration parameters.
     */
    @Getter
    private final PdpParameters config;

    /**
     * Used to publish requests to the PDPs.
     */
    @Getter
    private final Publisher publisher;

    /**
     * Timers for UPDATE requests.
     */
    @Getter
    private final TimerManager updateTimers;

    /**
     * Timers for STATE-CHANGE requests.
     */
    @Getter
    private final TimerManager stateChangeTimers;

    /**
     * Listens for {@link PdpStatus} messages and then routes them to the listener
     * associated with the ID of the originating request.
     */
    private final RequestIdDispatcher<PdpStatus> pdpRespDispatcher;

    /**
     * Lock used to prevent simultaneous updates to PDP data structures.
     */
    @Getter
    private final Object pdpLock;

    /**
     * Maps a PDP to its request data.
     */
    @Getter
    private final PdpModifyRequestMap modifyRequests;

    /**
     * Used to start and stop the communication objects.
     */
    private final ServiceManager svcmgr;


    /**
     * Constructs the object.
     *
     * @param params configuration parameters
     * @param pdpRespDispatcher dispatcher used to process responses from PDPs
     * @param pdpLock lock used to prevent simultaneous updates to PDP data structures
     * @throws PolicyPapException if an error occurs
     */
    public PdpCommObjectsImpl(PdpParameters params, RequestIdDispatcher<PdpStatus> pdpRespDispatcher, Object pdpLock)
                    throws PolicyPapException {

        this.config = params;
        this.publisher = new Publisher(POLICY_PDP_PAP);
        this.updateTimers = new TimerManager("update", params.getUpdateParameters().getMaxWaitMs());
        this.stateChangeTimers = new TimerManager("update", params.getStateChangeParameters().getMaxWaitMs());
        this.pdpRespDispatcher = pdpRespDispatcher;
        this.pdpLock = pdpLock;
        this.modifyRequests = new PdpModifyRequestMap();

        // @formatter:off
        svcmgr = new ServiceManager("PDP communication")
                        .addAction("publisher", () -> startThread(publisher), publisher::stop)
                        .addAction("update timers", () -> startThread(updateTimers), updateTimers::stop);
        // @formatter:on
    }

    /**
     * Starts a background thread.
     *
     * @param runner function to run in the background
     */
    private void startThread(Runnable runner) {
        Thread thread = new Thread(runner);
        thread.setDaemon(true);

        thread.start();
    }

    @Override
    public void enqueue(QueueToken<PdpMessage> ref) {
        publisher.enqueue(ref);
    }

    @Override
    public void registerListener(String reqid, TypedMessageListener<PdpStatus> listener) {
        pdpRespDispatcher.register(reqid, listener);
    }

    @Override
    public void unregisterListener(String reqid) {
        pdpRespDispatcher.unregister(reqid);
    }

    /**
     * Indicates that a PDP should be taken offline.
     *
     * @param name name of the PDP to take offline
     * @param reason reason the PDP is being taken offline
     */
    public void stopPdp(String name, String reason) {
        // TOOD Remove PDP from DB
    }

    @Override
    public boolean isAlive() {
        return svcmgr.isAlive();
    }

    @Override
    public void shutdown() {
        svcmgr.shutdown();
    }

    @Override
    public boolean start() {
        return svcmgr.start();
    }

    @Override
    public boolean stop() {
        return svcmgr.stop();
    }
}
