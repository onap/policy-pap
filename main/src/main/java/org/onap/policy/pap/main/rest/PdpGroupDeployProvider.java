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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.utils.dao.Transaction;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.keys.PolicyIdentOptVersion;
import org.onap.policy.models.base.keys.PolicyTypeIdent;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pap.concepts.PdpInstanceDetails;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.simple.concepts.ToscaEntityType;
import org.onap.policy.models.tosca.simple.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.comm.TimerManager;
import org.onap.policy.pap.main.dao.PapDao;
import org.onap.policy.pap.main.dao.PapDaoFactory;
import org.onap.policy.pap.main.internal.PdpGroup;
import org.onap.policy.pap.main.internal.PdpSubGroup;
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
public class PdpGroupDeployProvider {
    private static final Logger logger = LoggerFactory.getLogger(TimerManager.class);

    /**
     * Lock used when updating PDPs.
     */
    private final Object updateLock;

    /**
     * Used to send UPDATE and STATE-CHANGE requests to the PDPs.
     */
    private final PdpModifyRequestMap requestMap;

    /**
     * Factory for PAP DAO.
     */
    private final PapDaoFactory daoFactory;


    /**
     * Constructs the object.
     */
    public PdpGroupDeployProvider() {
        this.updateLock = Registry.get(PapConstants.REG_PDP_MODIFY_LOCK, Object.class);
        this.requestMap = Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class);
        this.daoFactory = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PapDaoFactory.class);
    }

    /**
     * Deploys or updates PDP groups.
     *
     * @param groups PDP group configurations
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeployResponse> deployGroup(
                    org.onap.policy.models.pap.concepts.PdpGroups groups) {

        return deploy(groups, this::deployGroups);
    }

    /**
     * Deploys or updates PDP groups.
     *
     * @param dao entity manager
     * @param groups PDP group configurations
     * @return a list of requests that should be sent to configure the PDPs
     */
    private List<PdpUpdate> deployGroups(PapDao dao, org.onap.policy.models.pap.concepts.PdpGroups groups) {

        // TODO update each group

        throw new PolicyPapRuntimeException("not implemented yet");
    }

    /**
     * Deploys or updates PDP policies.
     *
     * @param policies PDP policies
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeployResponse> deployPolicies(PdpDeployPolicies policies) {

        return deploy(policies, this::deploySimplePolicies);
    }

    /**
     * Deploys or updates PDP policies using the simple API.
     *
     * @param dao entity manager
     * @param policies PDP policies
     * @return a list of requests that should be sent to configure the PDPs
     */
    private List<PdpUpdate> deploySimplePolicies(PapDao dao, PdpDeployPolicies policies) {

        // list of requests to be published to the PDPs
        List<PdpUpdate> requests = new ArrayList<>(policies.getPolicies().size());

        for (PolicyIdentOptVersion desiredPolicy : policies.getPolicies()) {

            try {
                requests.addAll(processPolicy(dao, desiredPolicy));

            } catch (RuntimeException e) {
                // no need to log the error here, as it will be logged by the invoker
                logger.warn("failed to deploy policy: {}", desiredPolicy);
                throw e;
            }
        }

        return requests;
    }

    /**
     * Deploys or updates PDP policies.
     *
     * @param policies PDP policies
     * @param deployer function that deploys the policies and returns a list of PDP
     *        requests
     * @return a pair containing the status and the response
     */
    private <T> Pair<Response.Status, PdpGroupDeployResponse> deploy(T policies,
                    BiFunction<PapDao, T, List<PdpUpdate>> deployer) {

        synchronized (updateLock) {
            // list of requests to be published to the PDPs
            List<PdpUpdate> requests = Collections.emptyList();


            // deploy all policies within a single transaction
            try (PapDao dao = daoFactory.create(); Transaction trans = dao.beginTransaction()) {

                requests = deployer.apply(dao, policies);
                trans.commit();

            } catch (Exception e) {
                logger.warn("failed to deploy policies", e);

                PdpGroupDeployResponse resp = new PdpGroupDeployResponse();
                resp.setErrorDetails(e instanceof PolicyPapRuntimeException ? e.getMessage() : "request failed");

                return Pair.of(Response.Status.INTERNAL_SERVER_ERROR, resp);
            }


            // publish the requests
            for (PdpUpdate req : requests) {
                requestMap.addRequest(req);
            }
        }

        return Pair.of(Response.Status.OK, new PdpGroupDeployResponse());
    }

    /**
     * Process a single policy from the request.
     *
     * @param dao entity manager
     * @param desiredPolicy request policy
     * @return a list of requests that should be sent to configure the PDPs for the policy
     * @throws PolicyPapRuntimeException if an error occurs
     */
    private List<PdpUpdate> processPolicy(PapDao dao, PolicyIdentOptVersion desiredPolicy)
                    throws PolicyPapRuntimeException {

        if (desiredPolicy == null || desiredPolicy.getName() == null) {
            throw new PolicyPapRuntimeException("null policy");
        }

        ToscaPolicy policy = getPolicy(dao, desiredPolicy);

        // identify the PDP types that support the policy
        Set<String> pdpTypes = getPdpTypes(dao, policy);
        if (pdpTypes.isEmpty()) {
            throw new PolicyPapRuntimeException("not supported by any PDP type");
        }

        PdpGroup group = getGroup(dao, pdpTypes);
        if (group == null) {
            throw new PolicyPapRuntimeException("not supported by any PDP group");

        } else {
            return upgradeGroup(dao, policy, pdpTypes, group);
        }
    }

    /**
     * Finds a Policy type that matches the given name and version. If the specified
     * version is {@code null}, then it finds the matching Policy type with the latest
     * version.
     *
     * @param dao entity manager
     * @param desiredPolicy the policy desired, with the "name" and optional
     *        "policyVersion" populated
     * @return the matching Policy type
     * @throws PolicyPapRuntimeException if there is no matching policy type
     */
    private ToscaPolicy getPolicy(PapDao dao, PolicyIdentOptVersion desiredPolicy) {
        ToscaPolicy result = null;
        Version maxvers = new Version();

        for (ToscaPolicy policy : dao.getAll(ToscaPolicy.class, desiredPolicy)) {
            Version vers = Version.makeVersion("ToscaPolicy", policy.getType().getName(),
                            policy.getType().getVersion());
            if (vers != null && vers.compareTo(maxvers) >= 0) {
                result = policy;
                maxvers = vers;
            }
        }

        if (result == null) {
            throw new PolicyPapRuntimeException("no policy type for policy-id=" + desiredPolicy.getName()
                            + " and version=" + desiredPolicy.getVersion());
        }

        return result;
    }

    /**
     * Finds the PDP types that support the desired policy type and version.
     *
     * @param dao entity manager
     * @param policy the desired policy type & version
     * @return the set of PDP types supporting the desired policy type
     * @throws PolicyPapRuntimeException if no PDP types support the policy type
     */
    private Set<String> getPdpTypes(PapDao dao, ToscaPolicy policy) {
        Set<String> pdpTypes = new HashSet<>();
        PfConceptKey key = policy.getType();

        for (PdpType pdpType : dao.getAll(PdpType.class)) {
            if (pdpType.getSupportedPolicyTypes().stream().anyMatch(key::equals)) {
                pdpTypes.add(pdpType.getPdpType());
            }
        }

        if (pdpTypes.isEmpty()) {
            throw new PolicyPapRuntimeException(
                            "no PDP types support policy-type=" + key.getName() + " and version=" + key.getVersion());
        }

        return pdpTypes;
    }

    /**
     * Finds the active PDP group with the highest version that supports the given PDP
     * types.
     *
     * @param dao entity manager
     * @param pdpTypes set of PDPs that the group must support
     * @return the matching PDP group, or {@code null} if no active group supports the
     *         given PDP types
     */
    private PdpGroup getGroup(PapDao dao, Set<String> pdpTypes) {
        PdpGroup group = null;
        Version maxvers = new Version();

        for (PdpGroup group2 : dao.getAll(PdpGroup.class)) {
            if (group2.getPdpGroupState() != PdpState.ACTIVE) {
                continue;
            }

            TreeSet<String> set = new TreeSet<>(pdpTypes);
            for (PdpSubGroup subgroup : group2.getPdpSubgroups()) {
                set.remove(subgroup.getPdpType());
            }

            if (!set.isEmpty()) {
                // this group doesn't support all of the desired PDP types
                continue;
            }

            Version vers = Version.makeVersion("PdpGroup", group2.getKey().getName(), group2.getKey().getVersion());
            if (vers != null && vers.compareTo(maxvers) >= 0) {
                maxvers = vers;
                group = group2;
            }
        }

        return group;
    }

    /**
     * Makes an UPDATE message for a particular PDP.
     *
     * @param group group to which the PDP should belong
     * @param subgroup subgroup to which the PDP should belong
     * @param pdpInstance identifies the PDP of interest
     * @return a new UPDATE message
     */
    private PdpUpdate makeUpdate(PdpGroup group, PdpSubGroup subgroup, PdpInstanceDetails pdpInstance) {
        PdpUpdate update = new PdpUpdate();

        update.setName(pdpInstance.getInstanceId());

        // TODO description

        update.setPdpType(subgroup.getPdpType());
        update.setPdpGroup(group.getKey().getName());
        update.setPdpSubgroup(subgroup.getPdpType());
        update.setPolicies(subgroup.getPolicies());

        return update;
    }

    /**
     * Updates a group, assigning a new version number.
     *
     * @param dao entity manager
     * @param policy policy type
     * @param pdpTypes PDP types
     * @param oldGroup the original group, to be updated
     * @return a list of requests that should be sent to configure the PDPs for the policy
     */
    private List<PdpUpdate> upgradeGroup(PapDao dao, ToscaPolicy policy, Set<String> pdpTypes, PdpGroup oldGroup) {

        PdpGroup newGroup = new PdpGroup(oldGroup);
        newGroup.getKey().setVersion(makeNewVersion(dao, oldGroup).toString());

        List<PdpUpdate> requests = new ArrayList<>(newGroup.getPdpSubgroups().size());

        for (PdpSubGroup subgroup : newGroup.getPdpSubgroups()) {

            if (!pdpTypes.contains(subgroup.getPdpType()) || hasTypeAndVersion(subgroup, policy)) {
                continue;
            }

            /*
             * this subgroup has the correct PDP type, but is missing this particular
             * policy/version - add it
             */
            subgroup.getPolicies().add(policy);

            /*
             * generate an UPDATE for each PDP instance. Since the group is active, we
             * assume that the PDP is, too, thus no need for a STATE-CHANGE.
             */
            for (PdpInstanceDetails pdpInstance : subgroup.getPdpInstances()) {
                PdpUpdate update = makeUpdate(newGroup, subgroup, pdpInstance);
                requests.add(update);
            }
        }


        if (!requests.isEmpty()) {
            // something changed - persist the new group
            dao.create(newGroup);
        }

        return requests;
    }

    /**
     * Makes a new version for the PDP group.
     *
     * @param dao entity manager
     * @param group current group
     * @return a new version
     */
    private Version makeNewVersion(PapDao dao, PdpGroup group) {
        PfConceptKey key = new PfConceptKey();
        key.setName(group.getKey().getName());

        Version maxvers = new Version();

        for (PdpGroup group2 : dao.getAll(PdpGroup.class, key)) {
            Version vers = Version.makeVersion("PdpGroup", group2.getKey().getName(), group2.getKey().getVersion());
            if (vers != null && vers.compareTo(maxvers) > 0) {
                maxvers = vers;
            }
        }

        return maxvers.newVersion();
    }

    /**
     * Determines if a subgroup contains the desired policy type and version.
     *
     * @param subgroup subgroup to check
     * @param desiredPolicy desired policy
     * @return {@code true} if the subgroup contains the policy type, {@code false}
     *         otherwise
     */
    private boolean hasTypeAndVersion(PdpSubGroup subgroup, ToscaPolicy desiredPolicy) {
        for (ToscaPolicy policy : subgroup.getPolicies()) {
            if (policy.getKey().equals(desiredPolicy.getKey())) {
                return true;
            }
        }

        return false;
    }

    // TODO add to model?
    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor(force = true)
    @RequiredArgsConstructor
    private static class Version implements Comparable<Version> {

        /**
         * Pattern to match a version of the form, major or major.minor.patch, where all
         * components are numeric.
         */
        private static final Pattern VERSION_PAT = Pattern.compile("(\\d+)([.](\\d+)[.](\\d+))?");

        private final int major;
        private final int minor;
        private final int patch;

        public static Version makeVersion(String type, String ident, String versionText) {
            Matcher matcher = VERSION_PAT.matcher(versionText);
            if (!matcher.matches()) {
                logger.info("invalid version for {} {}: {}", type, ident, versionText);
                return null;
            }

            try {
                if (matcher.group(2) != null) {
                    // form: major.minor.patch
                    return new Version(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(3)),
                                    Integer.parseInt(matcher.group(4)));

                } else {
                    // form: major
                    return new Version(Integer.parseInt(matcher.group(1)), 0, 0);
                }

            } catch (NumberFormatException e) {
                logger.info("invalid version for {}: {}", type, versionText, e);
                return null;
            }
        }

        public Version newVersion() {
            return new Version(major + 1, minor, patch);
        }

        @Override
        public int compareTo(Version other) {
            int result = Integer.compare(major, other.major);
            if (result != 0) {
                return result;
            }
            if ((result = Integer.compare(minor, other.minor)) != 0) {
                return result;
            }
            return Integer.compare(patch, other.patch);
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
    }

    // TODO add to model
    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class PdpType extends ToscaEntityType {
        private static final long serialVersionUID = 1L;

        // TODO merge fields with superclass

        private String pdpType;
        private List<PolicyTypeIdent> supportedPolicyTypes;
    }
}
