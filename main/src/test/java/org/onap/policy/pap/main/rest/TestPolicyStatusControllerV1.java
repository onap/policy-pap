/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2021-2023 Nordix Foundation.
 *  Modifications Copyright (C) 2019-2020 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

/**
 * Note: this tests failure cases; success cases are tested by tests in the "e2e" package.
 */
@ActiveProfiles({ "test", "default" })
public class TestPolicyStatusControllerV1 extends CommonPapRestServer {

    private static final String POLICY_STATUS_ENDPOINT = "policies/deployed";
    private static final String POLICY_DEPLOYMENT_STATUS_ENDPOINT = "policies/status";

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(POLICY_STATUS_ENDPOINT);
        super.testSwagger(POLICY_STATUS_ENDPOINT + "/{name}");
        super.testSwagger(POLICY_STATUS_ENDPOINT + "/{name}/{version}");

        super.testSwagger(POLICY_DEPLOYMENT_STATUS_ENDPOINT);
        super.testSwagger(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/{pdpGroupName}");
        super.testSwagger(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/{pdpGroupName}/{policyName}");
        super.testSwagger(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/{pdpGroupName}/{policyName}/{policyVersion}");
    }

    @Test
    public void testQueryAllDeployedPolicies() throws Exception {
        // verify it fails when no authorization info is included
        checkUnauthRequest(POLICY_STATUS_ENDPOINT, SyncInvoker::get);
        checkRequest(POLICY_STATUS_ENDPOINT);
    }

    @Test
    public void testQueryAllDeployedPoliciesWithRegex() throws Exception {
        checkRequest(POLICY_STATUS_ENDPOINT + "?regex=my.(1)name");
        checkEmptyRegexRequest(POLICY_STATUS_ENDPOINT + "?regex=");
        checkInvalidRegexRequest(POLICY_STATUS_ENDPOINT + "?regex=my-(name");
    }

    @Test
    public void testQueryDeployedPolicies() throws Exception {
        checkRequest(POLICY_STATUS_ENDPOINT + "/my-name");
        checkRequest(POLICY_STATUS_ENDPOINT + "/my-name/1.2.3");
    }

    @Test
    public void testGetStatusOfAllPolicies() throws Exception {
        // verify it fails when no authorization info is included
        checkUnauthRequest(POLICY_DEPLOYMENT_STATUS_ENDPOINT, SyncInvoker::get);
    }

    @Test
    public void testGetStatusOfPolicies() throws Exception {
        checkRequest(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/my-group-name");
        checkRequest(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/my-group-name/my-name");
        checkRequest(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/my-group-name/my-name/1.2.3");
    }

    @Test
    public void testGetStatusOfPoliciesWithRegex() throws Exception {
        checkRequest(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/my-group-name?regex=my-%3F%5Bmn%5Da.%7B1%7De");
        checkRequest(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/my-group-name?regex=my.(1)name");
        // my-?[mna.{1}e
        checkInvalidRegexRequest(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/my-group-name?regex=my-%3F%5Bmna.%7B1%7De");
        checkInvalidRegexRequest(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/my-group-name?regex=my.(1name");
        checkEmptyRegexRequest(POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/my-group-name?regex=");
    }

    private void checkRequest(String uri) throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, SyncInvoker::get);
    }

    private void checkInvalidRegexRequest(String uri) throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        final String entity = rawresp.readEntity(String.class);
        assertThat(entity).contains("error parsing regexp");

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, SyncInvoker::get);
    }

    private void checkEmptyRegexRequest(String uri) throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        final String entity = rawresp.readEntity(String.class);
        assertThat(entity).contains("empty string passed as a regex");

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, SyncInvoker::get);
    }
}
