/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021 Nordix Foundation.
 *  Modifications Copyright (C) 2019-2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handler for PDP Status messages which either represent registration or heart beat.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpStatusMessageHandler extends PdpMessageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdpStatusMessageHandler.class);

    private final PdpParameters params;

    /**
     * List to store policies present in db.
     */
    List<ToscaPolicy> policies = new LinkedList<>();

    /**
     * List to store policies to be deployed (heartbeat).
     */
    Map<ToscaConceptIdentifier, ToscaPolicy> policiesToBeDeployed = new HashMap<>();

    /**
     * List to store policies to be undeployed (heartbeat).
     */
    List<ToscaConceptIdentifier> policiesToBeUndeployed = new LinkedList<>();

    /**
     * Constructs the object.
     *
     * @param params PDP parameters
     */
    public PdpStatusMessageHandler(PdpParameters params) {
        super(true);
        this.params = params;
    }

    /**
     * Handles the PdpStatus message coming from various PDP's.
     *
     * @param message the PdpStatus message
     */
    public void handlePdpStatus(final PdpStatus message) {
        long diffms = System.currentTimeMillis() - message.getTimestampMs();
        if (diffms > params.getMaxMessageAgeMs()) {
            long diffsec = TimeUnit.SECONDS.convert(diffms, TimeUnit.MILLISECONDS);
            LOGGER.info("discarding status message from {} age {}s", message.getName(), diffsec);
            return;
        }

        synchronized (updateLock) {
            try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
                if (message.getPdpSubgroup() == null) {
                    handlePdpRegistration(message, databaseProvider);
                } else {
                    handlePdpHeartbeat(message, databaseProvider);
                }
                /*
                 * Indicate that a heart beat was received from the PDP. This is invoked only if handleXxx() does not
                 * throw an exception.
                 */
                if (message.getName() != null) {
                    final var pdpTracker = (PdpTracker) Registry.get(PapConstants.REG_PDP_TRACKER);
                    pdpTracker.add(message.getName());
                }
            } catch (final PolicyPapException exp) {
                LOGGER.error("Operation Failed", exp);
            } catch (final Exception exp) {
                LOGGER.error("Failed connecting to database provider", exp);
            }
        }
    }

    private void handlePdpRegistration(final PdpStatus message, final PolicyModelsProvider databaseProvider)
            throws PfModelException, PolicyPapException {
        if (!findAndUpdatePdpGroup(message, databaseProvider)) {
            final var errorMessage = "Failed to register PDP. No matching PdpGroup/SubGroup Found - ";
            LOGGER.debug("{}{}", errorMessage, message);
            throw new PolicyPapException(errorMessage + message);
        }
    }

    private boolean findAndUpdatePdpGroup(final PdpStatus message, final PolicyModelsProvider databaseProvider)
            throws PfModelException {
        var pdpGroupFound = false;
        final PdpGroupFilter filter =
                PdpGroupFilter.builder().name(message.getPdpGroup()).groupState(PdpState.ACTIVE).build();

        final List<PdpGroup> pdpGroups = databaseProvider.getFilteredPdpGroups(filter);
        if (!pdpGroups.isEmpty()) {
            pdpGroupFound = registerPdp(message, databaseProvider, pdpGroups.get(0));
        }
        return pdpGroupFound;
    }

    private boolean registerPdp(final PdpStatus message, final PolicyModelsProvider databaseProvider,
            final PdpGroup finalizedPdpGroup) throws PfModelException {
        Optional<PdpSubGroup> subGroup;
        var pdpGroupFound = false;
        subGroup = findPdpSubGroup(message, finalizedPdpGroup);

        if (subGroup.isPresent()) {
            policies = getToscaPolicies(subGroup.get(), databaseProvider);
            policiesToBeDeployed = policies.stream().collect(Collectors
                    .toMap(ToscaPolicy::getIdentifier, policy -> policy));
            policiesToBeUndeployed = null;

            LOGGER.debug("Found pdpGroup - {}, going for registration of PDP - {}", finalizedPdpGroup, message);
            Optional<Pdp> pdp = findPdpInstance(message, subGroup.get());
            if (pdp.isPresent()) {
                updatePdp(finalizedPdpGroup, subGroup.get(), pdp.get(), message, databaseProvider);
            } else {
                addToPdpSubGroup(finalizedPdpGroup, subGroup.get(), message, databaseProvider);
            }
            sendPdpMessage(finalizedPdpGroup.getName(), subGroup.get(), message.getName(), null, databaseProvider);
            pdpGroupFound = true;
        }
        return pdpGroupFound;
    }

    private void updatePdp(PdpGroup pdpGroup, PdpSubGroup pdpSubGroup, Pdp pdpInstance, PdpStatus message,
                    PolicyModelsProvider databaseProvider) throws PfModelException {

        pdpInstance.setHealthy(message.getHealthy());
        pdpInstance.setMessage(message.getDescription());
        pdpInstance.setLastUpdate(Instant.now());

        databaseProvider.updatePdp(pdpGroup.getName(), pdpSubGroup.getPdpType(), pdpInstance);

        LOGGER.debug("Updated Pdp in DB - {} belonging to PdpGroup - {}, PdpSubGroup - {}", pdpInstance,
                        pdpGroup.getName(), pdpSubGroup.getPdpType());
    }

    private void addToPdpSubGroup(final PdpGroup pdpGroup, final PdpSubGroup pdpSubGroup, final PdpStatus message,
            final PolicyModelsProvider databaseProvider) throws PfModelException {

        final var pdpInstance = new Pdp();
        pdpInstance.setInstanceId(message.getName());
        pdpInstance.setPdpState(PdpState.ACTIVE);
        pdpInstance.setHealthy(message.getHealthy());
        pdpInstance.setMessage(message.getDescription());
        pdpInstance.setLastUpdate(Instant.now());
        pdpSubGroup.getPdpInstances().add(pdpInstance);

        pdpSubGroup.setCurrentInstanceCount(pdpSubGroup.getCurrentInstanceCount() + 1);

        databaseProvider.updatePdpSubGroup(pdpGroup.getName(), pdpSubGroup);

        LOGGER.debug("Updated PdpSubGroup in DB - {} belonging to PdpGroup - {}", pdpSubGroup, pdpGroup.getName());
    }

    private void handlePdpHeartbeat(final PdpStatus message, final PolicyModelsProvider databaseProvider)
            throws PfModelException {

        final PdpGroupFilter filter =
                PdpGroupFilter.builder().name(message.getPdpGroup()).groupState(PdpState.ACTIVE).build();
        final List<PdpGroup> pdpGroups = databaseProvider.getFilteredPdpGroups(filter);
        if (!pdpGroups.isEmpty()) {
            var pdpGroup = pdpGroups.get(0);
            Optional<PdpSubGroup> pdpSubgroup = findPdpSubGroup(message, pdpGroup);
            if (pdpSubgroup.isPresent()) {
                Optional<Pdp> pdpInstance = findPdpInstance(message, pdpSubgroup.get());
                if (pdpInstance.isPresent()) {
                    processPdpDetails(message, pdpSubgroup.get(), pdpInstance.get(), pdpGroup, databaseProvider);
                } else {
                    LOGGER.debug("PdpInstance not Found in DB. Sending Pdp for registration - {}", message);
                    registerPdp(message, databaseProvider, pdpGroup);
                }
            }
        }
    }

    private Optional<PdpSubGroup> findPdpSubGroup(final PdpStatus message, final PdpGroup pdpGroup) {
        PdpSubGroup pdpSubgroup = null;
        for (final PdpSubGroup subGroup : pdpGroup.getPdpSubgroups()) {
            if (message.getPdpType().equals(subGroup.getPdpType())) {
                pdpSubgroup = subGroup;
                break;
            }
        }
        return Optional.ofNullable(pdpSubgroup);
    }

    private Optional<Pdp> findPdpInstance(final PdpStatus message, final PdpSubGroup subGroup) {
        Pdp pdpInstance = null;
        for (final Pdp pdpInstanceDetails : subGroup.getPdpInstances()) {
            if (pdpInstanceDetails.getInstanceId().equals(message.getName())) {
                pdpInstance = pdpInstanceDetails;
                break;
            }
        }
        return Optional.ofNullable(pdpInstance);
    }

    private void processPdpDetails(final PdpStatus message, final PdpSubGroup pdpSubGroup, final Pdp pdpInstance,
            final PdpGroup pdpGroup, final PolicyModelsProvider databaseProvider)
                    throws PfModelException {
        // all policies
        policies = getToscaPolicies(pdpSubGroup, databaseProvider);

        Map<ToscaConceptIdentifier, ToscaPolicy> policyMap =
                        policies.stream().collect(Collectors.toMap(ToscaPolicy::getIdentifier, policy -> policy));

        // policies that the PDP already has (-) all
        policiesToBeUndeployed = message.getPolicies().stream().filter(policyId -> !policyMap.containsKey(policyId))
                        .collect(Collectors.toList());

        // all (-) policies that the PDP already has
        policiesToBeDeployed = policyMap;
        policiesToBeDeployed.keySet().removeAll(message.getPolicies());

        if (PdpState.TERMINATED.equals(message.getState())) {
            processPdpTermination(pdpSubGroup, pdpInstance, pdpGroup, databaseProvider);
        } else if (validatePdpDetails(message, pdpGroup, pdpSubGroup, pdpInstance)) {
            LOGGER.debug("PdpInstance details are correct. Saving current state in DB - {}", pdpInstance);
            updatePdpHealthStatus(message, pdpSubGroup, pdpInstance, pdpGroup, databaseProvider);

            if (validatePdpStatisticsDetails(message, pdpInstance, pdpGroup, pdpSubGroup)) {
                LOGGER.debug("PdpStatistics details are correct. Saving current statistics in DB - {}",
                        message.getStatistics());
                createPdpStatistics(message.getStatistics(), databaseProvider);
            } else {
                LOGGER.debug("PdpStatistics details are not correct - {}", message.getStatistics());
            }
        } else {
            LOGGER.debug("PdpInstance details are not correct. Sending PdpUpdate message - {}", pdpInstance);
            LOGGER.debug("Policy list in DB - {}. Policy list in heartbeat - {}", pdpSubGroup.getPolicies(),
                message.getPolicies());
            updatePdpHealthStatus(message, pdpSubGroup, pdpInstance, pdpGroup, databaseProvider);
            sendPdpMessage(pdpGroup.getName(), pdpSubGroup, pdpInstance.getInstanceId(), pdpInstance.getPdpState(),
                databaseProvider);
        }
    }

    private void processPdpTermination(final PdpSubGroup pdpSubGroup, final Pdp pdpInstance, final PdpGroup pdpGroup,
            final PolicyModelsProvider databaseProvider) throws PfModelException {
        pdpSubGroup.getPdpInstances().remove(pdpInstance);
        pdpSubGroup.setCurrentInstanceCount(pdpSubGroup.getCurrentInstanceCount() - 1);
        databaseProvider.updatePdpSubGroup(pdpGroup.getName(), pdpSubGroup);

        LOGGER.debug("Deleted PdpInstance - {} belonging to PdpSubGroup - {} and PdpGroup - {}", pdpInstance,
                pdpSubGroup, pdpGroup);
    }

    private boolean validatePdpDetails(final PdpStatus message, final PdpGroup pdpGroup, final PdpSubGroup subGroup,
            final Pdp pdpInstanceDetails) {
        /*
         * "EqualsBuilder" is a bit of a misnomer, as it uses containsAll() to check policies. Nevertheless, it does the
         * job and provides a convenient way to build a bunch of comparisons.
         */
        return new EqualsBuilder().append(message.getPdpGroup(), pdpGroup.getName())
                .append(message.getPdpSubgroup(), subGroup.getPdpType())
                .append(message.getPdpType(), subGroup.getPdpType())
                .append(message.getState(), pdpInstanceDetails.getPdpState())
                .append(message.getPolicies().containsAll(subGroup.getPolicies()), true)
                .append(subGroup.getPolicies().containsAll(message.getPolicies()), true).build();
    }

    private boolean validatePdpStatisticsDetails(final PdpStatus message, final Pdp pdpInstanceDetails,
            final PdpGroup pdpGroup, final PdpSubGroup pdpSubGroup) {
        if (message.getStatistics() != null) {
            return new EqualsBuilder()
                    .append(message.getStatistics().getPdpInstanceId(), pdpInstanceDetails.getInstanceId())
                    .append(message.getStatistics().getPdpGroupName(), pdpGroup.getName())
                    .append(message.getStatistics().getPdpSubGroupName(), pdpSubGroup.getPdpType())
                    .append(message.getStatistics().getPolicyDeployCount() < 0, false)
                    .append(message.getStatistics().getPolicyDeployFailCount() < 0, false)
                    .append(message.getStatistics().getPolicyDeploySuccessCount() < 0, false)
                    .append(message.getStatistics().getPolicyExecutedCount() < 0, false)
                    .append(message.getStatistics().getPolicyExecutedFailCount() < 0, false)
                    .append(message.getStatistics().getPolicyExecutedSuccessCount() < 0, false).build();
        } else {
            LOGGER.debug("PdpStatistics is null");
            return false;
        }
    }

    private void updatePdpHealthStatus(final PdpStatus message, final PdpSubGroup pdpSubgroup, final Pdp pdpInstance,
            final PdpGroup pdpGroup, final PolicyModelsProvider databaseProvider) throws PfModelException {
        pdpInstance.setHealthy(message.getHealthy());
        pdpInstance.setLastUpdate(Instant.now());
        databaseProvider.updatePdp(pdpGroup.getName(), pdpSubgroup.getPdpType(), pdpInstance);

        LOGGER.debug("Updated Pdp in DB - {}", pdpInstance);
    }

    private void createPdpStatistics(final PdpStatistics pdpStatistics, final PolicyModelsProvider databaseProvider)
            throws PfModelException {
        databaseProvider.createPdpStatistics(Arrays.asList(pdpStatistics));
        LOGGER.debug("Created PdpStatistics in DB - {}", pdpStatistics);
    }

    private void sendPdpMessage(final String pdpGroupName, final PdpSubGroup subGroup, final String pdpInstanceId,
            final PdpState pdpState, final PolicyModelsProvider databaseProvider)
                    throws PfModelException {
        final List<ToscaPolicy> polsToBeDeployed = new LinkedList<>(policiesToBeDeployed.values());
        final var pdpUpdatemessage =
            createPdpUpdateMessage(pdpGroupName, subGroup, pdpInstanceId,
                        polsToBeDeployed, policiesToBeUndeployed);
        final var pdpStateChangeMessage =
            createPdpStateChangeMessage(pdpGroupName, subGroup, pdpInstanceId, pdpState);
        updateDeploymentStatus(pdpGroupName, subGroup.getPdpType(), pdpInstanceId, pdpStateChangeMessage.getState(),
            databaseProvider, pdpUpdatemessage.getPoliciesToBeDeployed());

        requestMap.addRequest(pdpUpdatemessage, pdpStateChangeMessage);
        LOGGER.debug("Sent PdpUpdate message - {}", pdpUpdatemessage);
        LOGGER.debug("Sent PdpStateChange message - {}", pdpStateChangeMessage);
    }
}
