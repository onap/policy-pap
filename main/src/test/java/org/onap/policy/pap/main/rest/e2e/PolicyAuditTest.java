/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.rest.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.concepts.PolicyAudit.AuditAction;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.repository.PolicyAuditRepository;
import org.onap.policy.pap.main.rest.PolicyAuditControllerV1;
import org.onap.policy.pap.main.service.PolicyAuditService;
import org.springframework.beans.factory.annotation.Autowired;

class PolicyAuditTest extends End2EndBase {
    private static final String TEST_GROUP = "testGroup";
    private static final String TEST_PDP_TYPE = "testPdpType";
    private static final ToscaConceptIdentifier POLICY_A = new ToscaConceptIdentifier("PolicyA", "1.0.0");
    private static final ToscaConceptIdentifier POLICY_B = new ToscaConceptIdentifier("PolicyB", "2.0.0");
    private static final String DEFAULT_USER = "TEST";
    private static final String POLICY_AUDIT_ENDPOINT = "policies/audit";
    private static final String URI_SEPERATOR = "/";
    private static final String QUERY_PARAMS_INVALID = "?recordCount=5&startTime=2021-07-25T01:25:15";
    private static final String QUERY_PARAMS_CORRECT = "?recordCount=5&startTime=1627219515&endTime=1627478715";
    private static final String QUERY_PARAMS_INCORRECT = "?recordCount=5&startTime=1627478715&endTime=1627565115";
    private static final int NOT_FOUND_STATUS_CODE = 404;
    private static final int BAD_REQUEST_STATUS_CODE = 400;
    private static final String BAD_REQUEST_MSG = "NumberFormatException For";

    @Autowired
    private PolicyAuditService policyAuditService;

    @Autowired
    private PolicyAuditRepository policyAuditRepository;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        setupEnv();
    }

    /**
     * Teardown after tests.
     */
    @Override
    @AfterEach
    public void tearDown() {
        policyAuditRepository.deleteAll();
        super.tearDown();
    }

    private void setupEnv() {
        List<PolicyAudit> recordList = new ArrayList<>();
        Instant auditRecordTime = Instant.ofEpochSecond(1627392315L);

        try {
            PolicyAudit audit1 = PolicyAudit.builder().pdpGroup(TEST_GROUP).pdpType(TEST_PDP_TYPE)
                            .policy(POLICY_A).action(AuditAction.DEPLOYMENT)
                            .timestamp(auditRecordTime).user(DEFAULT_USER).build();
            PolicyAudit audit2 = PolicyAudit.builder().pdpGroup(TEST_GROUP).pdpType(TEST_PDP_TYPE)
                            .policy(POLICY_B).action(AuditAction.UNDEPLOYMENT)
                            .timestamp(auditRecordTime).user(DEFAULT_USER).build();
            recordList.add(audit1);
            recordList.add(audit2);
            policyAuditService.createAuditRecords(recordList);
        } catch (Exception exp) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, exp.getMessage());
        }
    }

    @Test
    void testGetAllAuditRecords() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT;
        sendAndValidateSuccess(uri, 2);
    }

    @Test
    void testGetAllAuditRecordsWithParams() throws Exception {
        // try with correct dates in query, should result in 2 records
        String uri = POLICY_AUDIT_ENDPOINT + QUERY_PARAMS_CORRECT;
        sendAndValidateSuccess(uri, 2);

        // try with incorrect dates in query, should result in 0 record
        uri = POLICY_AUDIT_ENDPOINT + QUERY_PARAMS_INCORRECT;
        sendAndValidateSuccess(uri, 0);

        // try with invalid date format, should result in error
        uri = POLICY_AUDIT_ENDPOINT + QUERY_PARAMS_INVALID;
        sendAndValidateError(uri, BAD_REQUEST_MSG, BAD_REQUEST_STATUS_CODE);
    }

    @Test
    void testGetAuditRecordsByGroup() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP;
        sendAndValidateSuccess(uri, 2);
    }

    @Test
    void testGetAuditRecordsByGroupWithParams() throws Exception {
        // try with correct dates in query, should result in 2 records
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + QUERY_PARAMS_CORRECT;
        sendAndValidateSuccess(uri, 2);

        // try with incorrect dates in query, should result in error
        uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + QUERY_PARAMS_INCORRECT;
        sendAndValidateError(uri, PolicyAuditControllerV1.NO_AUDIT_RECORD_FOUND, NOT_FOUND_STATUS_CODE);

        // try with invalid date format, should result in error
        uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + QUERY_PARAMS_INVALID;
        sendAndValidateError(uri, BAD_REQUEST_MSG, BAD_REQUEST_STATUS_CODE);
    }

    @Test
    void testGetAuditRecordsOfPolicyWithGroup() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + URI_SEPERATOR + POLICY_A.getName()
                        + URI_SEPERATOR + POLICY_A.getVersion();
        sendAndValidateSuccess(uri, 1);
    }

    @Test
    void testGetAuditRecordsOfPolicyWithGroupWithParams() throws Exception {
        // try with correct dates in query, should result in 1 record
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + URI_SEPERATOR + POLICY_A.getName()
                        + URI_SEPERATOR + POLICY_A.getVersion() + QUERY_PARAMS_CORRECT;
        sendAndValidateSuccess(uri, 1);

        // try with incorrect dates in query, should result in error
        uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + URI_SEPERATOR + POLICY_A.getName()
                        + URI_SEPERATOR + POLICY_A.getVersion() + QUERY_PARAMS_INCORRECT;
        sendAndValidateError(uri, PolicyAuditControllerV1.NO_AUDIT_RECORD_FOUND, NOT_FOUND_STATUS_CODE);

        // try with invalid date format, should result in error
        uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + URI_SEPERATOR + POLICY_A.getName() + URI_SEPERATOR
                        + POLICY_A.getVersion() + QUERY_PARAMS_INVALID;
        sendAndValidateError(uri, BAD_REQUEST_MSG, BAD_REQUEST_STATUS_CODE);
    }

    @Test
    void testGetAuditRecordsOfPolicyWithoutGroup() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + POLICY_A.getName() + URI_SEPERATOR + POLICY_A.getVersion();
        sendAndValidateSuccess(uri, 1);
    }

    @Test
    void testGetAuditRecordsOfPolicyWithoutGroupWithParams() throws Exception {
        // try with correct dates in query, should result in 1 record
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + POLICY_A.getName() + URI_SEPERATOR + POLICY_A.getVersion()
                        + QUERY_PARAMS_CORRECT;
        sendAndValidateSuccess(uri, 1);

        // try with incorrect dates in query, should result in error
        uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + POLICY_A.getName() + URI_SEPERATOR + POLICY_A.getVersion()
                        + QUERY_PARAMS_INCORRECT;
        sendAndValidateError(uri, PolicyAuditControllerV1.NO_AUDIT_RECORD_FOUND, NOT_FOUND_STATUS_CODE);

        // try with invalid date format, should result in error
        uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + POLICY_A.getName() + URI_SEPERATOR
                        + POLICY_A.getVersion() + QUERY_PARAMS_INVALID;
        sendAndValidateError(uri, BAD_REQUEST_MSG, BAD_REQUEST_STATUS_CODE);
    }

    private void sendAndValidateSuccess(String uri, int count) throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertThat(rawresp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        List<PolicyAudit> resp = rawresp.readEntity(new GenericType<List<PolicyAudit>>() {});
        assertThat(resp).hasSize(count);
        for (PolicyAudit audit : resp) {
            if (audit.getAuditId() == 123L) {
                assertThat(audit.getPdpGroup()).isEqualTo(TEST_GROUP);
                assertThat(audit.getPdpType()).isEqualTo(TEST_PDP_TYPE);
                assertThat(audit.getPolicy()).isEqualTo(POLICY_A);
                assertThat(audit.getAction()).isEqualTo(AuditAction.DEPLOYMENT);
                assertThat(audit.getUser()).isEqualTo(DEFAULT_USER);
            } else if (audit.getAuditId() == 456L) {
                assertThat(audit.getPdpGroup()).isEqualTo(TEST_GROUP);
                assertThat(audit.getPdpType()).isEqualTo(TEST_PDP_TYPE);
                assertThat(audit.getPolicy()).isEqualTo(POLICY_B);
                assertThat(audit.getAction()).isEqualTo(AuditAction.UNDEPLOYMENT);
                assertThat(audit.getUser()).isEqualTo(DEFAULT_USER);
            }
        }
    }

    private void sendAndValidateError(String uri, String errorMessage, int statusCode) throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertThat(rawresp.getStatus()).isEqualTo(statusCode);
        String resp = rawresp.readEntity(String.class);
        assertThat(resp).contains(errorMessage);
    }
}
