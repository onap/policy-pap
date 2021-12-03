/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.models.pdp.persistence.provider.PdpFilterParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST endpoints for PAP component statistics.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@DependsOn("papActivator")
@RestController
@RequestMapping(path = "/policy/pap/v1")
public class StatisticsRestControllerV1 extends PapRestControllerV1 {

    @Autowired
    private StatisticsRestProvider provider;

    /**
     * get statistics of PAP.
     *
     *
     * @return a response
     */
    @GetMapping("statistics")
    @ApiOperation(value = "Fetch current statistics",
            notes = "Returns current statistics of the Policy Administration component",
            response = StatisticsReport.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {
        @ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
        @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
        @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public ResponseEntity<StatisticsReport> statistics(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId) {
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                .body(provider.fetchCurrentStatistics())
                ;
    }

    /**
     * get all statistics of PDP groups.
     *
     *
     * @return a response
     * @throws PfModelException
     */
    @GetMapping("pdps/statistics")
    @ApiOperation(value = "Fetch  statistics for all PDP Groups and subgroups in the system",
            notes = "Returns for all PDP Groups and subgroups statistics of the Policy Administration component",
            response = Map.class, tags = {"PDP Statistics"},
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
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpStatistics(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
            @ApiParam(value = "Record Count") @RequestParam(
                defaultValue = "10", required = false,
                value = "recordCount") final int recordCount,
            @ApiParam(value = "Start time in epoch timestamp") @RequestParam(
                                required = false,
                                value = "startTime") final Long startTime,
            @ApiParam(value = "End time in epoch timestamp") @RequestParam(
                                required = false,
                                value = "endTime") final Long endTime) throws PfModelException {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                    .body(provider.fetchDatabaseStatistics(PdpFilterParameters.builder()
                                    .recordNum(recordCount)
                                    .startTime(convertEpochtoInstant(startTime))
                                    .endTime(convertEpochtoInstant(endTime))
                                    .build()))
                    ;
    }

    /**
     * get all statistics of a PDP group.
     *
     * @param groupName name of the PDP group
     * @return a response
     * @throws PfModelException
     */
    @GetMapping("pdps/statistics/{group}")
    @ApiOperation(value = "Fetch current statistics for given PDP Group",
            notes = "Returns statistics for given PDP Group of the Policy Administration component",
            response = Map.class, tags = {"PDP Statistics"},
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
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpGroupStatistics(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
            @ApiParam(value = "PDP Group Name") @PathVariable("group") final String groupName,
            @ApiParam(value = "Record Count") @RequestParam(
                defaultValue = "10",required = false,
                value = "recordCount") final int recordCount,
            @ApiParam(value = "Start time in epoch timestamp") @RequestParam(
                                required = false,
                                value = "startTime") final Long startTime,
            @ApiParam(value = "End time in epoch timestamp") @RequestParam(
                                required = false,
                                value = "endTime") final Long endTime) throws PfModelException {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                    .body(provider.fetchDatabaseStatistics(PdpFilterParameters.builder()
                                    .group(groupName)
                                    .recordNum(recordCount)
                                    .startTime(convertEpochtoInstant(startTime))
                                    .endTime(convertEpochtoInstant(endTime))
                                    .build()))
                    ;
    }

    /**
     * get all statistics of sub PDP group.
     *
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @return a response
     * @throws PfModelException
     */
    @GetMapping("pdps/statistics/{group}/{type}")
    @ApiOperation(value = "Fetch statistics for the specified subgroup",
            notes = "Returns  statistics for the specified subgroup of the Policy Administration component",
            response = Map.class, tags = {"PDP Statistics"},
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
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpSubGroupStatistics(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
            @ApiParam(value = "PDP Group Name") @PathVariable("group") final String groupName,
            @ApiParam(value = "PDP SubGroup type") @PathVariable("type") final String subType,
            @ApiParam(value = "Record Count") @RequestParam(
                defaultValue = "10",required = false,
                value = "recordCount") final int recordCount,
            @ApiParam(value = "Start time in epoch timestamp") @RequestParam(
                                required = false,
                                value = "startTime") final Long startTime,
            @ApiParam(value = "End time in epoch timestamp") @RequestParam(
                                required = false,
                                value = "endTime") final Long endTime) throws PfModelException {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                    .body(provider.fetchDatabaseStatistics(PdpFilterParameters.builder()
                                    .group(groupName)
                                    .subGroup(subType)
                                    .recordNum(recordCount)
                                    .startTime(convertEpochtoInstant(startTime))
                                    .endTime(convertEpochtoInstant(endTime))
                                    .build()))
                    ;
    }

    /**
     * get all statistics of one PDP.
     *
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @param pdpName the name of the PDP
     * @param recordCount the count of the query response, optional, default return all statistics stored
     * @return a response
     * @throws PfModelException
     */
    @GetMapping("pdps/statistics/{group}/{type}/{pdp}")
    @ApiOperation(value = "Fetch statistics for the specified pdp",
            notes = "Returns  statistics for the specified pdp of the Policy Administration component",
            response = Map.class,
            tags = {"PDP Statistics"},
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
    public ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> pdpInstanceStatistics(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
            @ApiParam(value = "PDP Group Name") @PathVariable("group") final String groupName,
            @ApiParam(value = "PDP SubGroup type") @PathVariable("type") final String subType,
            @ApiParam(value = "PDP Instance name") @PathVariable("pdp") final String pdpName,
            @ApiParam(value = "Record Count") @RequestParam(
                defaultValue = "10", required = false,
                value = "recordCount") final int recordCount,
            @ApiParam(value = "Start time in epoch timestamp") @RequestParam(
                                required = false,
                                value = "startTime") final Long startTime,
            @ApiParam(value = "End time in epoch timestamp") @RequestParam(
                                required = false,
                                value = "endTime") final Long endTime) throws PfModelException {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                    .body(provider.fetchDatabaseStatistics(
                            PdpFilterParameters.builder()
                                    .group(groupName)
                                    .subGroup(subType)
                                    .name(pdpName)
                                    .recordNum(recordCount)
                                    .startTime(convertEpochtoInstant(startTime))
                                    .endTime(convertEpochtoInstant(endTime))
                                    .build()))
                    ;
    }

    private Instant convertEpochtoInstant(Long epochSecond) {
        return (epochSecond == null ? null : Instant.ofEpochSecond(epochSecond));
    }
}
