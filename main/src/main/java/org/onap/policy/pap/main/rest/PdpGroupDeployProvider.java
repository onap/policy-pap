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

package org.onap.policy.pap.main.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.onap.policy.models.pdp.concepts.DeploymentGroup;
import org.onap.policy.models.pdp.concepts.DeploymentGroups;
import org.onap.policy.models.pdp.concepts.DeploymentSubGroup;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
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
     * Updates policies in specific PDP groups.
     *
     * @param groups PDP group deployments to be updated
     * @throws PfModelException if an error occurred
     */
    public void updateGroupPolicies(DeploymentGroups groups) throws PfModelException {
        ValidationResult result = groups.validatePapRest();

        if (!result.isValid()) {
            String msg = result.getResult().trim();
            logger.warn(msg);
            throw new PfModelException(Status.BAD_REQUEST, msg);
        }

        process(groups, this::updateGroups);
    }

    /**
     * Updates policies in specific PDP groups. This is the method that does the actual work.
     *
     * @param data session data
     * @param groups PDP group deployments
     * @throws PfModelException if an error occurred
     */
    private void updateGroups(SessionData data, DeploymentGroups groups) throws PfModelException {
        BeanValidationResult result = new BeanValidationResult("groups", groups);

        for (DeploymentGroup group : groups.getGroups()) {
            PdpGroup dbgroup = data.getGroup(group.getName());

            if (dbgroup == null) {
                result.addResult(new ObjectValidationResult(group.getName(), group,
                                ValidationStatus.INVALID, "unknown group"));

            } else {
                result.addResult(updateGroup(data, dbgroup, group));
            }
        }

        if (!result.isValid()) {
            throw new PfModelException(Status.BAD_REQUEST, result.getResult().trim());
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
    private ValidationResult updateGroup(SessionData data, PdpGroup dbgroup, DeploymentGroup group)
                    throws PfModelException {

        BeanValidationResult result = new BeanValidationResult(group.getName(), group);

        boolean updated = updateSubGroups(data, dbgroup, group, result);

        if (result.isValid() && updated) {
            data.update(dbgroup);
        }

        return result;
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
    private boolean updateSubGroups(SessionData data, PdpGroup dbgroup, DeploymentGroup group,
                    BeanValidationResult result) throws PfModelException {

        // create a map of existing subgroups
        Map<String, PdpSubGroup> type2sub = new HashMap<>();
        dbgroup.getPdpSubgroups().forEach(subgrp -> type2sub.put(subgrp.getPdpType(), subgrp));

        boolean updated = false;

        for (DeploymentSubGroup subgrp : group.getDeploymentSubgroups()) {
            PdpSubGroup dbsub = type2sub.get(subgrp.getPdpType());
            BeanValidationResult subResult = new BeanValidationResult(subgrp.getPdpType(), subgrp);

            if (dbsub == null) {
                subResult.addResult(new ObjectValidationResult(subgrp.getPdpType(), subgrp,
                                ValidationStatus.INVALID, "unknown subgroup"));

            } else {
                updated = updateSubGroup(data, dbgroup, dbsub, subgrp, subResult) || updated;
            }

            result.addResult(subResult);
        }

        return updated;
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
    private boolean updateSubGroup(SessionData data, PdpGroup dbgroup, PdpSubGroup dbsub, DeploymentSubGroup subgrp,
                    BeanValidationResult container) throws PfModelException {

        // perform additional validations first
        if (!validateSubGroup(data, dbsub, subgrp, container)) {
            return false;
        }

        boolean updated = false;

        switch (subgrp.getAction()) {
            case POST:
                updated = addPolicies(data, dbsub, subgrp);
                break;
            case DELETE:
                updated = deletePolicies(data, dbsub, subgrp);
                break;
            case PATCH:
            default:
                updated = updatePolicies(data, dbsub, subgrp);
                break;
        }

        if (updated) {
            // publish any changes to the PDPs
            makeUpdates(data, dbgroup, dbsub);
            return true;
        }

        return false;
    }

    private boolean addPolicies(SessionData data, PdpSubGroup dbsub, DeploymentSubGroup subgrp)
                    throws PfModelException {

        Set<ToscaPolicyIdentifier> policies = new LinkedHashSet<>(dbsub.getPolicies());
        policies.addAll(subgrp.getPolicies());

        DeploymentSubGroup subgrp2 = new DeploymentSubGroup(subgrp);
        subgrp2.getPolicies().clear();
        subgrp2.getPolicies().addAll(policies);

        return updatePolicies(data, dbsub, subgrp2);
    }

    private boolean deletePolicies(SessionData data, PdpSubGroup dbsub, DeploymentSubGroup subgrp)
                    throws PfModelException {

        Set<ToscaPolicyIdentifier> policies = new LinkedHashSet<>(dbsub.getPolicies());
        policies.removeAll(subgrp.getPolicies());

        DeploymentSubGroup subgrp2 = new DeploymentSubGroup(subgrp);
        subgrp2.getPolicies().clear();
        subgrp2.getPolicies().addAll(policies);

        return updatePolicies(data, dbsub, subgrp2);
    }

    private boolean updatePolicies(SessionData data, PdpSubGroup dbsub, DeploymentSubGroup subgrp)
                    throws PfModelException {

        Set<ToscaPolicyIdentifier> undeployed = new HashSet<>(dbsub.getPolicies());
        undeployed.removeAll(subgrp.getPolicies());

        Set<ToscaPolicyIdentifier> deployed = new HashSet<>(subgrp.getPolicies());
        deployed.removeAll(dbsub.getPolicies());

        if (deployed.isEmpty() && undeployed.isEmpty()) {
            // lists are identical
            return false;
        }


        Set<String> pdps = dbsub.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toSet());

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
    private boolean validateSubGroup(SessionData data, PdpSubGroup dbsub, DeploymentSubGroup subgrp,
                    BeanValidationResult container) throws PfModelException {

        BeanValidationResult result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        result.addResult(validatePolicies(data, dbsub, subgrp));
        container.addResult(result);

        return result.isValid();
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
    private ValidationResult validatePolicies(SessionData data, PdpSubGroup dbsub, DeploymentSubGroup subgrp)
                    throws PfModelException {

        // build a map of the DB data, from policy name to (fully qualified) policy
        // version
        Map<String, String> dbname2vers = new HashMap<>();
        dbsub.getPolicies().forEach(ident -> dbname2vers.put(ident.getName(), ident.getVersion()));

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

            } else if (!isPolicySupported(dbsub.getSupportedPolicyTypes(), policy.getTypeIdentifier())) {
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

            if (!isPolicySupported(subgroup.getSupportedPolicyTypes(), desiredType)) {
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
     * Determines if a policy type is supported.
     *
     * @param supportedTypes supported policy types, any of which may end with ".*"
     * @param desiredType policy type of interest
     * @return {@code true} if the policy type is supported, {@code false} otherwise
     */
    private boolean isPolicySupported(List<ToscaPolicyTypeIdentifier> supportedTypes,
                    ToscaPolicyTypeIdentifier desiredType) {

        if (supportedTypes.contains(desiredType)) {
            return true;
        }

        String desiredTypeName = desiredType.getName();
        for (ToscaPolicyTypeIdentifier type : supportedTypes) {
            String supType = type.getName();
            if (supType.endsWith(".*") && desiredTypeName.startsWith(supType.substring(0, supType.length() - 1))) {
                // matches everything up to, AND INCLUDING, the "."
                return true;
            }
        }

        return false;
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
