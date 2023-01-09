/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.pap.main.service.PolicyAuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to retrieve the audit information for
 * various operations on policies.
 */
@RestController
@RequiredArgsConstructor
public class PolicyAuditControllerV1 extends PapRestControllerV1 implements PolicyAuditControllerV1Api {

    public static final String NO_AUDIT_RECORD_FOUND = "No records found matching the input parameters";

    private final PolicyAuditService policyAuditService;

    /**
     * Queries audit information of all policies.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param startTime the starting time for the query in epoch timestamp
     * @param endTime the ending time for the query in epoch timestamp
     * @return a response
     * @throws PfModelException the exception
     */
    @Override
    public ResponseEntity<List<PolicyAudit>> getAllAuditRecords(
            UUID requestId,
            Integer recordCount,
            Long startTime,
            Long endTime) {

        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId).body(policyAuditService
            .getAuditRecords(recordCount, convertEpochtoInstant(startTime), convertEpochtoInstant(endTime)));
    }

    /**
     * Queries audit information of policies in a specific PdpGroup.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param startTime the starting time for the query in epoch timestamp
     * @param endTime the ending time for the query in epoch timestamp
     * @param pdpGroupName the pdp group name for the query
     * @return a response
     * @throws PfModelException the exception
     */
    @Override
    public ResponseEntity<Object> getAuditRecordsByGroup(
            String pdpGroupName,
            UUID requestId,
            Integer recordCount,
            Long startTime,
            Long endTime) {

        return makeOkOrNotFoundResponse(requestId, policyAuditService.getAuditRecords(pdpGroupName, recordCount,
            convertEpochtoInstant(startTime), convertEpochtoInstant(endTime)));
    }

    /**
     * Queries audit information of a specific version of a policy in a PdpGroup.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param startTime the starting time for the query in epoch timestamp
     * @param endTime the ending time for the query in epoch timestamp
     * @param pdpGroupName the pdp group name for the query
     * @param policyName name of the Policy
     * @param policyVersion version of the Policy
     * @return a response
     * @throws PfModelException the exception
     */
    @Override
    public ResponseEntity<Object> getAuditRecordsOfPolicyinPdpGroup(
            String pdpGroupName,
            String policyName,
            String policyVersion,
            UUID requestId,
            Integer recordCount,
            Long startTime,
            Long endTime) {

        return makeOkOrNotFoundResponse(requestId, policyAuditService.getAuditRecords(pdpGroupName, policyName,
            policyVersion, recordCount, convertEpochtoInstant(startTime), convertEpochtoInstant(endTime)));
    }

    /**
     * Queries audit information of a specific version of a policy.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param startTime the starting time for the query in epoch timestamp
     * @param endTime the ending time for the query in epoch timestamp
     * @param policyName name of the Policy
     * @param policyVersion version of the Policy
     * @return a response
     * @throws PfModelException the exception
     */
    @Override
    public ResponseEntity<Object> getAuditRecordsOfPolicy(
            String policyName,
            String policyVersion,
            UUID requestId,
            Integer recordCount,
            Long startTime,
            Long endTime) {

        return makeOkOrNotFoundResponse(requestId, policyAuditService.getAuditRecords(policyName, policyVersion,
            recordCount, convertEpochtoInstant(startTime), convertEpochtoInstant(endTime)));
    }

    private ResponseEntity<Object> makeOkOrNotFoundResponse(UUID requestId, Collection<PolicyAudit> result) {
        if (result.isEmpty()) {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(HttpStatus.NOT_FOUND)), requestId)
                .body(NO_AUDIT_RECORD_FOUND);
        } else {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId).body(result);
        }
    }

    private Instant convertEpochtoInstant(Long epochSecond) {
        return (epochSecond == null ? null : Instant.ofEpochSecond(epochSecond));
    }
}
