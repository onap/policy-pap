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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ObjectValidationResult;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
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
public class PdpGroupDeployProvider extends ProviderBase {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupDeployProvider.class);

    private static final String POLICY_RESULT_NAME = "policy";


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
     * @throws PfModelException if an error occurred
     */
    public void createOrUpdateGroups(PdpGroups groups) throws PfModelException {
        ValidationResult result = groups.validatePapRest();

        if (!result.isValid()) {
            String msg = result.getResult().trim();
            logger.warn(msg);
            throw new PfModelException(Status.BAD_REQUEST, msg);
        }

        process(groups, this::createOrUpdate);
    }

    /**
     * Creates or updates PDP groups. This is the method that does the actual work.
     *
     * @param data session data
     * @param groups PDP group configurations
     * @throws PfModelException if an error occurred
     */
    private void createOrUpdate(SessionData data, PdpGroups groups) throws PfModelException {
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
            throw new PfModelException(Status.BAD_REQUEST, result.getResult().trim());
        }
    }

    /**
     * Adds a new group.
     *
     * @param data session data
     * @param group the group to be added
     * @return the validation result
     * @throws PfModelException if an error occurred
     */
    private ValidationResult addGroup(SessionData data, PdpGroup group) throws PfModelException {
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
     * @throws PfModelException if an error occurred
     */
    private ValidationResult updateGroup(SessionData data, PdpGroup dbgroup, PdpGroup group) throws PfModelException {
        BeanValidationResult result = new BeanValidationResult(group.getName(), group);

        if (!ObjectUtils.equals(dbgroup.getProperties(), group.getProperties())) {
            result.addResult(new ObjectValidationResult("properties", "", ValidationStatus.INVALID,
                            "cannot change properties"));
        }

        boolean updated = updateField(dbgroup.getDescription(), group.getDescription(), dbgroup::setDescription);
        updated = notifyPdpsDelSubGroups(data, dbgroup, group) || updated;
        updated = addOrUpdateSubGroups(data, dbgroup, group, result) || updated;

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
     * Adds or updates subgroups within the group.
     *
     * @param data session data
     * @param dbgroup the group, as it appears within the DB
     * @param group the group to be added
     * @param result the validation result
     * @return {@code true} if the DB group was modified, {@code false} otherwise
     * @throws PfModelException if an error occurred
     */
    private boolean addOrUpdateSubGroups(SessionData data, PdpGroup dbgroup, PdpGroup group,
                    BeanValidationResult result) throws PfModelException {

        // create a map of existing subgroups
        Map<String, PdpSubGroup> type2sub = new HashMap<>();
        dbgroup.getPdpSubgroups().forEach(subgrp -> type2sub.put(subgrp.getPdpType(), subgrp));

        boolean updated = false;

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

        return updated;
    }

    /**
     * Notifies any PDPs whose subgroups are being removed.
     *
     * @param data session data
     * @param dbgroup the group, as it appears within the DB
     * @param group the group being updated
     * @return {@code true} if a subgroup was removed, {@code false} otherwise
     * @throws PfModelException if an error occurred
     */
    private boolean notifyPdpsDelSubGroups(SessionData data, PdpGroup dbgroup, PdpGroup group) throws PfModelException {
        boolean updated = false;

        // subgroups, as they appear within the updated group
        Set<String> subgroups = new HashSet<>();
        group.getPdpSubgroups().forEach(subgrp -> subgroups.add(subgrp.getPdpType()));

        // loop through subgroups as they appear within the DB
        for (PdpSubGroup subgrp : dbgroup.getPdpSubgroups()) {

            if (!subgroups.contains(subgrp.getPdpType())) {
                // this subgroup no longer appears - notify its PDPs
                updated = true;
                notifyPdpsDelSubGroup(data, subgrp);
                trackPdpsDelSubGroup(data, subgrp);
            }
        }

        return updated;
    }

    /**
     * Notifies the PDPs that their subgroup is being removed.
     *
     * @param data session data
     * @param subgrp subgroup that is being removed
     */
    private void notifyPdpsDelSubGroup(SessionData data, PdpSubGroup subgrp) {
        for (Pdp pdp : subgrp.getPdpInstances()) {
            String name = pdp.getInstanceId();

            // make it passive
            PdpStateChange change = new PdpStateChange();
            change.setName(name);
            change.setState(PdpState.PASSIVE);

            // remove it from subgroup and undeploy all policies
            PdpUpdate update = new PdpUpdate();
            update.setName(name);

            data.addRequests(update, change);
        }
    }

    /**
     * Tracks PDP responses when their subgroup is removed.
     *
     * @param data session data
     * @param subgrp subgroup that is being removed
     * @throws PfModelException if an error occurred
     */
    private void trackPdpsDelSubGroup(SessionData data, PdpSubGroup subgrp) throws PfModelException {
        Set<String> pdps = subgrp.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toSet());

        for (ToscaPolicyIdentifier policyId : subgrp.getPolicies()) {
            data.trackUndeploy(policyId, pdps);
        }
    }

    /**
     * Adds a new subgroup.
     *
     * @param data session data
     * @param subgrp the subgroup to be added, updated to fully qualified versions upon
     *        return
     * @return the validation result
     * @throws PfModelException if an error occurred
     */
    private ValidationResult addSubGroup(SessionData data, PdpSubGroup subgrp) throws PfModelException {
        subgrp.setCurrentInstanceCount(0);
        subgrp.setPdpInstances(Collections.emptyList());

        BeanValidationResult result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        result.addResult(validateSupportedTypes(data, subgrp));
        result.addResult(validatePolicies(data, null, subgrp));

        return result;
    }

    /**
     * Updates an existing subgroup.
     *
     * @param data session data
     * @param dbgroup the group, from the DB, containing the subgroup
     * @param dbsub the subgroup, from the DB
     * @param subgrp the subgroup to be updated, updated to fully qualified versions upon
     *        return
     * @param container container for additional validation results
     * @return {@code true} if the subgroup content was changed, {@code false} if there
     *         were no changes
     * @throws PfModelException if an error occurred
     */
    private boolean updateSubGroup(SessionData data, PdpGroup dbgroup, PdpSubGroup dbsub, PdpSubGroup subgrp,
                    BeanValidationResult container) throws PfModelException {

        // perform additional validations first
        if (!validateSubGroup(data, dbsub, subgrp, container)) {
            return false;
        }

        /*
         * first, apply the changes about which the PDPs care
         */
        boolean updated = updatePolicies(data, dbsub, subgrp);

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

    private boolean updatePolicies(SessionData data, PdpSubGroup dbsub, PdpSubGroup subgrp) throws PfModelException {
        Set<ToscaPolicyIdentifier> undeployed = new HashSet<>(dbsub.getPolicies());
        undeployed.removeAll(subgrp.getPolicies());

        Set<ToscaPolicyIdentifier> deployed = new HashSet<>(subgrp.getPolicies());
        deployed.removeAll(dbsub.getPolicies());

        if (deployed.isEmpty() && undeployed.isEmpty()) {
            // lists are identical
            return false;
        }


        Set<String> pdps = subgrp.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toSet());

        for (ToscaPolicyIdentifier policyId : deployed) {
            data.trackDeploy(policyId, pdps);
        }

        for (ToscaPolicyIdentifier policyId : undeployed) {
            data.trackUndeploy(policyId, pdps);
        }

        dbsub.setPolicies(new ArrayList<>(subgrp.getPolicies()));
        return true;
    }

    /**
     * Performs additional validations of a subgroup.
     *
     * @param data session data
     * @param dbsub the subgroup, from the DB
     * @param subgrp the subgroup to be validated, updated to fully qualified versions
     *        upon return
     * @param container container for additional validation results
     * @return {@code true} if the subgroup is valid, {@code false} otherwise
     * @throws PfModelException if an error occurred
     */
    private boolean validateSubGroup(SessionData data, PdpSubGroup dbsub, PdpSubGroup subgrp,
                    BeanValidationResult container) throws PfModelException {

        BeanValidationResult result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        if (!ObjectUtils.equals(dbsub.getProperties(), subgrp.getProperties())) {
            result.addResult(new ObjectValidationResult("properties", "", ValidationStatus.INVALID,
                            "cannot change properties"));
        }

        result.addResult(validatePolicies(data, dbsub, subgrp));
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

        Set<T> dbTypes = new HashSet<>(dblist);
        Set<T> newTypes = new HashSet<>(newList);

        if (dbTypes.equals(newTypes)) {
            return false;
        }

        setter.accept(new ArrayList<>(newTypes));

        return true;
    }

    /**
     * Performs additional validations of the supported policy types within a subgroup.
     *
     * @param data session data
     * @param subgrp the subgroup to be validated
     * @param result the validation result
     * @throws PfModelException if an error occurred
     */
    private ValidationResult validateSupportedTypes(SessionData data, PdpSubGroup subgrp) throws PfModelException {
        BeanValidationResult result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        for (ToscaPolicyTypeIdentifier type : subgrp.getSupportedPolicyTypes()) {
            if (data.getPolicyType(type) == null) {
                result.addResult(new ObjectValidationResult("policy type", type, ValidationStatus.INVALID,
                                "unknown policy type"));
            }
        }

        return result;
    }

    /**
     * Performs additional validations of the policies within a subgroup.
     *
     * @param data session data
     * @param dbsub subgroup from the DB, or {@code null} if this is a new subgroup
     * @param subgrp the subgroup whose policies are to be validated, updated to fully
     *        qualified versions upon return
     * @param result the validation result
     * @throws PfModelException if an error occurred
     */
    private ValidationResult validatePolicies(SessionData data, PdpSubGroup dbsub, PdpSubGroup subgrp)
                    throws PfModelException {

        // build a map of the DB data, from policy name to (fully qualified) policy
        // version
        Map<String, String> dbname2vers = new HashMap<>();
        if (dbsub != null) {
            dbsub.getPolicies().forEach(ident -> dbname2vers.put(ident.getName(), ident.getVersion()));
        }

        BeanValidationResult result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        for (ToscaPolicyIdentifier ident : subgrp.getPolicies()) {
            // note: "ident" may not have a fully qualified version

            String expectedVersion = dbname2vers.get(ident.getName());
            if (expectedVersion != null) {
                // policy exists in the DB list - compare the versions
                validateVersion(expectedVersion, ident, result);
                ident.setVersion(expectedVersion);
                continue;
            }

            // policy doesn't appear in the DB's policy list - look it up

            ToscaPolicy policy = data.getPolicy(new ToscaPolicyIdentifierOptVersion(ident));
            if (policy == null) {
                result.addResult(new ObjectValidationResult(POLICY_RESULT_NAME, ident, ValidationStatus.INVALID,
                                "unknown policy"));

            } else if (!subgrp.getSupportedPolicyTypes().contains(policy.getTypeIdentifier())) {
                result.addResult(new ObjectValidationResult(POLICY_RESULT_NAME, ident, ValidationStatus.INVALID,
                                "not a supported policy for the subgroup"));

            } else {
                // replace version with the fully qualified version from the policy
                ident.setVersion(policy.getVersion());
            }
        }

        return result;
    }

    /**
     * Determines if the new version matches the version in the DB.
     *
     * @param dbvers fully qualified version from the DB
     * @param ident identifier whose version is to be validated; the version need not be
     *        fully qualified
     * @param result the validation result
     */
    private void validateVersion(String dbvers, ToscaPolicyIdentifier ident, BeanValidationResult result) {
        String idvers = ident.getVersion();
        if (dbvers.equals(idvers)) {
            return;
        }

        // did not match - see if it's a prefix

        if (SessionData.isVersionPrefix(idvers) && dbvers.startsWith(idvers + ".")) {
            // ident has a prefix of this version
            return;
        }

        result.addResult(new ObjectValidationResult(POLICY_RESULT_NAME, ident, ValidationStatus.INVALID,
                        "different version already deployed: " + dbvers));
    }

    /**
     * Deploys or updates PDP policies using the simple API.
     *
     * @param policies PDP policies
     * @throws PfModelException if an error occurred
     */
    public void deployPolicies(PdpDeployPolicies policies) throws PfModelException {
        process(policies, this::deploySimplePolicies);
    }

    /**
     * Deploys or updates PDP policies using the simple API. This is the method that does
     * the actual work.
     *
     * @param data session data
     * @param extPolicies external PDP policies
     * @return a list of requests that should be sent to configure the PDPs
     * @throws PfModelException if an error occurred
     */
    private void deploySimplePolicies(SessionData data, PdpDeployPolicies policies) throws PfModelException {

        for (ToscaPolicyIdentifierOptVersion desiredPolicy : policies.getPolicies()) {

            try {
                processPolicy(data, desiredPolicy);

            } catch (PfModelException | RuntimeException e) {
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
    protected Updater makeUpdater(SessionData data, ToscaPolicy policy,
                    ToscaPolicyIdentifierOptVersion requestedIdent) {

        ToscaPolicyIdentifier desiredIdent = policy.getIdentifier();
        ToscaPolicyTypeIdentifier desiredType = policy.getTypeIdentifier();

        return (group, subgroup) -> {

            if (!subgroup.getSupportedPolicyTypes().contains(desiredType)) {
                // doesn't support the desired policy type
                return false;
            }

            if (containsPolicy(group, subgroup, desiredIdent)) {
                return false;
            }

            if (subgroup.getPdpInstances().isEmpty()) {
                throw new PfModelRuntimeException(Status.BAD_REQUEST, "group " + group.getName() + " subgroup "
                                + subgroup.getPdpType() + " has no active PDPs");
            }


            // add the policy to the subgroup
            subgroup.getPolicies().add(desiredIdent);

            logger.info("add policy {} {} to subgroup {} {} count={}", desiredIdent.getName(),
                            desiredIdent.getVersion(), group.getName(), subgroup.getPdpType(),
                            subgroup.getPolicies().size());

            Set<String> pdps = subgroup.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toSet());
            data.trackDeploy(desiredIdent, pdps);

            return true;
        };
    }

    /**
     * Determines if a subgroup already contains the desired policy.
     *
     * @param group group that contains the subgroup
     * @param subgroup subgroup of interest
     * @param desiredIdent identifier of the desired policy
     * @return {@code true} if the subgroup contains the desired policy, {@code false}
     *         otherwise
     * @throws PfModelRuntimeException if the subgroup contains a different version of the
     *         desired policy
     */
    private boolean containsPolicy(PdpGroup group, PdpSubGroup subgroup, ToscaPolicyIdentifier desiredIdent) {
        String desnm = desiredIdent.getName();
        String desvers = desiredIdent.getVersion();

        for (ToscaPolicyIdentifier actualIdent : subgroup.getPolicies()) {
            if (!actualIdent.getName().equals(desnm)) {
                continue;
            }

            // found the policy - ensure the version matches
            if (!actualIdent.getVersion().equals(desvers)) {
                throw new PfModelRuntimeException(Status.BAD_REQUEST,
                                "group " + group.getName() + " subgroup " + subgroup.getPdpType() + " policy " + desnm
                                                + " " + desvers + " different version already deployed: "
                                                + actualIdent.getVersion());
            }

            // already has the desired policy & version
            logger.info("subgroup {} {} already contains policy {} {}", group.getName(), subgroup.getPdpType(), desnm,
                            desvers);
            return true;
        }

        return false;
    }
}
