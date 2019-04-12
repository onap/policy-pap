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
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.PolicyPapRuntimeException;

/**
 * Data used during a single REST call when updating PDP policies.
 */
public class SessionData {
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
    private final Map<ToscaPolicyIdentifier, ToscaPolicy> policyCache = new HashMap<>();

    /**
     * Maps a policy name to its latest policy. Every policy appearing within this map has
     * a corresponding entry in {@link #policyCache}.
     */
    private final Map<String, ToscaPolicy> latestPolicy = new HashMap<>();


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
     * @param ident policy identifier
     * @return the specified policy
     * @throws PolicyPapRuntimeException if an error occurs
     */
    public ToscaPolicy getPolicy(ToscaPolicyIdentifier ident) {

        return policyCache.computeIfAbsent(ident, key -> {

            try {
                List<ToscaPolicy> lst = dao.getPolicyList(ident.getName(), ident.getVersion());

                if (lst.isEmpty()) {
                    throw new PolicyPapRuntimeException(
                                    "cannot find policy: " + ident.getName() + " " + ident.getVersion());
                }

                if (lst.size() > 1) {
                    throw new PolicyPapRuntimeException(
                                    "too many policies match: " + ident.getName() + " " + ident.getVersion());
                }

                return lst.get(0);

            } catch (PfModelException e) {
                throw new PolicyPapRuntimeException("cannot get policy: " + ident.getName() + " " + ident.getVersion(),
                                e);
            }
        });
    }

    /**
     * Adds an update and state-change to the sets, replacing any previous entries for the
     * given PDP.
     *
     * @param update the update to be added
     * @param change the state-change to be addded
     */
    public void addUpdate(PdpUpdate update, PdpStateChange change) {
        if (!update.getName().equals(change.getName())) {
            throw new IllegalArgumentException("PDP name mismatch" + update.getName() + ", " + change.getName());
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
     * Gets the accumulated PDP requests.
     *
     * @return the PDP requests
     */
    public Collection<Pair<PdpUpdate, PdpStateChange>> getPdpRequests() {
        return pdpRequests.values();
    }

    /**
     * Gets the policy having the given name and the maximum version.
     *
     * @param name name of the desired policy
     * @return the desired policy, or {@code null} if there is no policy with given name
     * @throws PfModelException if an error occurs
     */
    public ToscaPolicy getPolicyMaxVersion(String name) throws PfModelException {
        ToscaPolicy policy = latestPolicy.get(name);
        if (policy != null) {
            return policy;
        }

        ToscaPolicyFilter filter =
                        ToscaPolicyFilter.builder().name(name).version(ToscaPolicyFilter.LATEST_VERSION).build();
        List<ToscaPolicy> policies = dao.getFilteredPolicyList(filter);
        if (policies.isEmpty()) {
            throw new PolicyPapRuntimeException("cannot find policy: " + name);
        }

        policy = policies.get(0);
        policyCache.putIfAbsent(policy.getIdentifier(), policy);
        latestPolicy.put(name, policy);

        return policy;
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

        return groupCache.computeIfAbsent(name, key -> {

            try {
                List<PdpGroup> lst = dao.getPdpGroups(key, PdpGroup.VERSION);
                if (lst.isEmpty()) {
                    return null;
                }

                return new GroupData(lst.get(0));

            } catch (PfModelException e) {
                throw new PolicyPapRuntimeException("cannot get group: " + name, e);
            }

        }).getGroup();
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
}
