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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.QueueToken;

/**
 * Notifier for completion of policy updates.
 */
public class PolicyNotifier {
    /**
     * Notification publisher.
     */
    private final Publisher<PolicyNotification> publisher;

    /**
     * Deployment tracker.
     */
    private final PolicyDeployTracker deployTracker = makeDeploymentTracker();

    /**
     * Undeployment tracker.
     */
    private final PolicyUndeployTracker undeployTracker = makeUndeploymentTracker();


    /**
     * Constructs the object.
     *
     * @param publisher notification publisher
     */
    public PolicyNotifier(Publisher<PolicyNotification> publisher) {
        this.publisher = publisher;
    }

    /**
     * Adds data to the deployment tracker. If a PDP appears within the undeployment
     * tracker, then it's removed from there.
     *
     * @param data data to be added
     */
    public synchronized void addDeploymentData(PolicyPdpNotificationData data) {
        PolicyNotification notification = new PolicyNotification();

        undeployTracker.removeData(data, notification.getDeleted());
        deployTracker.addData(data);

        publish(notification);
    }

    /**
     * Adds data to the undeployment tracker. If a PDP appears within the deployment
     * tracker, then it's removed from there.
     *
     * @param data data to be added
     */
    public synchronized void addUndeploymentData(PolicyPdpNotificationData data) {
        PolicyNotification notification = new PolicyNotification();

        deployTracker.removeData(data, notification.getAdded());
        undeployTracker.addData(data);

        publish(notification);
    }

    /**
     * Processes a response from a PDP.
     *
     * @param pdp PDP of interest
     * @param activePolicies policies that are still active on the PDP, as specified in
     *        the response
     */
    public synchronized void processResponse(String pdp, Collection<ToscaPolicyIdentifier> activePolicies) {
        processResponse(pdp, new HashSet<>(activePolicies));
    }

    /**
     * Processes a response from a PDP.
     *
     * @param pdp PDP of interest
     * @param activePolicies policies that are still active on the PDP, as specified in
     *        the response
     */
    public synchronized void processResponse(String pdp, Set<ToscaPolicyIdentifier> activePolicies) {
        PolicyNotification notification = new PolicyNotification();

        undeployTracker.processResponse(pdp, activePolicies, notification.getDeleted());
        deployTracker.processResponse(pdp, activePolicies, notification.getAdded());

        publish(notification);
    }

    /**
     * Removes a PDP from any policies still awaiting responses from it, generating
     * notifications for any of those policies that become complete as a result.
     *
     * @param pdp PDP to be removed
     */
    public synchronized void removePdp(String pdp) {
        PolicyNotification notification = new PolicyNotification();

        undeployTracker.removePdp(pdp, notification.getDeleted());
        deployTracker.removePdp(pdp, notification.getAdded());

        publish(notification);
    }

    /**
     * Publishes a notification, if it is not empty.
     *
     * @param notification notification to be published
     */
    private void publish(PolicyNotification notification) {
        if (!notification.isEmpty()) {
            publisher.enqueue(new QueueToken<>(notification));
        }
    }


    // the following methods may be overridden by junit tests

    protected PolicyDeployTracker makeDeploymentTracker() {
        return new PolicyDeployTracker();
    }

    protected PolicyUndeployTracker makeUndeploymentTracker() {
        return new PolicyUndeployTracker();
    }
}
