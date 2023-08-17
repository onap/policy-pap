/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.service;

import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.persistence.concepts.JpaPolicyAudit;
import org.onap.policy.pap.main.repository.PolicyAuditRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PolicyAuditService {

    private static final Integer DEFAULT_MAX_RECORDS = 100;
    private static final Integer DEFAULT_MIN_RECORDS = 10;

    private final PolicyAuditRepository policyAuditRepository;

    /**
     * Create audit records.
     *
     * @param audits list of policy audit
     */
    public void createAuditRecords(@NonNull final List<PolicyAudit> audits) {
        List<JpaPolicyAudit> jpaAudits = audits.stream().map(JpaPolicyAudit::new).collect(Collectors.toList());

        var result = new BeanValidationResult("createAuditRecords", jpaAudits);

        var count = 0;
        for (JpaPolicyAudit jpaAudit : jpaAudits) {
            result.addResult(jpaAudit.validate(String.valueOf(count++)));
        }

        if (!result.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, result.getResult());
        }
        policyAuditRepository.saveAll(jpaAudits);
    }

    /**
     * Collect the audit records.
     *
     * @param recordCount the number of records to return
     * @param startTime start time of the records to be returned
     * @param endTime end time of the records to be returned
     * @return list of {@link PolicyAudit} records found
     */
    public List<PolicyAudit> getAuditRecords(int recordCount, Instant startTime, Instant endTime) {
        Pageable recordSize = getRecordSize(recordCount);
        if (startTime != null && endTime != null) {
            return asPolicyAuditList(
                policyAuditRepository.findByTimeStampBetween(Date.from(startTime), Date.from(endTime), recordSize));
        } else if (startTime == null && endTime == null) {
            return asPolicyAuditList(policyAuditRepository.findAll(recordSize).toList());
        } else if (startTime != null) {
            return asPolicyAuditList(
                policyAuditRepository.findByTimeStampGreaterThanEqual(Date.from(startTime), recordSize));
        } else {
            return asPolicyAuditList(
                policyAuditRepository.findByTimeStampLessThanEqual(Date.from(endTime), recordSize));
        }
    }

    /**
     * Collect the audit records.
     *
     * @param pdpGroup the name of the group
     * @param recordCount the number of records to return
     * @param startTime start time of the records to be returned
     * @param endTime end time of the records to be returned
     * @return list of {@link PolicyAudit} records found
     */
    public List<PolicyAudit> getAuditRecords(@NonNull String pdpGroup, int recordCount, Instant startTime,
        Instant endTime) {
        Pageable recordSize = getRecordSize(recordCount);
        if (startTime != null && endTime != null) {
            return asPolicyAuditList(policyAuditRepository.findByPdpGroupAndTimeStampBetween(pdpGroup,
                Date.from(startTime), Date.from(endTime), recordSize));
        } else if (startTime == null && endTime == null) {
            return asPolicyAuditList(policyAuditRepository.findByPdpGroup(pdpGroup, recordSize));
        } else if (startTime != null) {
            return asPolicyAuditList(policyAuditRepository.findByPdpGroupAndTimeStampGreaterThanEqual(pdpGroup,
                Date.from(startTime), recordSize));
        } else {
            return asPolicyAuditList(policyAuditRepository.findByPdpGroupAndTimeStampLessThanEqual(pdpGroup,
                Date.from(endTime), recordSize));
        }
    }

    /**
     * Collect the audit records.
     *
     * @param pdpGroup the name of the group
     * @param policyName the name of the policy
     * @param policyVersion the version of the policy
     * @param recordCount the number of records to return
     * @param startTime start time of the records to be returned
     * @param endTime end time of the records to be returned
     * @return list of {@link PolicyAudit} records found
     */
    public List<PolicyAudit> getAuditRecords(@NonNull String pdpGroup, @NonNull String policyName,
        @NonNull String policyVersion, int recordCount, Instant startTime, Instant endTime) {
        Pageable recordSize = getRecordSize(recordCount);
        if (startTime != null && endTime != null) {
            return asPolicyAuditList(policyAuditRepository.findByPdpGroupAndNameAndVersionAndTimeStampBetween(pdpGroup,
                policyName, policyVersion, Date.from(startTime), Date.from(endTime), recordSize));
        } else if (startTime == null && endTime == null) {
            return asPolicyAuditList(
                policyAuditRepository.findByPdpGroupAndNameAndVersion(pdpGroup, policyName, policyVersion, recordSize));
        } else if (startTime != null) {
            return asPolicyAuditList(policyAuditRepository.findByPdpGroupAndNameAndVersionAndTimeStampGreaterThanEqual(
                pdpGroup, policyName, policyVersion, Date.from(startTime), recordSize));
        } else {
            return asPolicyAuditList(policyAuditRepository.findByPdpGroupAndNameAndVersionAndTimeStampLessThanEqual(
                pdpGroup, policyName, policyVersion, Date.from(endTime), recordSize));
        }
    }

    /**
     * Collect the audit records.
     *
     * @param policyName the name of the policy
     * @param policyVersion the version of the policy
     * @param recordCount the number of records to return
     * @param startTime start time of the records to be returned
     * @param endTime end time of the records to be returned
     * @return list of {@link PolicyAudit} records found
     */
    public List<PolicyAudit> getAuditRecords(@NonNull String policyName, @NonNull String policyVersion, int recordCount,
        Instant startTime, Instant endTime) {
        Pageable recordSize = getRecordSize(recordCount);
        if (startTime != null && endTime != null) {
            return asPolicyAuditList(policyAuditRepository.findByNameAndVersionAndTimeStampBetween(policyName,
                policyVersion, Date.from(startTime), Date.from(endTime), recordSize));
        } else if (startTime == null && endTime == null) {
            return asPolicyAuditList(policyAuditRepository.findByNameAndVersion(policyName, policyVersion, recordSize));
        } else if (startTime != null) {
            return asPolicyAuditList(policyAuditRepository.findByNameAndVersionAndTimeStampGreaterThanEqual(policyName,
                policyVersion, Date.from(startTime), recordSize));
        } else {
            return asPolicyAuditList(policyAuditRepository.findByNameAndVersionAndTimeStampLessThanEqual(policyName,
                policyVersion, Date.from(endTime), recordSize));
        }
    }

    private Pageable getRecordSize(int recordCount) {
        if (recordCount < 1) {
            recordCount = DEFAULT_MIN_RECORDS;
        } else if (recordCount > DEFAULT_MAX_RECORDS) {
            recordCount = DEFAULT_MAX_RECORDS;
        }
        return PageRequest.of(0, recordCount, Sort.by("timeStamp").descending());
    }

    private List<PolicyAudit> asPolicyAuditList(List<JpaPolicyAudit> jpaPolicyAuditList) {
        return jpaPolicyAuditList.stream().map(JpaPolicyAudit::toAuthorative).collect(Collectors.toList());
    }
}
