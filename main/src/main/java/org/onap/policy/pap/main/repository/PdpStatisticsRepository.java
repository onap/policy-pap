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
import org.onap.policy.models.pdp.persistence.concepts.JpaPdpStatistics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PdpStatisticsRepository extends JpaRepository<JpaPdpStatistics, PfGeneratedIdKey> {

    public List<JpaPdpStatistics> findByTimeStampBetween(Date startTime, Date endTime, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByTimeStampGreaterThanEqual(Date startTime, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByTimeStampLessThanEqual(Date endTime, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupName(String pdpGroup, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndTimeStampBetween(String pdpGroup, Date startTime, Date endTime,
        Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndTimeStampGreaterThanEqual(String pdpGroup, Date startTime,
        Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndTimeStampLessThanEqual(String pdpGroup, Date endTime,
        Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndPdpSubGroupName(String pdpGroup, String pdpSubGroup,
        Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndPdpSubGroupNameAndTimeStampBetween(String pdpGroup,
        String pdpSubGroup, Date startTime, Date endTime, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndPdpSubGroupNameAndTimeStampGreaterThanEqual(String pdpGroup,
        String pdpSubGroup, Date startTime, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndPdpSubGroupNameAndTimeStampLessThanEqual(String pdpGroup,
        String pdpSubGroup, Date endTime, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndPdpSubGroupNameAndKeyName(String pdpGroup, String pdpSubGroup,
        String pdp, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndPdpSubGroupNameAndKeyNameAndTimeStampGreaterThanEqual(
        String pdpGroup, String pdpSubGroup, String pdp, Date startTime, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndPdpSubGroupNameAndKeyNameAndTimeStampLessThanEqual(
        String pdpGroup, String pdpSubGroup, String pdp, Date endTime, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByPdpGroupNameAndPdpSubGroupNameAndKeyNameAndTimeStampBetween(String pdpGroup,
        String pdpSubGroup, String pdp, Date startTime, Date endTime, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByKeyName(String pdp, Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByKeyNameAndTimeStampGreaterThanEqual(String pdp, Date startTime,
        Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByKeyNameAndTimeStampLessThanEqual(String pdp, Date startTime,
        Pageable topRecordsSize);

    public List<JpaPdpStatistics> findByKeyNameAndTimeStampBetween(String pdp, Date startTime, Date endTime,
        Pageable topRecordsSize);

}
