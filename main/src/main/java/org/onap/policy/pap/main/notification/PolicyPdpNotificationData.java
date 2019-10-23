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
import lombok.Getter;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

/**
 * Data used to track deploy/undeploy of a policy to PDPs.
 */
@Getter
public class PolicyPdpNotificationData {
    private final ToscaPolicyIdentifier policyId;
    private final ToscaPolicyTypeIdentifier policyType;
    private final Set<String> pdps = new HashSet<>();


    /**
     * Constructs the object.
     *
     * @param policyId ID of the policy being deployed/undeployed
     * @param policyType type of the associated policy
     */
    public PolicyPdpNotificationData(ToscaPolicyIdentifier policyId, ToscaPolicyTypeIdentifier policyType) {
        this.policyId = policyId;
        this.policyType = policyType;
    }

    /**
     * Determines if there are any PDPs in the data.
     *
     * @return {@code true} if the data contains at least one PDP, {@code false} otherwise
     */
    public boolean isEmpty() {
        return pdps.isEmpty();
    }

    /**
     * Adds a PDP to the set of PDPs.
     *
     * @param pdp PDP to be added
     */
    public void add(String pdp) {
        pdps.add(pdp);
    }

    /**
     * Adds PDPs to the set of PDPs.
     *
     * @param pdps PDPs to be added
     */
    public void addAll(Collection<String> pdps) {
        this.pdps.addAll(pdps);
    }

    /**
     * Removes PDPs from the set of PDPs.
     *
     * @param pdps PDPs to be removed
     */
    public void removeAll(Collection<String> pdps) {
        this.pdps.removeAll(pdps);
    }
}
