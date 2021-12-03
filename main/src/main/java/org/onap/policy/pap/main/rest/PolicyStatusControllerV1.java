/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Nordix Foundation.
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

import com.google.re2j.PatternSyntaxException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import java.util.Collection;
import java.util.UUID;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to retrieve the status of deployed
 * policies.
 */
@DependsOn("papActivator")
@RestController
@RequestMapping(path = "/policy/pap/v1")
public class PolicyStatusControllerV1 extends PapRestControllerV1 {
    private static final String EMPTY_REGEX_ERROR_MESSAGE = "An empty string passed as a regex is not allowed";
    private static final String EMPTY_REGEX_WARNING = ". Empty string passed as Regex.";
    private static final String GET_DEPLOYMENTS_FAILED = "get deployments failed";

    private static final Logger logger = LoggerFactory.getLogger(PolicyStatusControllerV1.class);

    @Autowired
    private PolicyStatusProvider provider;

    /**
     * Queries status of all deployed policies. If regex is not null or empty, the function will only return
     * policies that match regex
     *
     * @param requestId request ID used in ONAP logging
     * @param regex regex for a policy name
     * @return a response
     */
    // @formatter:off
    @GetMapping("policies/deployed")
    @ApiOperation(value = "Queries status of all deployed policies",
        notes = "Queries status of all deployed policies, returning success and failure counts of the PDPs",
        responseContainer = "List", response = PolicyStatus.class,
        tags = {"Policy Deployment Status"},
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

    public ResponseEntity<Object> queryAllDeployedPolicies(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
        @ApiParam(value = "Regex for a policy name") @RequestParam(required = false, value = "regex") String regex) {
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

        } catch (PfModelException | PfModelRuntimeException e) {
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
    // @formatter:off
    @GetMapping("policies/deployed/{name}")
    @ApiOperation(value = "Queries status of specific deployed policies",
        notes = "Queries status of specific deployed policies, returning success and failure counts of the PDPs",
        responseContainer = "List", response = PolicyStatus.class,
        tags = {"Policy Deployment Status"},
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

    public ResponseEntity<Object> queryDeployedPolicies(
                    @ApiParam(value = "Policy Id") @PathVariable("name") String name,
                    @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
                        required = false,
                        value = REQUEST_ID_NAME) final String requestId) {

        try {
            Collection<PolicyStatus> result = provider.getStatus(new ToscaConceptIdentifierOptVersion(name, null));
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                                .body(result);
            }

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(e.getErrorResponse().getErrorMessage());
        }
    }


    /**
     * Queries status of a specific deployed policy.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    // @formatter:off
    @GetMapping("policies/deployed/{name}/{version}")
    @ApiOperation(value = "Queries status of a specific deployed policy",
        notes = "Queries status of a specific deployed policy, returning success and failure counts of the PDPs",
        response = PolicyStatus.class,
        tags = {"Policy Deployment Status"},
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

    public ResponseEntity<Object> queryDeployedPolicy(@ApiParam(value = "Policy Id") @PathVariable("name") String name,
                    @ApiParam(value = "Policy Version") @PathVariable("version") String version,
                    @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
                        required = false,
                        value = REQUEST_ID_NAME) final String requestId) {

        try {
            Collection<PolicyStatus> result = provider.getStatus(new ToscaConceptIdentifierOptVersion(name, version));
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                    .body(result.iterator().next());
            }

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(e.getErrorResponse().getErrorMessage());
        }
    }


    /**
     * Queries status of all policies.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    // @formatter:off
    @GetMapping("policies/status")
    @ApiOperation(value = "Queries status of policies in all PdpGroups",
        notes = "Queries status of policies in all PdpGroups, "
            + "returning status of policies in all the PDPs belonging to all PdpGroups",
        responseContainer = "List", response = PdpPolicyStatus.class,
        tags = {"Policy Status"},
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

    public ResponseEntity<Object> getStatusOfAllPolicies(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId) {

        try {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                .body(provider.getPolicyStatus());

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
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
    // @formatter:off
    @GetMapping("policies/status/{pdpGroupName}")
    @ApiOperation(value = "Queries status of policies in a specific PdpGroup",
        notes = "Queries status of policies in a specific PdpGroup, "
            + "returning status of policies in all the PDPs belonging to the PdpGroup",
        responseContainer = "List", response = PdpPolicyStatus.class,
        tags = {"Policy Status"},
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

    public ResponseEntity<Object> getStatusOfPoliciesByGroup(
        @ApiParam(value = "PDP Group Name") @PathVariable("pdpGroupName") String pdpGroupName,
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
        @ApiParam(value = "Regex for a policy name") @RequestParam(required = false, value = "regex") String regex) {

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

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
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
    // @formatter:off
    @GetMapping("policies/status/{pdpGroupName}/{policyName}")
    @ApiOperation(value = "Queries status of all versions of a specific policy in a specific PdpGroup",
        notes = "Queries status of all versions of a specific policy in a specific PdpGroup,"
            + " returning status of all versions of the policy in the PDPs belonging to the PdpGroup",
        responseContainer = "List", response = PdpPolicyStatus.class,
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

    public ResponseEntity<Object> getStatusOfPolicies(
        @ApiParam(value = "PDP Group Name") @PathVariable("pdpGroupName") String pdpGroupName,
        @ApiParam(value = "Policy Id") @PathVariable("policyName") String policyName,
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId) {

        try {
            Collection<PdpPolicyStatus> result =
                provider.getPolicyStatus(pdpGroupName, new ToscaConceptIdentifierOptVersion(policyName, null));
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                    .body(result);
            }

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
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
    // @formatter:off
    @GetMapping("policies/status/{pdpGroupName}/{policyName}/{policyVersion}")
    @ApiOperation(value = "Queries status of a specific version of a specific policy in a specific PdpGroup",
        notes = "Queries status of a specific version of a specific policy in a specific PdpGroup,"
            + " returning status of the policy in the PDPs belonging to the PdpGroup",
        response = PdpPolicyStatus.class,
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

    public ResponseEntity<Object> getStatusOfPolicy(
        @ApiParam(value = "PDP Group Name") @PathVariable("pdpGroupName") String pdpGroupName,
        @ApiParam(value = "Policy Id") @PathVariable("policyName") String policyName,
        @ApiParam(value = "Policy Version") @PathVariable("policyVersion") String policyVersion,
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId) {

        try {
            Collection<PdpPolicyStatus> result = provider.getPolicyStatus(pdpGroupName,
                new ToscaConceptIdentifierOptVersion(policyName, policyVersion));
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                    .body(result.iterator().next());
            }

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(e.getErrorResponse().getErrorMessage());
        }
    }

    /**
     * Makes a "not found" response.
     *
     * @param requestId request ID
     * @return a "not found" response
     */
    private ResponseEntity<Object> makeNotFoundResponse(final String requestId) {
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(HttpStatus.NOT_FOUND)), requestId)
                        .build();
    }

    private ResponseEntity<Object> makeRegexNotFoundResponse(String requestId) {
        logger.warn(GET_DEPLOYMENTS_FAILED + EMPTY_REGEX_WARNING);
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(HttpStatus.BAD_REQUEST)),
            requestId).body(EMPTY_REGEX_ERROR_MESSAGE);
    }

    private ResponseEntity<Object> makeListOrNotFoundResponse(String requestId, Collection<?> result) {
        if (result.isEmpty()) {
            return makeNotFoundResponse(requestId);
        } else {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId).body(result);
        }
    }
}
