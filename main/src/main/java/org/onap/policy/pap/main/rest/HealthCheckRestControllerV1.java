package org.onap.policy.pap.main.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DependsOn("papActivator")
@RestController
@RequestMapping(path = "/policy/pap/v1")
public class HealthCheckRestControllerV1  extends PapRestControllerV1 {

    @Autowired
    private HealthCheckProvider provider;

    @GetMapping("healthcheck")
    @ApiOperation(value = "Perform healthcheck",
    notes = "Returns healthy status of the Policy Administration component",
    response = HealthCheckReport.class, authorizations = @Authorization(value = AUTHORIZATION_TYPE))
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
        @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
        @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    public ResponseEntity<HealthCheckReport> healthcheck() {
        return ResponseEntity.ok().body(provider.performHealthCheck());
    }

}
