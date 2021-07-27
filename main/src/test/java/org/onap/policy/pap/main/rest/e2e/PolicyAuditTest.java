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

package org.onap.policy.pap.main.rest.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.concepts.PolicyAudit.AuditAction;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;

public class PolicyAuditTest extends End2EndBase {
    private static final String TEST_GROUP = "testGroup";
    private static final String TEST_PDP_TYPE = "testPdpType";
    private static final ToscaConceptIdentifier POLICY_A = new ToscaConceptIdentifier("PolicyA", "1.0.0");
    private static final ToscaConceptIdentifier POLICY_B = new ToscaConceptIdentifier("PolicyB", "2.0.0");
    private static final String DEFAULT_USER = "TEST";
    private static final String POLICY_AUDIT_ENDPOINT = "policies/audit";
    private static final String URI_SEPERATOR = "/";
    private static final String DATE_PARSE_ERROR = "fromDate & toDate must be in yyyy-MM-dd'T'HH:mm:ss format";
    private static final String QUERY_PARAMS_INCORRECT = "?recordCount=5&fromDate=2020-07-27T12:10";
    private static final String QUERY_PARAMS_CORRECT = "?recordCount=5&fromDate=2020-07-27T12:10:55";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        setupEnv();
    }

    private void setupEnv() {
        List<PolicyAudit> recordList = new ArrayList<>();
        PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                        Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);

        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            PolicyAudit audit1 = PolicyAudit.builder().auditId(123L).pdpGroup(TEST_GROUP).pdpType(TEST_PDP_TYPE)
                            .policy(POLICY_A).action(AuditAction.DEPLOYMENT).timestamp(Instant.now()).user(DEFAULT_USER)
                            .build();
            PolicyAudit audit2 = PolicyAudit.builder().auditId(456L).pdpGroup(TEST_GROUP).pdpType(TEST_PDP_TYPE)
                            .policy(POLICY_B).action(AuditAction.UNDEPLOYMENT).timestamp(Instant.now())
                            .user(DEFAULT_USER).build();
            recordList.add(audit1);
            recordList.add(audit2);
            databaseProvider.createAuditRecords(recordList);
        } catch (final PfModelException exp) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, exp.getMessage());
        }
    }

    @Test
    public void testGetAllAuditRecords() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT;
        sendAndValidateSuccess(uri, 2);
    }

    @Test
    public void testGetAllAuditRecordsWithParams() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT + QUERY_PARAMS_CORRECT;
        sendAndValidateSuccess(uri, 2);

        // try with incorrect date format, should result in error
        uri = POLICY_AUDIT_ENDPOINT + QUERY_PARAMS_INCORRECT;
        sendAndValidateError(uri);
    }

    @Test
    public void testGetAuditRecordsByGroup() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP;
        sendAndValidateSuccess(uri, 2);
    }

    @Test
    public void testGetAuditRecordsByGroupWithParams() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + QUERY_PARAMS_CORRECT;
        sendAndValidateSuccess(uri, 2);

        // try with incorrect date format, should result in error
        uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + QUERY_PARAMS_INCORRECT;
        sendAndValidateError(uri);
    }

    @Test
    public void testGetAuditRecordsOfPolicyWithGroup() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + URI_SEPERATOR + POLICY_A.getName()
                        + URI_SEPERATOR + POLICY_A.getVersion();
        sendAndValidateSuccess(uri, 1);
    }

    @Test
    public void testGetAuditRecordsOfPolicyWithGroupWithParams() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + URI_SEPERATOR + POLICY_A.getName()
                        + URI_SEPERATOR + POLICY_A.getVersion() + QUERY_PARAMS_CORRECT;
        sendAndValidateSuccess(uri, 1);

        // try with incorrect date format, should result in error
        uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + TEST_GROUP + URI_SEPERATOR + POLICY_A.getName() + URI_SEPERATOR
                        + POLICY_A.getVersion() + QUERY_PARAMS_INCORRECT;
        sendAndValidateError(uri);
    }

    @Test
    public void testGetAuditRecordsOfPolicyWithoutGroup() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + POLICY_A.getName() + URI_SEPERATOR + POLICY_A.getVersion();
        sendAndValidateSuccess(uri, 1);
    }

    @Test
    public void testGetAuditRecordsOfPolicyWithoutGroupWithParams() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + POLICY_A.getName() + URI_SEPERATOR + POLICY_A.getVersion()
                        + QUERY_PARAMS_CORRECT;
        sendAndValidateSuccess(uri, 1);

        // try with incorrect date format, should result in error
        uri = POLICY_AUDIT_ENDPOINT + URI_SEPERATOR + POLICY_A.getName() + URI_SEPERATOR
                        + POLICY_A.getVersion() + QUERY_PARAMS_INCORRECT;
        sendAndValidateError(uri);
    }

    private void sendAndValidateSuccess(String uri, int count) throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
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

    private void sendAndValidateError(String uri) throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        String response = rawresp.readEntity(String.class);
        assertTrue(response.contains(DATE_PARSE_ERROR));
    }
}
