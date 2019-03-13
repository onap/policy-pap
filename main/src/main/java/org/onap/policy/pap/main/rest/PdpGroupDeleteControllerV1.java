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

package org.onap.policy.pap.main.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import java.util.UUID;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.pap.concepts.PdpGroupDeleteResponse;

/**
 * Class to provide REST end points for PAP component to delete a PDP group.
 */
public class PdpGroupDeleteControllerV1 extends PapRestControllerV1 {

    private final PdpGroupDeleteProvider provider = new PdpGroupDeleteProvider();

    /**
     * Deletes a PDP group.
     *
     * @param requestId request ID used in ONAP logging
     * @param groupName name of the PDP group to be deleted
     * @return a response
     */
    // @formatter:off
    @DELETE
    @Path("pdps/groups/{name}")
    @ApiOperation(value = "Delete PDP Group",
        notes = "Deletes a PDP Group, returning optional error details",
        response = PdpGroupDeleteResponse.class,
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

    public Response deleteGroup(@HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                    @ApiParam(value = "PDP Group Name", required = true) @PathParam("name") String groupName) {

        Pair<Status, PdpGroupDeleteResponse> pair = provider.deleteGroup(groupName, null);

        return addLoggingHeaders(addVersionControlHeaders(Response.status(pair.getLeft())), requestId)
                        .entity(pair.getRight()).build();
    }

    /**
     * Deletes a PDP group.
     *
     * @param requestId request ID used in ONAP logging
     * @param groupName name of the PDP group to be deleted
     * @param version version to be deleted
     * @return a response
     */
    // @formatter:off
    @DELETE
    @Path("pdps/groups/{name}/versions/{version}")
    @ApiOperation(value = "Delete version of a PDP Group",
        notes = "Deletes a version of PDP Group, returning optional error details",
        response = PdpGroupDeleteResponse.class,
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

    public Response deleteGroupVersion(
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                    @ApiParam(value = "PDP Group Name", required = true) @PathParam("name") String groupName,
                    @ApiParam(value = "PDP Group Version", required = true) @PathParam("version") String version) {

        Pair<Status, PdpGroupDeleteResponse> pair = provider.deleteGroup(groupName, version);

        return addLoggingHeaders(addVersionControlHeaders(Response.status(pair.getLeft())), requestId)
                        .entity(pair.getRight()).build();
    }

    /**
     * Deletes a PDP policy.
     *
     * @param requestId request ID used in ONAP logging
     * @param policyName name of the PDP Policy to be deleted
     * @return a response
     */
    // @formatter:off
    @DELETE
    @Path("pdps/policies/{name}")
    @ApiOperation(value = "Delete PDP Policy",
        notes = "Deletes a PDP Policy, returning optional error details",
        response = PdpGroupDeleteResponse.class,
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

    public Response deletePolicies(@HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                    @ApiParam(value = "PDP Policy Name", required = true) @PathParam("name") String policyName) {

        Pair<Status, PdpGroupDeleteResponse> pair = provider.deletePolicy(policyName, null);

        return addLoggingHeaders(addVersionControlHeaders(Response.status(pair.getLeft())), requestId)
                        .entity(pair.getRight()).build();
    }

    /**
     * Deletes a PDP policy.
     *
     * @param requestId request ID used in ONAP logging
     * @param policyName name of the PDP Policy to be deleted
     * @param version version to be deleted
     * @return a response
     */
    // @formatter:off
    @DELETE
    @Path("pdps/policies/{name}/versions/{version}")
    @ApiOperation(value = "Delete version of a PDP Policy",
        notes = "Deletes a version of a PDP Policy, returning optional error details",
        response = PdpGroupDeleteResponse.class,
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

    public Response deletePoliciesVersion(
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                    @ApiParam(value = "PDP Policy Name", required = true) @PathParam("name") String policyName,
                    @ApiParam(value = "PDP Policy Version", required = true) @PathParam("version") String version) {

        Pair<Status, PdpGroupDeleteResponse> pair = provider.deletePolicy(policyName, version);

        return addLoggingHeaders(addVersionControlHeaders(Response.status(pair.getLeft())), requestId)
                        .entity(pair.getRight()).build();
    }
}
