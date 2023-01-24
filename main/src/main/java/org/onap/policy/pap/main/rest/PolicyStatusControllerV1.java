/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021-2023 Nordix Foundation.
 * Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

import com.google.re2j.PatternSyntaxException;
import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to retrieve the status of deployed
 * policies.
 */
@RestController
@RequiredArgsConstructor
@Profile("default")
public class PolicyStatusControllerV1 extends PapRestControllerV1 implements PolicyStatusControllerV1Api {
    private static final String EMPTY_REGEX_ERROR_MESSAGE = "An empty string passed as a regex is not allowed";
    private static final String EMPTY_REGEX_WARNING = ". Empty string passed as Regex.";
    private static final String GET_DEPLOYMENTS_FAILED = "get deployments failed";

    private static final Logger logger = LoggerFactory.getLogger(PolicyStatusControllerV1.class);

    private final PolicyStatusProvider provider;

    /**
     * Queries status of all deployed policies. If regex is not null or empty, the function will only return
     * policies that match regex
     *
     * @param requestId request ID used in ONAP logging
     * @param regex regex for a policy name
     * @return a response
     */
    @Override
    public ResponseEntity<Object> queryAllDeployedPolicies(UUID requestId, String regex) {
        try {
            final Collection<PolicyStatus> result;
            if (regex == null) {
                result = provider.getStatus();
            } else if (regex.isBlank()) {
                return makeRegexNotFoundResponse(requestId);
            } else {
                result = provider.getByRegex(regex);
            }
            return makeListOrNotFoundResponse(requestId, result);

        } catch (PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(e.getErrorResponse().getErrorMessage());
        } catch (PatternSyntaxException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(HttpStatus.BAD_REQUEST)), requestId)
                .body(e.getMessage());
        }
    }

    /**
     * Queries status of specific deployed policies.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    @Override
    public ResponseEntity<Object> queryDeployedPolicies(String name, UUID requestId) {

        try {
            Collection<PolicyStatus> result = provider.getStatus(new ToscaConceptIdentifierOptVersion(name, null));
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId).body(result);
            }

        } catch (PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(e.getErrorResponse().getErrorMessage());
        }
    }


    /**
     * Queries status of a specific deployed policy.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    @Override
    public ResponseEntity<Object> queryDeployedPolicy(String name, String version, UUID requestId) {

        try {
            Collection<PolicyStatus> result = provider.getStatus(new ToscaConceptIdentifierOptVersion(name, version));
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                    .body(result.iterator().next());
            }

        } catch (PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(e.getErrorResponse().getErrorMessage());
        }
    }


    /**
     * Queries status of all policies.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    @Override
    public ResponseEntity<Object> getStatusOfAllPolicies(UUID requestId) {

        try {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                .body(provider.getPolicyStatus());

        } catch (PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(e.getErrorResponse().getErrorMessage());
        }
    }

    /**
     * Queries status of policies in a specific PdpGroup. if regex is not null or empty, the function will only return
     * policies that match regex
     *
     * @param pdpGroupName name of the PdpGroup
     * @param requestId request ID used in ONAP logging
     * @param regex regex for a policy name
     * @return a response
     */
    @Override
    public ResponseEntity<Object> getStatusOfPoliciesByGroup(
        String pdpGroupName,
        UUID requestId,
        String regex) {

        try {
            final Collection<PdpPolicyStatus> result;
            if (regex == null) {
                result = provider.getPolicyStatus(pdpGroupName);
            } else if (regex.isBlank()) {
                return makeRegexNotFoundResponse(requestId);
            } else {
                result = provider.getPolicyStatusByRegex(pdpGroupName, regex);
            }
            return makeListOrNotFoundResponse(requestId, result);

        } catch (PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(e.getErrorResponse().getErrorMessage());
        } catch (PatternSyntaxException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(HttpStatus.BAD_REQUEST)), requestId)
                .body(e.getMessage());
        }
    }

    /**
     * Queries status of all versions of a specific policy in a specific PdpGroup.
     *
     * @param pdpGroupName name of the PdpGroup
     * @param policyName name of the Policy
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    @Override
    public ResponseEntity<Object> getStatusOfPolicies(
        String pdpGroupName,
        String policyName,
        UUID requestId) {

        try {
            Collection<PdpPolicyStatus> result =
                provider.getPolicyStatus(pdpGroupName, new ToscaConceptIdentifierOptVersion(policyName, null));
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                    .body(result);
            }

        } catch (PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(e.getErrorResponse().getErrorMessage());
        }
    }


    /**
     * Queries status of a specific version of a specific policy in a specific PdpGroup.
     *
     * @param pdpGroupName name of the PdpGroup
     * @param policyName name of the Policy
     * @param policyVersion version of the Policy
     * @param requestId request ID used in ONAP logging
     * @return a response
     */

    @Override
    public ResponseEntity<Object> getStatusOfPolicy(
            String pdpGroupName,
            String policyName,
            String policyVersion,
            UUID requestId) {

        try {
            Collection<PdpPolicyStatus> result = provider.getPolicyStatus(pdpGroupName,
                new ToscaConceptIdentifierOptVersion(policyName, policyVersion));
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                    .body(result.iterator().next());
            }

        } catch (PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(e.getErrorResponse().getErrorMessage());
        }
    }

    /**
     * Makes a "not found" response.
     *
     * @param requestId request ID
     * @return a "not found" response
     */
    private ResponseEntity<Object> makeNotFoundResponse(final UUID requestId) {
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(HttpStatus.NOT_FOUND)), requestId)
                        .build();
    }

    private ResponseEntity<Object> makeRegexNotFoundResponse(UUID requestId) {
        logger.warn(GET_DEPLOYMENTS_FAILED + EMPTY_REGEX_WARNING);
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(HttpStatus.BAD_REQUEST)),
            requestId).body(EMPTY_REGEX_ERROR_MESSAGE);
    }

    private ResponseEntity<Object> makeListOrNotFoundResponse(UUID requestId, Collection<?> result) {
        if (result.isEmpty()) {
            return makeNotFoundResponse(requestId);
        } else {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId).body(result);
        }
    }
}
