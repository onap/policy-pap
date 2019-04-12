/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.rest.depundep;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import java.util.UUID;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.pap.main.rest.PapRestControllerV1;

/**
 * Class to provide REST end points for PAP component to deploy a PDP group.
 */
public class PdpGroupDeployControllerV1 extends PapRestControllerV1 {

    private final PdpGroupDeployProvider provider = new PdpGroupDeployProvider();

    /**
     * Deploys or updates a PDP group.
     *
     * @param requestId request ID used in ONAP logging
     * @param groups PDP group configuration
     * @return a response
     */
    // @formatter:off
    @POST
    @Path("pdps")
    @ApiOperation(value = "Deploy or update PDP Groups",
        notes = "Deploys or updates a PDP Group, returning optional error details",
        response = PdpGroupDeployResponse.class,
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
    // @formatter:on

    public Response deployGroup(@HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                    @ApiParam(value = "List of PDP Group Configuration", required = true) PdpGroups groups) {

        Pair<Status, PdpGroupDeployResponse> pair = provider.createOrUpdateGroups(groups);

        return addLoggingHeaders(addVersionControlHeaders(Response.status(pair.getLeft())), requestId)
                        .entity(pair.getRight()).build();
    }

    /**
     * Deploys or updates PDP policies.
     *
     * @param requestId request ID used in ONAP logging
     * @param policies PDP policies
     * @return a response
     */
    // @formatter:off
    @POST
    @Path("pdps/policies")
    @ApiOperation(value = "Deploy or update PDP Policies",
        notes = "Deploys or updates PDP Policies, returning optional error details",
        response = PdpGroupDeployResponse.class,
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
    // @formatter:on

    public Response deployPolicies(@HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                    @ApiParam(value = "PDP Policies; only the name is required",
                                    required = true) PdpDeployPolicies policies) {

        Pair<Status, PdpGroupDeployResponse> pair = provider.deployPolicies(policies);

        return addLoggingHeaders(addVersionControlHeaders(Response.status(pair.getLeft())), requestId)
                        .entity(pair.getRight()).build();
    }
}
