/*-
 * ============LICENSE_START=======================================================
 * pap-service
 * ================================================================================
 * Copyright (C) 2018 Ericsson Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.service.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.onap.policy.pap.pdpclient.ApiException;
import org.onap.policy.pap.service.PapService;
import org.onap.policy.pap.service.nexus.NexusRestWrapperException;
import org.onap.policy.pap.service.rest.model.PdpStatusParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-06-13T13:56:19.747Z")

@Controller
public class PapRestApiController implements PapRestApi {

    private static final Logger logger = LoggerFactory.getLogger(PapRestApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public PapRestApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @Override
    public ResponseEntity<Void> reportPdpStatusPut(@ApiParam(
            value = "The parameters containing the PDP status information") @Valid @RequestBody PdpStatusParameters pdpStatusParameters) {
        String accept = request.getHeader("Accept");
        PapService papService = new PapService();
        try {
            papService.handlePdpStatusReport(pdpStatusParameters);
        } catch (NexusRestWrapperException | IOException | ApiException e) {
            logger.error("Error handling PDP status report", e);
            return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

}
