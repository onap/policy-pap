/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2022 Nordix Foundation.
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

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

/**
 * Note: this tests failure cases; success cases are tested by tests in the "e2e" package.
 */
@ActiveProfiles("test")
public class TestPolicyAuditControllerV1 extends CommonPapRestServer {

    private static final String POLICY_AUDIT_ENDPOINT = "policies/audit";

    @Test
    public void testSwagger() throws Exception {

        super.testSwagger(POLICY_AUDIT_ENDPOINT);
        super.testSwagger(POLICY_AUDIT_ENDPOINT + "/{pdpGroupName}");
        super.testSwagger(POLICY_AUDIT_ENDPOINT + "/{pdpGroupName}/{policyName}/{policyVersion}");
        super.testSwagger(POLICY_AUDIT_ENDPOINT + "/{policyName}/{policyVersion}");
    }

    @Test
    public void testGetAllAuditRecords() throws Exception {
        String uri = POLICY_AUDIT_ENDPOINT;

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, req -> req.get());
    }

    @Test
    public void testGetAuditRecordsByGroup() throws Exception {
        checkRequest(POLICY_AUDIT_ENDPOINT + "/my-group-name");
    }

    @Test
    public void testGetAuditRecordsOfPolicy() throws Exception {
        checkRequest(POLICY_AUDIT_ENDPOINT + "/my-group-name/my-name/1.2.3");
        checkRequest(POLICY_AUDIT_ENDPOINT + "/my-name/1.2.3");
    }

    private void checkRequest(String uri) throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, req -> req.get());
    }
}
