/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.pap.service.rest;

import io.swagger.annotations.ApiParam;
import java.io.IOException;
import javax.validation.Valid;
import org.onap.policy.pap.pdpclient.ApiException;
import org.onap.policy.pap.service.PapService;
import org.onap.policy.pap.service.nexus.NexusRestWrapperException;
import org.onap.policy.pap.service.rest.model.PdpStatusParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-06-13T13:56:19.747Z")

@RestController
public class PapRestApiController implements PapRestApi {

    @Autowired
    PapService papService;

    private static final Logger logger = LoggerFactory.getLogger(PapRestApiController.class);

    @Override
    public ResponseEntity<Void> reportPdpStatusPut(@ApiParam(
            value = "The parameters containing the PDP status information") @Valid @RequestBody PdpStatusParameters pdpStatusParameters) {
        try {
            papService.handlePdpStatusReport(pdpStatusParameters);
        } catch (NexusRestWrapperException | IOException | ApiException e) {
            logger.error("Error handling PDP status report", e);
            return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

}
