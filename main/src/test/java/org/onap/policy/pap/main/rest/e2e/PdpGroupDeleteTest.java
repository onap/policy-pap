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
import static org.junit.Assert.assertNull;

import java.util.Collections;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PdpGroupDeleteResponse;
import org.onap.policy.models.pdp.concepts.PdpStatus;

public class PdpGroupDeleteTest extends End2EndBase {
    private static final String DELETE_GROUP_ENDPOINT = "pdps/groups";
    private static final String DELETE_POLICIES_ENDPOINT = "pdps/policies";

    /**
     * Starts Main and adds policies to the DB.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        End2EndBase.setUpBeforeClass();

        addToscaPolicyTypes("monitoring.policy-type.yaml");
        addToscaPolicies("monitoring.policy.yaml");
    }

    /**
     * Sets up.
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context = new End2EndContext();
    }

    @Test
    public void testDeleteGroup() throws Exception {
        addGroups("deleteGroup.json");

        context.addPdp("pdpAA_1", "pdpTypeA");
        context.addPdp("pdpAA_2", "pdpTypeA");
        context.addPdp("pdpAB_1", "pdpTypeB");

        context.startThreads();

        String uri = DELETE_GROUP_ENDPOINT + "/deleteGroup";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.delete();
        PdpGroupDeleteResponse resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        context.await();

        // none of the PDPs should have handled any requests
        assertEquals(context.getPdps().size(),
                        context.getPdps().stream().filter(pdp -> pdp.getHandled().isEmpty()).count());

        // repeat - should fail
        rawresp = invocationBuilder.delete();
        resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rawresp.getStatus());
        assertEquals("group not found", resp.getErrorDetails());
    }

    @Test
    public void testDeletePolicy() throws Exception {
        addGroups("undeployPolicy.json");

        PdpStatus status1 = new PdpStatus();
        status1.setName("pdpBA_1");
        status1.setPdpGroup("undeployPolicy");
        status1.setPdpSubgroup("pdpTypeA");
        status1.setPolicies(Collections.emptyList());

        PdpStatus status2 = new PdpStatus();
        status2.setName("pdpBA_2");
        status2.setPdpGroup("undeployPolicy");
        status2.setPdpSubgroup("pdpTypeA");
        status2.setPolicies(Collections.emptyList());

        context.addPdp("pdpBA_1", "pdpTypeA").addReply(status1);
        context.addPdp("pdpBA_2", "pdpTypeA").addReply(status2);
        context.addPdp("pdpBB_1", "pdpTypeB");

        context.startThreads();

        String uri = DELETE_POLICIES_ENDPOINT + "/onap.restart.tcaB";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.delete();
        PdpGroupDeleteResponse resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        context.await();

        rawresp = invocationBuilder.delete();
        resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());
    }

    @Test
    public void testDeletePolicyVersion() throws Exception {
        addGroups("undeployPolicyVersion.json");

        PdpStatus status1 = new PdpStatus();
        status1.setName("pdpCA_1");
        status1.setPdpGroup("undeployPolicyVersion");
        status1.setPdpSubgroup("pdpTypeA");
        status1.setPolicies(Collections.emptyList());

        PdpStatus status2 = new PdpStatus();
        status2.setName("pdpCA_2");
        status2.setPdpGroup("undeployPolicyVersion");
        status2.setPdpSubgroup("pdpTypeA");
        status2.setPolicies(Collections.emptyList());

        context.addPdp("pdpCA_1", "pdpTypeA").addReply(status1);
        context.addPdp("pdpCA_2", "pdpTypeA").addReply(status2);
        context.addPdp("pdpCB_1", "pdpTypeB");

        context.startThreads();

        String uri = DELETE_POLICIES_ENDPOINT + "/onap.restart.tcaC/versions/1.0.0";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.delete();
        PdpGroupDeleteResponse resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        context.await();

        rawresp = invocationBuilder.delete();
        resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());
    }
}
