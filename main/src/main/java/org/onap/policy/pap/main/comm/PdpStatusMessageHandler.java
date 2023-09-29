/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021,2023 Nordix Foundation.
 *  Modifications Copyright (C) 2019-2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2023 Bell Canada. All rights reserved.
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

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.onap.policy.pap.main.service.PdpGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Handler for PDP Status messages which either represent registration or heart beat.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */

@Component
public class PdpStatusMessageHandler extends PdpMessageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdpStatusMessageHandler.class);

    private final PdpParameters params;

    private final PdpGroupService pdpGroupService;

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
     * @param parameterGroup the parameterGroup
     * @param pdpGroupService the pdpGroupService
     */
    public PdpStatusMessageHandler(PapParameterGroup parameterGroup, PdpGroupService pdpGroupService) {
        super(true);
        this.params = parameterGroup.getPdpParameters();
        this.pdpGroupService = pdpGroupService;
    }

    /**
     * Handles the PdpStatus message coming from various PDP's.
     *
     * @param message the PdpStatus message
     */
    public void handlePdpStatus(final PdpStatus message) {
        if (message.getPolicies() == null) {
            message.setPolicies(Collections.emptyList());
        }

        long diffms = System.currentTimeMillis() - message.getTimestampMs();
        if (diffms > params.getMaxMessageAgeMs()) {
            long diffsec = TimeUnit.SECONDS.convert(diffms, TimeUnit.MILLISECONDS);
            LOGGER.info("discarding status message from {} age {}s", message.getName(), diffsec);
            return;
        }

        synchronized (updateLock) {
            try {
                if (message.getPdpSubgroup() == null) {
                    handlePdpRegistration(message);
                } else {
                    handlePdpHeartbeat(message);
                }
            } catch (final PolicyPapException exp) {
                LOGGER.error("Operation Failed", exp);
            } catch (final Exception exp) {
                if (isDuplicateKeyException(exp, Exception.class)) {
                    /*
                     * this is to be expected, if multiple PAPs are processing the same
                     * heartbeat at a time, thus we log the exception at a trace level
                     * instead of an error level.
                     */
                    LOGGER.info("Failed updating PDP information for {} - may have been added by another PAP",
                                    message.getName());
                    LOGGER.trace("Failed updating PDP information for {}", message.getName(), exp);
                } else {
                    LOGGER.error("Failed connecting to database provider", exp);
                }
            }
        }
    }

    /**
     * Determines if the exception indicates a duplicate key.
     *
     * @param thrown exception to check
     * @param exceptionClazz the class to check against
     * @return {@code true} if the exception occurred due to a duplicate key
     */
    protected static boolean isDuplicateKeyException(Throwable thrown, Class<? extends Throwable> exceptionClazz) {
        while (thrown != null) {
            if (thrown instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }

            if (exceptionClazz.isInstance(thrown) && isDuplicateKeyException(thrown.getCause(), exceptionClazz)) {
                return true;
            }

            thrown = thrown.getCause();
        }

        return false;
    }

    private void handlePdpRegistration(final PdpStatus message) throws PfModelException, PolicyPapException {
        if (!findAndUpdatePdpGroup(message)) {
            final var errorMessage = "Failed to register PDP. No matching PdpGroup/SubGroup Found - ";
            LOGGER.debug("{}{}", errorMessage, message);
            throw new PolicyPapException(errorMessage + message);
        }
    }

    private boolean findAndUpdatePdpGroup(final PdpStatus message)
            throws PfModelException {
        var pdpGroupFound = false;
        final PdpGroupFilter filter =
                PdpGroupFilter.builder().name(message.getPdpGroup()).groupState(PdpState.ACTIVE).build();

        final List<PdpGroup> pdpGroups = pdpGroupService.getFilteredPdpGroups(filter);
        if (!pdpGroups.isEmpty()) {
            pdpGroupFound = registerPdp(message, pdpGroups.get(0));
        }
        return pdpGroupFound;
    }

    private boolean registerPdp(final PdpStatus message, final PdpGroup finalizedPdpGroup) throws PfModelException {
        Optional<PdpSubGroup> subGroup;
        var pdpGroupFound = false;
        subGroup = findPdpSubGroup(message, finalizedPdpGroup);

        if (subGroup.isPresent()) {
            policies = getToscaPolicies(subGroup.get());
            policiesToBeDeployed = policies.stream().collect(Collectors
                    .toMap(ToscaPolicy::getIdentifier, policy -> policy));
            policiesToBeUndeployed = null;

            LOGGER.debug("Found pdpGroup - {}, going for registration of PDP - {}", finalizedPdpGroup, message);
            Optional<Pdp> pdp = findPdpInstance(message, subGroup.get());
            if (pdp.isPresent()) {
                updatePdpHealthStatus(message, subGroup.get(), pdp.get(), finalizedPdpGroup);
            } else {
                updatePdpSubGroup(finalizedPdpGroup, subGroup.get(), message);
            }
            sendPdpMessage(finalizedPdpGroup.getName(), subGroup.get(), message.getName(), null);
            pdpGroupFound = true;
        }
        return pdpGroupFound;
    }

    private void updatePdpSubGroup(final PdpGroup pdpGroup, final PdpSubGroup pdpSubGroup, final PdpStatus message) {

        final var pdpInstance = new Pdp();
        pdpInstance.setInstanceId(message.getName());
        pdpInstance.setPdpState(PdpState.ACTIVE);
        pdpInstance.setHealthy(message.getHealthy());
        pdpInstance.setMessage(message.getDescription());
        pdpInstance.setLastUpdate(Instant.now());
        pdpSubGroup.getPdpInstances().add(pdpInstance);

        pdpSubGroup.setCurrentInstanceCount(pdpSubGroup.getCurrentInstanceCount() + 1);

        pdpGroupService.updatePdpSubGroup(pdpGroup.getName(), pdpSubGroup);

        LOGGER.debug("Updated PdpSubGroup in DB - {} belonging to PdpGroup - {}", pdpSubGroup, pdpGroup.getName());
    }

    private void handlePdpHeartbeat(final PdpStatus message) throws PfModelException {

        final PdpGroupFilter filter =
                PdpGroupFilter.builder().name(message.getPdpGroup()).groupState(PdpState.ACTIVE).build();
        final List<PdpGroup> pdpGroups = pdpGroupService.getFilteredPdpGroups(filter);
        if (!pdpGroups.isEmpty()) {
            var pdpGroup = pdpGroups.get(0);
            Optional<PdpSubGroup> pdpSubgroup = findPdpSubGroup(message, pdpGroup);
            if (pdpSubgroup.isPresent()) {
                Optional<Pdp> pdpInstance = findPdpInstance(message, pdpSubgroup.get());
                if (pdpInstance.isPresent()) {
                    processPdpDetails(message, pdpSubgroup.get(), pdpInstance.get(), pdpGroup);
                } else {
                    LOGGER.debug("PdpInstance not Found in DB. Sending Pdp for registration - {}", message);
                    registerPdp(message, pdpGroup);
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
            final PdpGroup pdpGroup) throws PfModelException {
        // all policies
        policies = getToscaPolicies(pdpSubGroup);

        Map<ToscaConceptIdentifier, ToscaPolicy> policyMap =
                        policies.stream().collect(Collectors.toMap(ToscaPolicy::getIdentifier, policy -> policy));

        // policies that the PDP already has (-) all
        policiesToBeUndeployed = message.getPolicies().stream().filter(policyId -> !policyMap.containsKey(policyId))
                        .collect(Collectors.toList());

        // all (-) policies that the PDP already has
        policiesToBeDeployed = policyMap;
        policiesToBeDeployed.keySet().removeAll(message.getPolicies());

        if (PdpState.TERMINATED.equals(message.getState())) {
            processPdpTermination(pdpSubGroup, pdpInstance, pdpGroup);
        } else if (validatePdpDetails(message, pdpGroup, pdpSubGroup, pdpInstance)) {
            LOGGER.debug("PdpInstance details are correct. Saving current state in DB - {}", pdpInstance);
            updatePdpHealthStatus(message, pdpSubGroup, pdpInstance, pdpGroup);
        } else {
            LOGGER.debug("PdpInstance details are not correct. Sending PdpUpdate message - {}", pdpInstance);
            LOGGER.debug("Policy list in DB - {}. Policy list in heartbeat - {}", pdpSubGroup.getPolicies(),
                message.getPolicies());
            updatePdpHealthStatus(message, pdpSubGroup, pdpInstance, pdpGroup);
            sendPdpMessage(pdpGroup.getName(), pdpSubGroup, pdpInstance.getInstanceId(), pdpInstance.getPdpState());
        }
    }

    private void processPdpTermination(final PdpSubGroup pdpSubGroup, final Pdp pdpInstance, final PdpGroup pdpGroup) {
        pdpSubGroup.getPdpInstances().remove(pdpInstance);
        pdpSubGroup.setCurrentInstanceCount(pdpSubGroup.getCurrentInstanceCount() - 1);
        pdpGroupService.updatePdpSubGroup(pdpGroup.getName(), pdpSubGroup);

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

    private void updatePdpHealthStatus(final PdpStatus message, final PdpSubGroup pdpSubgroup, final Pdp pdpInstance,
            final PdpGroup pdpGroup) {
        pdpInstance.setHealthy(message.getHealthy());
        pdpInstance.setMessage(message.getDescription());
        pdpInstance.setLastUpdate(Instant.now());
        pdpGroupService.updatePdp(pdpGroup.getName(), pdpSubgroup.getPdpType(), pdpInstance);

        LOGGER.debug("Updated Pdp in DB - {}", pdpInstance);
    }

    private void sendPdpMessage(final String pdpGroupName, final PdpSubGroup subGroup, final String pdpInstanceId,
            final PdpState pdpState) {
        final List<ToscaPolicy> polsToBeDeployed = new LinkedList<>(policiesToBeDeployed.values());
        final var pdpUpdatemessage =
            createPdpUpdateMessage(pdpGroupName, subGroup, pdpInstanceId,
                        polsToBeDeployed, policiesToBeUndeployed);
        final var pdpStateChangeMessage =
            createPdpStateChangeMessage(pdpGroupName, subGroup, pdpInstanceId, pdpState);
        updateDeploymentStatus(pdpGroupName, subGroup.getPdpType(), pdpInstanceId, pdpStateChangeMessage.getState(),
            pdpUpdatemessage.getPoliciesToBeDeployed());

        requestMap.addRequest(pdpUpdatemessage, pdpStateChangeMessage);
        LOGGER.debug("Sent PdpUpdate message - {}", pdpUpdatemessage);
        LOGGER.debug("Sent PdpStateChange message - {}", pdpStateChangeMessage);
    }
}
