/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021, 2023 Nordix Foundation.
 * Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 * Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.rest;

import jakarta.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provider for PAP component to create or update PDP groups. The following items must be in the
 * {@link Registry}:
 * <ul>
 * <li>PDP Modification Lock</li>
 * <li>PDP Modify Request Map</li>
 * <li>PAP DAO Factory</li>
 * </ul>
 */
@Service
public class PdpGroupCreateOrUpdateProvider extends ProviderBase {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupCreateOrUpdateProvider.class);

    /**
     * Creates or updates PDP groups.
     *
     * @param groups PDP group configurations to be created or updated
     * @throws PfModelException if an error occurred
     */
    public void createOrUpdateGroups(PdpGroups groups) throws PfModelException {
        var result = new BeanValidationResult("groups", groups);
        groups.checkForDuplicateGroups(result);
        if (!result.isValid()) {
            String msg = result.getResult().trim();
            logger.warn(msg);
            throw new PfModelException(Status.BAD_REQUEST, msg);
        }
        // During PdpGroup create/update, policies are not supposed to be deployed/undeployed into the group.
        // There is a separate API for this.
        List<PdpSubGroup> subGroupsWithPolicies =
            groups.getGroups().parallelStream().flatMap(group -> group.getPdpSubgroups().parallelStream())
                .filter(subGroup -> null != subGroup.getPolicies() && !subGroup.getPolicies().isEmpty())
                .toList();
        if (!subGroupsWithPolicies.isEmpty()) {
            logger.warn(
                "Policies cannot be deployed during PdpGroup Create/Update operation. Ignoring the list of policies");
            subGroupsWithPolicies.forEach(subGroup -> subGroup.setPolicies(Collections.emptyList()));
        }
        process(groups, this::createOrUpdate);
    }

    /**
     * Creates or updates PDP groups. This is the method that does the actual work.
     *
     * @param data   session data
     * @param groups PDP group configurations
     * @throws PfModelException if an error occurred
     */
    private void createOrUpdate(SessionData data, PdpGroups groups) throws PfModelException {
        var result = new BeanValidationResult("groups", groups);

        for (PdpGroup group : groups.getGroups()) {
            PdpGroup dbgroup = data.getGroup(group.getName());
            ValidationResult groupValidationResult;
            if (dbgroup == null) {
                // create group flow
                groupValidationResult = group.validatePapRest(false);
                if (groupValidationResult.isValid()) {
                    result.addResult(addGroup(data, group));
                } else {
                    result.addResult(groupValidationResult);
                }

            } else {
                // update group flow
                groupValidationResult = group.validatePapRest(true);
                if (groupValidationResult.isValid()) {
                    result.addResult(updateGroup(data, dbgroup, group));
                } else {
                    result.addResult(groupValidationResult);
                }
            }
        }

        if (!result.isValid()) {
            throw new PfModelException(Status.BAD_REQUEST, result.getResult().trim());
        }
    }

    /**
     * Adds a new group.
     *
     * @param data  session data
     * @param group the group to be added
     * @return the validation result
     * @throws PfModelException if an error occurred
     */
    private ValidationResult addGroup(SessionData data, PdpGroup group) throws PfModelException {
        var result = new BeanValidationResult(group.getName(), group);

        validateGroupOnly(group, result);
        if (!result.isValid()) {
            return result;
        }

        // default to active
        if (group.getPdpGroupState() == null) {
            group.setPdpGroupState(PdpState.ACTIVE);
        }

        for (PdpSubGroup subgrp : group.getPdpSubgroups()) {
            result.addResult(addSubGroup(data, subgrp));
        }

        if (result.isValid()) {
            data.create(group);
        }

        return result;
    }

    /**
     * Performs additional validations of a group, but does not examine the subgroups.
     *
     * @param group  the group to be validated
     * @param result the validation result
     */
    private void validateGroupOnly(PdpGroup group, BeanValidationResult result) {
        if (group.getPdpGroupState() == null) {
            return;
        }

        PdpState pdpGroupState = group.getPdpGroupState();
        if (pdpGroupState != PdpState.ACTIVE && pdpGroupState != PdpState.PASSIVE) {
            result.addResult("pdpGroupState", group.getPdpGroupState(),
                ValidationStatus.INVALID, "must be null, ACTIVE, or PASSIVE");
        }
    }

    /**
     * Updates an existing group.
     *
     * @param data    session data
     * @param dbgroup the group, as it appears within the DB
     * @param group   the group to be added
     * @return the validation result
     * @throws PfModelException if an error occurred
     */
    private ValidationResult updateGroup(SessionData data, PdpGroup dbgroup, PdpGroup group) throws PfModelException {
        var result = new BeanValidationResult(group.getName(), group);

        if (!Objects.equals(dbgroup.getProperties(), group.getProperties())) {
            result.addResult("properties", "", ValidationStatus.INVALID, "cannot change properties");
        }

        boolean updated = updateField(dbgroup.getDescription(), group.getDescription(), dbgroup::setDescription);
        updated =
            updateField(dbgroup.getPdpGroupState(), group.getPdpGroupState(), dbgroup::setPdpGroupState) || updated;
        updated = notifyPdpsDelSubGroups(data, dbgroup, group) || updated;
        updated = addOrUpdateSubGroups(data, dbgroup, group, result) || updated;

        if (result.isValid() && updated) {
            data.update(group);
        }

        return result;
    }

    /**
     * Updates a field, if the new value is different from the old value.
     *
     * @param oldValue old value
     * @param newValue new value
     * @param setter   function to set the field to the new value
     * @return {@code true} if the field was updated, {@code false} if it already matched the new value
     */
    private <T> boolean updateField(T oldValue, T newValue, Consumer<T> setter) {
        if (oldValue == newValue) {
            return false;
        }

        if (oldValue != null && oldValue.equals(newValue)) {
            return false;
        }

        setter.accept(newValue);
        return true;
    }

    /**
     * Adds or updates subgroups within the group.
     *
     * @param data    session data
     * @param dbgroup the group, as it appears within the DB
     * @param group   the group to be added
     * @param result  the validation result
     * @return {@code true} if the DB group was modified, {@code false} otherwise
     * @throws PfModelException if an error occurred
     */
    private boolean addOrUpdateSubGroups(SessionData data, PdpGroup dbgroup, PdpGroup group,
                                         BeanValidationResult result) throws PfModelException {

        // create a map of existing subgroups
        Map<String, PdpSubGroup> type2sub = new HashMap<>();
        dbgroup.getPdpSubgroups().forEach(subgrp -> type2sub.put(subgrp.getPdpType(), subgrp));

        var updated = false;

        for (PdpSubGroup subgrp : group.getPdpSubgroups()) {
            PdpSubGroup dbsub = type2sub.get(subgrp.getPdpType());
            var subResult = new BeanValidationResult(subgrp.getPdpType(), subgrp);

            if (dbsub == null) {
                updated = true;
                subResult.addResult(addSubGroup(data, subgrp));
                dbgroup.getPdpSubgroups().add(subgrp);

            } else {
                updated = updateSubGroup(dbsub, subgrp, subResult) || updated;
            }

            result.addResult(subResult);
        }

        return updated;
    }

    /**
     * Notifies any PDPs whose subgroups are being removed.
     *
     * @param data    session data
     * @param dbgroup the group, as it appears within the DB
     * @param group   the group being updated
     * @return {@code true} if a subgroup was removed, {@code false} otherwise
     * @throws PfModelException if an error occurred
     */
    private boolean notifyPdpsDelSubGroups(SessionData data, PdpGroup dbgroup, PdpGroup group) throws PfModelException {
        var updated = false;

        // subgroups, as they appear within the updated group
        Set<String> subgroups = new HashSet<>();
        group.getPdpSubgroups().forEach(subgrp -> subgroups.add(subgrp.getPdpType()));

        // loop through subgroups as they appear within the DB
        for (PdpSubGroup subgrp : dbgroup.getPdpSubgroups()) {

            if (!subgroups.contains(subgrp.getPdpType())) {
                // this subgroup no longer appears - notify its PDPs
                updated = true;
                notifyPdpsDelSubGroup(data, subgrp);
                trackPdpsDelSubGroup(data, dbgroup.getName(), subgrp);
            }
        }

        return updated;
    }

    /**
     * Notifies the PDPs that their subgroup is being removed.
     *
     * @param data   session data
     * @param subgrp subgroup that is being removed
     */
    private void notifyPdpsDelSubGroup(SessionData data, PdpSubGroup subgrp) {
        for (Pdp pdp : subgrp.getPdpInstances()) {
            String name = pdp.getInstanceId();

            // make it passive
            var change = new PdpStateChange();
            change.setSource(PapConstants.PAP_NAME);
            change.setName(name);
            change.setState(PdpState.PASSIVE);

            // remove it from subgroup and undeploy all policies
            var update = new PdpUpdate();
            update.setSource(PapConstants.PAP_NAME);
            update.setName(name);

            data.addRequests(update, change);
        }
    }

    /**
     * Tracks PDP responses when their subgroup is removed.
     *
     * @param data     session data
     * @param pdpGroup PdpGroup name
     * @param subgrp   subgroup that is being removed
     * @throws PfModelException if an error occurred
     */
    private void trackPdpsDelSubGroup(SessionData data, String pdpGroup, PdpSubGroup subgrp) throws PfModelException {
        Set<String> pdps = subgrp.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toSet());

        for (ToscaConceptIdentifier policyId : subgrp.getPolicies()) {
            data.trackUndeploy(policyId, pdps, pdpGroup, subgrp.getPdpType());
        }
    }

    /**
     * Adds a new subgroup.
     *
     * @param data   session data
     * @param subgrp the subgroup to be added, updated to fully qualified versions upon
     *               return
     * @return the validation result
     * @throws PfModelException if an error occurred
     */
    private ValidationResult addSubGroup(SessionData data, PdpSubGroup subgrp) throws PfModelException {
        subgrp.setCurrentInstanceCount(0);
        subgrp.setPdpInstances(Collections.emptyList());

        var result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        result.addResult(validateSupportedTypes(data, subgrp));
        return result;
    }

    /**
     * Updates an existing subgroup.
     *
     * @param dbsub     the subgroup, from the DB
     * @param subgrp    the subgroup to be updated, updated to fully qualified versions upon
     *                  return
     * @param container container for additional validation results
     * @return {@code true} if the subgroup content was changed, {@code false} if there were no changes
     */
    private boolean updateSubGroup(PdpSubGroup dbsub, PdpSubGroup subgrp, BeanValidationResult container) {

        // perform additional validations first
        if (!validateSubGroup(dbsub, subgrp, container)) {
            return false;
        }

        if (null != subgrp.getSupportedPolicyTypes() && !new HashSet<>(dbsub.getSupportedPolicyTypes())
            .equals(new HashSet<>(subgrp.getSupportedPolicyTypes()))) {
            logger.warn("Supported policy types cannot be updated while updating PdpGroup. "
                + "Hence, ignoring the new set of supported policy types.");
        }

        // while updating PdpGroup, list of policies (already deployed ones) and supported policies (the ones provided
        // during PdpGroup creation) has to be retained
        subgrp.setSupportedPolicyTypes(dbsub.getSupportedPolicyTypes());
        subgrp.setPolicies(dbsub.getPolicies());
        return updateField(dbsub.getDesiredInstanceCount(), subgrp.getDesiredInstanceCount(),
            dbsub::setDesiredInstanceCount);
    }

    /**
     * Performs additional validations of a subgroup.
     *
     * @param dbsub     the subgroup, from the DB
     * @param subgrp    the subgroup to be validated, updated to fully qualified versions
     *                  upon return
     * @param container container for additional validation results
     * @return {@code true} if the subgroup is valid, {@code false} otherwise
     */
    private boolean validateSubGroup(PdpSubGroup dbsub, PdpSubGroup subgrp, BeanValidationResult container) {

        var result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        if (!Objects.equals(dbsub.getProperties(), subgrp.getProperties())) {
            result.addResult("properties", "", ValidationStatus.INVALID, "cannot change properties");
        }

        container.addResult(result);

        return result.isValid();
    }

    /**
     * Performs validations of the supported policy types within a subgroup.
     *
     * @param data   session data
     * @param subgrp the subgroup to be validated
     * @return the validation result
     * @throws PfModelException if an error occurred
     */
    private ValidationResult validateSupportedTypes(SessionData data, PdpSubGroup subgrp) throws PfModelException {
        var result = new BeanValidationResult(subgrp.getPdpType(), subgrp);
        for (ToscaConceptIdentifier type : subgrp.getSupportedPolicyTypes()) {
            if (!type.getName().endsWith(".*") && data.getPolicyType(type) == null) {
                result.addResult("policy type", type, ValidationStatus.INVALID, "unknown policy type");
            }
        }

        return result;
    }

    @Override
    protected Updater makeUpdater(SessionData data, ToscaPolicy policy,
                                  ToscaConceptIdentifierOptVersion desiredPolicy) {
        throw new UnsupportedOperationException("makeUpdater should not be invoked");
    }
}
