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
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide REST end points for PAP component to retrieve the status of deployed
 * policies.
 */
public class PolicyStatusControllerV1 extends PapRestControllerV1 {
    private static final String GET_DEPLOYMENTS_FAILED = "get deployments failed";

    private static final Logger logger = LoggerFactory.getLogger(PolicyStatusControllerV1.class);

    private final PolicyStatusProvider provider = new PolicyStatusProvider();
    private final PolicyStatusUtils statusUtils = new PolicyStatusUtils();

    /**
     * Queries status of all deployed policies.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    // @formatter:off
    @GET
    @Path("policies/deployed")
    @ApiOperation(value = "Queries status of all deployed policies",
        notes = "Queries status of all deployed policies, returning success and failure counts of the PDPs",
        responseContainer = "List", response = PolicyStatus.class,
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
        extensions = {@Extension(name = EXTENSION_NAME,
            properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
                    @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
                    @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    // @formatter:on

    public Response queryAllDeployedPolicies(
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId) {

        try {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                            .entity(provider.getStatus()).build();

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).entity(e.getErrorResponse().getErrorMessage()).build();
        }
    }


    /**
     * Queries status of specific deployed policies.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    // @formatter:off
    @GET
    @Path("policies/deployed/{name}")
    @ApiOperation(value = "Queries status of specific deployed policies",
        notes = "Queries status of specific deployed policies, returning success and failure counts of the PDPs",
        responseContainer = "List", response = PolicyStatus.class,
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
        extensions = {@Extension(name = EXTENSION_NAME,
            properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
                    @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
                    @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    // @formatter:on

    public Response queryDeployedPolicies(
                    @ApiParam(value = "Policy Id", required = true) @PathParam("name") String name,
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId) {

        try {
            // check if name is a regex pattern
            final boolean isRegex = statusUtils.isRegex(name);
            final Collection<PolicyStatus> result;
            if (isRegex) {
                //  if so, get all deployed policies and  test, which one is matched by regex
                result = provider.getByRegex(name);
            } else {
                result = provider.getStatus(new ToscaConceptIdentifierOptVersion(name, null));
            }

            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                                .entity(result).build();
            }

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).entity(e.getErrorResponse().getErrorMessage()).build();
        }
    }


    /**
     * Queries status of a specific deployed policy.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    // @formatter:off
    @GET
    @Path("policies/deployed/{name}/{version}")
    @ApiOperation(value = "Queries status of a specific deployed policy",
        notes = "Queries status of a specific deployed policy, returning success and failure counts of the PDPs",
        response = PolicyStatus.class,
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
        extensions = {@Extension(name = EXTENSION_NAME,
            properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
                    @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
                    @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    // @formatter:on

    public Response queryDeployedPolicy(@ApiParam(value = "Policy Id", required = true) @PathParam("name") String name,
                    @ApiParam(value = "Policy Version", required = true) @PathParam("version") String version,
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId) {

        try {
            final Collection<PolicyStatus> result;
            // check if name is a regex pattern
            final boolean isRegex = statusUtils.isRegex(name);
            if (isRegex) {
                //  if so, get all deployed policies and  test, which one is matched by regex
                result = provider.getByRegex(name, version);
            } else {
                result = provider.getStatus(new ToscaConceptIdentifierOptVersion(name, version));
            }

            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                                .entity(result.iterator().next()).build();
            }

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).entity(e.getErrorResponse().getErrorMessage()).build();
        }
    }


    /**
     * Queries status of all policies.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    // @formatter:off
    @GET
    @Path("policies/status")
    @ApiOperation(value = "Queries status of policies in all PdpGroups",
        notes = "Queries status of policies in all PdpGroups, "
            + "returning status of policies in all the PDPs belonging to all PdpGroups",
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
        extensions = {@Extension(name = EXTENSION_NAME,
            properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
                    @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
                    @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    // @formatter:on

    public Response getStatusOfAllPolicies(
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId) {

        try {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                            .entity(provider.getPolicyStatus()).build();

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).entity(e.getErrorResponse().getErrorMessage()).build();
        }
    }

    /**
     * Queries status of policies in a specific PdpGroup.
     *
     * @param pdpGroupName name of the PdpGroup
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    // @formatter:off
    @GET
    @Path("policies/status/{pdpGroupName}")
    @ApiOperation(value = "Queries status of policies in a specific PdpGroup",
        notes = "Queries status of policies in a specific PdpGroup, "
            + "returning status of policies in all the PDPs belonging to the PdpGroup",
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
        extensions = {@Extension(name = EXTENSION_NAME,
            properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
                    @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
                    @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    // @formatter:on

    public Response getStatusOfPoliciesByGroup(
                    @ApiParam(value = "PDP Group Name", required = true) @PathParam("pdpGroupName") String pdpGroupName,
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId) {

        try {
            Collection<PdpPolicyStatus> result = provider.getPolicyStatus(pdpGroupName);
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                                .entity(result).build();
            }

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).entity(e.getErrorResponse().getErrorMessage()).build();
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
    @GET
    @Path("policies/status/{pdpGroupName}/{policyName}")
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
        extensions = {@Extension(name = EXTENSION_NAME,
            properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
                    @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
                    @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    // @formatter:on

    public Response getStatusOfPolicies(
        @ApiParam(value = "PDP Group Name", required = true) @PathParam("pdpGroupName") String pdpGroupName,
        @ApiParam(value = "Policy Id", required = true) @PathParam("policyName") String policyName,
        @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId) {

        try {
            Collection<PdpPolicyStatus> result =
                provider.getPolicyStatus(pdpGroupName, new ToscaConceptIdentifierOptVersion(policyName, null));
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                                .entity(result).build();
            }

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).entity(e.getErrorResponse().getErrorMessage()).build();
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
    @GET
    @Path("policies/status/{pdpGroupName}/{policyName}/{policyVersion}")
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
        extensions = {@Extension(name = EXTENSION_NAME,
            properties = {@ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)})})
    @ApiResponses(value = {@ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
                    @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
                    @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)})
    // @formatter:on

    public Response getStatusOfPolicy(
        @ApiParam(value = "PDP Group Name", required = true) @PathParam("pdpGroupName") String pdpGroupName,
        @ApiParam(value = "Policy Id", required = true) @PathParam("policyName") String policyName,
        @ApiParam(value = "Policy Version", required = true) @PathParam("policyVersion") String policyVersion,
        @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId) {

        try {
            Collection<PdpPolicyStatus> result = provider.getPolicyStatus(pdpGroupName,
                new ToscaConceptIdentifierOptVersion(policyName, policyVersion));
            if (result.isEmpty()) {
                return makeNotFoundResponse(requestId);

            } else {
                return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                                .entity(result.iterator().next()).build();
            }

        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(GET_DEPLOYMENTS_FAILED, e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).entity(e.getErrorResponse().getErrorMessage()).build();
        }
    }

    /**
     * Makes a "not found" response.
     *
     * @param requestId request ID
     * @return a "not found" response
     */
    private Response makeNotFoundResponse(final UUID requestId) {
        return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.NOT_FOUND)), requestId)
                        .build();
    }
}
