/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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

import lombok.RequiredArgsConstructor;
import org.onap.policy.common.utils.report.HealthCheckReport;
import org.onap.policy.pap.main.rest.HealthCheckRestControllerV1Api;
import org.onap.policy.pap.main.rest.PapRestControllerV1;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile("stub")
public class HealthCheckRestControllerV1Stub extends PapRestControllerV1
    implements HealthCheckRestControllerV1Api {

    private final StubUtils stubUtils;

    @Override
    public ResponseEntity<HealthCheckReport> healthcheck() {
        return stubUtils.getStubbedResponse(HealthCheckReport.class);
    }

}
