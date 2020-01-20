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
 * Tracks PDPs.  Each time the timer expires, it checks the PDPs associated with
 * all groups.  If a PDP hasn't been added to the tracker since the last time
 * the timer expired, then a state-change request is sent to the PDP, acting like
 * an "are you alive" query.
 */
public class PdpTracker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PdpTracker.class);

    /**
     * Identifies PDPs that have been added to the tracker since the last check.
     */
    private final Set<String> pdpSeen = ConcurrentHashMap.newKeySet();

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
            try (PolicyModelsProvider dao = daoFactory.create()) {
                for (PdpGroup group : dao.getPdpGroups(null)) {
                    checkPdpsFromGroup(group);
                }

            } catch (PfModelException e) {
                throw new PolicyPapRuntimeException("cannot load PDPs from the DB", e);
            }
        }
    }

    /**
     * Checks the PDPs appearing within a group.
     *
     * @param group group whose PDPs are to be loaded
     */
    private void checkPdpsFromGroup(PdpGroup group) {
        PdpState state = group.getPdpGroupState();
        String groupName = group.getName();

        for (PdpSubGroup subgrp : group.getPdpSubgroups()) {
            String pdpType = subgrp.getPdpType();

            for (Pdp pdp : subgrp.getPdpInstances()) {
                checkPdp(state, groupName, pdpType, pdp.getInstanceId());
            }
        }
    }

    /**
     * Checks a single PDP.
     * @param state the group's state
     * @param groupName the group's name
     * @param pdpType the subgroup's PDP type
     * @param pdpName the PDP name
     */
    private void checkPdp(PdpState state, String groupName, String pdpType, String pdpName) {
        if (pdpSeen.remove(pdpName)) {
            logger.info("{} is still active", pdpName);
            return;
        }

        logger.warn("missed heart beat - sending query to {}", pdpName);

        PdpStateChange change = new PdpStateChange();
        change.setName(pdpName);
        change.setPdpGroup(groupName);
        change.setPdpSubgroup(pdpType);
        change.setState(state);

        requestMap.addRequest(change);
    }

    /**
     * Adds a PDP to the tracker, indicating that a message has been received from it.
     *
     * @param pdpName name of the PDP
     */
    public void add(String pdpName) {
        pdpSeen.add(pdpName);
    }
}
