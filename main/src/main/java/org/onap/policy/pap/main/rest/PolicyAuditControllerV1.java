/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Bell Canada. All rights reserved.
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
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.persistence.provider.PolicyAuditProvider.AuditFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide REST end points for PAP component to retrieve the audit information for
 * various operations on policies.
 */
public class PolicyAuditControllerV1 extends PapRestControllerV1 {

    private static final Logger logger = LoggerFactory.getLogger(PolicyAuditControllerV1.class);
    private static final String GET_AUDIT_RECORD_FAILED = "get audit records failed";
    public static final String NO_AUDIT_RECORD_FOUND = "No records found matching the input parameters";

    private final PolicyAuditProvider provider = new PolicyAuditProvider();

    /**
     * Queries audit information of all policies.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param fromDate the starting date for the query in epoch timestamp
     * @param toDate the ending date for the query in epoch timestamp
     * @return a response
     */
    // @formatter:off
    @GET
    @Path("policies/audit")
    @ApiOperation(value = "Queries audit information for all the policies",
        notes = "Queries audit information for all the policies, "
            + "returning audit information for all the policies in the database",
        responseContainer = "List", response = PolicyAudit.class,
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

    public Response getAllAuditRecords(
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId,
                    @ApiParam(value = "Record count between 1-100",
                                    required = false) @QueryParam("recordCount") final int recordCount,
                    @ApiParam(value = "From date in epoch timestamp",
                                    required = false) @QueryParam("fromDate") final long fromDate,
                    @ApiParam(value = "To date in epoch timestamp",
                                    required = false) @QueryParam("toDate") final long toDate) {

        try {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                            .entity(provider.getAuditRecords(AuditFilter.builder().recordNum(recordCount)
                                            .fromDate(convertEpochtoInstant(fromDate))
                                            .toDate(convertEpochtoInstant(toDate)).build()))
                            .build();

        } catch (PfModelException | PfModelRuntimeException exp) {
            logger.warn(GET_AUDIT_RECORD_FAILED, exp);
            return addLoggingHeaders(
                            addVersionControlHeaders(Response.status(exp.getErrorResponse().getResponseCode())),
                            requestId).entity(exp.getErrorResponse().getErrorMessage()).build();
        }
    }

    /**
     * Queries audit information of policies in a specific PdpGroup.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param fromDate the starting date for the query in epoch timestamp
     * @param toDate the ending date for the query in epoch timestamp
     * @param pdpGroupName the pdp group name for the query
     * @return a response
     */
    // @formatter:off
    @GET
    @Path("policies/audit/{pdpGroupName}")
    @ApiOperation(value = "Queries audit information for all the policies in a PdpGroup",
        notes = "Queries audit information for all the policies in a PdpGroup, "
            + "returning audit information for all the policies belonging to the PdpGroup",
        responseContainer = "List", response = PolicyAudit.class,
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

    public Response getAuditRecordsByGroup(
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId,
                    @ApiParam(value = "Record count between 1-100",
                                    required = false) @QueryParam("recordCount") final int recordCount,
                    @ApiParam(value = "From date in epoch timestamp",
                                    required = false) @QueryParam("fromDate") final long fromDate,
                    @ApiParam(value = "To date in epoch timestamp",
                                    required = false) @QueryParam("toDate") final long toDate,
                    @ApiParam(value = "PDP Group Name",
                                    required = true) @PathParam("pdpGroupName") String pdpGroupName) {

        try {
            return makeOkOrNotFoundResponse(requestId,
                            provider.getAuditRecords(AuditFilter.builder().recordNum(recordCount)
                                            .fromDate((convertEpochtoInstant(fromDate)))
                                            .toDate(convertEpochtoInstant(toDate)).pdpGroup(pdpGroupName).build()));

        } catch (PfModelException | PfModelRuntimeException exp) {
            logger.warn(GET_AUDIT_RECORD_FAILED, exp);
            return addLoggingHeaders(
                            addVersionControlHeaders(Response.status(exp.getErrorResponse().getResponseCode())),
                            requestId).entity(exp.getErrorResponse().getErrorMessage()).build();
        }
    }

    /**
     * Queries audit information of a specific version of a policy in a PdpGroup.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param fromDate the starting date for the query in epoch timestamp
     * @param toDate the ending date for the query in epoch timestamp
     * @param pdpGroupName the pdp group name for the query
     * @param policyName name of the Policy
     * @param policyVersion version of the Policy
     * @return a response
     */
    // @formatter:off
    @GET
    @Path("policies/audit/{pdpGroupName}/{policyName}/{policyVersion}")
    @ApiOperation(value = "Queries audit information for a specific version of a policy in a PdpGroup",
        notes = "Queries audit information for a specific version of a policy in a PdpGroup,"
            + " returning audit information for the policy belonging to the PdpGroup",
        response = PolicyAudit.class,
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

    public Response getAuditRecordsOfPolicy(
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId,
                    @ApiParam(value = "Record count between 1-100",
                                    required = false) @QueryParam("recordCount") final int recordCount,
                    @ApiParam(value = "From date in epoch timestamp",
                                    required = false) @QueryParam("fromDate") final long fromDate,
                    @ApiParam(value = "To date in epoch timestamp",
                                    required = false) @QueryParam("toDate") final long toDate,
                    @ApiParam(value = "PDP Group Name", required = true) @PathParam("pdpGroupName") String pdpGroupName,
                    @ApiParam(value = "Policy Name", required = true) @PathParam("policyName") String policyName,
                    @ApiParam(value = "Policy Version",
                                    required = true) @PathParam("policyVersion") String policyVersion) {

        try {
            return makeOkOrNotFoundResponse(requestId,
                            provider.getAuditRecords(AuditFilter.builder().recordNum(recordCount)
                                            .fromDate(convertEpochtoInstant(fromDate))
                                            .toDate(convertEpochtoInstant(toDate)).pdpGroup(pdpGroupName)
                                            .name(policyName).version(policyVersion).build()));

        } catch (PfModelException | PfModelRuntimeException exp) {
            logger.warn(GET_AUDIT_RECORD_FAILED, exp);
            return addLoggingHeaders(
                            addVersionControlHeaders(Response.status(exp.getErrorResponse().getResponseCode())),
                            requestId).entity(exp.getErrorResponse().getErrorMessage()).build();
        }
    }

    /**
     * Queries audit information of a specific version of a policy.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param fromDate the starting date for the query in epoch timestamp
     * @param toDate the ending date for the query in epoch timestamp
     * @param policyName name of the Policy
     * @param policyVersion version of the Policy
     * @return a response
     */
    // @formatter:off
    @GET
    @Path("policies/audit/{policyName}/{policyVersion}")
    @ApiOperation(value = "Queries audit information for a specific version of a policy",
        notes = "Queries audit information for a specific version of a policy,"
            + " returning audit information for the policy",
        response = PolicyAudit.class,
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

    public Response getAuditRecordsOfPolicy(
                    @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) final UUID requestId,
                    @ApiParam(value = "Record count between 1-100",
                                    required = false) @QueryParam("recordCount") final int recordCount,
                    @ApiParam(value = "From date in epoch timestamp",
                                    required = false) @QueryParam("fromDate") final long fromDate,
                    @ApiParam(value = "To date in epoch timestamp",
                                    required = false) @QueryParam("toDate") final long toDate,
                    @ApiParam(value = "Policy Name", required = true) @PathParam("policyName") String policyName,
                    @ApiParam(value = "Policy Version",
                                    required = true) @PathParam("policyVersion") String policyVersion) {

        try {
            return makeOkOrNotFoundResponse(requestId, provider.getAuditRecords(AuditFilter.builder()
                            .recordNum(recordCount).fromDate(convertEpochtoInstant(fromDate))
                            .toDate(convertEpochtoInstant(toDate)).name(policyName).version(policyVersion).build()));

        } catch (PfModelException | PfModelRuntimeException exp) {
            logger.warn(GET_AUDIT_RECORD_FAILED, exp);
            return addLoggingHeaders(
                            addVersionControlHeaders(Response.status(exp.getErrorResponse().getResponseCode())),
                            requestId).entity(exp.getErrorResponse().getErrorMessage()).build();
        }
    }

    private Response makeOkOrNotFoundResponse(UUID requestId, Collection<PolicyAudit> result) {
        if (result.isEmpty()) {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.NOT_FOUND)), requestId)
                            .entity(NO_AUDIT_RECORD_FOUND).build();
        } else {
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                            .entity(result).build();
        }
    }

    private Instant convertEpochtoInstant(long epochSecond) {
        return (epochSecond == 0L ? null : Instant.ofEpochSecond(epochSecond));
    }
}
