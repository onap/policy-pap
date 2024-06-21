/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2021, 2024 Nordix Foundation.
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

import com.google.re2j.Pattern;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.onap.policy.pap.main.notification.DeploymentTracker;
import org.onap.policy.pap.main.service.PolicyStatusService;
import org.springframework.stereotype.Service;

/**
 * Provider for PAP component to query policy deployment status.
 */
@Service
@RequiredArgsConstructor
public class PolicyStatusProvider {

    private final PolicyStatusService policyStatusService;

    /**
     * Gets the deployment status of all policies.
     *
     * @return the deployment status of all policies
     */
    public Collection<PolicyStatus> getStatus() {
        return accumulate(policyStatusService.getAllPolicyStatus());
    }

    /**
     * Gets the deployment status of a policy.
     *
     * @param policy policy of interest
     * @return the deployment status of all policies
     */
    public Collection<PolicyStatus> getStatus(ToscaConceptIdentifierOptVersion policy) {
        return accumulate(policyStatusService.getAllPolicyStatus(policy));
    }

    /**
     * Gets the deployment status of a policy, returns only statuses, which matches the regex.
     *
     * @param patternString policy of interest
     * @return the deployment status of all policies
     */
    public Collection<PolicyStatus> getByRegex(String patternString) {
        // try to make pattern out of regex
        final var pattern = Pattern.compile(patternString);
        // get all the statuses
        final List<PdpPolicyStatus> policyStatuses = policyStatusService.getAllPolicyStatus();
        // filter out statuses with the wrong name
        final Collection<PdpPolicyStatus> pdpPolicyStatuses = filterWithPattern(pattern, policyStatuses);

        return accumulate(pdpPolicyStatuses);
    }


    /**
     * Accumulates the deployment status of individual PDP/policy pairs into a status for
     * a policy.
     *
     * @param source PDP/policy pairs
     * @return the deployment status of the policies
     */
    private Collection<PolicyStatus> accumulate(Collection<PdpPolicyStatus> source) {
        var tracker = new DeploymentTracker();

        for (PdpPolicyStatus status : source) {
            if (status.isDeploy()) {
                tracker.add(status);
            }
        }

        return tracker.getDeploymentStatus();
    }


    /**
     * Gets the status of all policies.
     *
     * @return the status of all policies
     */
    public Collection<PdpPolicyStatus> getPolicyStatus() {
        return policyStatusService.getAllPolicyStatus();
    }

    /**
     * Gets the status of policies in a PdpGroup.
     *
     * @param pdpGroupName the pdp group
     */
    public Collection<PdpPolicyStatus> getPolicyStatus(String pdpGroupName) {
        return policyStatusService.getGroupPolicyStatus(pdpGroupName);
    }

    /**
     * Gets the status of a policy in a PdpGroup.
     *
     * @param pdpGroupName the pdp group
     * @param policy the policy
     * @return the deployment status of the policy
     */
    public Collection<PdpPolicyStatus> getPolicyStatus(String pdpGroupName, ToscaConceptIdentifierOptVersion policy) {
        return policyStatusService.getAllPolicyStatus(policy).stream()
            .filter(p -> p.getPdpGroup().equals(pdpGroupName)).toList();
    }

    /**
     * Gets the status of policies in a PdpGroup that match the given regex.
     *
     * @param pdpGroupName  the pdp group
     * @param patternString regex
     * @return the deployment status of policies
     */
    public Collection<PdpPolicyStatus> getPolicyStatusByRegex(String pdpGroupName, String patternString) {
        final var pattern = Pattern.compile(patternString);
        // get all the statuses
        final Collection<PdpPolicyStatus> policyStatuses = getPolicyStatus(pdpGroupName);
        // filter out statuses with the wrong name
        return filterWithPattern(pattern, policyStatuses);
    }

    private Collection<PdpPolicyStatus> filterWithPattern(Pattern pattern, Collection<PdpPolicyStatus> policyStatuses) {
        return policyStatuses
            .stream()
            .filter(policyStatus -> {
                // Check policy name
                final String policyName = policyStatus
                    .getPolicy()
                    .getName();
                return pattern.matcher(policyName).matches();
            }).toList();
    }
}
