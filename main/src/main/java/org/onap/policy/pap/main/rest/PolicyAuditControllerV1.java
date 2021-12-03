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
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.persistence.provider.PolicyAuditProvider.AuditFilter;
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
 * Class to provide REST end points for PAP component to retrieve the audit information for
 * various operations on policies.
 */
@DependsOn("papActivator")
@RestController
@RequestMapping(path = "/policy/pap/v1")
public class PolicyAuditControllerV1 extends PapRestControllerV1 {

    public static final String NO_AUDIT_RECORD_FOUND = "No records found matching the input parameters";

    @Autowired
    private PolicyAuditProvider provider;

    /**
     * Queries audit information of all policies.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param startTime the starting time for the query in epoch timestamp
     * @param endTime the ending time for the query in epoch timestamp
     * @return a response
     * @throws PfModelException
     */
    // @formatter:off
    @GetMapping("policies/audit")
    @ApiOperation(value = "Queries audit information for all the policies",
        notes = "Queries audit information for all the policies, "
            + "returning audit information for all the policies in the database",
        responseContainer = "List", response = PolicyAudit.class,
        tags = {"Policy Audit"},
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
    public ResponseEntity<Collection<PolicyAudit>> getAllAuditRecords(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
        @ApiParam(value = "Record count between 1-100") @RequestParam(
            defaultValue = "10",
            required = false,
            value = "recordCount") final int recordCount,
        @ApiParam(value = "Start time in epoch timestamp") @RequestParam(
            required = false,
            value = "startTime") final Long startTime,
        @ApiParam(value = "End time in epoch timestamp") @RequestParam(
            required = false,
            value = "endTime") final Long endTime)
        throws PfModelException {

        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
            .body(provider.getAuditRecords(AuditFilter.builder().recordNum(recordCount)
                .fromDate(convertEpochtoInstant(startTime)).toDate(convertEpochtoInstant(endTime)).build()));
    }

    /**
     * Queries audit information of policies in a specific PdpGroup.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param startTime the starting time for the query in epoch timestamp
     * @param endTime the ending time for the query in epoch timestamp
     * @param pdpGroupName the pdp group name for the query
     * @return a response
     * @throws PfModelException
     */
    // @formatter:off
    @GetMapping("policies/audit/{pdpGroupName}")
    @ApiOperation(value = "Queries audit information for all the policies in a PdpGroup",
        notes = "Queries audit information for all the policies in a PdpGroup, "
            + "returning audit information for all the policies belonging to the PdpGroup",
        responseContainer = "List", response = PolicyAudit.class,
        tags = {"Policy Audit"},
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
    public ResponseEntity<Object> getAuditRecordsByGroup(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
        @ApiParam(value = "Record count between 1-100") @RequestParam(
            defaultValue = "10",
            required = false,
            value = "recordCount") final int recordCount,
        @ApiParam(value = "Start time in epoch timestamp") @RequestParam(
            required = false,
            value = "startTime") final Long startTime,
        @ApiParam(value = "End time in epoch timestamp") @RequestParam(
            required = false,
            value = "endTime") final Long endTime,
        @ApiParam(value = "PDP Group Name") @PathVariable("pdpGroupName") String pdpGroupName) throws PfModelException {

        return makeOkOrNotFoundResponse(requestId,
            provider.getAuditRecords(
                AuditFilter.builder().recordNum(recordCount).fromDate((convertEpochtoInstant(startTime)))
                    .toDate(convertEpochtoInstant(endTime)).pdpGroup(pdpGroupName).build()));
    }

    /**
     * Queries audit information of a specific version of a policy in a PdpGroup.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param startTime the starting time for the query in epoch timestamp
     * @param endTime the ending time for the query in epoch timestamp
     * @param pdpGroupName the pdp group name for the query
     * @param policyName name of the Policy
     * @param policyVersion version of the Policy
     * @return a response
     * @throws PfModelException
     */
    // @formatter:off
    @GetMapping("policies/audit/{pdpGroupName}/{policyName}/{policyVersion}")
    @ApiOperation(value = "Queries audit information for a specific version of a policy in a PdpGroup",
        notes = "Queries audit information for a specific version of a policy in a PdpGroup,"
            + " returning audit information for the policy belonging to the PdpGroup",
        response = PolicyAudit.class,
        tags = {"Policy Audit"},
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

    public ResponseEntity<Object> getAuditRecordsOfPolicy(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
        @ApiParam(value = "Record count between 1-100", required = false) @RequestParam(
            defaultValue = "10",
            required = false,
            value = "recordCount") final int recordCount,
        @ApiParam(value = "Start time in epoch timestamp", required = false) @RequestParam(
            required = false,
            value = "startTime") final Long startTime,
        @ApiParam(value = "End time in epoch timestamp") @RequestParam(
            required = false,
            value = "endTime") final Long endTime,
        @ApiParam(value = "PDP Group Name") @PathVariable("pdpGroupName") String pdpGroupName,
        @ApiParam(value = "Policy Name") @PathVariable("policyName") String policyName,
        @ApiParam(value = "Policy Version") @PathVariable(value = "policyVersion") String policyVersion)
        throws PfModelException {

        return makeOkOrNotFoundResponse(requestId,
            provider.getAuditRecords(AuditFilter.builder().recordNum(recordCount)
                .fromDate(convertEpochtoInstant(startTime)).toDate(convertEpochtoInstant(endTime))
                .pdpGroup(pdpGroupName).name(policyName).version(policyVersion).build()));
    }

    /**
     * Queries audit information of a specific version of a policy.
     *
     * @param requestId request ID used in ONAP logging
     * @param recordCount number of records to fetch
     * @param startTime the starting time for the query in epoch timestamp
     * @param endTime the ending time for the query in epoch timestamp
     * @param policyName name of the Policy
     * @param policyVersion version of the Policy
     * @return a response
     * @throws PfModelException
     */
    // @formatter:off
    @GetMapping("policies/audit/{policyName}/{policyVersion}")
    @ApiOperation(value = "Queries audit information for a specific version of a policy",
        notes = "Queries audit information for a specific version of a policy,"
            + " returning audit information for the policy",
        response = PolicyAudit.class,
        tags = {"Policy Audit"},
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

    public ResponseEntity<Object> getAuditRecordsOfPolicy(
        @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) @RequestHeader(
            required = false,
            value = REQUEST_ID_NAME) final String requestId,
        @ApiParam(value = "Record count between 1-100") @RequestParam(
            defaultValue = "10",
            required = false,
            value = "recordCount") final int recordCount,
        @ApiParam(value = "Start time in epoch timestamp") @RequestParam(
            required = false,
            value = "startTime") final Long startTime,
        @ApiParam(value = "End time in epoch timestamp") @RequestParam(
            required = false,
            value = "endTime") final Long endTime,
        @ApiParam(value = "Policy Name") @PathVariable(required = true, value = "policyName") String policyName,
        @ApiParam(
            value = "Policy Version") @PathVariable(required = true, value = "policyVersion") String policyVersion)
        throws PfModelException {

        return makeOkOrNotFoundResponse(requestId,
            provider
                .getAuditRecords(AuditFilter.builder().recordNum(recordCount).fromDate(convertEpochtoInstant(startTime))
                    .toDate(convertEpochtoInstant(endTime)).name(policyName).version(policyVersion).build()));
    }

    private ResponseEntity<Object> makeOkOrNotFoundResponse(String requestId, Collection<PolicyAudit> result) {
        if (result.isEmpty()) {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(HttpStatus.NOT_FOUND)), requestId)
                .body(NO_AUDIT_RECORD_FOUND);
        } else {
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId).body(result);
        }
    }

    private Instant convertEpochtoInstant(Long epochSecond) {
        return (epochSecond == null ? null : Instant.ofEpochSecond(epochSecond));
    }
}
