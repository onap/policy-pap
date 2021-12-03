/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019,2021 Nordix Foundation.
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PdpGroupUpdateResponse;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to create or update PDP groups.
 */
@RestController
@RequestMapping(path = "/policy/pap/v1")
public class PdpGroupCreateOrUpdateControllerV1 extends PapRestControllerV1 {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupCreateOrUpdateControllerV1.class);

    @Autowired
    private PdpGroupCreateOrUpdateProvider provider;

    /**
     * Creates or updates one or more PDP groups.
     *
     * @param requestId request ID used in ONAP logging
     * @param groups PDP group configuration
     * @return a response
     */
    // @formatter:off
    @PostMapping("pdps/groups/batch")
    @ApiOperation(value = "Create or update PDP Groups",
        notes = "Create or update one or more PDP Groups, returning optional error details",
        response = PdpGroupUpdateResponse.class,
        tags = {"PdpGroup Create/Update"},
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

    public ResponseEntity<PdpGroupUpdateResponse> createOrUpdateGroups(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
        @ApiParam(value = "List of PDP Group Configuration", required = true) @RequestBody PdpGroups groups) {

        return doOperation(requestId, "create groups failed", () -> provider.createOrUpdateGroups(groups));
    }

    /**
     * Invokes an operation.
     *
     * @param requestId request ID
     * @param errmsg error message to log if the operation throws an exception
     * @param runnable operation to invoke
     * @return a {@link PdpGroupUpdateResponse} response entity
     */
    private ResponseEntity<PdpGroupUpdateResponse> doOperation(String requestId, String errmsg,
        RunnableWithPfEx runnable) {
        try {
            runnable.run();
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                .body(new PdpGroupUpdateResponse());
        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(errmsg, e);
            var resp = new PdpGroupUpdateResponse();
            resp.setErrorDetails(e.getErrorResponse().getErrorMessage());
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(resp);
        }
    }
}
