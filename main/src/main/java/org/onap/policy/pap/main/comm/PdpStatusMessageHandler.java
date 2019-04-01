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

package org.onap.policy.pap.main.comm;

import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for PDP Status messages which either represent registration or heart beat.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpStatusMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdpStatusMessageHandler.class);

    /**
     * Handles the PdpStatus message coming from various PDP's.
     *
     * @param message the PdpStatus message
     */
    public void handlePdpStatus(final PdpStatus message) {
        final PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            databaseProvider.init();
            if (message.getPdpGroup().isEmpty() && message.getPdpSubgroup().isEmpty()) {
                handlePdpRegistration(message, databaseProvider);
            } else {
                handlePdpHeartbeat(message, databaseProvider);
            }
        } catch (final PolicyPapException exp) {
            LOGGER.error("Failed registering PDP", exp);
        } catch (final Exception exp) {
            LOGGER.error("Failed connecting to database provider", exp);
        }
    }

    private void handlePdpRegistration(final PdpStatus message, final PolicyModelsProvider databaseProvider)
            throws PfModelException, PolicyPapException {
        if (!findAndUpdatePdpGroup(message, databaseProvider)) {
            final String errorMessage = "Failed to register PDP. No matching PdpGroup/SubGroup Found - ";
            LOGGER.debug("{} {}", errorMessage, message);
            throw new PolicyPapException(errorMessage + message);
        }
    }

    private boolean findAndUpdatePdpGroup(final PdpStatus message, final PolicyModelsProvider databaseProvider)
            throws PfModelException {
        boolean pdpGroupFound = false;

        final PdpGroups pdpGroups = databaseProvider.getPdpGroups("");
        for (final PdpGroup pdpGroup : pdpGroups.getGroups()) {
            for (final PdpSubGroup subGroup : pdpGroup.getPdpSubgroups()) {
                if (subGroup.getPdpType().equals(message.getPdpType())
                        && subGroup.getSupportedPolicyTypes().containsAll(message.getSupportedPolicyTypes())) {
                    LOGGER.debug("Found pdpGroup - {}, going for registration of PDP - {}", pdpGroup, message);
                    updatePdpSubGroup(subGroup, message);
                    sendPdpMessage(pdpGroup.getName(), subGroup, message.getInstance(), null);
                    pdpGroupFound = true;
                    break;
                }
            }
        }
        if (pdpGroupFound) {
            databaseProvider.updatePdpGroups(pdpGroups);
            LOGGER.debug("Updated PdpGroups in DB - {}", pdpGroups);
        }
        return pdpGroupFound;
    }

    private void updatePdpSubGroup(final PdpSubGroup pdpSubGroup, final PdpStatus message) {

        final Pdp pdpInstance = new Pdp();
        pdpInstance.setInstanceId(message.getInstance());
        pdpInstance.setPdpState(message.getState());
        pdpInstance.setHealthy(message.getHealthy());
        pdpInstance.setMessage(message.getDescription());
        pdpSubGroup.getPdpInstances().add(pdpInstance);

        pdpSubGroup.setCurrentInstanceCount(pdpSubGroup.getCurrentInstanceCount() + 1);
    }

    private void handlePdpHeartbeat(final PdpStatus message, final PolicyModelsProvider databaseProvider)
            throws PfModelException {
        final PdpGroups pdpGroups = databaseProvider.getPdpGroups("");
        for (final PdpGroup pdpGroup : pdpGroups.getGroups()) {
            if (message.getPdpGroup().equals(pdpGroup.getName())) {
                findPdpSubGroup(message, pdpGroup);
            }
        }
    }

    private void findPdpSubGroup(final PdpStatus message, final PdpGroup pdpGroup) {
        for (final PdpSubGroup subGroup : pdpGroup.getPdpSubgroups()) {
            if (message.getPdpSubgroup().equals(subGroup.getPdpType())) {
                findPdpInstance(message, pdpGroup, subGroup);
            }
        }
    }

    private void findPdpInstance(final PdpStatus message, final PdpGroup pdpGroup, final PdpSubGroup subGroup) {
        for (final Pdp pdpInstanceDetails : subGroup.getPdpInstances()) {
            if (message.getInstance().equals(pdpInstanceDetails.getInstanceId())) {
                if (validatePdpDetails(message, pdpGroup, subGroup, pdpInstanceDetails)) {
                    LOGGER.debug("PdpInstance details are correct. Saving current state in DB - {}",
                            pdpInstanceDetails);
                    // TODO: details are correct save health & statistics details in DB
                } else {
                    LOGGER.debug("PdpInstance details are not correct. Sending PdpUpdate message - {}",
                            pdpInstanceDetails);
                    sendPdpMessage(pdpGroup.getName(), subGroup, pdpInstanceDetails.getInstanceId(),
                            pdpInstanceDetails.getPdpState());
                }
            }
        }
    }

    private boolean validatePdpDetails(final PdpStatus message, final PdpGroup pdpGroup, final PdpSubGroup subGroup,
            final Pdp pdpInstanceDetails) {

        return message.getPdpGroup().equals(pdpGroup.getName())
                && message.getPdpSubgroup().equals(subGroup.getPdpType())
                && message.getState().equals(pdpInstanceDetails.getPdpState())
                && message.getSupportedPolicyTypes().containsAll(subGroup.getSupportedPolicyTypes())
                && message.getPdpType().equals(subGroup.getPdpType());
    }

    private void sendPdpMessage(final String pdpGroupName, final PdpSubGroup subGroup, final String pdpInstanceId,
            final PdpState pdpState) {
        final PdpUpdate pdpUpdatemessage = createPdpUpdateMessage(pdpGroupName, subGroup, pdpInstanceId);
        final PdpStateChange pdpStateChangeMessage =
                createPdpStateChangeMessage(pdpGroupName, subGroup, pdpInstanceId, pdpState);
        final PdpModifyRequestMap requestMap = Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class);
        requestMap.addRequest(pdpUpdatemessage, pdpStateChangeMessage);
        LOGGER.debug("Sent PdpUpdate message - {}", pdpUpdatemessage);
        LOGGER.debug("Sent PdpStateChange message - {}", pdpStateChangeMessage);
    }

    private PdpUpdate createPdpUpdateMessage(final String pdpGroupName, final PdpSubGroup subGroup,
            final String pdpInstanceId) {

        final PdpUpdate update = new PdpUpdate();
        update.setName(pdpInstanceId);
        update.setPdpType(subGroup.getPdpType());
        update.setPdpGroup(pdpGroupName);
        update.setPdpSubgroup(subGroup.getPdpType());
        update.setPolicies(subGroup.getPolicies());
        LOGGER.debug("Created PdpUpdate message - {}", update);
        return update;
    }

    private PdpStateChange createPdpStateChangeMessage(final String pdpGroupName, final PdpSubGroup subGroup,
            final String pdpInstanceId, final PdpState pdpState) {

        final PdpStateChange stateChange = new PdpStateChange();
        stateChange.setName(pdpInstanceId);
        stateChange.setPdpGroup(pdpGroupName);
        stateChange.setPdpSubgroup(subGroup.getPdpType());
        stateChange.setState(pdpState == null ? PdpState.ACTIVE : pdpState);
        LOGGER.debug("Created PdpStateChange message - {}", stateChange);
        return stateChange;
    }
}
