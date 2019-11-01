/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;

import java.util.List;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PolicyStatus;

public class PolicyStatusTest extends End2EndBase {
    private static final String POLICY_STATUS_ENDPOINT = "policies/deployed";

    /**
     * Starts Main and adds policies to the DB.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // don't start Main until AFTER we add the policies to the DB
        End2EndBase.setUpBeforeClass(false);

        addToscaPolicyTypes("monitoring.policy-type.yaml");
        addToscaPolicies("monitoring.policy.yaml");
        addGroups("policyStatus.json");

        startMain();
    }

    @Test
    public void testQueryAllDeployedPolicies() throws Exception {
        String uri = POLICY_STATUS_ENDPOINT;

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        List<PolicyStatus> resp = rawresp.readEntity(new GenericType<List<PolicyStatus>>() {});
        assertEquals(1, resp.size());

        PolicyStatus status = resp.get(0);
        assertEquals("onap.restart.tca", status.getPolicyId());
        assertEquals("1.0.0", status.getPolicyVersion());
        assertEquals("onap.policies.monitoring.cdap.tca.hi.lo.app", status.getPolicyTypeId());
        assertEquals("1.0.0", status.getPolicyTypeVersion());
        assertEquals(0, status.getFailureCount());
        assertEquals(1, status.getIncompleteCount());
        assertEquals(0, status.getSuccessCount());
    }

    @Test
    public void testQueryDeployedPolicies() throws Exception {
        String uri = POLICY_STATUS_ENDPOINT + "/onap.restart.tca";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        List<PolicyStatus> resp = rawresp.readEntity(new GenericType<List<PolicyStatus>>() {});
        assertEquals(1, resp.size());

        PolicyStatus status = resp.get(0);
        assertEquals("onap.restart.tca", status.getPolicyId());
        assertEquals("1.0.0", status.getPolicyVersion());
        assertEquals("onap.policies.monitoring.cdap.tca.hi.lo.app", status.getPolicyTypeId());
        assertEquals("1.0.0", status.getPolicyTypeVersion());
        assertEquals(0, status.getFailureCount());
        assertEquals(1, status.getIncompleteCount());
        assertEquals(0, status.getSuccessCount());
    }

    @Test
    public void testQueryDeployedPolicy() throws Exception {
        String uri = POLICY_STATUS_ENDPOINT + "/onap.restart.tca/1.0.0";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        PolicyStatus status = rawresp.readEntity(PolicyStatus.class);
        assertEquals("onap.restart.tca", status.getPolicyId());
        assertEquals("1.0.0", status.getPolicyVersion());
        assertEquals("onap.policies.monitoring.cdap.tca.hi.lo.app", status.getPolicyTypeId());
        assertEquals("1.0.0", status.getPolicyTypeVersion());
        assertEquals(0, status.getFailureCount());
        assertEquals(1, status.getIncompleteCount());
        assertEquals(0, status.getSuccessCount());
    }
}
