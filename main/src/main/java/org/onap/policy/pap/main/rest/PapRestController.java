/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.BasicAuthDefinition;
import io.swagger.annotations.Info;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onap.policy.common.endpoints.report.HealthCheckReport;

/**
 * Class to provide REST endpoints for PAP component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@Path("/policy/pap/v1")
@Api(value = "Policy Administration Point (PAP) API")
@Produces(MediaType.APPLICATION_JSON)
@SwaggerDefinition(info = @Info(description = "Policy Pap Service", version = "v1.0", title = "Policy Pap"),
        consumes = { MediaType.APPLICATION_JSON }, produces = { MediaType.APPLICATION_JSON },
        schemes = { SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS },
        tags = { @Tag(name = "policy-pap", description = "Policy Pap Service Operations") },
        securityDefinition = @SecurityDefinition(basicAuthDefinitions = { @BasicAuthDefinition(key = "basicAuth") }))
public class PapRestController {

    @GET
    @Path("healthcheck")
    @ApiOperation(value = "Perform a system healthcheck", notes = "Returns healthy status of the Policy Pap component",
            response = HealthCheckReport.class, authorizations = @Authorization(value = "basicAuth"))
    @ApiResponses(value = { @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    public Response healthcheck() {
        return Response.status(Response.Status.OK).entity(new HealthCheckProvider().performHealthCheck()).build();
    }

    @GET
    @Path("statistics")
    @ApiOperation(value = "Fetch current statistics", notes = "Returns current statistics of the Policy PAP component",
            response = StatisticsReport.class, authorizations = @Authorization(value = "basicAuth"))
    @ApiResponses(value = { @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 500, message = "Internal Server Error") })
    public Response statistics() {
        return Response.status(Response.Status.OK).entity(new StatisticsProvider().fetchCurrentStatistics()).build();
    }
}
