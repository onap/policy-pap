/*-
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

package org.onap.policy.pap.main.comm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;

/**
 * Tracks responses from PDPs for policy updates so that notifications can be sent.
 */
public class PolicyTracker {

    /**
     * Maps a policy id to its deployment data.
     */
    private final Map<ToscaPolicyIdentifier, PolicyTrackerData> policy2deploy = new HashMap<>();

    /**
     * Maps a policy id to its undeployment data.
     */
    private final Map<ToscaPolicyIdentifier, PolicyTrackerData> policy2undeploy = new HashMap<>();


    /**
     * Constructs the object.
     */
    public PolicyTracker() {
        super();
    }

    /**
     * Adds deployment data.
     *
     * @param data data to be added
     * @return a notification to be sent, if the data had no PDPs (and is thus already
     *         complete), or {@code null} otherwise
     */
    public synchronized PolicyNotification addDeployData(PolicyPdpNotificationData data) {
        PolicyNotification notify = new PolicyNotification();

        addData(data, policy2deploy, notify.getAdded(), policy2undeploy, notify.getDeleted());

        return (notify.isEmpty() ? null : notify);
    }

    /**
     * Adds undeployment data.
     *
     * @param data data to be added
     * @return a notification to be sent, if the data had no PDPs (and is thus already
     *         complete), or {@code null} otherwise
     */
    public synchronized PolicyNotification addUndeployData(PolicyPdpNotificationData data) {
        PolicyNotification notify = new PolicyNotification();

        addData(data, policy2undeploy, notify.getDeleted(), policy2deploy, notify.getAdded());

        return (notify.isEmpty() ? null : notify);
    }

    /**
     * Adds data to a map, removing it another map.
     *
     * @param data data to be added
     * @param addMap map to which it should be added
     * @param addList list to which notifications should be added if it completes when
     *        added to "addMap"
     * @param removeMap map from which it should be removed
     * @param removeList list to which notifications should be added if it completes when
     *        removed from "removeMap"
     */
    private synchronized void addData(PolicyPdpNotificationData data,
                    Map<ToscaPolicyIdentifier, PolicyTrackerData> addMap, List<PolicyStatus> addList,
                    Map<ToscaPolicyIdentifier, PolicyTrackerData> removeMap, List<PolicyStatus> removeList) {

        ToscaPolicyIdentifier policyId = data.getPolicyId();

        if (data.isEmpty()) {
            if (!addMap.containsKey(policyId)) {
                addList.add(new PolicyStatus(data.getPolicyType(), policyId));
            }

            return;
        }

        removeMap.computeIfPresent(policyId, (key, removeData) -> {
            if (removeData.removePdps(data.getPdps())) {
                removeList.add(makeStatus(policyId, removeData));
                return null;

            } else {
                return removeData;
            }
        });

        /*
         * note: this can never generate a notification, because there is always at least
         * one PDP being added
         */
        addMap.computeIfAbsent(policyId, key -> new PolicyTrackerData(data.getPolicyType())).addPdps(data.getPdps());
    }

    /**
     * Removes a PDP from the tracker, generating a notification if any of the policies
     * become complete once the PDP is removed.
     *
     * @param pdp PDP to be removed
     * @return a notification to be sent, or {@code null} if no notification should be
     *         sent yet
     */
    public synchronized PolicyNotification removePdp(String pdp) {
        PolicyNotification notify = new PolicyNotification();

        removePdp(pdp, policy2deploy, notify.getAdded());
        removePdp(pdp, policy2undeploy, notify.getDeleted());

        return (notify.isEmpty() ? null : notify);
    }

    /**
     * Removes a PDP from a policy map.
     *
     * @param pdp PDP to be removed
     * @param theMap policy map from which the PDP should be removed
     * @param statusList status messages are added to this list
     */
    private synchronized void removePdp(String pdp, Map<ToscaPolicyIdentifier, PolicyTrackerData> theMap,
                    List<PolicyStatus> statusList) {

        Iterator<Entry<ToscaPolicyIdentifier, PolicyTrackerData>> iter = theMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<ToscaPolicyIdentifier, PolicyTrackerData> ent = iter.next();
            ToscaPolicyIdentifier policyId = ent.getKey();
            PolicyTrackerData data = ent.getValue();

            if (!data.removePdp(pdp)) {
                // not complete yet
                continue;
            }

            // this policy is complete - remove it and notify
            iter.remove();
            statusList.add(makeStatus(policyId, data));
        }
    }

    /**
     * Indicates that a PDP has successfully updated some policies.
     *
     * @param pdp name of the PDP of interest
     * @param policies the policies that the PDP successfully updated
     * @return a notification to be sent, or {@code null} if no notification should be
     *         sent yet
     */
    public synchronized PolicyNotification completed(String pdp, Set<ToscaPolicyIdentifier> policies) {
        PolicyNotification notify = new PolicyNotification();

        processUndeploy(pdp, policies, notify);
        processDeploy(pdp, policies, notify);

        return (notify.isEmpty() ? null : notify);
    }

    /**
     * Indicates that a PDP has updated some policies, processing those that are being
     * undeployed.
     *
     * @param pdp name of the PDP of interest
     * @param activePolicies the policies that are still active, based on the PDP's
     *        response
     * @param notify if an undeployed policy is now complete, it's notification is added
     *        to this notification
     */
    private synchronized void processUndeploy(String pdp, Set<ToscaPolicyIdentifier> activePolicies,
                    PolicyNotification notify) {

        final List<PolicyStatus> statusList = notify.getDeleted();

        Iterator<Entry<ToscaPolicyIdentifier, PolicyTrackerData>> iter = policy2undeploy.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<ToscaPolicyIdentifier, PolicyTrackerData> ent = iter.next();
            ToscaPolicyIdentifier policyId = ent.getKey();
            PolicyTrackerData data = ent.getValue();
            if (!checkUndeploy(pdp, activePolicies, policyId, data)) {
                // either this policy is still active or it isn't complete yet
                continue;
            }

            // this policy is complete - remove it and notify
            iter.remove();
            statusList.add(makeStatus(policyId, data));
        }
    }

    /**
     * Checks an undeployed of a policy. If the policy is still active, then it's always
     * treated as a failure. Otherwise, it applies the given test.
     *
     * @param pdp name of the PDP of interest
     * @param activePolicies the policies that are still active, based on the PDP's
     *        response
     * @param policyId ID of the policy to be checked
     * @param data data associated with the policy to be checked
     * @return {@code true} if the policy is complete and a notification should be sent,
     *         {@code false} otherwise
     */
    private synchronized boolean checkUndeploy(String pdp, Set<ToscaPolicyIdentifier> activePolicies,
                    ToscaPolicyIdentifier policyId, PolicyTrackerData data) {

        if (activePolicies.contains(policyId)) {
            // still active - always treat this as a failure
            return data.fail(pdp);
        }

        // no longer active - apply the test
        return data.success(pdp);
    }

    /**
     * Indicates that a PDP has updated some policies, processing those that are being
     * deployed.
     *
     * @param pdp name of the PDP of interest
     * @param activePolicies the policies that are still active, based on the PDP's
     *        response
     * @param notify if a deployed policy is now complete, it's notification is added to
     *        this notification
     */
    private synchronized void processDeploy(String pdp, Set<ToscaPolicyIdentifier> activePolicies,
                    PolicyNotification notify) {

        final List<PolicyStatus> statusList = notify.getAdded();

        for (ToscaPolicyIdentifier policyId : activePolicies) {
            PolicyTrackerData data = policy2deploy.get(policyId);
            if (data == null || !data.success(pdp)) {
                // either didn't care about this policy or it isn't complete yet
                continue;
            }

            // this policy is complete - remove it and notify
            policy2deploy.remove(policyId);
            statusList.add(makeStatus(policyId, data));
        }
    }

    /**
     * Makes a status for the given policy.
     *
     * @param policyId policy ID
     * @param data data to be used to set the status fields
     * @return a new status
     */
    private synchronized PolicyStatus makeStatus(ToscaPolicyIdentifier policyId, PolicyTrackerData data) {

        PolicyStatus status = new PolicyStatus(data.getPolicyType(), policyId);
        data.putValuesInto(status);
        return status;
    }
}
