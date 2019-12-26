/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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


    @GET
    @Path("statistics")
    @ApiOperation(value = "Fetch current statistics",
            notes = "Returns current statistics of the Policy Administration component",
            response = StatisticsReport.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response statistics() {
        return Response.status(Response.Status.OK).entity(new StatisticsProvider().fetchCurrentStatistics()).build();
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
            response = Map.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response pdpStatistics() {
        try {
            return Response.status(Response.Status.OK)
                    .entity(new StatisticsProvider().fetchDatabaseStatistics(null, null, null)).build();
        } catch (final PfModelException exp) {
            LOGGER.info(GET_STATISTICS_ERR_MSG, exp);
            return Response.status(Response.Status.NOT_FOUND).build();
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
    @ApiOperation(value = "Fetch current statistics for given PDP Groups and subgroups",
            notes = "Returns statistics for given PDP Groups and subgroups of the Policy Administration component",
            response = Map.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response pdpGroupStatistics(
            @ApiParam(value = "PDP Group Name", required = true) @PathParam("group") final String groupName) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(new StatisticsProvider().fetchDatabaseStatistics(groupName, null, null)).build();
        } catch (final PfModelException exp) {
            LOGGER.info(GET_STATISTICS_ERR_MSG, exp);
            return Response.status(Response.Status.NOT_FOUND).build();
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
            response = Map.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response pdpSubGroupStatistics(
            @ApiParam(value = "PDP Group Name", required = true) @PathParam("group") final String groupName,
            @ApiParam(value = "PDP SubGroup type", required = true) @PathParam("type") final String subType) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(new StatisticsProvider().fetchDatabaseStatistics(groupName, subType, null)).build();
        } catch (final PfModelException exp) {
            LOGGER.info(GET_STATISTICS_ERR_MSG, exp);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * get all statistics of one PDP.
     *
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @param pdpName the name of the PDP
     * @return a response
     */
    @GET
    @Path("pdps/statistics/{group}/{type}/{pdp}")
    @ApiOperation(value = "Fetch statistics for the specified subgroup",
            notes = "Returns  statistics for the specified subgroup of the Policy Administration component",
            response = Map.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response pdpInstanceStatistics(
            @ApiParam(value = "PDP Group Name", required = true) @PathParam("group") final String groupName,
            @ApiParam(value = "PDP SubGroup type", required = true) @PathParam("type") final String subType,
            @ApiParam(value = "PDP Instance name", required = true) @PathParam("pdp") final String pdpName) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(new StatisticsProvider().fetchDatabaseStatistics(groupName, subType, pdpName)).build();
        } catch (final PfModelException exp) {
            LOGGER.info(GET_STATISTICS_ERR_MSG, exp);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * get specified latest statistics of one PDP.
     *
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @param pdpName the name of the PDP
     * @param num the number to read
     * @return a response
     */
    @GET
    @Path("pdps/statistics/{group}/{type}/{pdp}/{lastnum}")
    @ApiOperation(value = "Fetch specified latest statistics for the specified pdp",
            notes = "Returns specified latest statistics for the specified pdp of the Policy Administration component",
            response = List.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response pdpInstanceLatestStatistics(
            @ApiParam(value = "PDP Group Name", required = true) @PathParam("group") final String groupName,
            @ApiParam(value = "PDP SubGroup type", required = true) @PathParam("type") final String subType,
            @ApiParam(value = "PDP Instance name", required = true) @PathParam("pdp") final String pdpName,
            @ApiParam(value = "statistics num to read", required = true) @PathParam("lastnum") final int num) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(new StatisticsProvider().fetchLatestDatabaseStatistics(groupName, subType, pdpName, num))
                    .build();
        } catch (final PfModelException exp) {
            LOGGER.info(GET_STATISTICS_ERR_MSG, exp);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * get the latest statistics of one PDP.
     *
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @param pdpName the name of the PDP
     * @return a response
     */
    @GET
    @Path("pdps/statistics/{group}/{type}/{pdp}/latest")
    @ApiOperation(value = "Fetch the latest statistics of one PDP",
            notes = "Returns  the latest statistics of one PDP of the Policy Administration component",
            response = List.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public Response pdpInstanceLatestStatistics(
            @ApiParam(value = "PDP Group Name", required = true) @PathParam("group") final String groupName,
            @ApiParam(value = "PDP SubGroup type", required = true) @PathParam("type") final String subType,
            @ApiParam(value = "PDP Instance name", required = true) @PathParam("pdp") final String pdpName) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(new StatisticsProvider().fetchLatestDatabaseStatistics(groupName, subType, pdpName, 1))
                    .build();
        } catch (final PfModelException exp) {
            LOGGER.info(GET_STATISTICS_ERR_MSG, exp);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

}
