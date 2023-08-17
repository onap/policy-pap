/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021, 2023 Nordix Foundation.
 *  Modifications Copyright (C) 2019-2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

import jakarta.ws.rs.core.Response;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpGroupStateChangeResponse;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.comm.PdpMessageGenerator;
import org.onap.policy.pap.main.service.PdpGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Provider for PAP component to change state of PDP group.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@Service
public class PdpGroupStateChangeProvider extends PdpMessageGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdpGroupStateChangeProvider.class);

    @Autowired
    private PdpGroupService pdpGroupService;

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
    public Pair<HttpStatus, PdpGroupStateChangeResponse> changeGroupState(final String groupName,
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
            return Pair.of(HttpStatus.OK, new PdpGroupStateChangeResponse());
        }
    }

    private void handleActiveState(final String groupName) throws PfModelException {
        final List<PdpGroup> pdpGroups = pdpGroupService.getPdpGroups(groupName);
        if (!pdpGroups.isEmpty() && !PdpState.ACTIVE.equals(pdpGroups.get(0).getPdpGroupState())) {
            updatePdpGroupAndPdp(pdpGroups, PdpState.ACTIVE);
            sendPdpMessage(pdpGroups.get(0), PdpState.ACTIVE);
        }
    }

    private void handlePassiveState(final String groupName) throws PfModelException {
        final List<PdpGroup> pdpGroups = pdpGroupService.getPdpGroups(groupName);
        if (!pdpGroups.isEmpty() && !PdpState.PASSIVE.equals(pdpGroups.get(0).getPdpGroupState())) {
            updatePdpGroupAndPdp(pdpGroups, PdpState.PASSIVE);
            sendPdpMessage(pdpGroups.get(0), PdpState.PASSIVE);
        }
    }

    private void updatePdpGroupAndPdp(final List<PdpGroup> pdpGroups, final PdpState pdpState) {
        pdpGroups.get(0).setPdpGroupState(pdpState);
        for (final PdpSubGroup subGroup : pdpGroups.get(0).getPdpSubgroups()) {
            for (final Pdp pdp : subGroup.getPdpInstances()) {
                pdp.setPdpState(pdpState);
            }
        }
        pdpGroupService.updatePdpGroups(pdpGroups);

        LOGGER.debug("Updated PdpGroup and Pdp in DB - {} ", pdpGroups);
    }

    private void sendPdpMessage(final PdpGroup pdpGroup, final PdpState pdpState) throws PfModelException {
        String pdpGroupName = pdpGroup.getName();
        for (final PdpSubGroup subGroup : pdpGroup.getPdpSubgroups()) {
            List<ToscaPolicy> policies = getToscaPolicies(subGroup);
            for (final Pdp pdp : subGroup.getPdpInstances()) {
                String pdpInstanceId = pdp.getInstanceId();
                final var pdpUpdatemessage =
                    createPdpUpdateMessage(pdpGroup.getName(), subGroup, pdp.getInstanceId(),
                                policies, null);
                final var pdpStateChangeMessage =
                    createPdpStateChangeMessage(pdpGroupName, subGroup, pdpInstanceId, pdpState);
                updateDeploymentStatus(pdpGroupName, subGroup.getPdpType(), pdpInstanceId,
                    pdpStateChangeMessage.getState(), pdpUpdatemessage.getPoliciesToBeDeployed());
                requestMap.addRequest(pdpUpdatemessage, pdpStateChangeMessage);
                LOGGER.debug("Sent PdpUpdate message - {}", pdpUpdatemessage);
                LOGGER.debug("Sent PdpStateChange message - {}", pdpStateChangeMessage);
            }
        }
    }
}
