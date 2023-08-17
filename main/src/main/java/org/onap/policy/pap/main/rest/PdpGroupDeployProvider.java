/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020-2021, 2023 Nordix Foundation.
 * Modifications Copyright (C) 2021, 2023 Bell Canada. All rights reserved.
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

import com.google.gson.annotations.SerializedName;
import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.BeanValidator;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Pattern;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pdp.concepts.DeploymentGroup;
import org.onap.policy.models.pdp.concepts.DeploymentGroups;
import org.onap.policy.models.pdp.concepts.DeploymentSubGroup;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provider for PAP component to deploy PDP groups. The following items must be in the
 * {@link Registry}:
 * <ul>
 * <li>PDP Modification Lock</li>
 * <li>PDP Modify Request Map</li>
 * <li>PAP DAO Factory</li>
 * </ul>
 */
@Service
public class PdpGroupDeployProvider extends ProviderBase {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupDeployProvider.class);
    private static final StandardCoder coder = new StandardCoder();

    private static final String POLICY_RESULT_NAME = "policy";

    /**
     * Updates policies in specific PDP groups.
     *
     * @param groups PDP group deployments to be updated
     * @param user user triggering deployment
     * @throws PfModelException if an error occurred
     */
    public void updateGroupPolicies(DeploymentGroups groups, String user) throws PfModelException {
        ValidationResult result = groups.validatePapRest();
        if (!result.isValid()) {
            String msg = result.getResult().trim();
            throw new PfModelException(Status.BAD_REQUEST, msg);
        }

        process(user, groups, this::updateGroups);
    }

    /**
     * Updates policies in specific PDP groups. This is the method that does the actual work.
     *
     * @param data session data
     * @param groups PDP group deployments
     * @throws PfModelException if an error occurred
     */
    private void updateGroups(SessionData data, DeploymentGroups groups) throws PfModelException {
        var result = new BeanValidationResult("groups", groups);

        for (DeploymentGroup group : groups.getGroups()) {
            PdpGroup dbgroup = data.getGroup(group.getName());

            if (dbgroup == null) {
                result.addResult(group.getName(), group, ValidationStatus.INVALID, "unknown group");

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

        var result = new BeanValidationResult(group.getName(), group);

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

        var updated = false;

        for (DeploymentSubGroup subgrp : group.getDeploymentSubgroups()) {
            PdpSubGroup dbsub = type2sub.get(subgrp.getPdpType());
            var subResult = new BeanValidationResult(subgrp.getPdpType(), subgrp);

            if (dbsub == null) {
                subResult.addResult(subgrp.getPdpType(), subgrp, ValidationStatus.INVALID, "unknown subgroup");

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

        var updated = false;

        switch (subgrp.getAction()) {
            case POST:
                updated = addPolicies(data, dbgroup.getName(), dbsub, subgrp);
                break;
            case DELETE:
                updated = deletePolicies(data, dbgroup.getName(), dbsub, subgrp);
                break;
            case PATCH:
            default:
                updated = updatePolicies(data, dbgroup.getName(), dbsub, subgrp);
                break;
        }

        if (updated) {
            // publish any changes to the PDPs
            makeUpdates(data, dbgroup, dbsub);
            return true;
        }

        return false;
    }

    private boolean addPolicies(SessionData data, String pdpGroup, PdpSubGroup dbsub, DeploymentSubGroup subgrp)
                    throws PfModelException {

        Set<ToscaConceptIdentifier> policies = new LinkedHashSet<>(dbsub.getPolicies());
        policies.addAll(subgrp.getPolicies());

        var subgrp2 = new DeploymentSubGroup(subgrp);
        subgrp2.getPolicies().clear();
        subgrp2.getPolicies().addAll(policies);

        return updatePolicies(data, pdpGroup, dbsub, subgrp2);
    }

    private boolean deletePolicies(SessionData data, String pdpGroup, PdpSubGroup dbsub, DeploymentSubGroup subgrp)
                    throws PfModelException {

        Set<ToscaConceptIdentifier> policies = new LinkedHashSet<>(dbsub.getPolicies());
        policies.removeAll(subgrp.getPolicies());

        var subgrp2 = new DeploymentSubGroup(subgrp);
        subgrp2.getPolicies().clear();
        subgrp2.getPolicies().addAll(policies);

        return updatePolicies(data, pdpGroup, dbsub, subgrp2);
    }

    private boolean updatePolicies(SessionData data, String pdpGroup, PdpSubGroup dbsub, DeploymentSubGroup subgrp)
                    throws PfModelException {

        Set<ToscaConceptIdentifier> undeployed = new HashSet<>(dbsub.getPolicies());
        undeployed.removeAll(subgrp.getPolicies());

        Set<ToscaConceptIdentifier> deployed = new HashSet<>(subgrp.getPolicies());
        deployed.removeAll(dbsub.getPolicies());

        if (deployed.isEmpty() && undeployed.isEmpty()) {
            // lists are identical
            return false;
        }

        Set<String> pdps = dbsub.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toSet());

        for (ToscaConceptIdentifier policyId : deployed) {
            ToscaPolicy policyToBeDeployed = data.getPolicy(new ToscaConceptIdentifierOptVersion(policyId));
            data.trackDeploy(policyToBeDeployed, pdps, pdpGroup, dbsub.getPdpType());
        }

        for (ToscaConceptIdentifier policyId : undeployed) {
            data.trackUndeploy(policyId, pdps, pdpGroup, dbsub.getPdpType());
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

        var result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

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
     * @return the validation result
     * @throws PfModelException if an error occurred
     */
    private ValidationResult validatePolicies(SessionData data, PdpSubGroup dbsub, DeploymentSubGroup subgrp)
                    throws PfModelException {

        // build a map of the DB data, from policy name to (fully qualified) policy
        // version
        Map<String, String> dbname2vers = new HashMap<>();
        dbsub.getPolicies().forEach(ident -> dbname2vers.put(ident.getName(), ident.getVersion()));

        var result = new BeanValidationResult(subgrp.getPdpType(), subgrp);

        for (ToscaConceptIdentifier ident : subgrp.getPolicies()) {
            // note: "ident" may not have a fully qualified version

            String expectedVersion = dbname2vers.get(ident.getName());
            if (expectedVersion != null) {
                // policy exists in the DB list - compare the versions
                validateVersion(expectedVersion, ident, result);
                ident.setVersion(expectedVersion);
                continue;
            }

            // policy doesn't appear in the DB's policy list - look it up

            ToscaPolicy policy = data.getPolicy(new ToscaConceptIdentifierOptVersion(ident));
            if (policy == null) {
                result.addResult(POLICY_RESULT_NAME, ident, ValidationStatus.INVALID, "unknown policy");

            } else if (!isPolicySupported(dbsub.getSupportedPolicyTypes(), policy.getTypeIdentifier())) {
                result.addResult(POLICY_RESULT_NAME, ident, ValidationStatus.INVALID,
                                "not a supported policy for the subgroup");

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
    private void validateVersion(String dbvers, ToscaConceptIdentifier ident, BeanValidationResult result) {
        String idvers = ident.getVersion();
        if (dbvers.equals(idvers)) {
            return;
        }

        // did not match - see if it's a prefix

        if (SessionData.isVersionPrefix(idvers) && dbvers.startsWith(idvers + ".")) {
            // ident has a prefix of this version
            return;
        }

        result.addResult(POLICY_RESULT_NAME, ident, ValidationStatus.INVALID,
                        "different version already deployed: " + dbvers);
    }

    /**
     * Deploys or updates PDP policies using the simple API.
     *
     * @param policies PDP policies
     * @param user user triggering deployment
     * @throws PfModelException if an error occurred
     */
    public void deployPolicies(PdpDeployPolicies policies, String user) throws PfModelException {
        try {
            MyPdpDeployPolicies checked = coder.convert(policies, MyPdpDeployPolicies.class);
            ValidationResult result = new BeanValidator().validateTop(PdpDeployPolicies.class.getSimpleName(), checked);
            if (!result.isValid()) {
                String msg = result.getResult().trim();
                throw new PfModelException(Status.BAD_REQUEST, msg);
            }
        } catch (CoderException e) {
            throw new PfModelException(Status.INTERNAL_SERVER_ERROR, "cannot decode request", e);
        }

        process(user, policies, this::deploySimplePolicies);
    }

    /**
     * Deploys or updates PDP policies using the simple API. This is the method that does
     * the actual work.
     *
     * @param data session data
     * @param policies external PDP policies
     * @throws PfModelException if an error occurred
     */
    private void deploySimplePolicies(SessionData data, PdpDeployPolicies policies) throws PfModelException {

        for (ToscaConceptIdentifierOptVersion desiredPolicy : policies.getPolicies()) {
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
                    ToscaConceptIdentifierOptVersion requestedIdent) {

        ToscaConceptIdentifier desiredIdent = policy.getIdentifier();
        ToscaConceptIdentifier desiredType = policy.getTypeIdentifier();

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

            logger.info("add policy {} to subgroup {} {} count={}", desiredIdent, group.getName(),
                            subgroup.getPdpType(), subgroup.getPolicies().size());

            Set<String> pdps = subgroup.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toSet());
            ToscaPolicy policyToBeDeployed = data.getPolicy(new ToscaConceptIdentifierOptVersion(desiredIdent));
            data.trackDeploy(policyToBeDeployed, pdps, group.getName(), subgroup.getPdpType());

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
    private boolean isPolicySupported(List<ToscaConceptIdentifier> supportedTypes,
                    ToscaConceptIdentifier desiredType) {

        if (supportedTypes.contains(desiredType)) {
            return true;
        }

        String desiredTypeName = desiredType.getName();
        for (ToscaConceptIdentifier type : supportedTypes) {
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
    private boolean containsPolicy(PdpGroup group, PdpSubGroup subgroup, ToscaConceptIdentifier desiredIdent) {
        String desnm = desiredIdent.getName();
        String desvers = desiredIdent.getVersion();

        for (ToscaConceptIdentifier actualIdent : subgroup.getPolicies()) {
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
            logger.info("subgroup {} {} already contains policy {}", group.getName(), subgroup.getPdpType(),
                        desiredIdent);
            return true;
        }

        return false;
    }

    /*
     * These are only used to validate the incoming request.
     */

    @Getter
    public static class MyPdpDeployPolicies {
        @NotNull
        private List<@NotNull @Valid PolicyIdent> policies;
    }

    @Getter
    public static class PolicyIdent {
        @SerializedName("policy-id")
        @NotNull
        @Pattern(regexp = PfKey.NAME_REGEXP)
        private String name;

        @SerializedName("policy-version")
        @Pattern(regexp = "\\d+([.]\\d+[.]\\d+)?")
        private String version;
    }
}
