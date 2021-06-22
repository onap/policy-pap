/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.rest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.concepts.PolicyAudit.AuditAction;
import org.onap.policy.models.pap.persistence.provider.PolicyAuditProvider.AuditFilter;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.State;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to manage operations related to audit of pdp policies.
 *
 * @author Adheli Tavares (adheli.tavares@est.tech)
 *
 */
public class PolicyAuditManager {
    private static final Logger logger = LoggerFactory.getLogger(PolicyAuditManager.class);

    /*
     * Set of policies to be audited.
     */
    @Getter(value = AccessLevel.PROTECTED)
    private List<PolicyAudit> deployments = new ArrayList<>();

    private PolicyModelsProvider provider;

    /**
     * Default constructor.
     */
    public PolicyAuditManager(PolicyModelsProvider provider) {
        this.provider = provider;
    }

    /**
     * Builds an audit object.
     *
     * @param policyId pdp policy under action
     * @param pdpGroup PdpGroup containing the PDP of interest
     * @param pdpType PdpGroup containing the PDP of interest
     * @param action which action was taken on policy
     * @param user which user started the action
     * @return PdpPolicyDeploymentAudit object
     */
    public PolicyAudit buildAudit(ToscaConceptIdentifier policyId, String pdpGroup, String pdpType, AuditAction action,
            String user) {
        return PolicyAudit.builder().action(action).pdpGroup(pdpGroup).pdpType(pdpType).policy(policyId)
                .timestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS)).user(user).build();
    }

    /**
     * Add deployments to the list of audits.
     *
     * @param policyId pdp policy under deploy/undeploy
     * @param pdpGroup PdpGroup containing the PDP of interest
     * @param pdpType PDP type (i.e., PdpSubGroup) containing the PDP of interest
     */
    public void addDeploymentAudit(ToscaConceptIdentifier policyId, String pdpGroup, String pdpType) {
        logger.info("Registering a deploy for policy {}", policyId);
        deployments.add(buildAudit(policyId, pdpGroup, pdpType, AuditAction.DEPLOYMENT, "PAP"));
    }

    /**
     * Add deployments to the list of audits.
     *
     * @param policyId pdp policy under deploy/undeploy
     * @param pdpGroup PdpGroup containing the PDP of interest
     * @param pdpType PDP type (i.e., PdpSubGroup) containing the PDP of interest
     */
    public void addUndeploymentAudit(ToscaConceptIdentifier policyId, String pdpGroup, String pdpType) {
        logger.info("Registering an undeploy for policy {}", policyId);
        deployments.add(buildAudit(policyId, pdpGroup, pdpType, AuditAction.UNDEPLOYMENT, "PAP"));
    }

    /**
     * Create deployment audit registers on DB.
     */
    public void saveDeploymentsAudits() {
        if (!deployments.isEmpty()) {
            logger.info("sending deploy audit records to database");
            provider.createAuditRecords(deployments);
            deployments.clear();
        }
    }

    /**
     * Check if processed policy for deployment was accepted and completed/failed.
     * Adds audit result as failure if that's the case.
     *
     * @param policyStatus policy status for checking deployment
     */
    public void checkForFailure(PdpPolicyStatus policyStatus) {
        if (policyStatus.getState().equals(State.FAILURE)) {
            deployments.add(buildAudit(policyStatus.getPolicy(), policyStatus.getPdpGroup(), policyStatus.getPdpType(),
                    AuditAction.DEPLOYMENT, "PAP_FAILURE"));
        }
        logger.info("processed deployments: {}", deployments);
    }

    /**
     * Collect the audits based on params informed as filter.
     * @param policy - policy name/version
     * @param action - which sort of action performed on policies
     * @param pdpGroup - PDP group which policy belongs to
     * @param fromDate - date from where should start the range
     * @param toDate - date where the range should end
     * @param maxRecords - number max of records to be collected
     * @return list of {@link PolicyAudit} or empty list if no match to filter
     */
    public List<PolicyAudit> getPolicyAudits(ToscaConceptIdentifier policy, AuditAction action, String pdpGroup,
            Instant fromDate, Instant toDate, int maxRecords) {
        // @formatter:off
        AuditFilter filter = AuditFilter.builder()
                                        .pdpGroup(pdpGroup)
                                        .action(action)
                                        .fromDate(fromDate)
                                        .toDate(toDate)
                                        .recordNum(maxRecords)
                                        .build();
        // @formatter:on

        if (policy != null) {
            filter.setName(policy.getName());
            filter.setVersion(policy.getVersion());
        }

        return provider.getAuditRecords(filter);
    }
}
