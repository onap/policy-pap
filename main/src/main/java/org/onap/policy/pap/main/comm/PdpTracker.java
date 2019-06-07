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

import java.util.HashMap;
import java.util.Map;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.onap.policy.models.pdp.concepts.PdpHealthCheck;
import org.onap.policy.pap.main.comm.TimerManager.Timer;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;

/**
 * Tracks PDPs. When a PDP is added to the tracker, a timer is started. If the PDP is not
 * re-added to the tracker before the timer expires, then a HEALTH-CHECK request is sent
 * to the PDP.
 */
public class PdpTracker extends ServiceManagerContainer {

    /**
     * PDP expiration timers.
     */
    private final TimerManager timers;

    /**
     * Maps a PDP name to its expiration timer.
     */
    private final Map<String, TimerManager.Timer> pdp2timer = new HashMap<>();

    /**
     * PDP modification lock.
     */
    private final Object modifyLock;

    /**
     * Health check requests are published to this.
     */
    private final PdpModifyRequestMap requestMap;

    // TODO start the tracker
    // TODO circular: PdpTracker->PdpModifyRequestMap->PdpTracker

    /**
     * Constructs the object.
     *
     * @param requestMap where to publish health check requests
     * @param params parameters
     */
    public PdpTracker(PdpModifyRequestMap requestMap, PdpModifyRequestMapParams params) {
        this.requestMap = requestMap;
        this.modifyLock = params.getModifyLock();
        long heartBeatMs = params.getParams().getHeartBeatMs();
        this.timers = new TimerManager("heart beat", heartBeatMs + Math.max(1, heartBeatMs / 5));

        // @formatter:off
        addAction("PDP heart beat timers",
            () -> {
                Thread thread = new Thread(timers);
                thread.setDaemon(true);
                thread.start();
            },
            () -> timers.stop());

        addAction("PDP list",
            this::loadPdps,
            () -> { });
        // @formatter:on
    }

    /**
     * Loads the PDPs from the DB.
     */
    private void loadPdps() {
        // TODO populate
    }

    /**
     * Adds a PDP to the tracker and starts its timer. If a timer is already running, the
     * old timer is cancelled.
     *
     * @param pdpName name of the PDP
     */
    public void add(String pdpName) {
        synchronized (modifyLock) {
            Timer timer = pdp2timer.get(pdpName);
            if (timer != null) {
                timer.cancel();
            }

            timer = timers.register(pdpName, this::handleTimeout);
            pdp2timer.put(pdpName, timer);
        }
    }

    /**
     * Handles a timeout. Removes the PDP from {@link #pdp2timer} and cancels it's timer.
     *
     * @param pdpName name of the PDP whose timer has expired
     */
    private void handleTimeout(String pdpName) {
        synchronized (modifyLock) {
            Timer timer = pdp2timer.remove(pdpName);
            if (timer == null) {
                // PDP is no longer in the map - discard the timeout
                return;
            }

            timer.cancel();

            PdpHealthCheck check = new PdpHealthCheck();
            check.setName(pdpName);

            requestMap.addRequest(check);
        }
    }
}
