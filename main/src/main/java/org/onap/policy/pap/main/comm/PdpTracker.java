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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.NonNull;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks PDPs. Each time the timer expires, it checks the PDPs associated with all
 * groups, bumping their counts. If the count reaches {@link #MAX_MISSED_HEARTBEATS},
 * then, then a state-change request is sent to the PDP, acting like an "are you alive"
 * query. When {@link #add(String)} is invoked, the specified PDP's count is removed,
 * effectively resetting it.
 */
public class PdpTracker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PdpTracker.class);

    /**
     * Max number of heat beats that can be missed before PAP removes a PDP.
     */
    public static final int MAX_MISSED_HEARTBEATS = 3;

    /**
     * Maps a PDP to the number of heart beats that have been missed.
     */
    private final Map<String, Integer> pdp2count = new ConcurrentHashMap<>();

    /**
     * PDP modification lock.
     */
    private final Object modifyLock;

    /**
     * DAO factory.
     */
    private final PolicyModelsProviderFactoryWrapper daoFactory;

    /**
     * Used to remove a PDP from its group/subgroup.
     */
    private final PdpModifyRequestMap requestMap;


    /**
     * Constructs the object. Loads the list of PDPs to be tracked, from the DB.
     *
     * @param requestMap map used to remove a PDP from its group/subgroup
     * @param modifyLock object to be locked while data structures are updated
     * @param daoFactory DAO factory
     */
    @Builder
    public PdpTracker(@NonNull PdpModifyRequestMap requestMap, @NonNull Object modifyLock,
                    @NonNull PolicyModelsProviderFactoryWrapper daoFactory) {

        this.requestMap = requestMap;
        this.modifyLock = modifyLock;
        this.daoFactory = daoFactory;
    }

    /**
     * Checks the PDPs found in the DB.
     */
    @Override
    public void run() {
        synchronized (modifyLock) {
            logger.info("checking for missed heart beats");

            try (PolicyModelsProvider dao = daoFactory.create()) {
                Set<String> stillInDb = new HashSet<>();

                for (PdpGroup group : dao.getPdpGroups(null)) {
                    checkPdpsFromGroup(stillInDb, group);
                }

                // remove PDPs that no longer appear within the DB
                pdp2count.keySet().retainAll(stillInDb);

            } catch (PfModelException e) {
                throw new PolicyPapRuntimeException("cannot load PDPs from the DB", e);
            }
        }
    }

    /**
     * Checks the PDPs appearing within a group.
     *
     * @param stillInDb set of PDPs that still appear within the DB
     * @param group group whose PDPs are to be loaded
     */
    private void checkPdpsFromGroup(Set<String> stillInDb, PdpGroup group) {
        PdpState state = group.getPdpGroupState();
        String groupName = group.getName();

        for (PdpSubGroup subgrp : group.getPdpSubgroups()) {
            String pdpType = subgrp.getPdpType();

            for (Pdp pdp : subgrp.getPdpInstances()) {
                String pdpName = pdp.getInstanceId();
                stillInDb.add(pdpName);
                checkPdp(state, groupName, pdpType, pdpName);
            }
        }
    }

    /**
     * Checks a single PDP.
     *
     * @param state the group's state
     * @param groupName the group's name
     * @param pdpType the subgroup's PDP type
     * @param pdpName the PDP name
     */
    private void checkPdp(PdpState state, String groupName, String pdpType, String pdpName) {
        pdp2count.compute(pdpName, (key, oldCount) -> {
            if (oldCount == null) {
                // first appearance - nothing missed yet
                logger.info("{} is still active", pdpName);
                return 0;
            }

            int newCount = oldCount + 1;
            if (newCount != MAX_MISSED_HEARTBEATS) {
                logger.info("missed heart beat {} for {}", newCount, pdpName);
                return newCount;
            }

            logger.warn("missed heart beat {} for {} - sending query", newCount, pdpName);

            PdpStateChange change = new PdpStateChange();
            change.setName(pdpName);
            change.setPdpGroup(groupName);
            change.setPdpSubgroup(pdpType);
            change.setState(state);

            requestMap.addRequest(change);

            /*
             * Update the count so we don't generate a message again.  The count will
             * be removed at the first check after it's removed from the DB.
             */
            return newCount;
        });
    }

    /**
     * Adds a PDP to the tracker, indicating that a message has been received from it.
     *
     * @param pdpName name of the PDP
     */
    public void add(String pdpName) {
        // remove the PDP from the counters to indicate that it has been seen
        pdp2count.remove(pdpName);
    }
}
