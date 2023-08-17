/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.rest.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.State;

class PolicyStatusTest extends End2EndBase {
    private static final String POLICY_NAME = "onap.restart.tca";
    private static final String POLICY_TYPE_NAME = "onap.policies.monitoring.cdap.tca.hi.lo.app";
    private static final String VERSION = "1.0.0";
    private static final String POLICY_STATUS_ENDPOINT = "policies/deployed";
    private static final String POLICY_DEPLOYMENT_STATUS_ENDPOINT = "policies/status";

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        addPdpPolicyStatus("policyStatus.json");
        super.setUp();
    }

    @Test
    void testQueryAllDeployedPolicies() throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(POLICY_STATUS_ENDPOINT);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        List<PolicyStatus> resp = rawresp.readEntity(new GenericType<>() {});
        assertEquals(1, resp.size());
        checkAssertions(resp.get(0));
    }

    @Test
    void testQueryDeployedPolicies() throws Exception {
        String uri = POLICY_STATUS_ENDPOINT + "/onap.restart.tca";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        List<PolicyStatus> resp = rawresp.readEntity(new GenericType<>() {});
        assertEquals(1, resp.size());
        checkAssertions(resp.get(0));
    }

    @Test
    void testQueryDeployedPolicy() throws Exception {
        String uri = POLICY_STATUS_ENDPOINT + "/onap.restart.tca/1.0.0";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        PolicyStatus status = rawresp.readEntity(PolicyStatus.class);
        checkAssertions(status);
    }

    @Test
    void testGetStatusOfAllDeployedPolicies() throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(POLICY_DEPLOYMENT_STATUS_ENDPOINT);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        List<PdpPolicyStatus> resp = rawresp.readEntity(new GenericType<>() {});
        assertEquals(1, resp.size());
        checkAssertionsForDeploymentStatus(resp.get(0));
    }

    @Test
    void testGetStatusOfDeployedPolicies() throws Exception {
        String uri = POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/policyStatus/onap.restart.tca";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        List<PdpPolicyStatus> resp = rawresp.readEntity(new GenericType<>() {});
        assertEquals(1, resp.size());
        checkAssertionsForDeploymentStatus(resp.get(0));
    }

    @Test
    void testGetStatusOfDeployedPolicy() throws Exception {
        String uri = POLICY_DEPLOYMENT_STATUS_ENDPOINT + "/policyStatus/onap.restart.tca/1.0.0";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        PdpPolicyStatus status = rawresp.readEntity(PdpPolicyStatus.class);
        checkAssertionsForDeploymentStatus(status);
    }


    private void checkAssertions(PolicyStatus status) {
        assertEquals(POLICY_NAME, status.getPolicyId());
        assertEquals(VERSION, status.getPolicyVersion());
        assertEquals(POLICY_TYPE_NAME, status.getPolicyTypeId());
        assertEquals(VERSION, status.getPolicyTypeVersion());
        assertEquals(0, status.getFailureCount());
        assertEquals(1, status.getIncompleteCount());
        assertEquals(0, status.getSuccessCount());
    }

    private void checkAssertionsForDeploymentStatus(PdpPolicyStatus status) {
        assertEquals(POLICY_NAME, status.getPolicy().getName());
        assertEquals(VERSION, status.getPolicy().getVersion());
        assertEquals(POLICY_TYPE_NAME, status.getPolicyType().getName());
        assertEquals(VERSION, status.getPolicyType().getVersion());
        assertEquals("policyStatus", status.getPdpGroup());
        assertEquals("pdpB_1", status.getPdpId());
        assertEquals("pdpTypeB", status.getPdpType());
        assertEquals(State.WAITING, status.getState());
        assertTrue(status.isDeploy());
    }
}
