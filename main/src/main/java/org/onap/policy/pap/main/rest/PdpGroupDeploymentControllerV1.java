/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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
import java.util.ArrayList;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.onap.policy.pap.main.model.PdpGroup;
import org.onap.policy.pap.main.model.PdpGroupDeploymentResponse;

/**
 * Class to provide REST endpoints for PAP component PDP group deployment.
 *
 * <p>Note: topic endpoints must be configured before this is created.
 */
public class PdpGroupDeploymentControllerV1 extends PapRestControllerV1 {

    /**
     * Deploys or updates a PDP group.
     *
     * @param group PDP group configuration
     * @return a response
     */
    @POST
    @Path("pdps")
    @ApiOperation(value = "Deploy or update PDP Group", notes = "Returns PDP policies",
                    response = PdpGroupDeploymentResponse.class,
                    authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
                    @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
                    @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})

    public Response deploy(@ApiParam(value = "PDP Group Configuration", required = true) PdpGroup group) {

        PdpGroupDeploymentResponse resp = new PdpGroupDeploymentResponse();
        resp.setSuccess(true);
        resp.setPdps(new ArrayList<>());

        return Response.status(Response.Status.OK).entity(resp).build();
    }
}
