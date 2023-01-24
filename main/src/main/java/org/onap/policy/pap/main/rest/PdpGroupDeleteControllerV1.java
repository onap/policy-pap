/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021-2023 Nordix Foundation.
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
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to delete a PDP group.
 */
@RestController
@RequiredArgsConstructor
@Profile("default")
public class PdpGroupDeleteControllerV1 extends PapRestControllerV1
    implements PdpGroupDeleteControllerV1Api {

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
    @Override
    public ResponseEntity<PdpGroupDeleteResponse> deleteGroup(String groupName, UUID requestId) {
        return doOperation(requestId, () -> provider.deleteGroup(groupName));
    }

    /**
     * Undeploys the latest version of a policy from the PDPs.
     *
     * @param requestId request ID used in ONAP logging
     * @param policyName name of the PDP Policy to be deleted
     * @return a response
     */
    @Override
    public ResponseEntity<PdpGroupDeployResponse> deletePolicy(String policyName, UUID requestId) {
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
    @Override
    public ResponseEntity<PdpGroupDeployResponse> deletePolicyVersion(
            String policyName,
            String version,
            UUID requestId) {
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
