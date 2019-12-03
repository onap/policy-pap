/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PdpGroupUpdateResponse;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide REST end points for PAP component to create or update PDP groups.
 */
public class PdpGroupCreateOrUpdateControllerV1 extends PapRestControllerV1 {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupCreateOrUpdateControllerV1.class);

    private final PdpGroupCreateOrUpdateProvider provider = new PdpGroupCreateOrUpdateProvider();

    /**
     * Creates or updates one or more PDP groups.
     *
     * @param requestId request ID used in ONAP logging
     * @param groups PDP group configuration
     * @return a response
     */
    // @formatter:off
    @POST
    @Path("pdps/groups")
    @ApiOperation(value = "Create or update PDP Groups",
        notes = "Create or update one or more PDP Groups, returning optional error details",
        response = PdpGroupUpdateResponse.class,
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

    public Response createOrUpdateGroups(
        @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "List of PDP Group Configuration", required = true) PdpGroups groups) {

        return doOperation(requestId, "create groups failed",
            () -> provider.createOrUpdateGroups(groups));
    }

    /**
     * Invokes an operation.
     *
     * @param requestId request ID
     * @param errmsg error message to log if the operation throws an exception
     * @param runnable operation to invoke
     * @return a {@link PdpGroupUpdateResponse} response entity
     */
    private Response doOperation(UUID requestId, String errmsg, RunnableWithPfEx runnable) {
        try {
            runnable.run();
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Status.OK)), requestId)
                .entity(new PdpGroupUpdateResponse()).build();

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(errmsg, e);
            PdpGroupUpdateResponse resp = new PdpGroupUpdateResponse();
            resp.setErrorDetails(e.getErrorResponse().getErrorMessage());
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).entity(resp).build();
        }
    }
}
