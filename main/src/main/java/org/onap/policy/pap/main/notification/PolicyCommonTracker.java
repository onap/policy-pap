/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.main.notification;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;

/**
 * Common super class for deploy and undeploy trackers.
 */
public abstract class PolicyCommonTracker {

    /**
     * Maps a policy id to its deployment data. The subclass determines when an entry is
     * removed.
     *
     * <p/>
     * Use a LinkedHashMap, because we'll be doing lots of iteration over the map, and
     * iteration over a LinkedHashMap is faster than over a plain HashMap.
     */
    private final Map<ToscaPolicyIdentifier, PolicyTrackerData> policy2data = new LinkedHashMap<>();


    /**
     * Constructs the object.
     */
    public PolicyCommonTracker() {
        super();
    }

    /**
     * Gets the status of all policies being tracked.
     *
     * @return the status of all policies
     */
    public List<PolicyStatus> getStatus() {
        return policy2data.entrySet().stream().map(this::makeStatus).collect(Collectors.toList());
    }

    /**
     * Gets the status of all versions of a policy.
     *
     * @param policyId ID of the policy of interest, without the version
     * @return the status of all versions of the policy having the given ID
     */
    public List<PolicyStatus> getStatus(String policyId) {
        // version is not specified - have to scan the whole list
        return policy2data.entrySet().stream().filter(ent -> ent.getKey().getName().equals(policyId))
                        .map(this::makeStatus).collect(Collectors.toList());
    }

    /**
     * Gets the status of a particular policy.
     *
     * @param ident identifier of the policy of interest
     * @return the status of the given policy, or empty if the policy is not found
     */
    public Optional<PolicyStatus> getStatus(ToscaPolicyIdentifier ident) {
        ToscaPolicyIdentifier ident2 = new ToscaPolicyIdentifier(ident.getName(), ident.getVersion());
        PolicyTrackerData data = policy2data.get(ident2);
        return Optional.ofNullable(data == null ? null : makeStatus(ident2, data));
    }

    /**
     * Adds data to the tracker.
     *
     * @param data data to be added to the tracker
     */
    public void addData(PolicyPdpNotificationData data) {
        policy2data.computeIfAbsent(data.getPolicyId(), policyId -> new PolicyTrackerData(data.getPolicyType()))
                        .addPdps(data.getPdps());
    }

    /**
     * Removes a set of PDPs from all policies within the tracker.
     *
     * @param notifyData data identifying the policy and the PDPs to be removed from it
     */
    public void removeData(PolicyPdpNotificationData notifyData) {

        policy2data.computeIfPresent(notifyData.getPolicyId(), (policyId, data) -> {

            if (!data.removePdps(notifyData.getPdps())) {
                // not complete yet
                return data;
            }

            // this policy is complete
            return (shouldRemove(data) ? null : data);
        });
    }

    /**
     * Removes a PDP from all policies within the tracker.
     *
     * @param pdp PDP to be removed
     * @param statusList status messages are added here if policies become complete as a
     *        result of this operation
     */
    public void removePdp(String pdp, List<PolicyStatus> statusList) {
        updateMap(statusList, (policyId, data) -> data.removePdp(pdp));
    }

    /**
     * Processes a response from a PDP.
     *
     * @param pdp PDP of interest
     * @param activePolicies policies that are still active on the PDP, as specified in
     *        the response
     * @param statusList status messages are added here if policies become complete as a
     *        result of this operation
     */
    public void processResponse(String pdp, Collection<ToscaPolicyIdentifier> activePolicies,
                    List<PolicyStatus> statusList) {
        processResponse(pdp, new HashSet<>(activePolicies), statusList);
    }

    /**
     * Processes a response from a PDP.
     *
     * @param pdp PDP of interest
     * @param activePolicies policies that are still active on the PDP, as specified in
     *        the response
     * @param statusList status messages are added here if policies become complete as a
     *        result of this operation
     */
    public void processResponse(String pdp, Set<ToscaPolicyIdentifier> activePolicies, List<PolicyStatus> statusList) {
        updateMap(statusList, (policyId, data) -> updateData(pdp, data, activePolicies.contains(policyId)));
    }

    /**
     * Updates the map.
     *
     * <p/>
     * Note: this iterates through the whole map. While it may be more efficient to
     * iterate through just the policies relevant to the PDP, that would complicate the
     * code and complicate the testing. In addition, this should still perform well
     * enough, but if not, it can always be enhanced.
     *
     * @param statusList status messages are added here if policies become complete as a
     *        result of this operation
     * @param updater function to update a policy's data. Returns {@code true} if the
     *        policy is complete (i.e., no longer awaiting any responses)
     */
    private void updateMap(List<PolicyStatus> statusList,
                    BiPredicate<ToscaPolicyIdentifier, PolicyTrackerData> updater) {

        Iterator<Entry<ToscaPolicyIdentifier, PolicyTrackerData>> iter = policy2data.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<ToscaPolicyIdentifier, PolicyTrackerData> ent = iter.next();

            ToscaPolicyIdentifier policyId = ent.getKey();
            PolicyTrackerData data = ent.getValue();

            if (!updater.test(policyId, data)) {
                // not complete yet
                continue;
            }

            // this policy is complete - notify
            statusList.add(makeStatus(ent));

            if (shouldRemove(data)) {
                iter.remove();
            }
        }
    }

    /**
     * Updates the policy data, based on a response from a PDP.
     *
     * @param pdp PDP whose response was just received
     * @param data data associated with the policy of interest
     * @param stillActive {@code true} if the policy is still active for the PDP,
     *        {@code false} otherwise
     * @return {@code true} if the policy is complete (i.e., no longer awaiting any
     *         responses), {@code false} otherwise
     */
    protected abstract boolean updateData(String pdp, PolicyTrackerData data, boolean stillActive);

    /**
     * Determines if a policy should be removed from the tracker, based on the state of
     * its data.
     *
     * @param data data associated with the policy of interest
     * @return {@code true} if the policy should be removed from the tracker,
     *         {@code false} otherwise
     */
    protected abstract boolean shouldRemove(PolicyTrackerData data);

    /**
     * Makes a status notification for the given policy entry.
     *
     * @param entry policy entry
     * @return a new status notification
     */
    private PolicyStatus makeStatus(Map.Entry<ToscaPolicyIdentifier, PolicyTrackerData> entry) {
        return makeStatus(entry.getKey(), entry.getValue());
    }

    /**
     * Makes a status notification for the given policy.
     *
     * @param policyId policy ID
     * @param data data to be used to set the status fields
     * @return a new status notification
     */
    private PolicyStatus makeStatus(ToscaPolicyIdentifier policyId, PolicyTrackerData data) {

        PolicyStatus status = new PolicyStatus(data.getPolicyType(), policyId);
        data.putValuesInto(status);
        return status;
    }
}
