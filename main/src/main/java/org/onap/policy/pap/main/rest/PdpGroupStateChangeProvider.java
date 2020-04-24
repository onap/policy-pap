/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019-2020 AT&T Intellectual Property.
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

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpGroupStateChangeResponse;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.comm.PdpMessageGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PAP component to change state of PDP group.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpGroupStateChangeProvider extends PdpMessageGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdpGroupStateChangeProvider.class);

    /**
     * Constructs the object.
     */
    public PdpGroupStateChangeProvider() {
        super(false);
    }

    /**
     * Changes state of a PDP group.
     *
     * @param groupName name of the PDP group
     * @param pdpGroupState state of the PDP group
     * @return a pair containing the status and the response
     * @throws PfModelException in case of errors
     */
    public Pair<Response.Status, PdpGroupStateChangeResponse> changeGroupState(final String groupName,
            final PdpState pdpGroupState) throws PfModelException {
        synchronized (updateLock) {
            switch (pdpGroupState) {
                case ACTIVE:
                    handleActiveState(groupName);
                    break;
                case PASSIVE:
                    handlePassiveState(groupName);
                    break;
                default:
                    throw new PfModelException(Response.Status.BAD_REQUEST,
                            "Only ACTIVE or PASSIVE state changes are allowed");
            }
            return Pair.of(Response.Status.OK, new PdpGroupStateChangeResponse());
        }
    }

    private void handleActiveState(final String groupName) throws PfModelException {
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            final List<PdpGroup> pdpGroups = databaseProvider.getPdpGroups(groupName);
            if (!pdpGroups.isEmpty() && !PdpState.ACTIVE.equals(pdpGroups.get(0).getPdpGroupState())) {
                updatePdpGroupAndPdp(databaseProvider, pdpGroups, PdpState.ACTIVE);
                sendPdpMessage(pdpGroups.get(0), PdpState.ACTIVE, databaseProvider);
            }
        }
    }

    private void handlePassiveState(final String groupName) throws PfModelException {
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            final List<PdpGroup> pdpGroups = databaseProvider.getPdpGroups(groupName);
            if (!pdpGroups.isEmpty() && !PdpState.PASSIVE.equals(pdpGroups.get(0).getPdpGroupState())) {
                updatePdpGroupAndPdp(databaseProvider, pdpGroups, PdpState.PASSIVE);
                sendPdpMessage(pdpGroups.get(0), PdpState.PASSIVE, databaseProvider);
            }
        }
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
}
