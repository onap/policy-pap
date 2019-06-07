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
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpHealthCheck;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.TimerManager.Timer;
import org.onap.policy.pap.main.parameters.PdpTrackerParams;

/**
 * Tracks PDPs. When a PDP is added to the tracker, a timer is started. If the PDP is not
 * re-added to the tracker before the timer expires, then a HEALTH-CHECK request is sent
 * to the PDP.
 */
public class PdpTracker implements Runnable {

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
    private final PdpRequestMap requestMap;


    /**
     * Constructs the object. Loads the list of PDPs to be tracked, from the DB.
     *
     * @param params parameters used to configure the tracker
     */
    public PdpTracker(PdpTrackerParams params) {
        this.requestMap = params.getRequestMap();
        this.modifyLock = params.getModifyLock();

        long heartBeatMs = params.getHeartBeatMs();
        this.timers = new TimerManager("heart beat", heartBeatMs + Math.max(1, heartBeatMs / 5));

        loadPdps(params.getDaoFactory());
    }

    /**
     * Loads the PDPs from the DB and starts the timer thread.
     */
    @Override
    public void run() {
        timers.run();
    }

    /**
     * Stops the timer thread.
     */
    public void stop() {
        timers.stop();
    }

    /**
     * Loads the PDPs from the DB.
     *
     * @param daoFactory DAO factory
     */
    private void loadPdps(PolicyModelsProviderFactoryWrapper daoFactory) {
        synchronized (modifyLock) {
            try (PolicyModelsProvider dao = daoFactory.create()) {
                for (PdpGroup group : dao.getPdpGroups(null)) {
                    loadPdpsFromGroup(group);
                }

            } catch (PfModelException e) {
                throw new PolicyPapRuntimeException("cannot load PDPs from the DB", e);
            }
        }
    }

    /**
     * Loads the PDPs appearing within a group.
     *
     * @param group group whose PDPs are to be loaded
     */
    private void loadPdpsFromGroup(PdpGroup group) {
        for (PdpSubGroup subgrp : group.getPdpSubgroups()) {
            for (Pdp pdp : subgrp.getPdpInstances()) {
                add(pdp.getInstanceId());
            }
        }
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
