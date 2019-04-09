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

package org.onap.policy.pap.main.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpGroupStateChangeResponse;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PAP component to change state of PDP group.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpGroupStateChangeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdpGroupStateChangeProvider.class);

    /**
     * Lock used when updating PDPs.
     */
    private final Object updateLock;

    /**
     * Used to send UPDATE and STATE-CHANGE requests to the PDPs.
     */
    private final PdpModifyRequestMap requestMap;

    /**
     * Factory for PAP DAO.
     */
    PolicyModelsProviderFactoryWrapper modelProviderWrapper;

    /**
     * Constructs the object.
     */
    public PdpGroupStateChangeProvider() {
        modelProviderWrapper = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        updateLock = Registry.get(PapConstants.REG_PDP_MODIFY_LOCK, Object.class);
        requestMap = Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class);
    }

    /**
     * Changes state of a PDP group.
     *
     * @param groupName name of the PDP group
     * @param groupVersion version of the PDP group
     * @param pdpGroupState state of the PDP group
     * @return a pair containing the status and the response
     * @throws PfModelException in case of errors
     */
    public Pair<Response.Status, PdpGroupStateChangeResponse> changeGroupState(final String groupName,
            final String groupVersion, final PdpState pdpGroupState) throws PfModelException {
        synchronized (updateLock) {
            switch (pdpGroupState) {
                case ACTIVE:
                    handleActiveState(groupName, groupVersion);
                    break;
                case PASSIVE:
                    handlePassiveState(groupName, groupVersion);
                    break;
                default:
                    throw new PfModelException(Response.Status.BAD_REQUEST,
                            "Only ACTIVE or PASSIVE state changes are allowed");
            }
            return Pair.of(Response.Status.OK, new PdpGroupStateChangeResponse());
        }
    }

    private void handleActiveState(final String groupName, final String groupVersion) throws PfModelException {
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            final PdpGroupFilter filter = PdpGroupFilter.builder().name(groupName).groupState(PdpState.ACTIVE).build();
            final List<PdpGroup> activePdpGroups = databaseProvider.getFilteredPdpGroups(filter);
            final List<PdpGroup> pdpGroups = databaseProvider.getPdpGroups(groupName, groupVersion);
            if (activePdpGroups.isEmpty() && !pdpGroups.isEmpty()) {
                updatePdpGroupAndPdp(databaseProvider, pdpGroups, PdpState.ACTIVE);
                sendPdpMessage(pdpGroups.get(0), PdpState.ACTIVE, databaseProvider);
            } else if (!pdpGroups.isEmpty() && !activePdpGroups.isEmpty()
                    && !pdpGroups.get(0).getVersion().equals(activePdpGroups.get(0).getVersion())) {
                updatePdpGroupAndPdp(databaseProvider, pdpGroups, PdpState.ACTIVE);
                updatePdpGroup(databaseProvider, activePdpGroups, PdpState.PASSIVE);
                sendPdpMessage(pdpGroups.get(0), PdpState.ACTIVE, databaseProvider);
            }
        }
    }

    private void handlePassiveState(final String groupName, final String groupVersion) throws PfModelException {
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            final List<PdpGroup> pdpGroups = databaseProvider.getPdpGroups(groupName, groupVersion);
            if (!pdpGroups.isEmpty() && !PdpState.PASSIVE.equals(pdpGroups.get(0).getPdpGroupState())) {
                updatePdpGroupAndPdp(databaseProvider, pdpGroups, PdpState.PASSIVE);
                sendPdpMessage(pdpGroups.get(0), PdpState.PASSIVE, databaseProvider);
            }
        }
    }

    private void updatePdpGroup(final PolicyModelsProvider databaseProvider, final List<PdpGroup> pdpGroups,
            final PdpState pdpState) throws PfModelException {
        pdpGroups.get(0).setPdpGroupState(pdpState);
        databaseProvider.updatePdpGroups(pdpGroups);

        LOGGER.debug("Updated PdpGroup in DB - {} ", pdpGroups);
    }

    private void updatePdpGroupAndPdp(final PolicyModelsProvider databaseProvider, final List<PdpGroup> pdpGroups,
            final PdpState pdpState) throws PfModelException {
        pdpGroups.get(0).setPdpGroupState(pdpState);
        for (final PdpSubGroup subGroup : pdpGroups.get(0).getPdpSubgroups()) {
            for (final Pdp pdp : subGroup.getPdpInstances()) {
                pdp.setPdpState(pdpState);
            }
        }
        databaseProvider.updatePdpGroups(pdpGroups);

        LOGGER.debug("Updated PdpGroup and Pdp in DB - {} ", pdpGroups);
    }

    private void sendPdpMessage(final PdpGroup pdpGroup, final PdpState pdpState,
            final PolicyModelsProvider databaseProvider) throws PfModelException {

        for (final PdpSubGroup subGroup : pdpGroup.getPdpSubgroups()) {
            for (final Pdp pdp : subGroup.getPdpInstances()) {
                final PdpUpdate pdpUpdatemessage =
                        createPdpUpdateMessage(pdpGroup.getName(), subGroup, pdp.getInstanceId(), databaseProvider);
                final PdpStateChange pdpStateChangeMessage =
                        createPdpStateChangeMessage(pdpGroup.getName(), subGroup, pdp.getInstanceId(), pdpState);
                requestMap.addRequest(pdpUpdatemessage, pdpStateChangeMessage);
                LOGGER.debug("Sent PdpUpdate message - {}", pdpUpdatemessage);
                LOGGER.debug("Sent PdpStateChange message - {}", pdpStateChangeMessage);
            }
        }
    }

    private PdpUpdate createPdpUpdateMessage(final String pdpGroupName, final PdpSubGroup subGroup,
            final String pdpInstanceId, final PolicyModelsProvider databaseProvider) throws PfModelException {

        final PdpUpdate update = new PdpUpdate();
        update.setName(pdpInstanceId);
        update.setPdpGroup(pdpGroupName);
        update.setPdpSubgroup(subGroup.getPdpType());
        update.setPolicies(getToscaPolicies(subGroup, databaseProvider));

        LOGGER.debug("Created PdpUpdate message - {}", update);
        return update;
    }

    private List<ToscaPolicy> getToscaPolicies(final PdpSubGroup subGroup, final PolicyModelsProvider databaseProvider)
            throws PfModelException {
        final List<ToscaPolicy> policies = new ArrayList<>();
        for (final ToscaPolicyIdentifier policyIdentifier : subGroup.getPolicies()) {
            policies.addAll(databaseProvider.getPolicyList(policyIdentifier.getName(), policyIdentifier.getVersion()));
        }
        LOGGER.debug("Created ToscaPolicy list - {}", policies);
        return policies;
    }

    private PdpStateChange createPdpStateChangeMessage(final String pdpGroupName, final PdpSubGroup subGroup,
            final String pdpInstanceId, final PdpState pdpState) {

        final PdpStateChange stateChange = new PdpStateChange();
        stateChange.setName(pdpInstanceId);
        stateChange.setPdpGroup(pdpGroupName);
        stateChange.setPdpSubgroup(subGroup.getPdpType());
        stateChange.setState(pdpState);
        LOGGER.debug("Created PdpStateChange message - {}", stateChange);
        return stateChange;
    }
}
