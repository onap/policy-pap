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
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpGroupStateChangeResponse;
import org.onap.policy.models.pdp.enums.PdpState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to change state of a PDP group.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@DependsOn("papActivator")
@RestController
@RequestMapping(path = "/policy/pap/v1")
public class PdpGroupStateChangeControllerV1 extends PapRestControllerV1 {

    @Autowired
    private PdpGroupStateChangeProvider provider;

    /**
     * Changes state of a PDP group.
     *
     * @param requestId request ID used in ONAP logging
     * @param groupName name of the PDP group to be deleted
     * @param state state of the PDP group
     * @return a response
     * @throws PfModelException
     */
    // @formatter:off
    @PutMapping("pdps/groups/{name}")
    @ApiOperation(value = "Change state of a PDP Group",
    notes = "Changes state of PDP Group, returning optional error details",
    response = PdpGroupStateChangeResponse.class,
    tags = {"PdpGroup State Change"},
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
    public ResponseEntity<PdpGroupStateChangeResponse> changeGroupState(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
        @ApiParam(value = "PDP Group Name") @PathVariable("name") String groupName,
        @ApiParam(value = "PDP Group State") @RequestParam("state") final PdpState state) throws PfModelException {

        final Pair<HttpStatus, PdpGroupStateChangeResponse> pair = provider.changeGroupState(groupName, state);
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(pair.getLeft())), requestId)
            .body(pair.getRight());
    }
}
