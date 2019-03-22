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
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.utils.jpa.EntityMgrCloser;
import org.onap.policy.common.utils.jpa.EntityTransCloser;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.keys.PolicyIdentOptVersion;
import org.onap.policy.models.base.keys.PolicyTypeIdent;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pap.concepts.PdpInstanceDetails;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.simple.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.comm.TimerManager;
import org.onap.policy.pap.main.concepts.PdpGroup;
import org.onap.policy.pap.main.concepts.PdpSubGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PAP component to deploy PDP groups. The following items must be in the
 * {@link Registry}:
 * <ul>
 * <li>PDP Modification Lock</li>
 * <li>PDP Modify Request Map</li>
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
     * Constructs the object.
     */
    public PdpGroupDeployProvider() {
        this.updateLock = Registry.get(PapConstants.REG_PDP_MODIFY_LOCK, Object.class);
        this.requestMap = Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class);
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
     * @param entmgr entity manager
     * @param groups PDP group configurations
     * @return a list of requests that should be sent to configure the PDPs
     */
    private List<Request> deployGroups(EntityManager entmgr, org.onap.policy.models.pap.concepts.PdpGroups groups) {

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
     * @param entmgr entity manager
     * @param policies PDP policies
     * @return a list of requests that should be sent to configure the PDPs
     */
    private List<Request> deploySimplePolicies(EntityManager entmgr, PdpDeployPolicies policies) {

        // list of requests to be published to the PDPs
        List<Request> requests = new ArrayList<>(policies.getPolicies().size());

        for (PolicyIdentOptVersion desiredPolicy : policies.getPolicies()) {

            try {
                requests.addAll(processPolicy(entmgr, desiredPolicy));

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
                    BiFunction<EntityManager, T, List<Request>> deployer) {

        synchronized (updateLock) {
            // list of requests to be published to the PDPs
            List<Request> requests = Collections.emptyList();


            // deploy all policies within a single transaction
            try (EntityMgrCloser mgrCloser = new EntityMgrCloser(getEntityManager());
                            EntityTransCloser transCloser =
                                            new EntityTransCloser(mgrCloser.getManager().getTransaction())) {

                requests = deployer.apply(mgrCloser.getManager(), policies);
                transCloser.commit();

            } catch (RuntimeException e) {
                logger.warn("failed to deploy policies", e);

                PdpGroupDeployResponse resp = new PdpGroupDeployResponse();
                resp.setErrorDetails(e instanceof PolicyPapRuntimeException ? e.getMessage() : "request failed");

                return Pair.of(Response.Status.INTERNAL_SERVER_ERROR, resp);
            }


            // publish the requests
            for (Request req : requests) {
                requestMap.addRequest(req.update, req.stateChange);
            }
        }

        return Pair.of(Response.Status.OK, new PdpGroupDeployResponse());
    }

    /**
     * Process a single policy from the request.
     *
     * @param entmgr entity manager *
     * @param desiredPolicy request policy
     * @return a list of requests that should be sent to configure the PDPs for the policy
     * @throws PolicyPapRuntimeException if an error occurs
     */
    private List<Request> processPolicy(EntityManager entmgr, PolicyIdentOptVersion desiredPolicy)
                    throws PolicyPapRuntimeException {

        if (desiredPolicy == null || desiredPolicy.getName() == null) {
            throw new PolicyPapRuntimeException("null policy");
        }

        ToscaPolicy policy = getPolicy(entmgr, desiredPolicy);

        // identify the PDP types that support the policy
        Set<String> pdpTypes = getPdpTypes(entmgr, policy);
        if (pdpTypes.isEmpty()) {
            throw new PolicyPapRuntimeException("not supported by any PDP type");
        }

        PdpGroup group = getGroup(entmgr, pdpTypes);
        if (group == null) {
            throw new PolicyPapRuntimeException("not supported by any PDP group");

        } else {
            return upgradeGroup(entmgr, policy, pdpTypes, group);
        }
    }

    /**
     * Finds a Policy type that matches the given name and version. If the specified
     * version is {@code null}, then it finds the matching Policy type with the latest
     * version.
     *
     * @param entmgr entity manager
     * @param desiredPolicy the policy desired, with the "name" and optional
     *        "policyVersion" populated
     * @return the matching Policy type
     * @throws PolicyPapRuntimeException if there is no matching policy type
     */
    private ToscaPolicy getPolicy(EntityManager entmgr, PolicyIdentOptVersion desiredPolicy) {
        ToscaPolicy result = null;

        if (desiredPolicy.getVersion() == null) {
            Version maxvers = new Version();
            TypedQuery<ToscaPolicy> query =
                            entmgr.createQuery("select * from ToscaPolicy where name=?", ToscaPolicy.class);
            for (ToscaPolicy policy : query.getResultList()) {
                Version vers = Version.makeVersion("ToscaPolicy", policy.getType().getVersion());
                if (vers != null && vers.compareTo(maxvers) >= 0) {
                    result = policy;
                    maxvers = vers;
                }
            }

        } else {
            TypedQuery<ToscaPolicy> query = entmgr.createQuery("select * from ToscaPolicy where name=? and version=?",
                            ToscaPolicy.class);
            query.setParameter(1, desiredPolicy.getName());
            query.setParameter(2, desiredPolicy.getVersion());
            result = query.getSingleResult();
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
     * @param entmgr entity manager
     * @param policy the desired policy type & version
     * @return the set of PDP types supporting the desired policy type
     * @throws PolicyPapRuntimeException if no PDP types support the policy type
     */
    private Set<String> getPdpTypes(EntityManager entmgr, ToscaPolicy policy) {
        Set<String> pdpTypes = new HashSet<>();
        PfConceptKey key = policy.getType();

        TypedQuery<PdpType> query = entmgr.createQuery("select * from PdpType", PdpType.class);
        for (PdpType pdpType : query.getResultList()) {
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
     * @param entmgr entity manager
     * @param pdpTypes set of PDPs that the group must support
     * @return the matching PDP group, or {@code null} if no active group supports the
     *         given PDP types
     */
    private PdpGroup getGroup(EntityManager entmgr, Set<String> pdpTypes) {
        PdpGroup group = null;
        Version maxvers = new Version();

        TypedQuery<PdpGroup> query = entmgr.createQuery("select * from PdpGroup where state=?", PdpGroup.class);
        query.setParameter(1, PdpState.ACTIVE.name());

        for (PdpGroup group2 : query.getResultList()) {
            TreeSet<String> set = new TreeSet<>(pdpTypes);

            for (PdpSubGroup subgroup : group2.getPdpSubgroups()) {
                set.remove(subgroup.getPdpType());
            }

            if (!set.isEmpty()) {
                continue;
            }

            Version vers = Version.makeVersion("PdpGroup", group2.getVersion());
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
        update.setPdpGroup(group.getName());
        update.setPdpSubgroup(subgroup.getPdpType());
        update.setPolicies(subgroup.getPolicies());

        return update;
    }

    /**
     * Updates a group, assigning a new version number.
     *
     * @param entmgr entity manager
     * @param policy policy type
     * @param pdpTypes PDP types
     * @param oldGroup the original group, to be updated
     * @return a list of requests that should be sent to configure the PDPs for the policy
     */
    private List<Request> upgradeGroup(EntityManager entmgr, ToscaPolicy policy, Set<String> pdpTypes,
                    PdpGroup oldGroup) {

        // TODO create group via entity provider, but must support copy constructor
        PdpGroup newGroup = new PdpGroup(oldGroup);

        newGroup.setVersion(makeNewVersion(entmgr, oldGroup).toString());

        List<Request> requests = new ArrayList<>(newGroup.getPdpSubgroups().size());

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
                requests.add(new Request(update, null));
            }
        }


        if (!requests.isEmpty()) {
            // something changed - persist the new group
            entmgr.persist(newGroup);
        }

        return requests;
    }

    /**
     * Makes a new version for the PDP group.
     *
     * @param entmgr entity manager
     * @param group current group
     * @return a new version
     */
    private Version makeNewVersion(EntityManager entmgr, PdpGroup group) {
        TypedQuery<PdpGroup> query = entmgr.createQuery("select * from PdpGroup where name=?", PdpGroup.class);
        query.setParameter(1, group.getName());

        Version maxvers = new Version();

        for (PdpGroup group2 : query.getResultList()) {
            Version vers = Version.makeVersion("PdpGroup", group2.getVersion());
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

    private static class Request {
        private final PdpUpdate update;
        private final PdpStateChange stateChange;

        public Request(PdpUpdate update, PdpStateChange stateChange) {
            this.update = update;
            this.stateChange = stateChange;
        }
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

        public static Version makeVersion(String type, String versionText) {
            Matcher matcher = VERSION_PAT.matcher(versionText);
            if (!matcher.matches()) {
                logger.info("invalid version for {}: {}", type, versionText);
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
    @Getter
    @Setter
    @ToString
    private static class PdpType {
        private String pdpType;
        private List<PolicyTypeIdent> supportedPolicyTypes;
    }

    // these may be overridden by junit tests

    /**
     * Allocates an entity manager.
     *
     * @return an entity manager
     */
    protected EntityManager getEntityManager() {
        throw new PolicyPapRuntimeException("not implemented yet");
    }
}
