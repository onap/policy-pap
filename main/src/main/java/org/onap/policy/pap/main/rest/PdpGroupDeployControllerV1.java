/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2022-2023 Nordix Foundation.
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
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.DeploymentGroups;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to deploy a PDP group.
 */
@RestController
@RequiredArgsConstructor
@Profile("default")
public class PdpGroupDeployControllerV1 extends PapRestControllerV1 implements PdpGroupDeployControllerV1Api {
    public static final String POLICY_STATUS_URI = "/policy/pap/v1/policies/status";

    public static final String DEPLOYMENT_RESPONSE_MSG = "Use the policy status url to fetch the latest status. "
            + "Kindly note that when a policy is successfully undeployed,"
            + " it will no longer appear in policy status response.";

    private static final Logger logger = LoggerFactory.getLogger(PdpGroupDeployControllerV1.class);

    private final PdpGroupDeployProvider provider;
    private Timer deploySuccessTimer;
    private Timer deployFailureTimer;


    @Autowired
    public PdpGroupDeployControllerV1(PdpGroupDeployProvider provider, MeterRegistry meterRegistry) {
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
        deploySuccessTimer = Timer.builder(metricName).description(description)
            .tags(PrometheusUtils.OPERATION_METRIC_LABEL, PrometheusUtils.DEPLOY_OPERATION,
                PrometheusUtils.STATUS_METRIC_LABEL, PdpPolicyStatus.State.SUCCESS.name())
            .register(meterRegistry);
        deployFailureTimer = Timer.builder(metricName).description(description)
            .tags(PrometheusUtils.OPERATION_METRIC_LABEL, PrometheusUtils.DEPLOY_OPERATION,
                PrometheusUtils.STATUS_METRIC_LABEL, PdpPolicyStatus.State.FAILURE.name())
            .register(meterRegistry);
    }

    /**
     * Updates policy deployments within specific PDP groups.
     *
     * @param requestId request ID used in ONAP logging
     * @param groups PDP group configuration
     * @return a response
     */
    @Override
    public ResponseEntity<PdpGroupDeployResponse> updateGroupPolicies(UUID requestId, DeploymentGroups groups) {
        return doOperation(requestId, "update policy deployments failed",
            () -> provider.updateGroupPolicies(groups, getPrincipal()));
    }

    /**
     * Deploys or updates PDP policies.
     *
     * @param requestId request ID used in ONAP logging
     * @param policies PDP policies
     * @return a response
     */
    @Override
    public ResponseEntity<PdpGroupDeployResponse> deployPolicies(UUID requestId, PdpDeployPolicies policies) {
        return doOperation(requestId, "deploy policies failed",
            () -> provider.deployPolicies(policies, getPrincipal()));
    }

    /**
     * Invokes an operation.
     *
     * @param requestId request ID
     * @param errmsg error message to log if the operation throws an exception
     * @param runnable operation to invoke
     * @return a {@link PdpGroupDeployResponse} response entity
     */
    private ResponseEntity<PdpGroupDeployResponse> doOperation(UUID requestId, String errmsg,
        RunnableWithPfEx runnable) {
        Instant start = Instant.now();
        try {
            runnable.run();
            deploySuccessTimer.record(Duration.between(start, Instant.now()));
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.accepted()), requestId)
                .body(new PdpGroupDeployResponse(DEPLOYMENT_RESPONSE_MSG, POLICY_STATUS_URI));

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(errmsg, e);
            var resp = new PdpGroupDeployResponse();
            resp.setErrorDetails(e.getErrorResponse().getErrorMessage());
            deployFailureTimer.record(Duration.between(start, Instant.now()));
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(resp);
        }
    }

}
