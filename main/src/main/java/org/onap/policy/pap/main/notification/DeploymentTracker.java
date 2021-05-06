/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Tracks policy status so that notifications can be generated.
 */
public class DeploymentTracker {
    public final Map<ToscaConceptIdentifier, PolicyStatus> deployMap = new HashMap<>();
    public final Map<ToscaConceptIdentifier, PolicyStatus> undeployMap = new HashMap<>();


    /**
     * Gets the status of all deployments.
     *
     * @return the status of all deployments
     */
    public Collection<PolicyStatus> getDeploymentStatus() {
        return deployMap.values();
    }

    /**
     * Gets the status of all undeployments.
     *
     * @return the status of all undeployments
     */
    public Collection<PolicyStatus> getUndeploymentStatus() {
        return undeployMap.values();
    }

    /**
     * Compares this tracking data with new tracking data, adding new policy status to a
     * notification based on the differences.
     *
     * @param notif notification to which to add policy status
     * @param newTracker new tracking data
     */
    public void addNotifications(PolicyNotification notif, DeploymentTracker newTracker) {
        merge(notif.getAdded(), deployMap, newTracker.deployMap);
        merge(notif.getDeleted(), undeployMap, newTracker.undeployMap);

        // include those that no longer exist
        addMissing(notif, newTracker);
    }

    /**
     * Merges original tracking data with new tracking data, adding new policy status to
     * the given list.
     *
     * @param list list to which to add policy status
     * @param originalMap original tracking data
     * @param newMap new tracking data
     */
    private void merge(List<PolicyStatus> list, Map<ToscaConceptIdentifier, PolicyStatus> originalMap,
                    Map<ToscaConceptIdentifier, PolicyStatus> newMap) {

        for (Entry<ToscaConceptIdentifier, PolicyStatus> entry : newMap.entrySet()) {
            ToscaConceptIdentifier policy = entry.getKey();
            PolicyStatus newStat = entry.getValue();
            PolicyStatus oldStat = originalMap.get(policy);

            if (needNotification(oldStat, newStat)) {
                list.add(newStat);
            }
        }
    }

    /**
     * Determines if a notification is needed.
     *
     * @param oldStat original status, or {@code null}
     * @param newStat new status
     * @return {@code true} if a notification is needed for the policy, {@code false}
     *         otherwise
     */
    protected boolean needNotification(PolicyStatus oldStat, PolicyStatus newStat) {
        if (oldStat == null) {
            // new policy - need notification if it's complete now
            return (newStat.getIncompleteCount() == 0);
        }

        if (newStat.getIncompleteCount() == 0) {
            // don't care if the success count changes (i.e., new PDPs can be added
            // without requiring a notification)
            return (oldStat.getIncompleteCount() > 0 || newStat.getFailureCount() != oldStat.getFailureCount());
        }

        // something is incomplete - only notify if it was previously complete
        return (oldStat.getIncompleteCount() == 0);
    }

    /**
     * Adds notifications for previously deployed policies that are missing from the new
     * tracker.
     *
     * @param notif notification to which to add policy status
     * @param newTracker new tracking data
     */
    private void addMissing(PolicyNotification notif, DeploymentTracker newTracker) {
        Map<ToscaConceptIdentifier, PolicyStatus> newDeployMap = newTracker.deployMap;

        for (Entry<ToscaConceptIdentifier, PolicyStatus> entry : deployMap.entrySet()) {
            if (entry.getValue().getIncompleteCount() != 0 || newDeployMap.containsKey(entry.getKey())) {
                /*
                 * This policy deployment was previously incomplete, or it still exists in
                 * the new tracker. Either way, it needs no notification.
                 */
                continue;
            }

            // no longer deployed
            PolicyStatus status = entry.getValue();

            // create a status with counts that are all zero
            var newStatus = new PolicyStatus();
            newStatus.setPolicyId(status.getPolicyId());
            newStatus.setPolicyVersion(status.getPolicyVersion());
            newStatus.setPolicyTypeId(status.getPolicyTypeId());
            newStatus.setPolicyTypeVersion(status.getPolicyTypeVersion());

            /*
             * Adding the status to the "added" set may be a bit unexpected, but when all
             * status records are deleted from the group, we don't actually undeploy the
             * policy from the subgroup. Instead, we leave it in the subgroup so that as
             * soon as a PDP registers, we immediately deploy the policy to the PDP and
             * continue on; the client can always undeploy the policy when it receives the
             * notification, if so desired.
             */
            notif.getAdded().add(newStatus);
        }
    }

    /**
     * Adds status to the tracking data. Assumes the associated PDP/policy pair has not be
     * added before.
     *
     * @param status status to be added
     */
    public void add(StatusAction status) {
        if (status.getAction() == StatusAction.Action.DELETED) {
            return;
        }

        add(status.getStatus());
    }

    /**
     * Adds status to the tracking data. Assumes the associated PDP/policy pair has not be
     * added before.
     *
     * @param status status to be added
     */
    public void add(PdpPolicyStatus status) {

        ToscaConceptIdentifier policy = status.getPolicy();

        // get the entry from the relevant map, creating an entry if necessary
        Map<ToscaConceptIdentifier, PolicyStatus> map = (status.isDeploy() ? deployMap : undeployMap);

        PolicyStatus newStat = map.computeIfAbsent(policy, key -> {
            var value = new PolicyStatus();
            value.setPolicyId(policy.getName());
            value.setPolicyVersion(policy.getVersion());
            value.setPolicyTypeId(status.getPolicyType().getName());
            value.setPolicyTypeVersion(status.getPolicyType().getVersion());
            return value;
        });

        // bump the relevant count
        switch (status.getState()) {
            case SUCCESS:
                newStat.setSuccessCount(newStat.getSuccessCount() + 1);
                break;
            case FAILURE:
                newStat.setFailureCount(newStat.getFailureCount() + 1);
                break;
            default:
                newStat.setIncompleteCount(newStat.getIncompleteCount() + 1);
                break;
        }
    }
}
