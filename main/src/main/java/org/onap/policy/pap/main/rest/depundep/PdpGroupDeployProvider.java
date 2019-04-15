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

package org.onap.policy.pap.main.rest.depundep;

import com.att.aft.dme2.internal.apache.commons.lang.ObjectUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ObjectValidationResult;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PAP component to deploy PDP groups. The following items must be in the
 * {@link Registry}:
 * <ul>
 * <li>PDP Modification Lock</li>
 * <li>PDP Modify Request Map</li>
 * <li>PAP DAO Factory</li>
 * </ul>
 */
public class PdpGroupDeployProvider extends ProviderBase<PdpGroupDeployResponse> {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupDeployProvider.class);


    /**
     * Constructs the object.
     */
    public PdpGroupDeployProvider() {
        super();
    }

    /**
     * Creates or updates PDP groups.
     *
     * @param groups PDP group configurations to be created or updated
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeployResponse> createOrUpdateGroups(PdpGroups groups) {
        ValidationResult result = groups.validatePapRest();

        if (!result.isValid()) {
            String msg = result.getResult().trim();
            logger.warn(msg);
            return Pair.of(Response.Status.INTERNAL_SERVER_ERROR, makeResponse(msg));
        }

        return process(groups, this::createOrUpdate);
    }

    /**
     * Creates or updates PDP groups. This is the method that does the actual work.
     *
     * @param data session data
     * @param groups PDP group configurations
     */
    private void createOrUpdate(SessionData data, PdpGroups groups) {
        BeanValidationResult result = new BeanValidationResult("groups", groups);

        for (PdpGroup group : groups.getGroups()) {
            PdpGroup dbgroup = data.getGroup(group.getName());

            if (dbgroup == null) {
                result.addResult(addGroup(data, group));

            } else {
                result.addResult(updateGroup(data, dbgroup, group));
            }
        }

        if (!result.isValid()) {
            throw new PolicyPapRuntimeException(result.getResult().trim());
        }
    }

    /**
     * Adds a new group.
     *
     * @param data session data
     * @param group the group to be added
     * @return the validation result
     */
    private ValidationResult addGroup(SessionData data, PdpGroup group) {
        BeanValidationResult result = new BeanValidationResult(group.getName(), group);

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
     * @param group the group to be validated
     * @param result the validation result
     */
    private void validateGroupOnly(PdpGroup group, BeanValidationResult result) {
        if (group.getPdpGroupState() == null) {
            return;
        }

        switch (group.getPdpGroupState()) {
            case ACTIVE:
            case PASSIVE:
                break;

            default:
                result.addResult(new ObjectValidationResult("pdpGroupState", group.getPdpGroupState(),
                                ValidationStatus.INVALID, "must be null, ACTIVE, or PASSIVE"));
                break;
        }
    }

    /**
     * Updates an existing group.
     *
     * @param data session data
     * @param dbgroup the group, as it appears within the DB
     * @param group the group to be added
     * @return the validation result
     */
    private ValidationResult updateGroup(SessionData data, PdpGroup dbgroup, PdpGroup group) {
        BeanValidationResult result = new BeanValidationResult(group.getName(), group);

        if (!ObjectUtils.equals(dbgroup.getProperties(), group.getProperties())) {
            result.addResult(new ObjectValidationResult("properties", "", ValidationStatus.INVALID,
                            "cannot change properties"));
        }

        // create a map of existing subgroups
        Map<String, PdpSubGroup> type2sub = new HashMap<>();
        dbgroup.getPdpSubgroups().forEach(subgrp -> type2sub.put(subgrp.getPdpType(), subgrp));

        boolean updated = updateField(dbgroup.getDescription(), group.getDescription(), dbgroup::setDescription);

        for (PdpSubGroup subgrp : group.getPdpSubgroups()) {
            PdpSubGroup dbsub = type2sub.get(subgrp.getPdpType());
            BeanValidationResult subResult = new BeanValidationResult(subgrp.getPdpType(), subgrp);

            if (dbsub == null) {
                updated = true;
                subResult.addResult(addSubGroup(data, subgrp));
                dbgroup.getPdpSubgroups().add(subgrp);

            } else {
                updated = updateSubGroup(data, group, dbsub, subgrp, subResult) || updated;
            }

            result.addResult(subResult);
        }

        if (result.isValid() && updated) {
            data.update(group);
        }

        return result;
    }

    /**
     * Updates a field, if the new value is different than the old value.
     *
     * @param oldValue old value
     * @param newValue new value
     * @param setter function to set the field to the new value
     * @return {@code true} if the field was updated, {@code false} if it already matched
     *         the new value
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
     * Adds a new subgroup.
     *
     * @param data session data
     * @param subgrp the subgroup to be added
     * @return the validation result
     */
    private ValidationResult addSubGroup(SessionData data, PdpSubGroup subgrp) {
        subgrp.setCurrentInstanceCount(0);
        subgrp.setPdpInstances(Collections.emptyList());

        BeanValidationResult result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        result.addResult(validatePolicies(data, subgrp));

        return result;
    }

    /**
     * Updates an existing subgroup.
     *
     * @param data session data
     * @param dbgroup the group, from the DB, containing the subgroup
     * @param dbsub the subgroup, from the DB
     * @param subgrp the subgroup to be updated
     * @param container container for additional validation results
     * @return {@code true} if the subgroup content was changed, {@code false} if there
     *         were no changes
     */
    private boolean updateSubGroup(SessionData data, PdpGroup dbgroup, PdpSubGroup dbsub, PdpSubGroup subgrp,
                    BeanValidationResult container) {

        // perform additional validations first
        if (!validateSubGroup(data, dbsub, subgrp, container)) {
            return false;
        }

        /*
         * first, apply the changes about which the PDPs care
         */
        boolean updated = updateList(dbsub.getPolicies(), subgrp.getPolicies(), dbsub::setPolicies);

        // publish any changes to the PDPs
        if (updated) {
            makeUpdates(data, dbgroup, dbsub);
        }

        /*
         * now make any remaining changes
         */
        updated = updateList(dbsub.getSupportedPolicyTypes(), subgrp.getSupportedPolicyTypes(),
                        dbsub::setSupportedPolicyTypes) || updated;

        return updateField(dbsub.getDesiredInstanceCount(), subgrp.getDesiredInstanceCount(),
                        dbsub::setDesiredInstanceCount) || updated;
    }

    /**
     * Performs additional validations of a subgroup.
     *
     * @param data session data
     * @param dbsub the subgroup, from the DB
     * @param subgrp the subgroup to be validated
     * @param container container for additional validation results
     * @return {@code true} if the subgroup is valid, {@code false} otherwise
     */
    private boolean validateSubGroup(SessionData data, PdpSubGroup dbsub, PdpSubGroup subgrp,
                    BeanValidationResult container) {

        BeanValidationResult result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        if (!ObjectUtils.equals(dbsub.getProperties(), subgrp.getProperties())) {
            result.addResult(new ObjectValidationResult("properties", "", ValidationStatus.INVALID,
                            "cannot change properties"));
        }

        result.addResult(validatePolicies(data, subgrp));
        container.addResult(result);

        return result.isValid();
    }

    /**
     * Updates a DB list with items from a new list.
     *
     * @param dblist the list from the DB
     * @param newList the new list
     * @param setter function to set the new list
     * @return {@code true} if the list changed, {@code false} if the lists were the same
     */
    private <T> boolean updateList(List<T> dblist, List<T> newList, Consumer<List<T>> setter) {

        Set<T> dbTypes = new HashSet<T>(dblist);
        Set<T> newTypes = new HashSet<T>(newList);

        if (dbTypes.equals(newTypes)) {
            return false;
        }

        setter.accept(new ArrayList<>(newTypes));

        return true;
    }

    /**
     * Performs additional validations of the policies within a subgroup.
     *
     * @param data session data
     * @param subgrp the subgroup to be validated
     * @param result the validation result
     */
    private ValidationResult validatePolicies(SessionData data, PdpSubGroup subgrp) {
        BeanValidationResult result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        for (ToscaPolicyIdentifier ident : subgrp.getPolicies()) {
            ToscaPolicy policy = data.getPolicy(new ToscaPolicyIdentifierOptVersion(ident));

            if (!subgrp.getSupportedPolicyTypes().contains(policy.getTypeIdentifier())) {
                result.addResult(new ObjectValidationResult("policy", ident, ValidationStatus.INVALID,
                                "not a supported policy for the subgroup"));
            }
        }

        return result;
    }

    /**
     * Deploys or updates PDP policies using the simple API.
     *
     * @param policies PDP policies
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeployResponse> deployPolicies(PdpDeployPolicies policies) {
        return process(policies, this::deploySimplePolicies);
    }

    /**
     * Deploys or updates PDP policies using the simple API. This is the method that does
     * the actual work.
     *
     * @param data session data
     * @param extPolicies external PDP policies
     * @return a list of requests that should be sent to configure the PDPs
     */
    private void deploySimplePolicies(SessionData data, PdpDeployPolicies policies) {

        for (ToscaPolicyIdentifierOptVersion desiredPolicy : policies.getPolicies()) {

            try {
                processPolicy(data, desiredPolicy);

            } catch (PfModelException e) {
                // no need to log the error here, as it will be logged by the invoker
                logger.warn("failed to deploy policy: {}", desiredPolicy);
                throw new PolicyPapRuntimeException(DB_ERROR_MSG, e);

            } catch (RuntimeException e) {
                // no need to log the error here, as it will be logged by the invoker
                logger.warn("failed to deploy policy: {}", desiredPolicy);
                throw e;
            }
        }
    }

    /**
     * Adds a policy to a subgroup, if it isn't there already.
     */
    @Override
    protected BiFunction<PdpGroup, PdpSubGroup, Boolean> makeUpdater(ToscaPolicy policy) {
        ToscaPolicyIdentifier desiredIdent = policy.getIdentifier();
        ToscaPolicyTypeIdentifier desiredType = policy.getTypeIdentifier();

        return (group, subgroup) -> {

            if (!subgroup.getSupportedPolicyTypes().contains(desiredType)) {
                // doesn't support the desired policy type
                return false;
            }

            if (subgroup.getPolicies().contains(desiredIdent)) {
                // already has the desired policy
                return false;
            }

            if (subgroup.getPdpInstances().isEmpty()) {
                throw new PolicyPapRuntimeException("group " + group.getName() + " subgroup " + subgroup.getPdpType()
                                + " has no active PDPs");
            }


            // add the policy to the subgroup
            subgroup.getPolicies().add(desiredIdent);
            return true;
        };
    }

    @Override
    public PdpGroupDeployResponse makeResponse(String errorMsg) {
        PdpGroupDeployResponse resp = new PdpGroupDeployResponse();
        resp.setErrorDetails(errorMsg);
        return resp;
    }
}
