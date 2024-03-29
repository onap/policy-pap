/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020-2021 Nordix Foundation.
 * Modifications Copyright (C) 2022 Bell Canada. All rights reserved.
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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.QueueToken;
import org.onap.policy.pap.main.service.PolicyStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Notifier for completion of policy updates.
 */
@RequiredArgsConstructor
@Component
public class PolicyNotifier {
    private static final Logger logger = LoggerFactory.getLogger(PolicyNotifier.class);

    private final PolicyStatusService policyStatusService;

    /**
     * Notification publisher.
     */
    @Setter
    private Publisher<PolicyNotification> publisher;

    /**
     * Processes a response from a PDP.
     *
     * @param pdp PDP of interest
     * @param pdpGroup name of the PdpGroup containing the PDP
     * @param expectedPolicies policies that expected to be deployed on the PDP
     * @param actualPolicies policies that are still active on the PDP, as specified in
     *        the response
     */
    public synchronized void processResponse(String pdp, String pdpGroup, Set<ToscaConceptIdentifier> expectedPolicies,
                    Set<ToscaConceptIdentifier> actualPolicies) {

        try {
            DeploymentStatus status = makeDeploymentTracker();
            status.loadByGroup(pdpGroup);
            status.completeDeploy(pdp, expectedPolicies, actualPolicies);

            var notification = new PolicyNotification();
            status.flush(notification);

            publish(notification);

        } catch (RuntimeException e) {
            logger.warn("cannot update deployment status", e);
        }
    }

    /**
     * Publishes a notification, if it is not empty.
     *
     * @param notification notification to be published
     */
    public synchronized void publish(PolicyNotification notification) {
        if (!notification.isEmpty()) {
            publisher.enqueue(new QueueToken<>(notification));
        }
    }


    // the following methods may be overridden by junit tests

    protected DeploymentStatus makeDeploymentTracker() {
        return new DeploymentStatus(policyStatusService);
    }
}
