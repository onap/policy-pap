/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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
import lombok.Builder;
import lombok.NonNull;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.TimerManager.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks PDPs. When a PDP is added to the tracker, a timer is started. If the PDP is not
 * re-added to the tracker before the timer expires, then
 * {@link PdpModifyRequestMap#removeFromGroups(String)} is called.
 */
public class PdpTracker {
    private static final Logger logger = LoggerFactory.getLogger(PdpTracker.class);

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
     * Used to remove a PDP from its group/subgroup.
     */
    private final PdpModifyRequestMap requestMap;


    /**
     * Constructs the object. Loads the list of PDPs to be tracked, from the DB.
     *
     * @param requestMap map used to remove a PDP from its group/subgroup
     * @param modifyLock object to be locked while data structures are updated
     * @param timers timers used to detect missed heart beats
     * @param daoFactory DAO factory
     */
    @Builder
    public PdpTracker(@NonNull PdpModifyRequestMap requestMap, @NonNull Object modifyLock, @NonNull TimerManager timers,
                    @NonNull PolicyModelsProviderFactoryWrapper daoFactory) {

        this.requestMap = requestMap;
        this.modifyLock = modifyLock;
        this.timers = timers;

        loadPdps(daoFactory);
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
            Timer timer = pdp2timer.remove(pdpName);
            if (timer != null) {
                timer.cancel();
            }

            timer = timers.register(pdpName, this::handleTimeout);
            pdp2timer.put(pdpName, timer);
        }
    }

    /**
     * Handles a timeout. Removes the PDP from {@link #pdp2timer}.
     *
     * @param pdpName name of the PDP whose timer has expired
     */
    private void handleTimeout(String pdpName) {
        synchronized (modifyLock) {
            // remove timer - no need to cancel it, as TimerManager does that
            logger.warn("missed heart beats - removing PDP {}", pdpName);
            pdp2timer.remove(pdpName);

            try {
                requestMap.removeFromGroups(pdpName);

            } catch (PfModelException e) {
                logger.warn("unable to remove PDP {} from its group/subgroup", pdpName, e);
            }
        }
    }
}
