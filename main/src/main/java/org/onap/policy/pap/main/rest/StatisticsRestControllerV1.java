/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2020 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property.
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide REST endpoints for PAP component statistics.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class StatisticsRestControllerV1 extends PapRestControllerV1 {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsRestControllerV1.class);
    private static final String GET_STATISTICS_ERR_MSG = "get pdpStatistics failed";
    private static final int NO_COUNT_LIMIT = 0;

    @GET
    @Path("statistics")
    @ApiOperation(value = "Fetch current statistics",
            notes = "Returns current statistics of the Policy Administration component",
            response = StatisticsReport.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response statistics() {
        return (Response.status(Response.Status.OK)).entity(new StatisticsRestProvider().fetchCurrentStatistics())
                .build();
    }

    /**
     * get all statistics of PDP groups.
     *
     *
     * @return a response
     */
    @GET
    @Path("pdps/statistics")
    @ApiOperation(value = "Fetch  statistics for all PDP Groups and subgroups in the system",
            notes = "Returns for all PDP Groups and subgroups statistics of the Policy Administration component",
            response = Map.class, tags = {"Policy Administration (PAP) API"},
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
                    extensions = {@Extension(name = EXTENSION_NAME,
                            properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                                    @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})

    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response pdpStatistics(
            @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId) {
        try {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(new StatisticsRestProvider().fetchDatabaseStatistics(null, null, null, NO_COUNT_LIMIT))
                    .build();
        } catch (final PfModelException exp) {
            LOGGER.info(GET_STATISTICS_ERR_MSG, exp);
            return Response.status(exp.getErrorResponse().getResponseCode()).build();
        }
    }

    /**
     * get all statistics of a PDP group.
     *
     * @param groupName name of the PDP group
     * @return a response
     */
    @GET
    @Path("pdps/statistics/{group}")
    @ApiOperation(value = "Fetch current statistics for given PDP Group",
            notes = "Returns statistics for given PDP Group of the Policy Administration component",
            response = Map.class, tags = {"Policy Administration (PAP) API"},
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
            extensions = {@Extension(name = EXTENSION_NAME,
                    properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                            @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response pdpGroupStatistics(
            @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId,
            @ApiParam(value = "PDP Group Name", required = true) @PathParam("group") final String groupName) {
        try {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(new StatisticsRestProvider().fetchDatabaseStatistics(groupName, null, null, NO_COUNT_LIMIT))
                    .build();
        } catch (final PfModelException exp) {
            LOGGER.info(GET_STATISTICS_ERR_MSG, exp);
            return Response.status(exp.getErrorResponse().getResponseCode()).build();
        }
    }

    /**
     * get all statistics of sub PDP group.
     *
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @return a response
     */
    @GET
    @Path("pdps/statistics/{group}/{type}")
    @ApiOperation(value = "Fetch statistics for the specified subgroup",
            notes = "Returns  statistics for the specified subgroup of the Policy Administration component",
            response = Map.class, tags = {"Policy Administration (PAP) API"},
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
            extensions = {@Extension(name = EXTENSION_NAME,
                    properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                            @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response pdpSubGroupStatistics(
            @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId,
            @ApiParam(value = "PDP Group Name", required = true) @PathParam("group") final String groupName,
            @ApiParam(value = "PDP SubGroup type", required = true) @PathParam("type") final String subType) {
        try {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(new StatisticsRestProvider().fetchDatabaseStatistics(groupName, subType, null,
                            NO_COUNT_LIMIT))
                    .build();
        } catch (final PfModelException exp) {
            LOGGER.info(GET_STATISTICS_ERR_MSG, exp);
            return Response.status(exp.getErrorResponse().getResponseCode()).build();
        }
    }

    /**
     * get all statistics of one PDP.
     *
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @param pdpName the name of the PDP
     * @param recordCount the count of the query response
     * @return a response
     */
    @GET
    @Path("pdps/statistics/{group}/{type}/{pdp}")
    @ApiOperation(value = "Fetch statistics for the specified pdp",
            notes = "Returns  statistics for the specified pdp of the Policy Administration component",
            response = Map.class,
            tags = {"Policy Administration (PAP) API"},
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
            extensions = {@Extension(name = EXTENSION_NAME,
                    properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                            @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})

    public Response pdpInstanceStatistics(
            @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId,
            @ApiParam(value = "PDP Group Name", required = true) @PathParam("group") final String groupName,
            @ApiParam(value = "PDP SubGroup type", required = true) @PathParam("type") final String subType,
            @ApiParam(value = "PDP Instance name", required = true) @PathParam("pdp") final String pdpName,
            @ApiParam(value = "Record Count",
                    required = false) @DefaultValue("0") @QueryParam("recordCount") final int recordCount) {
        try {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(new StatisticsRestProvider().fetchDatabaseStatistics(groupName, subType, pdpName,
                            recordCount))
                    .build();
        } catch (final PfModelException exp) {
            LOGGER.info(GET_STATISTICS_ERR_MSG, exp);
            return Response.status(exp.getErrorResponse().getResponseCode()).build();
        }
    }
}
