/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021-2022 Nordix Foundation.
 * Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.common.utils.resources.PrometheusUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PdpGroupDeleteResponse;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to delete a PDP group.
 */
@RestController
@RequestMapping(path = "/policy/pap/v1")
@RequiredArgsConstructor
public class PdpGroupDeleteControllerV1 extends PapRestControllerV1 {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupDeleteControllerV1.class);

    private final PdpGroupDeleteProvider provider;
    private Timer undeploySuccessTimer;
    private Timer undeployFailureTimer;


    @Autowired
    public PdpGroupDeleteControllerV1(PdpGroupDeleteProvider provider, MeterRegistry meterRegistry) {
        this.provider = provider;
        initMetrics(meterRegistry);
    }

    /**
     * Initializes the metrics for delete operation.
     *
     * @param meterRegistry spring bean for MeterRegistry to add the new metric
     */
    public void initMetrics(MeterRegistry meterRegistry) {
        String metricName = String.join(".", "pap", "policy", "deployments");
        String description = "Timer for HTTP request to deploy/undeploy a policy";
        undeploySuccessTimer = Timer.builder(metricName).description(description)
            .tags(PrometheusUtils.OPERATION_METRIC_LABEL, PrometheusUtils.UNDEPLOY_OPERATION,
                PrometheusUtils.STATUS_METRIC_LABEL, PdpPolicyStatus.State.SUCCESS.name())
            .register(meterRegistry);
        undeployFailureTimer = Timer.builder(metricName).description(description)
            .tags(PrometheusUtils.OPERATION_METRIC_LABEL, PrometheusUtils.UNDEPLOY_OPERATION,
                PrometheusUtils.STATUS_METRIC_LABEL, PdpPolicyStatus.State.FAILURE.name())
            .register(meterRegistry);
    }

    /**
     * Deletes a PDP group.
     *
     * @param requestId request ID used in ONAP logging
     * @param groupName name of the PDP group to be deleted
     * @return a response
     */
    // @formatter:off
    @DeleteMapping("pdps/groups/{name}")
    @ApiOperation(value = "Delete PDP Group",
        notes = "Deletes a PDP Group, returning optional error details",
        response = PdpGroupDeleteResponse.class,
        tags = {"PdpGroup Delete"},
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
    public ResponseEntity<PdpGroupDeleteResponse> deleteGroup(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final UUID requestId,
        @ApiParam(value = "PDP Group Name") @PathVariable("name") String groupName) {
        return doOperation(requestId, () -> provider.deleteGroup(groupName));
    }

    /**
     * Undeploys the latest version of a policy from the PDPs.
     *
     * @param requestId request ID used in ONAP logging
     * @param policyName name of the PDP Policy to be deleted
     * @return a response
     */
    // @formatter:off
    @DeleteMapping("pdps/policies/{name}")
    @ApiOperation(value = "Undeploy a PDP Policy from PDPs",
        notes = "Undeploys the latest version of a policy from the PDPs, returning optional error details",
        response = PdpGroupDeployResponse.class,
        tags = {"PdpGroup Delete"},
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
    public ResponseEntity<PdpGroupDeployResponse> deletePolicy(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final UUID requestId,
        @ApiParam(value = "PDP Policy Name") @PathVariable("name") String policyName) {

        return doUndeployOperation(requestId,
            () -> provider.undeploy(new ToscaConceptIdentifierOptVersion(policyName, null), getPrincipal()));
    }

    /**
     * Undeploys a specific version of a policy from the PDPs.
     *
     * @param requestId request ID used in ONAP logging
     * @param policyName name of the PDP Policy to be deleted
     * @param version version to be deleted
     * @return a response
     */
    // @formatter:off
    @DeleteMapping("pdps/policies/{name}/versions/{version}")
    @ApiOperation(value = "Undeploy version of a PDP Policy from PDPs",
        notes = "Undeploys a specific version of a policy from the PDPs, returning optional error details",
        response = PdpGroupDeployResponse.class,
        tags = {"PdpGroup Delete"},
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
    public ResponseEntity<PdpGroupDeployResponse> deletePolicyVersion(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final UUID requestId,
        @ApiParam(value = "PDP Policy Name") @PathVariable("name") String policyName,
        @ApiParam(value = "PDP Policy Version") @PathVariable("version") String version) {

        return doUndeployOperation(requestId,
            () -> provider.undeploy(new ToscaConceptIdentifierOptVersion(policyName, version), getPrincipal()));
    }

    /**
     * Invokes an operation.
     *
     * @param requestId request ID
     * @param runnable operation to invoke
     * @return a {@link PdpGroupDeleteResponse} response entity
     */
    private ResponseEntity<PdpGroupDeleteResponse> doOperation(UUID requestId, RunnableWithPfEx runnable) {
        try {
            runnable.run();
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                .body(new PdpGroupDeleteResponse());

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn("delete group failed", e);
            var resp = new PdpGroupDeleteResponse();
            resp.setErrorDetails(e.getErrorResponse().getErrorMessage());
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(resp);
        }
    }

    /**
     * Invokes the undeployment operation.
     *
     * @param requestId request ID
     * @param runnable operation to invoke
     * @return a {@link PdpGroupDeployResponse} response entity
     */
    private ResponseEntity<PdpGroupDeployResponse> doUndeployOperation(UUID requestId, RunnableWithPfEx runnable) {
        Instant start = Instant.now();
        try {
            runnable.run();
            undeploySuccessTimer.record(Duration.between(start, Instant.now()));
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.accepted()), requestId)
                .body(new PdpGroupDeployResponse(PdpGroupDeployControllerV1.DEPLOYMENT_RESPONSE_MSG,
                    PdpGroupDeployControllerV1.POLICY_STATUS_URI));

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn("undeploy policy failed", e);
            var resp = new PdpGroupDeployResponse();
            resp.setErrorDetails(e.getErrorResponse().getErrorMessage());
            undeployFailureTimer.record(Duration.between(start, Instant.now()));
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(resp);
        }
    }
}
