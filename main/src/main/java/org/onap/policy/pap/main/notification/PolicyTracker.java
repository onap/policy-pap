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

package org.onap.policy.pap.main.notification;

import java.util.Set;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;

/**
 * Tracks responses from PDPs for policy updates so that notifications can be sent.
 */
public class PolicyTracker {

    /**
     * Deployment tracker.
     */
    private final PolicyDeployTracker deployTracker = new PolicyDeployTracker();

    /**
     * Undeployment tracker.
     */
    private final PolicyUndeployTracker undeployTracker = new PolicyUndeployTracker();


    /**
     * Constructs the object.
     */
    public PolicyTracker() {
        super();
    }

    /**
     * Adds data to the deployment tracker. If a PDP appears within the undeployment
     * tracker, then it's removed from there.
     *
     * @param data data to be added
     * @return a notification to be sent, or {@code null} if no notification should be
     *         sent yet
     */
    public PolicyNotification addDeploymentData(PolicyPdpNotificationData data) {
        PolicyNotification notification = new PolicyNotification();

        undeployTracker.removeData(data, notification.getDeleted());
        deployTracker.addData(data);

        return notifyOrNull(notification);
    }

    /**
     * Adds data to the undeployment tracker. If a PDP appears within the deployment
     * tracker, then it's removed from there.
     *
     * @param data data to be added
     * @return a notification to be sent, or {@code null} if no notification should be
     *         sent yet
     */
    public PolicyNotification addUndeploymentData(PolicyPdpNotificationData data) {
        PolicyNotification notification = new PolicyNotification();

        deployTracker.removeData(data, notification.getAdded());
        undeployTracker.addData(data);

        return notifyOrNull(notification);
    }

    /**
     * Removes a PDP from the tracker, generating a notification if any of the policies
     * become complete once the PDP is removed.
     *
     * @param pdp PDP to be removed
     * @return a notification to be sent, or {@code null} if no notification should be
     *         sent yet
     */
    public PolicyNotification removePdp(String pdp) {
        PolicyNotification notification = new PolicyNotification();

        undeployTracker.removePdp(pdp, notification.getDeleted());
        deployTracker.removePdp(pdp, notification.getAdded());

        return notifyOrNull(notification);
    }

    /**
     * Processes a response from a PDP.
     *
     * @param pdp PDP of interest
     * @param activePolicies policies that are still active on the PDP, as specified in
     *        the response
     * @return a notification to be sent, or {@code null} if no notification should be
     *         sent yet
     */
    public PolicyNotification processResponse(String pdp, Set<ToscaPolicyIdentifier> activePolicies) {
        PolicyNotification notification = new PolicyNotification();

        undeployTracker.processResponse(pdp, activePolicies, notification.getDeleted());
        deployTracker.processResponse(pdp, activePolicies, notification.getAdded());

        return notifyOrNull(notification);
    }

    /**
     * Returns a notification, if it isn't empty.
     *
     * @param notification notification of interest
     * @return the notification, or {@code null} if the notification is empty
     */
    private PolicyNotification notifyOrNull(PolicyNotification notification) {
        return notification.isEmpty() ? null : notification;
    }
}
