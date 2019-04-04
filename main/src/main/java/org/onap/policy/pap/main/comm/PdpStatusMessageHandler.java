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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.pdp.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
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
            if (message.getPdpGroup().isEmpty() && message.getPdpSubgroup().isEmpty()) {
                handlePdpRegistration(message, databaseProvider);
            } else {
                handlePdpHeartbeat(message, databaseProvider);
            }
        } catch (final PolicyPapException exp) {
            LOGGER.error("Operation Failed", exp);
        } catch (final Exception exp) {
            LOGGER.error("Failed connecting to database provider", exp);
        }
    }

    private void handlePdpRegistration(final PdpStatus message, final PolicyModelsProvider databaseProvider)
            throws PfModelException, PolicyPapException {
        if (!findAndUpdatePdpGroup(message, databaseProvider)) {
            final String errorMessage = "Failed to register PDP. No matching PdpGroup/SubGroup Found - ";
            LOGGER.debug("{}{}", errorMessage, message);
            throw new PolicyPapException(errorMessage + message);
        }
    }

    private boolean findAndUpdatePdpGroup(final PdpStatus message, final PolicyModelsProvider databaseProvider)
            throws PfModelException {
        boolean pdpGroupFound = false;
        Optional<PdpSubGroup> subGroup = null;
        final List<Pair<String, String>> supportedPolicyTypesPair = createSupportedPolictTypesPair(message);
        final List<PdpGroup> pdpGroups =
                databaseProvider.getFilteredPdpGroups(message.getPdpType(), supportedPolicyTypesPair);
        for (final PdpGroup pdpGroup : pdpGroups) {
            subGroup = findPdpSubGroup(message, pdpGroup);
            if (subGroup.isPresent()) {
                LOGGER.debug("Found pdpGroup - {}, going for registration of PDP - {}", pdpGroup, message);
                if (!findPdpInstance(message, subGroup.get()).isPresent()) {
                    updatePdpSubGroup(pdpGroup, subGroup.get(), message, databaseProvider);
                }
                sendPdpMessage(pdpGroup.getName(), subGroup.get(), message.getInstance(), null, databaseProvider);
                pdpGroupFound = true;
                break;
            }
        }
        return pdpGroupFound;
    }

    private List<Pair<String, String>> createSupportedPolictTypesPair(final PdpStatus message) {
        final List<Pair<String, String>> supportedPolicyTypesPair = new ArrayList<>();
        for (final ToscaPolicyTypeIdentifier policyTypeIdent : message.getSupportedPolicyTypes()) {
            supportedPolicyTypesPair.add(Pair.of(policyTypeIdent.getName(), policyTypeIdent.getVersion()));
        }
        return supportedPolicyTypesPair;
    }

    private void updatePdpSubGroup(final PdpGroup pdpGroup, final PdpSubGroup pdpSubGroup, final PdpStatus message,
            final PolicyModelsProvider databaseProvider) throws PfModelException {

        final Pdp pdpInstance = new Pdp();
        pdpInstance.setInstanceId(message.getInstance());
        pdpInstance.setPdpState(message.getState());
        pdpInstance.setHealthy(message.getHealthy());
        pdpInstance.setMessage(message.getDescription());
        pdpSubGroup.getPdpInstances().add(pdpInstance);

        pdpSubGroup.setCurrentInstanceCount(pdpSubGroup.getCurrentInstanceCount() + 1);

        databaseProvider.updatePdpSubGroup(pdpGroup.getName(), pdpGroup.getVersion(), pdpSubGroup);

        LOGGER.debug("Updated PdpSubGroup in DB - {} belonging to PdpGroup - {}", pdpSubGroup, pdpGroup);
    }

    private void handlePdpHeartbeat(final PdpStatus message, final PolicyModelsProvider databaseProvider)
            throws PfModelException, PolicyPapException {
        boolean pdpInstanceFound = false;
        Optional<PdpSubGroup> pdpSubgroup = null;
        Optional<Pdp> pdpInstance = null;

        final List<PdpGroup> pdpGroups = databaseProvider.getLatestPdpGroups(message.getPdpGroup());
        if (!pdpGroups.isEmpty()) {
            final PdpGroup pdpGroup = pdpGroups.get(0);
            pdpSubgroup = findPdpSubGroup(message, pdpGroup);
            if (pdpSubgroup.isPresent()) {
                pdpInstance = findPdpInstance(message, pdpSubgroup.get());
                if (pdpInstance.isPresent()) {
                    processPdpDetails(message, pdpSubgroup.get(), pdpInstance.get(), pdpGroup, databaseProvider);
                    pdpInstanceFound = true;
                }
            }
        }
        if (!pdpInstanceFound) {
            final String errorMessage = "Failed to process heartbeat. No matching PdpGroup/SubGroup Found - ";
            LOGGER.debug("{}{}", errorMessage, message);
            throw new PolicyPapException(errorMessage + message);
        }
    }

    private Optional<PdpSubGroup> findPdpSubGroup(final PdpStatus message, final PdpGroup pdpGroup) {
        PdpSubGroup pdpSubgroup = null;
        for (final PdpSubGroup subGroup : pdpGroup.getPdpSubgroups()) {
            if (message.getPdpSubgroup().equals(subGroup.getPdpType())) {
                pdpSubgroup = subGroup;
                break;
            }
        }
        return Optional.ofNullable(pdpSubgroup);
    }

    private Optional<Pdp> findPdpInstance(final PdpStatus message, final PdpSubGroup subGroup) {
        Pdp pdpInstance = null;
        for (final Pdp pdpInstanceDetails : subGroup.getPdpInstances()) {
            if (message.getInstance().equals(pdpInstanceDetails.getInstanceId())) {
                pdpInstance = pdpInstanceDetails;
                break;
            }
        }
        return Optional.ofNullable(pdpInstance);
    }

    private void processPdpDetails(final PdpStatus message, final PdpSubGroup pdpSubgroup, final Pdp pdpInstance,
            final PdpGroup pdpGroup, final PolicyModelsProvider databaseProvider) throws PfModelException {
        if (validatePdpDetails(message, pdpGroup, pdpSubgroup, pdpInstance)) {
            LOGGER.debug("PdpInstance details are correct. Saving current state in DB - {}", pdpInstance);
            updatePdpHealthStatus(message, pdpSubgroup, pdpInstance, pdpGroup, databaseProvider);
            updatePdpStatistics(message, pdpSubgroup, pdpInstance, pdpGroup, databaseProvider);
        } else {
            LOGGER.debug("PdpInstance details are not correct. Sending PdpUpdate message - {}", pdpInstance);
            sendPdpMessage(pdpGroup.getName(), pdpSubgroup, pdpInstance.getInstanceId(), pdpInstance.getPdpState(),
                    databaseProvider);
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

    private void updatePdpHealthStatus(final PdpStatus message, final PdpSubGroup pdpSubgroup, final Pdp pdpInstance,
            final PdpGroup pdpGroup, final PolicyModelsProvider databaseProvider) throws PfModelException {
        pdpInstance.setHealthy(message.getHealthy());
        databaseProvider.updatePdp(pdpGroup.getName(), pdpGroup.getVersion(), pdpSubgroup.getPdpType(), pdpInstance);

        LOGGER.debug("Updated Pdp in DB - {}", pdpInstance);
    }

    private void updatePdpStatistics(final PdpStatus message, final PdpSubGroup pdpSubgroup, final Pdp pdpInstance,
            final PdpGroup pdpGroup, final PolicyModelsProvider databaseProvider) throws PfModelException {
        final PdpStatistics pdpStatistics = new PdpStatistics();
        pdpStatistics.setPdpInstanceId(message.getInstance());
        pdpStatistics.setPolicyDeployCount(message.getStatistics().getPolicyDeployCount());
        pdpStatistics.setPolicyDeploySuccessCount(message.getStatistics().getPolicyDeploySuccessCount());
        pdpStatistics.setPolicyDeployFailCount(message.getStatistics().getPolicyDeployFailCount());
        pdpStatistics.setPolicyExecutedCount(message.getStatistics().getPolicyExecutedCount());
        pdpStatistics.setPolicyExecutedSuccessCount(message.getStatistics().getPolicyExecutedSuccessCount());
        pdpStatistics.setPolicyExecutedFailCount(message.getStatistics().getPolicyExecutedFailCount());

        databaseProvider.updatePdpStatistics(pdpGroup.getName(), pdpGroup.getVersion(), pdpSubgroup.getPdpType(),
                pdpInstance.getInstanceId(), pdpStatistics);

        LOGGER.debug("Updated PdpStatistics in DB - {}", pdpStatistics);
    }

    private void sendPdpMessage(final String pdpGroupName, final PdpSubGroup subGroup, final String pdpInstanceId,
            final PdpState pdpState, final PolicyModelsProvider databaseProvider) throws PfModelException {
        final PdpUpdate pdpUpdatemessage =
                createPdpUpdateMessage(pdpGroupName, subGroup, pdpInstanceId, databaseProvider);
        final PdpStateChange pdpStateChangeMessage =
                createPdpStateChangeMessage(pdpGroupName, subGroup, pdpInstanceId, pdpState);
        final PdpModifyRequestMap requestMap = Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class);
        requestMap.addRequest(pdpUpdatemessage, pdpStateChangeMessage);
        LOGGER.debug("Sent PdpUpdate message - {}", pdpUpdatemessage);
        LOGGER.debug("Sent PdpStateChange message - {}", pdpStateChangeMessage);
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
        stateChange.setState(pdpState == null ? PdpState.ACTIVE : pdpState);
        LOGGER.debug("Created PdpStateChange message - {}", stateChange);
        return stateChange;
    }
}
