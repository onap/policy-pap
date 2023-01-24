/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.rest.stub;

import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.pap.main.rest.PapRestControllerV1;
import org.onap.policy.pap.main.rest.PolicyAuditControllerV1Api;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile("stub")
public class PolicyAuditControllerV1Stub extends PapRestControllerV1
    implements PolicyAuditControllerV1Api {

    private final StubUtils stubUtils;

    @Override
    public ResponseEntity<List<PolicyAudit>> getAllAuditRecords(
            UUID requestId,
            @Min(1) @Max(100) @Valid Integer recordCount,
            @Valid Long startTime,
            @Valid Long endTime) {
        return stubUtils.getStubbedResponseList(PolicyAudit.class);
    }

    @Override
    public ResponseEntity<Object> getAuditRecordsByGroup(
            String pdpGroupName,
            UUID requestId,
            @Min(1) @Max(100) @Valid Integer recordCount,
            @Valid Long startTime,
            @Valid Long endTime) {
        return stubUtils.getStubbedResponse(Object.class);
    }

    @Override
    public ResponseEntity<Object> getAuditRecordsOfPolicy(
            String policyName,
            String policyVersion,
            UUID requestId,
            @Min(1) @Max(100) @Valid Integer recordCount,
            @Valid Long startTime,
            @Valid Long endTime) {
        return stubUtils.getStubbedResponse(Object.class);
    }

    @Override
    public ResponseEntity<Object> getAuditRecordsOfPolicyinPdpGroup(
            String pdpGroupName,
            String policyName,
            String policyVersion,
            UUID requestId,
            @Min(1) @Max(100) @Valid Integer recordCount,
            @Valid Long startTime,
            @Valid Long endTime) {
        return stubUtils.getStubbedResponse(Object.class);
    }

}