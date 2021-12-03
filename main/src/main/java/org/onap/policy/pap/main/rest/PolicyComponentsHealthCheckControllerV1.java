/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019,2021 Nordix Foundation.
 *  Modifications Copyright (C) 2020 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end point for PAP component to fetch all policy components, including PAP,
 * API, Distribution, and PDPs.
 *
 * @author Yehui Wang (yehui.wang@est.tech)
 */
@RestController
@RequestMapping(path = "/policy/pap/v1")
public class PolicyComponentsHealthCheckControllerV1 extends PapRestControllerV1 {

    @Autowired
    private PolicyComponentsHealthCheckProvider provider;


    /**
     * Returns health status of all Policy components, including PAP, API, Distribution, and PDPs.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    // @formatter:off
    @GetMapping("components/healthcheck")
    @ApiOperation(value = "Returns health status of all policy components, including PAP, API, Distribution, and PDPs",
        notes = "Queries health status of all policy components, returning all policy components health status",
        response = Map.class,
        tags = {"Consolidated Healthcheck"},
        authorizations = @Authorization(value = AUTHORIZATION_TYPE),
        responseHeaders = {
            @ResponseHeader(name = VERSION_MINOR_NAME, description = VERSION_MINOR_DESCRIPTION,
                            response = String.class),
            @ResponseHeader(name = VERSION_PATCH_NAME, description = VERSION_PATCH_DESCRIPTION,
                            response = String.class),
            @ResponseHeader(name = VERSION_LATEST_NAME, description = VERSION_LATEST_DESCRIPTION,
                            response = String.class),
            @ResponseHeader(name = REQUEST_ID_NAME, description = REQUEST_ID_HDR_DESCRIPTION,
                            response = UUID.class)},
        extensions = {
            @Extension(name = EXTENSION_NAME,
                properties = {
                    @ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                    @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)
                })
            })
    @ApiResponses(value = {
        @ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
        @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
        @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)
    })
    // @formatter:on
    public ResponseEntity<Map<String, Object>> policyComponentsHealthCheck(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final UUID requestId) {
        final Pair<HttpStatus, Map<String, Object>> pair = provider.fetchPolicyComponentsHealthStatus();
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(pair.getLeft())), requestId)
            .body(pair.getRight());
    }
}
