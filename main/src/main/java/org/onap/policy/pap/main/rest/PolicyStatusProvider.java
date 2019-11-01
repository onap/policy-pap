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

package org.onap.policy.pap.main.rest;

import java.util.List;
import java.util.Optional;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.notification.PolicyNotifier;

/**
 * Provider for Policy deploy/undeploy status.
 */
public class PolicyStatusProvider {

    /**
     * Container of policy deployment status.
     */
    private final PolicyNotifier notifier;


    /**
     * Constructs the object.
     */
    public PolicyStatusProvider() {
        this.notifier = Registry.get(PapConstants.REG_POLICY_NOTIFIER, PolicyNotifier.class);
    }

    /**
     * Gets the status of all deployed policies.
     *
     * @return the status of all deployed policies
     */
    public synchronized List<PolicyStatus> getStatus() {
        return notifier.getStatus();
    }

    /**
     * Gets the status of a particular deployed policy.
     *
     * @param policyId ID of the policy of interest, without the version
     * @return the status of all deployed policies matching the given identifier
     */
    public List<PolicyStatus> getStatus(String policyId) {
        return notifier.getStatus(policyId);
    }

    /**
     * Gets the status of a particular deployed policy.
     *
     * @param ident identifier of the policy of interest
     * @return the status of the given policy, or empty if the policy is not found
     */
    public Optional<PolicyStatus> getStatus(ToscaPolicyIdentifier ident) {
        return notifier.getStatus(ident);
    }
}
