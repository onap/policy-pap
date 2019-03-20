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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
import org.onap.policy.models.pap.concepts.PdpGroup;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pap.concepts.PdpGroups;
import org.onap.policy.models.pap.concepts.PdpInstanceDetails;
import org.onap.policy.models.pap.concepts.PdpPolicies;
import org.onap.policy.models.pap.concepts.PdpSubGroup;
import org.onap.policy.models.pap.concepts.Policy;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.comm.TimerManager;
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
     * Pattern to match the first number in a group version.
     */
    private static final Pattern VERSION_PAT = Pattern.compile("(\\d+)[.](\\d+)[.](\\d+)");

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
     * Deploys or updates a PDP group.
     *
     * @param groups PDP group configuration
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeployResponse> deployGroup(PdpGroups groups) {

        /*
         * TODO Lock for updates - return error if already locked.
         */

        /*
         * TODO Make updates - sending initial messages to PDPs and arranging for
         * listeners to complete the deployment actions (in the background). The final
         * step for the listener is to unlock.
         */

        /*
         * TODO Return error if unable to send updates to all PDPs.
         */

        return Pair.of(Response.Status.OK, new PdpGroupDeployResponse());
    }

    /**
     * Deploys or updates PDP policies.
     *
     * @param policies PDP policies
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeployResponse> deployPolicies(PdpPolicies policies) {

        PdpGroupDeployResponse resp = new PdpGroupDeployResponse();

        synchronized (updateLock) {
            for (Policy desiredPolicy : policies.getPolicies()) {

                try {
                    processPolicy(desiredPolicy);

                    // TODO add success to response

                } catch (RuntimeException ex) {
                    // TODO add failure, ex.getMessage() to response
                }
            }
        }

        return Pair.of(Response.Status.OK, resp);
    }

    /**
     * Process a single policy from the request.
     *
     * @param reqPolicy request policy
     * @throws PolicyPapRuntimeException if an error occurs
     */
    private void processPolicy(Policy reqPolicy) throws PolicyPapRuntimeException {
        if (reqPolicy == null || reqPolicy.getName() == null) {
            throw new PolicyPapRuntimeException("null policy");
        }

        // enclose everything in a transaction
        EntityManager entmgr = getEntityManager();

        try (EntityMgrCloser mgrCloser = new EntityMgrCloser(entmgr);
                        EntityTransCloser transCloser = new EntityTransCloser(entmgr.getTransaction())) {

            // map policy-id & version to policy type & type version
            Policy desiredType = getPolicyType(entmgr, reqPolicy);

            // identify the PDP types that support the policy type
            Set<String> pdpTypes = getPdpTypes(entmgr, desiredType);
            if (pdpTypes.isEmpty()) {
                throw new PolicyPapRuntimeException("not supported by any PDP type");
            }

            PdpGroup group = getGroup(entmgr, pdpTypes);
            if (group == null) {
                createGroup(entmgr, desiredType, pdpTypes);

            } else {
                upgradeGroup(entmgr, desiredType, pdpTypes, group);
            }
        }
    }

    /**
     * Finds a Policy type that matches the given name and version. If the specified
     * version is {@code null}, then it finds the matching Policy type with the latest
     * version.
     *
     * @param entmgr entity manager
     * @param desired the policy desired, with the "name" and optional "policyVersion"
     *        populated
     * @return the matching Policy type
     * @throws PolicyPapRuntimeException if there is no matching policy type
     */
    private Policy getPolicyType(EntityManager entmgr, Policy desired) {
        Policy result = null;

        if (desired.getPolicyVersion() == null) {
            Version maxvers = new Version();
            TypedQuery<Policy> query = entmgr.createQuery("select * from PolicyType where name=?", Policy.class);
            for (Policy type : query.getResultList()) {
                Version vers = Version.makeVersion("PolicyType", type.getPolicyTypeVersion());
                if (vers != null && vers.compareTo(maxvers) >= 0) {
                    result = type;
                    maxvers = vers;
                }
            }

        } else {
            TypedQuery<Policy> query = entmgr.createQuery("select * from PolicyType where name=? and policyVersion=?",
                            Policy.class);
            query.setParameter(1, desired.getName());
            query.setParameter(2, desired.getPolicyVersion());
            result = query.getSingleResult();
        }

        if (result == null) {
            throw new PolicyPapRuntimeException("no policy type for policy-id=" + desired.getName() + " and version="
                            + desired.getPolicyVersion());
        }

        return result;
    }

    /**
     * Finds the PDP types that support the desired policy type and version.
     *
     * @param entmgr entity manager
     * @param type the desired policy type & version
     * @return the set of PDP types supporting the desired policy type
     * @throws PolicyPapRuntimeException if no PDP types support the policy type
     */
    private Set<String> getPdpTypes(EntityManager entmgr, Policy type) {
        Set<String> pdpTypes = new HashSet<>();
        PfConceptKey key = new PfConceptKey(type.getPolicyType(), type.getPolicyTypeVersion());

        TypedQuery<PdpType> query = entmgr.createQuery("select * from PdpType", PdpType.class);
        for (PdpType pdpType : query.getResultList()) {
            if (pdpType.getSupportedPolicyTypes().stream().anyMatch(key::equals)) {
                pdpTypes.add(pdpType.getPdpType());
            }
        }

        if (pdpTypes.isEmpty()) {
            throw new PolicyPapRuntimeException("no PDP types support policy-type=" + type.getPolicyType()
                            + " and version=" + type.getPolicyVersion());
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
     * Creates a new group having the specified PDP types and supporting the given policy
     * type.
     *
     * @param entmgr entity manager
     * @param type policy type
     * @param pdpTypes PDP types
     */
    private void createGroup(EntityManager entmgr, Policy type, Set<String> pdpTypes) {
        // TODO create group via entity provider
        PdpGroup group = new PdpGroup();

        // TODO how to generate the name?
        group.setName(type.getPolicyType() + "-" + UUID.randomUUID().toString());

        group.setVersion(new Version(1, 0, 0).toString());
        group.setPdpSubgroups(new ArrayList<>(pdpTypes.size()));

        for (String pdpType : pdpTypes) {
            addSubgroup(entmgr, group, type, pdpType);
        }

        entmgr.persist(group);

        for (PdpSubGroup subgroup : group.getPdpSubgroups()) {
            for (PdpInstanceDetails pdpInstance : subgroup.getPdpInstances()) {
                PdpUpdate update = makeUpdate(group, subgroup, pdpInstance);
                PdpStateChange stateChange = makeActivate(pdpInstance);
                requestMap.addRequest(update, stateChange);
            }
        }
    }

    /**
     * Adds a subgroup to a group.
     *
     * @param entmgr entity manager
     * @param group group to which the subgroup should be added
     * @param type the policy type
     * @param pdpType type of PDP associated with the subgroup
     * @throws PolicyPapRuntimeException if no PDPs are available
     */
    private void addSubgroup(EntityManager entmgr, PdpGroup group, Policy type, String pdpType) {
        // TODO create subgroup via entity provider
        PdpSubGroup subgroup = new PdpSubGroup();

        TypedQuery<PdpType> query = entmgr.createQuery("select * from PdpType where name=?", PdpType.class);
        query.setParameter(1, pdpType);
        subgroup.setSupportedPolicyTypes(query.getSingleResult().getSupportedPolicyTypes());

        subgroup.setPdpType(pdpType);

        // TODO create policy via entity provider
        Policy policy = new Policy();
        policy.setName(type.getName());
        policy.setPolicyType(type.getPolicyType());
        policy.setPolicyTypeVersion(type.getPolicyTypeVersion());
        policy.setPolicyTypeImpl(type.getPolicyTypeImpl());
        subgroup.setPolicies(Arrays.asList(policy));

        // TODO how to populate these? from policy type object? from group properties?
        Map<String, String> properties = new HashMap<>();
        subgroup.setProperties(properties);

        // TODO set counts
        subgroup.setCurrentInstanceCount(1);
        subgroup.setDesiredInstanceCount(1);

        PdpInstanceDetails inst = allocPdp(entmgr, subgroup);
        if (inst == null) {
            // TODO throw exception here? add an empty subgroup? skip the subgroup?
            throw new PolicyPapRuntimeException("no PDPs available of type " + pdpType);
        }

        subgroup.setPdpInstances(Arrays.asList(inst));

        // TODO needed?
        entmgr.persist(policy);
        entmgr.persist(subgroup);

        group.getPdpSubgroups().add(subgroup);
    }

    /**
     * Allocates a PDP for a subgroup.
     *
     * @param entmgr entity manager
     * @param subgroup subgroup for which the PDP should be allocated
     * @return the allocated PDP, or {@code null} if no PDPs of the desired type are
     *         available
     */
    private PdpInstanceDetails allocPdp(EntityManager entmgr, PdpSubGroup subgroup) {
        TypedQuery<PdpInstance> query =
                        entmgr.createQuery("select * from PDP where type=? and state=?", PdpInstance.class);
        query.setParameter(1, subgroup.getPdpType());
        query.setParameter(2, PdpState.PASSIVE);

        try {
            PdpInstance inst = query.getSingleResult();
            if (inst == null) {
                logger.warn("cannot allocate a PDP of type {}", subgroup.getPdpType());
                return null;
            }

            // TODO create details via entity provider
            return new PdpInstanceDetails(inst);

        } catch (NoResultException e) {
            logger.warn("cannot allocate a PDP of type {}", subgroup.getPdpType(), e);
            return null;
        }
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
        update.setPolicies(subgroup.getPolicies().stream().map(this::translatePolicy).collect(Collectors.toList()));

        return update;
    }

    /**
     * Makes an STATE-CHANGE message to activate a particular PDP.
     *
     * @param pdpInstance identifies the PDP of interest
     * @return a new UPDATE message
     */
    private PdpStateChange makeActivate(PdpInstanceDetails pdpInstance) {
        PdpStateChange change = new PdpStateChange();

        change.setName(pdpInstance.getInstanceId());
        change.setState(PdpState.ACTIVE);

        return change;
    }

    /**
     * Translates an external policy to an internal policy.
     *
     * @param externalPolicy the external policy
     * @return a new, internal policy
     */
    private org.onap.policy.models.pdp.concepts.Policy translatePolicy(Policy externalPolicy) {
        org.onap.policy.models.pdp.concepts.Policy internalPolicy = new org.onap.policy.models.pdp.concepts.Policy();

        internalPolicy.setName(externalPolicy.getName());
        internalPolicy.setPolicyType(externalPolicy.getPolicyType());
        internalPolicy.setPolicyTypeVersion(externalPolicy.getPolicyTypeVersion());
        internalPolicy.setPolicyVersion(externalPolicy.getPolicyVersion());

        // properties

        return internalPolicy;
    }

    /**
     * Updates a group, assigning a new version number.
     *
     * @param entmgr entity manager
     * @param type policy type
     * @param pdpTypes PDP types
     * @param oldGroup the original group, to be updated
     */
    private void upgradeGroup(EntityManager entmgr, Policy type, Set<String> pdpTypes, PdpGroup oldGroup) {

        // TODO create group via entity provider, but must support copy constructor
        PdpGroup group = new PdpGroup(oldGroup);

        group.setVersion(makeNewVersion(entmgr, oldGroup).toString());

        boolean changed = false;

        for (PdpSubGroup subgroup : group.getPdpSubgroups()) {
            if (pdpTypes.contains(subgroup.getPdpType()) && !hasTypeAndVersion(subgroup, type)) {
                /*
                 * this subgroup has the correct PDP type, but is missing this particular
                 * policy type and version - add it
                 */
                subgroup.getPolicies().add(type);
                changed = true;
            }
        }

        if (!changed) {
            // TODO need to do anything to discard the newly allocated group from JPA?
            return;
        }

        entmgr.persist(group);

        for (PdpSubGroup subgroup : group.getPdpSubgroups()) {
            for (PdpInstanceDetails pdpInstance : subgroup.getPdpInstances()) {
                PdpUpdate update = makeUpdate(group, subgroup, pdpInstance);
                PdpStateChange stateChange = makeActivate(pdpInstance);
                requestMap.addRequest(update, stateChange);
            }
        }
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
     * @param desiredType desired policy type and version
     * @return {@code true} if the subgroup contains the policy type, {@code false}
     *         otherwise
     */
    private boolean hasTypeAndVersion(PdpSubGroup subgroup, Policy desiredType) {
        for (Policy type : subgroup.getPolicies()) {
            if (desiredType.getName().equals(type.getPolicyType())
                            && desiredType.getPolicyTypeVersion().equals(type.getPolicyTypeVersion())) {
                return true;
            }
        }

        return false;
    }

    // these may be overridden by junit tests

    /**
     * Allocates an entity manager.
     * @return an entity manager
     */
    protected EntityManager getEntityManager() {
        // TODO Auto-generated method stub
        return null;
    }

    // TODO add to model?
    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor(force = true)
    @RequiredArgsConstructor
    private static class Version implements Comparable<Version> {
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
                return new Version(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                                Integer.parseInt(matcher.group(3)));

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
        private List<PfConceptKey> supportedPolicyTypes;
    }

    // TODO add to model
    @Getter
    @Setter
    @ToString
    private static class PdpInstance extends PdpInstanceDetails {
        private String type;
    }
}
