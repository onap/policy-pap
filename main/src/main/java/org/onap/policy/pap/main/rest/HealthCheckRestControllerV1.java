/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2024 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property.
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

import lombok.RequiredArgsConstructor;
import org.onap.policy.common.utils.report.HealthCheckReport;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST endpoints for PAP component health check.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@RestController
@RequiredArgsConstructor
@Profile("default")
public class HealthCheckRestControllerV1  extends PapRestControllerV1 implements HealthCheckRestControllerV1Api {

    private final HealthCheckProvider provider;

    @Override
    public ResponseEntity<HealthCheckReport> healthcheck() {
        var report = provider.performHealthCheck(true);
        return ResponseEntity.status(report.getCode()).body(report);
    }

}
