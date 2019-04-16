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
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.PolicyPapRuntimeException;

/**
 * Data used during a single REST call when updating PDP policies.
 */
public class SessionData {
    /**
     * If a version string matches this, then it is just a prefix (i.e., major or major.minor).
     */
    private static final Pattern VERSION_PREFIX_PAT = Pattern.compile("[^.]+(?:[.][^.]*)?");

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
     * Constructs the object.
     *
     * @param dao DAO provider
     */
    public SessionData(PolicyModelsProvider dao) {
        this.dao = dao;
    }

    /**
     * Gets the policy, referenced by an identifier. Loads it from the cache, if possible.
     * Otherwise, gets it from the DB.
     *
     * @param desiredPolicy policy identifier
     * @return the specified policy
     * @throws PolicyPapRuntimeException if an error occurs
     */
    public ToscaPolicy getPolicy(ToscaPolicyIdentifierOptVersion desiredPolicy) {

        ToscaPolicy policy = policyCache.computeIfAbsent(desiredPolicy, key -> {

            try {
                ToscaPolicyFilterBuilder filterBuilder = ToscaPolicyFilter.builder().name(desiredPolicy.getName());
                setPolicyFilterVersion(filterBuilder, desiredPolicy.getVersion());

                List<ToscaPolicy> lst = dao.getFilteredPolicyList(filterBuilder.build());
                if (lst.isEmpty()) {
                    throw new PolicyPapRuntimeException("cannot find policy: " + desiredPolicy.getName() + " "
                                    + desiredPolicy.getVersion());
                }

                return lst.get(0);

            } catch (PfModelException e) {
                throw new PolicyPapRuntimeException(
                                "cannot get policy: " + desiredPolicy.getName() + " " + desiredPolicy.getVersion(), e);
            }
        });

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

        } else if (VERSION_PREFIX_PAT.matcher(desiredVersion).matches()) {
            // version prefix provided - match the prefix and then pick the latest
            filterBuilder.versionPrefix(desiredVersion + ".").version(ToscaPolicyFilter.LATEST_VERSION);

        } else {
            // must be an exact match
            filterBuilder.version(desiredVersion);
        }
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

        pdpRequests.put(update.getName(), Pair.of(update, change));
    }

    /**
     * Adds an update to the set of updates, replacing any previous entry for the given
     * PDP.
     *
     * @param update the update to be added
     */
    public void addUpdate(PdpUpdate update) {
        pdpRequests.compute(update.getName(), (name, data) -> Pair.of(update, (data == null ? null : data.getRight())));
    }

    /**
     * Adds a state-change to the set of state-change requests, replacing any previous entry for the given
     * PDP.
     *
     * @param change the state-change to be added
     */
    public void addStateChange(PdpStateChange change) {
        pdpRequests.compute(change.getName(), (name, data) -> Pair.of((data == null ? null : data.getLeft()), change));
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

        data.update(newGroup);
    }

    /**
     * Gets the group by the given name.
     *
     * @param name name of the group to get
     * @return the group, or {@code null} if it does not exist
     * @throws PolicyPapRuntimeException if an error occurs
     */
    public PdpGroup getGroup(String name) {

        GroupData data = groupCache.computeIfAbsent(name, key -> {

            try {
                List<PdpGroup> lst = dao.getPdpGroups(key);
                if (lst.isEmpty()) {
                    return null;
                }

                return new GroupData(lst.get(0));

            } catch (PfModelException e) {
                throw new PolicyPapRuntimeException("cannot get group: " + name, e);
            }

        });

        return (data == null ? null : data.getGroup());
    }

    /**
     * Gets the active groups supporting the given policy.
     *
     * @param type desired policy type
     * @return the active groups supporting the given policy
     * @throws PfModelException if an error occurs
     */
    public List<PdpGroup> getActivePdpGroupsByPolicyType(ToscaPolicyTypeIdentifier type) throws PfModelException {
        List<GroupData> data = type2groups.get(type);
        if (data != null) {
            return data.stream().map(GroupData::getGroup).collect(Collectors.toList());
        }

        PdpGroupFilter filter = PdpGroupFilter.builder().policyTypeList(Collections.singletonList(type))
                        .groupState(PdpState.ACTIVE).build();

        List<PdpGroup> groups = dao.getFilteredPdpGroups(filter);

        data = groups.stream().map(this::addGroup).collect(Collectors.toList());
        type2groups.put(type, data);

        return groups;
    }

    /**
     * Adds a group to the group cache, if it isn't already in the cache.
     *
     * @param group the group to be added
     * @return the cache entry
     */
    private GroupData addGroup(PdpGroup group) {
        GroupData data = groupCache.get(group.getName());
        if (data != null) {
            return data;
        }

        data = new GroupData(group);
        groupCache.put(group.getName(), data);

        return data;
    }

    /**
     * Update the DB with the changes.
     *
     * @throws PfModelException if an error occurs
     */
    public void updateDb() throws PfModelException {
        // create new groups
        List<GroupData> created = groupCache.values().stream().filter(GroupData::isNew).collect(Collectors.toList());
        if (!created.isEmpty()) {
            dao.createPdpGroups(created.stream().map(GroupData::getGroup).collect(Collectors.toList()));
        }

        // update existing groups
        List<GroupData> updated =
                        groupCache.values().stream().filter(GroupData::isUpdated).collect(Collectors.toList());
        if (!updated.isEmpty()) {
            dao.updatePdpGroups(updated.stream().map(GroupData::getGroup).collect(Collectors.toList()));
        }
    }

    /**
     * Deletes a group from the DB, immediately (i.e., without caching the request to be
     * executed later).
     *
     * @param group the group to be deleted
     * @throws PfModelException if an error occurs
     */
    public void deleteGroupFromDb(PdpGroup group) throws PfModelException {
        dao.deletePdpGroup(group.getName());
    }
}
