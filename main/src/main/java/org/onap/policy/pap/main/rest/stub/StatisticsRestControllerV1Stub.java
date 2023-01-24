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
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.pap.main.rest.PapRestControllerV1;
import org.onap.policy.pap.main.rest.StatisticsReport;
import org.onap.policy.pap.main.rest.StatisticsRestControllerV1Api;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile("stub")
public class StatisticsRestControllerV1Stub extends PapRestControllerV1
    implements StatisticsRestControllerV1Api {

    private final StubUtils stubUtils;

    @Override
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpGroupStatistics(
        String group,
        UUID requestId,
        @Valid Integer recordCount,
        @Valid Long startTime,
        @Valid Long endTime) {
        return stubUtils.getStubbedResponseStatistics();
    }

    @Override
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpInstanceStatistics(
        String group,
        String type,
        String pdp,
        UUID requestId,
        @Valid Integer recordCount,
        @Valid Long startTime,
        @Valid Long endTime) {
        return stubUtils.getStubbedResponseStatistics();
    }

    @Override
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpStatistics(
        UUID requestId, @Valid Integer recordCount, @Valid Long startTime, @Valid Long endTime) {
        return stubUtils.getStubbedResponseStatistics();
    }

    @Override
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpSubGroupStatistics(
        String group,
        String type,
        UUID requestId,
        @Valid Integer recordCount,
        @Valid Long startTime,
        @Valid Long endTime) {
        return stubUtils.getStubbedResponseStatistics();
    }

    @Override
    public ResponseEntity<StatisticsReport> statistics(UUID requestId) {
        return stubUtils.getStubbedResponse(StatisticsReport.class);
    }

}
