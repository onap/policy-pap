/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020-2021 Nordix Foundation.
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
import lombok.AllArgsConstructor;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.QueueToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notifier for completion of policy updates.
 */
@AllArgsConstructor
public class PolicyNotifier {
    private static final Logger logger = LoggerFactory.getLogger(PolicyNotifier.class);

    /**
     * Notification publisher.
     */
    private final Publisher<PolicyNotification> publisher;

    private final PolicyModelsProviderFactoryWrapper daoFactory;

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

        try (PolicyModelsProvider dao = daoFactory.create()) {
            DeploymentStatus status = makeDeploymentTracker(dao);
            status.loadByGroup(pdpGroup);
            status.completeDeploy(pdp, expectedPolicies, actualPolicies);

            var notification = new PolicyNotification();
            status.flush(notification);

            publish(notification);

        } catch (PfModelException e) {
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

    protected DeploymentStatus makeDeploymentTracker(PolicyModelsProvider dao) {
        return new DeploymentStatus(dao);
    }
}
