/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 *  Modifications Copyright (C) 2022 Bell Canada. All rights reserved.
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.service.PolicyAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to manage operations related to audit of policies.
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
    private List<PolicyAudit> auditRecords = new ArrayList<>();

    private PolicyAuditService policyAuditService;

    /**
     * Constructs the object.
     *
     * @param policyAuditService the service
     */
    public PolicyAuditManager(PolicyAuditService policyAuditService) {
        this.policyAuditService = policyAuditService;
    }


    /**
     * Builds an audit object.
     *
     * @param policyId policy under action
     * @param pdpGroup pdpGroup which the policy is related to
     * @param pdpType pdp type
     * @param action which action was taken on policy
     * @param user which user started the action
     * @return PolicyAudit object
     */
    public PolicyAudit buildAudit(ToscaConceptIdentifier policyId, String pdpGroup, String pdpType, AuditAction action,
            String user) {
        return PolicyAudit.builder().action(action).pdpGroup(pdpGroup).pdpType(pdpType).policy(policyId)
                .timestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS)).user(user).build();
    }

    /**
     * Add deployments to the list of audits.
     *
     * @param policyId policy under deploy
     * @param pdpGroup PdpGroup
     * @param pdpType PDP type
     * @param user user whom triggered the deploy
     */
    public void addDeploymentAudit(ToscaConceptIdentifier policyId, String pdpGroup, String pdpType, String user) {
        logger.info("Registering a deploy for policy {}", policyId);
        auditRecords.add(buildAudit(policyId, pdpGroup, pdpType, AuditAction.DEPLOYMENT, user));
    }

    /**
     * Add deployments to the list of audits.
     *
     * @param policyId policy under undeploy
     * @param pdpGroup pdpGroup which the policy is related to
     * @param pdpType PDP type
     * @param user user whom triggered the undeploy
     */
    public void addUndeploymentAudit(ToscaConceptIdentifier policyId, String pdpGroup, String pdpType, String user) {
        logger.info("Registering an undeploy for policy {}", policyId);
        auditRecords.add(buildAudit(policyId, pdpGroup, pdpType, AuditAction.UNDEPLOYMENT, user));
    }

    /**
     * Create audit registers in DB.
     * If an exception happens, list is not cleared up, exception is logged.
     */
    public void saveRecordsToDb() {
        if (!auditRecords.isEmpty()) {
            logger.info("sending audit records to database: {}", auditRecords);
            try {
                policyAuditService.createAuditRecords(auditRecords);
                auditRecords.clear();
            } catch (RuntimeException excpt) {
                // not throwing the exception to not stop the main request.
                logger.error("Failed saving the audit records in DB.", excpt);
            }
        }
    }
}
