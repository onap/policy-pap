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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
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
     * The names of newly created groups are added to this set to prevent the version
     * number from being updated in case the group is re-used during the same REST call.
     */
    private final Set<String> newGroups;

    /**
     * Maps a PDP name to its most recently generated update request.
     */
    private final Map<String, PdpUpdate> updates;

    /**
     * Maps a policy's identifier to the policy.
     */
    private final Map<ToscaPolicyIdentifier, ToscaPolicy> policyMap;


    /**
     * Constructs the object.
     *
     * @param dao DAO provider
     */
    public SessionData(PolicyModelsProvider dao) {
        this.dao = dao;
        this.newGroups = new HashSet<>();
        this.updates = new HashMap<>();
        this.policyMap = new HashMap<>();
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

        return policyMap.computeIfAbsent(ident, key -> {

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
     * Adds an update to the set of updates, replacing any previous entry for the given
     * PDP.
     *
     * @param update the update to be added
     */
    public void addUpdate(PdpUpdate update) {
        updates.put(update.getName(), update);
    }

    /**
     * Gets the accumulated UPDATE requests.
     *
     * @return the UPDATE requests
     */
    public Collection<PdpUpdate> getUpdates() {
        return updates.values();
    }

    /**
     * Determines if a group has been newly created as part of this REST call.
     *
     * @param group name to the group of interest
     * @return {@code true} if the group has been newly created, {@code false} otherwise
     */
    public boolean isNewlyCreated(String group) {
        return newGroups.contains(group);
    }

    /**
     * Gets the policy having the given name and the maximum version.
     *
     * @param name name of the desired policy
     * @return the desired policy, or {@code null} if there is no policy with given name
     * @throws PfModelException if an error occurs
     */
    public ToscaPolicy getPolicyMaxVersion(String name) throws PfModelException {
        ToscaPolicyFilter filter = ToscaPolicyFilter.builder().name(name).build();
        // TODO setLatest, setActive?

        List<ToscaPolicy> policies = dao.getFilteredPolicyList(filter);
        if (policies.isEmpty()) {
            throw new PolicyPapRuntimeException("cannot find policy: " + name);
        }

        return policies.get(0);
    }

    /**
     * Gets the group having the given name and the maximum version.
     *
     * @param name name of the desired group
     * @return the desired group, or {@code null} if there is no group with given name
     * @throws PfModelException if an error occurs
     */
    public PdpGroup getPdpGroupMaxVersion(String name) throws PfModelException {
        PdpGroupFilter filter = PdpGroupFilter.builder().name(name).build();
        // TODO setLatest

        List<PdpGroup> groups = dao.getFilteredPdpGroups(filter);
        if (groups.isEmpty()) {
            throw new PolicyPapRuntimeException("cannot find group: " + name);
        }

        return groups.get(0);
    }

    /**
     * Gets the active groups supporting the given policy.
     *
     * @param type desired policy type
     * @return the active groups supporting the given policy
     * @throws PfModelException if an error occurs
     */
    public List<PdpGroup> getActivePdpGroupsByPolicyType(ToscaPolicyTypeIdentifier type) throws PfModelException {

        PdpGroupFilter filter = PdpGroupFilter.builder().policyType(type).build();
        // TODO setActive, setHasPdps

        return dao.getFilteredPdpGroups(filter);
    }

    /**
     * Creates a PDP group.
     *
     * @param pdpGroup the group to be created
     * @return the created group
     * @throws PfModelException if an error occurs
     */
    public PdpGroup createPdpGroup(PdpGroup pdpGroup) throws PfModelException {
        newGroups.add(pdpGroup.getName());
        return dao.createPdpGroups(Collections.singletonList(pdpGroup)).get(0);
    }

    /**
     * Updates a PDP group.
     *
     * @param pdpGroup the group to be updated
     * @return the updated group
     * @throws PfModelException if an error occurs
     */
    public PdpGroup updatePdpGroup(PdpGroup pdpGroup) throws PfModelException {
        return dao.updatePdpGroups(Collections.singletonList(pdpGroup)).get(0);
    }
}
