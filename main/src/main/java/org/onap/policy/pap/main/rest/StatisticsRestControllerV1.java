/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2023 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.pap.main.service.PdpStatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST endpoints for PAP component statistics.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@RestController
@RequiredArgsConstructor
public class StatisticsRestControllerV1 extends PapRestControllerV1
    implements StatisticsRestControllerV1Api {

    private final PdpStatisticsService pdpStatisticsService;

    /**
     * get statistics of PAP.
     *
     *
     * @return a response
     */
    @Override
    public ResponseEntity<StatisticsReport> statistics(UUID requestId) {
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
            .body(pdpStatisticsService.fetchCurrentStatistics());
    }

    /**
     * get all statistics of PDP groups.
     *
     * @return a response
     */
    @Override
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpStatistics(
            UUID requestId,
            Integer recordCount,
            Long startTime,
            Long endTime) {
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId).body(pdpStatisticsService
            .fetchDatabaseStatistics(recordCount.intValue(), convertEpochtoInstant(startTime),
                    convertEpochtoInstant(endTime)));
    }


    /**
     * get all statistics of a PDP group.
     *
     * @param groupName name of the PDP group
     * @return a response
     */
    @Override
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpGroupStatistics(
            String groupName,
            UUID requestId,
            Integer recordCount,
            Long startTime,
            Long endTime) {
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
            .body(pdpStatisticsService.fetchDatabaseStatistics(groupName, recordCount.intValue(),
                    convertEpochtoInstant(startTime), convertEpochtoInstant(endTime)));
    }

    /**
     * get all statistics of sub PDP group.
     *
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @return a response
     */
    @Override
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpSubGroupStatistics(
            String groupName,
            String subType,
            UUID requestId,
            Integer recordCount,
            Long startTime,
            Long endTime) {
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
            .body(pdpStatisticsService.fetchDatabaseStatistics(groupName, subType, recordCount.intValue(),
                convertEpochtoInstant(startTime), convertEpochtoInstant(endTime)));
    }

    /**
     * get all statistics of one PDP.
     *
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @param pdpName the name of the PDP
     * @param recordCount the count of the query response, optional, default return all statistics stored
     * @return a response
     */
    @Override
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpInstanceStatistics(
            String groupName,
            String subType,
            String pdpName,
            UUID requestId,
            Integer recordCount,
            Long startTime,
            Long endTime) {
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
            .body(pdpStatisticsService.fetchDatabaseStatistics(groupName, subType, pdpName, recordCount.intValue(),
                convertEpochtoInstant(startTime), convertEpochtoInstant(endTime)));
    }

    private Instant convertEpochtoInstant(Long epochSecond) {
        return (epochSecond == null ? null : Instant.ofEpochSecond(epochSecond));
    }

}
