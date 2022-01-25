/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.repository;

import java.util.Date;
import java.util.List;
import org.onap.policy.models.base.PfGeneratedIdKey;
import org.onap.policy.models.pap.persistence.concepts.JpaPolicyAudit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyAuditRepository extends JpaRepository<JpaPolicyAudit, PfGeneratedIdKey> {

    List<JpaPolicyAudit> findByTimeStampBetween(Date startTime, Date endTime, Pageable topRecordsSize);

    List<JpaPolicyAudit> findByTimeStampGreaterThanEqual(Date startTime, Pageable topRecordsSize);

    List<JpaPolicyAudit> findByTimeStampLessThanEqual(Date endTime, Pageable topRecordsSize);

    List<JpaPolicyAudit> findByPdpGroup(String pdpGroup, Pageable topRecordsSize);

    List<JpaPolicyAudit> findByPdpGroupAndTimeStampGreaterThanEqual(String pdpGroup, Date startTime,
        Pageable topRecordsSize);

    List<JpaPolicyAudit> findByPdpGroupAndTimeStampLessThanEqual(String pdpGroup, Date endTime,
        Pageable topRecordsSize);

    List<JpaPolicyAudit> findByPdpGroupAndTimeStampBetween(String pdpGroup, Date startTime, Date endTime,
        Pageable topRecordsSize);

    List<JpaPolicyAudit> findByPdpGroupAndKeyNameAndKeyVersion(String pdpGroup, String policyName, String policyVersion,
        Pageable topRecordsSize);

    List<JpaPolicyAudit> findByPdpGroupAndKeyNameAndKeyVersionAndTimeStampGreaterThanEqual(String pdpGroup,
        String policyName, String policyVersion, Date startTime, Pageable topRecordsSize);

    List<JpaPolicyAudit> findByPdpGroupAndKeyNameAndKeyVersionAndTimeStampLessThanEqual(String pdpGroup,
        String policyName, String policyVersion, Date endTime, Pageable topRecordsSize);

    List<JpaPolicyAudit> findByPdpGroupAndKeyNameAndKeyVersionAndTimeStampBetween(String pdpGroup, String policyName,
        String policyVersion, Date startTime, Date endTime, Pageable topRecordsSize);

    List<JpaPolicyAudit> findByKeyNameAndKeyVersion(String policyName, String policyVersion, Pageable topRecordsSize);

    List<JpaPolicyAudit> findByKeyNameAndKeyVersionAndTimeStampGreaterThanEqual(String policyName, String policyVersion,
        Date startTime, Pageable topRecordsSize);

    List<JpaPolicyAudit> findByKeyNameAndKeyVersionAndTimeStampLessThanEqual(String policyName, String policyVersion,
        Date endTime, Pageable topRecordsSize);

    List<JpaPolicyAudit> findByKeyNameAndKeyVersionAndTimeStampBetween(String policyName, String policyVersion,
        Date startTime, Date endTime, Pageable topRecordsSize);
}
