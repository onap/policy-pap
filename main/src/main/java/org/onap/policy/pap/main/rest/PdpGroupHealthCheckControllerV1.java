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
import org.onap.policy.models.pdp.concepts.Pdps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DependsOn("papActivator")
@RestController
@RequestMapping(path = "/policy/pap/v1")
public class PdpGroupHealthCheckControllerV1 extends PapRestControllerV1 {

    @Autowired
    private PdpGroupHealthCheckProvider provider;

    // @formatter:off
    @GetMapping("pdps/healthcheck")
    @ApiOperation(value = "Returns health status of all PDPs registered with PAP",
    notes = "Queries health status of all PDPs, returning all pdps health status",
    response = Pdps.class,
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
    public ResponseEntity<Pdps> pdpGroupHealthCheck(@ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
        required = false,
        value = REQUEST_ID_NAME) final String requestId) throws PfModelException {
        Pair<HttpStatus, Pdps> pair = provider.fetchPdpGroupHealthStatus();
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(pair.getLeft())), requestId)
            .body(pair.getRight());
    }
}
