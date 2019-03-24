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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.pap.concepts.PdpGroupStateChangeResponse;
import org.onap.policy.models.pdp.enums.PdpState;

/**
 * Class to provide REST end points for PAP component to change state of a PDP group.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpGroupStateChangeControllerV1 extends PapRestControllerV1 {

    private final PdpGroupStateChangeProvider provider = new PdpGroupStateChangeProvider();

    /**
     * Changes state of a PDP group.
     *
     * @param requestId request ID used in ONAP logging
     * @param groupName name of the PDP group to be deleted
     * @param version version of the PDP group
     * @param state state of the PDP group
     * @return a response
     */
    // @formatter:off
    @PUT
    @Path("pdps/groups/{name}/versions/{version}")
    @ApiOperation(value = "Change state of a PDP Group",
        notes = "Changes state of PDP Group, returning optional error details",
        response = PdpGroupStateChangeResponse.class,
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

    public Response changeGroupState(
            @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId,
            @ApiParam(value = "PDP Group Name", required = true) @PathParam("name") final String groupName,
            @ApiParam(value = "PDP Group Version", required = true) @PathParam("version") final String version,
            @ApiParam(value = "PDP Group State", required = true) @QueryParam("state") final PdpState state) {

        final Pair<Status, PdpGroupStateChangeResponse> pair = provider.changeGroupState(groupName, version, state);

        return addLoggingHeaders(addVersionControlHeaders(Response.status(pair.getLeft())), requestId)
                .entity(pair.getRight()).build();
    }
}
