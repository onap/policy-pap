/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019,2021 Nordix Foundation.
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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import lombok.RequiredArgsConstructor;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST endpoints for PAP component health check.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@RestController
@RequestMapping(path = "/policy/pap/v1")
@RequiredArgsConstructor
public class HealthCheckRestControllerV1  extends PapRestControllerV1 {

    private final HealthCheckProvider provider;

    @GetMapping("healthcheck")
    @ApiOperation(value = "Perform healthcheck",
                    notes = "Returns healthy status of the Policy Administration component",
                    response = HealthCheckReport.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
        @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
        @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public ResponseEntity<HealthCheckReport> healthcheck() {
        return ResponseEntity.ok().body(provider.performHealthCheck(true));
    }

}
