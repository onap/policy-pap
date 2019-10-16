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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyFilter;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyFilter.ToscaPolicyFilterBuilder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.notification.PolicyPdpNotificationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data used during a single REST call when updating PDP policies.
 */
public class SessionData {
    private static final Logger logger = LoggerFactory.getLogger(SessionData.class);

    /**
     * If a version string matches this, then it is just a prefix (i.e., major or
     * major.minor).
     */
    private static final Pattern VERSION_PREFIX_PAT = Pattern.compile("[^.]+(?:[.][^.]+)?");

    /**
     * DB provider.
     */
    private final PolicyModelsProvider dao;

    /**
     * Maps a group name to its group data. This accumulates the set of groups to be
     * created and updated when the REST call completes.
     */
    private final Map<String, GroupData> groupCache = new HashMap<>();

    /**
     * Maps a policy type to the list of matching groups. Every group appearing within
     * this map has a corresponding entry in {@link #groupCache}.
     */
    private final Map<ToscaPolicyTypeIdentifier, List<GroupData>> type2groups = new HashMap<>();

    /**
     * Maps a PDP name to its most recently generated update and state-change requests.
     */
    private final Map<String, Pair<PdpUpdate, PdpStateChange>> pdpRequests = new HashMap<>();

    /**
     * Maps a policy's identifier to the policy.
     */
    private final Map<ToscaPolicyIdentifierOptVersion, ToscaPolicy> policyCache = new HashMap<>();

    /**
     * Maps a policy type's identifier to the policy.
     */
    private final Map<ToscaPolicyTypeIdentifier, ToscaPolicyType> typeCache = new HashMap<>();

    /**
     * Policies to be deployed.
     */
    private final Map<ToscaPolicyIdentifier, PolicyPdpNotificationData> deploy = new HashMap<>();

    /**
     * Policies to be undeployed.
     */
    private final Map<ToscaPolicyIdentifier, PolicyPdpNotificationData> undeploy = new HashMap<>();


    /**
     * Constructs the object.
     *
     * @param dao DAO provider
     */
    public SessionData(PolicyModelsProvider dao) {
        this.dao = dao;
    }

    /**
     * Gets the policy type, referenced by an identifier. Loads it from the cache, if
     * possible. Otherwise, gets it from the DB.
     *
     * @param desiredType policy type identifier
     * @return the specified policy type
     * @throws PfModelException if an error occurred
     */
    public ToscaPolicyType getPolicyType(ToscaPolicyTypeIdentifier desiredType) throws PfModelException {

        ToscaPolicyType type = typeCache.get(desiredType);
        if (type == null) {

            List<ToscaPolicyType> lst = dao.getPolicyTypeList(desiredType.getName(), desiredType.getVersion());
            if (lst.isEmpty()) {
                return null;
            }

            type = lst.get(0);
            typeCache.put(desiredType, type);
        }

        return type;
    }

    /**
     * Gets the policy, referenced by an identifier. Loads it from the cache, if possible.
     * Otherwise, gets it from the DB.
     *
     * @param desiredPolicy policy identifier
     * @return the specified policy
     * @throws PfModelException if an error occurred
     */
    public ToscaPolicy getPolicy(ToscaPolicyIdentifierOptVersion desiredPolicy) throws PfModelException {

        ToscaPolicy policy = policyCache.get(desiredPolicy);
        if (policy == null) {
            ToscaPolicyFilterBuilder filterBuilder = ToscaPolicyFilter.builder().name(desiredPolicy.getName());
            setPolicyFilterVersion(filterBuilder, desiredPolicy.getVersion());

            List<ToscaPolicy> lst = dao.getFilteredPolicyList(filterBuilder.build());
            if (lst.isEmpty()) {
                return null;
            }

            policy = lst.get(0);
            policyCache.put(desiredPolicy, policy);
        }

        // desired version may have only been a prefix - cache with full identifier, too
        policyCache.putIfAbsent(new ToscaPolicyIdentifierOptVersion(policy.getIdentifier()), policy);

        return policy;
    }

    /**
     * Sets the "version" in a policy filter.
     *
     * @param filterBuilder filter builder whose version should be set
     * @param desiredVersion desired version
     */
    private void setPolicyFilterVersion(ToscaPolicyFilterBuilder filterBuilder, String desiredVersion) {

        if (desiredVersion == null) {
            // no version specified - get the latest
            filterBuilder.version(ToscaPolicyFilter.LATEST_VERSION);

        } else if (isVersionPrefix(desiredVersion)) {
            // version prefix provided - match the prefix and then pick the latest
            filterBuilder.versionPrefix(desiredVersion + ".").version(ToscaPolicyFilter.LATEST_VERSION);

        } else {
            // must be an exact match
            filterBuilder.version(desiredVersion);
        }
    }

    /**
     * Determines if a version contains only a prefix.
     *
     * @param version version to inspect
     * @return {@code true} if the version contains only a prefix, {@code false} if it is
     *         fully qualified
     */
    public static boolean isVersionPrefix(String version) {
        return VERSION_PREFIX_PAT.matcher(version).matches();
    }

    /**
     * Adds an update and state-change to the sets, replacing any previous entries for the
     * given PDP.
     *
     * @param update the update to be added
     * @param change the state-change to be added
     */
    public void addRequests(PdpUpdate update, PdpStateChange change) {
        if (!update.getName().equals(change.getName())) {
            throw new IllegalArgumentException("PDP name mismatch " + update.getName() + ", " + change.getName());
        }

        logger.info("add update and state-change {} {} {} policies={}", update.getName(), update.getPdpGroup(),
                        update.getPdpSubgroup(), update.getPolicies().size());
        pdpRequests.put(update.getName(), Pair.of(update, change));
    }

    /**
     * Adds an update to the set of updates, replacing any previous entry for the given
     * PDP.
     *
     * @param update the update to be added
     */
    public void addUpdate(PdpUpdate update) {
        logger.info("add update {} {} {} policies={}", update.getName(), update.getPdpGroup(), update.getPdpSubgroup(),
                        update.getPolicies().size());
        pdpRequests.compute(update.getName(), (name, data) -> Pair.of(update, (data == null ? null : data.getRight())));
    }

    /**
     * Adds a state-change to the set of state-change requests, replacing any previous
     * entry for the given PDP.
     *
     * @param change the state-change to be added
     */
    public void addStateChange(PdpStateChange change) {
        logger.info("add state-change {}", change.getName());
        pdpRequests.compute(change.getName(), (name, data) -> Pair.of((data == null ? null : data.getLeft()), change));
    }

    /**
     * Determines if any changes were made due to the REST call.
     *
     * @return {@code true} if nothing was changed, {@code false} if something was changed
     */
    public boolean isUnchanged() {
        return groupCache.values().stream().allMatch(GroupData::isUnchanged);
    }

    /**
     * Gets the accumulated PDP requests.
     *
     * @return the PDP requests
     */
    public Collection<Pair<PdpUpdate, PdpStateChange>> getPdpRequests() {
        return pdpRequests.values();
    }

    /**
     * Gets the accumulated PDP update requests.
     *
     * @return the PDP requests
     */
    public List<PdpUpdate> getPdpUpdates() {
        return pdpRequests.values().stream().filter(req -> req.getLeft() != null).map(Pair::getLeft)
                        .collect(Collectors.toList());
    }

    /**
     * Gets the accumulated PDP state-change requests.
     *
     * @return the PDP requests
     */
    public List<PdpStateChange> getPdpStateChanges() {
        return pdpRequests.values().stream().filter(req -> req.getRight() != null).map(Pair::getRight)
                        .collect(Collectors.toList());
    }

    /**
     * Creates a group.
     *
     * @param newGroup the new group
     */
    public void create(PdpGroup newGroup) {
        String name = newGroup.getName();

        if (groupCache.put(name, new GroupData(newGroup, true)) != null) {
            throw new IllegalStateException("group already cached: " + name);
        }

        logger.info("create cached group {}", newGroup.getName());
    }

    /**
     * Updates a group.
     *
     * @param newGroup the updated group
     */
    public void update(PdpGroup newGroup) {
        String name = newGroup.getName();
        GroupData data = groupCache.get(name);
        if (data == null) {
            throw new IllegalStateException("group not cached: " + name);
        }

        logger.info("update cached group {}", newGroup.getName());
        data.update(newGroup);
    }

    /**
     * Gets the group by the given name.
     *
     * @param name name of the group to get
     * @return the group, or {@code null} if it does not exist
     * @throws PfModelException if an error occurred
     */
    public PdpGroup getGroup(String name) throws PfModelException {

        GroupData data = groupCache.get(name);
        if (data == null) {
            List<PdpGroup> lst = dao.getPdpGroups(name);
            if (lst.isEmpty()) {
                logger.info("unknown group {}", name);
                return null;
            }

            logger.info("cache group {}", name);
            data = new GroupData(lst.get(0));
            groupCache.put(name, data);

        } else {
            logger.info("use cached group {}", name);
        }

        return data.getGroup();
    }

    /**
     * Gets the active groups supporting the given policy.
     *
     * @param type desired policy type
     * @return the active groups supporting the given policy
     * @throws PfModelException if an error occurred
     */
    public List<PdpGroup> getActivePdpGroupsByPolicyType(ToscaPolicyTypeIdentifier type) throws PfModelException {
        List<GroupData> data = type2groups.get(type);
        if (data == null) {
            PdpGroupFilter filter = PdpGroupFilter.builder().policyTypeList(Collections.singletonList(type))
                            .groupState(PdpState.ACTIVE).build();

            List<PdpGroup> groups = dao.getFilteredPdpGroups(filter);

            data = groups.stream().map(this::addGroup).collect(Collectors.toList());
            type2groups.put(type, data);
        }

        return data.stream().map(GroupData::getGroup).collect(Collectors.toList());
    }

    /**
     * Adds a group to the group cache, if it isn't already in the cache.
     *
     * @param group the group to be added
     * @return the cache entry
     */
    private GroupData addGroup(PdpGroup group) {
        GroupData data = groupCache.get(group.getName());
        if (data == null) {
            logger.info("cache group {}", group.getName());
            data = new GroupData(group);
            groupCache.put(group.getName(), data);

        } else {
            logger.info("use cached group {}", group.getName());
        }

        return data;
    }

    /**
     * Update the DB with the changes.
     *
     * @throws PfModelException if an error occurred
     */
    public void updateDb() throws PfModelException {
        // create new groups
        List<GroupData> created = groupCache.values().stream().filter(GroupData::isNew).collect(Collectors.toList());
        if (!created.isEmpty()) {
            if (logger.isInfoEnabled()) {
                created.forEach(group -> logger.info("creating DB group {}", group.getGroup().getName()));
            }
            dao.createPdpGroups(created.stream().map(GroupData::getGroup).collect(Collectors.toList()));
        }

        // update existing groups
        List<GroupData> updated =
                        groupCache.values().stream().filter(GroupData::isUpdated).collect(Collectors.toList());
        if (!updated.isEmpty()) {
            if (logger.isInfoEnabled()) {
                updated.forEach(group -> logger.info("updating DB group {}", group.getGroup().getName()));
            }
            dao.updatePdpGroups(updated.stream().map(GroupData::getGroup).collect(Collectors.toList()));
        }
    }

    /**
     * Deletes a group from the DB, immediately (i.e., without caching the request to be
     * executed later).
     *
     * @param group the group to be deleted
     * @throws PfModelException if an error occurred
     */
    public void deleteGroupFromDb(PdpGroup group) throws PfModelException {
        logger.info("deleting DB group {}", group.getName());
        dao.deletePdpGroup(group.getName());
    }

    /**
     * Adds policy deployment data.
     *
     * @param policyId ID of the policy being deployed
     * @param pdps PDPs to which the policy is being deployed
     * @throws PfModelException if an error occurred
     */
    protected void trackDeploy(ToscaPolicyIdentifier policyId, Set<String> pdps) throws PfModelException {

        addData(policyId, pdps, deploy, undeploy);
    }

    /**
     * Adds policy undeployment data.
     *
     * @param policyId ID of the policy being undeployed
     * @param pdps PDPs to which the policy is being undeployed
     * @throws PfModelException if an error occurred
     */
    protected void trackUndeploy(ToscaPolicyIdentifier policyId, Set<String> pdps) throws PfModelException {

        addData(policyId, pdps, undeploy, deploy);
    }

    /**
     * Adds policy deployment/undeployment data.
     *
     * @param policyId ID of the policy being deployed/undeployed
     * @param pdps PDPs to which the policy is being deployed/undeployed
     * @param addMap map to which it should be added
     * @param removeMap map from which it should be removed
     * @throws PfModelException if an error occurred
     */
    private void addData(ToscaPolicyIdentifier policyId, Set<String> pdps,
                    Map<ToscaPolicyIdentifier, PolicyPdpNotificationData> addMap,
                    Map<ToscaPolicyIdentifier, PolicyPdpNotificationData> removeMap) throws PfModelException {

        PolicyPdpNotificationData removeData = removeMap.get(policyId);
        if (removeData != null) {
            removeData.removeAll(pdps);
        }

        ToscaPolicyIdentifierOptVersion optid = new ToscaPolicyIdentifierOptVersion(policyId);
        ToscaPolicyTypeIdentifier policyType = getPolicy(optid).getTypeIdentifier();
        PolicyPdpNotificationData data = new PolicyPdpNotificationData(policyId, policyType);
        data.addAll(pdps);

        addMap.compute(policyId, (key, addData) -> {
            if (addData == null) {
                return data;

            } else {
                addData.addAll(data.getPdps());
                return addData;
            }
        });
    }

    /**
     * Gets the policies to be deployed.
     *
     * @return the policies to be deployed
     */
    public Collection<PolicyPdpNotificationData> getDeployData() {
        return deploy.values();
    }

    /**
     * Gets the policies to be undeployed.
     *
     * @return the policies to be undeployed
     */
    public Collection<PolicyPdpNotificationData> getUndeployData() {
        return undeploy.values();
    }
}
