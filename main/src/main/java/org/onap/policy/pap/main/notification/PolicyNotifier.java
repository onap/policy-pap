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
import java.util.function.Supplier;
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
     * Tracks policies requiring notifications when their updates complete.
     */
    private final PolicyTracker tracker = new PolicyTracker();


    /**
     * Constructs the object.
     *
     * @param publisher notification publisher
     */
    public PolicyNotifier(Publisher<PolicyNotification> publisher) {
        this.publisher = publisher;
    }

    /**
     * Adds deployment data, generating a notification if appropriate.
     *
     * @param data data to be added
     */
    public synchronized void addDeploymentData(PolicyPdpNotificationData data) {
        process(() -> tracker.addDeploymentData(data));
    }

    /**
     * Adds undeployment data, generating a notification if appropriate.
     *
     * @param data data to be added
     */
    public synchronized void addUndeploymentData(PolicyPdpNotificationData data) {
        process(() -> tracker.addUndeploymentData(data));
    }

    /**
     * Processes a response from a PDP.
     *
     * @param pdp PDP of interest
     * @param activePolicies policies that are still active on the PDP, as specified in
     *        the response
     */
    public synchronized void processResponse(String pdp, Set<ToscaPolicyIdentifier> activePolicies) {
        process(() -> tracker.processResponse(pdp, activePolicies));
    }

    /**
     * Removes a PDP from any policies still awaiting responses from it, generating
     * notifications if any policies are now complete.
     *
     * @param pdp PDP to be removed
     */
    public synchronized void removePdp(String pdp) {
        process(() -> tracker.removePdp(pdp));
    }

    /**
     * Processes data, publishing a notification if one is generated.
     *
     * @param processor function to process the data
     */
    private void process(Supplier<PolicyNotification> processor) {
        PolicyNotification notify = processor.get();
        if (notify != null) {
            publisher.enqueue(new QueueToken<>(notify));
        }
    }
}
