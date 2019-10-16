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

import java.util.Set;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;

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
    public void addDeployData(PolicyPdpNotificationData data) {
        PolicyNotification notify = tracker.addDeployData(data);
        if (notify != null) {
            publisher.enqueue(new QueueToken<>(notify));
        }
    }

    /**
     * Adds undeployment data, generating a notification if appropriate.
     *
     * @param data data to be added
     */
    public void addUndeployData(PolicyPdpNotificationData data) {
        PolicyNotification notify = tracker.addUndeployData(data);
        if (notify != null) {
            publisher.enqueue(new QueueToken<>(notify));
        }
    }

    /**
     * Indicates that a PDP has successfully updated some policies.
     *
     * @param pdp name of the PDP of interest
     * @param policies the policies that the PDP successfully updated
     */
    public void completed(String pdp, Set<ToscaPolicyIdentifier> policies) {
        PolicyNotification notify = tracker.completed(pdp, policies);
        if (notify != null) {
            publisher.enqueue(new QueueToken<>(notify));
        }
    }

    /**
     * Removes a PDP from any policies still awaiting responses from it, generating
     * notifications if any policies are now complete.
     *
     * @param pdp PDP to be removed
     */
    public void removePdp(String pdp) {
        PolicyNotification notify = tracker.removePdp(pdp);
        if (notify != null) {
            publisher.enqueue(new QueueToken<>(notify));
        }
    }
}
