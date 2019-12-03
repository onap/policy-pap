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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdpGroupDeployTest extends End2EndBase {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupDeployTest.class);

    private static final String DEPLOY_GROUP_ENDPOINT = "pdps";
    private static final String DEPLOY_POLICIES_ENDPOINT = "pdps/policies";
    private static final String DELETE_GROUP_ENDPOINT = "pdps/groups";
    private static final String DEPLOY_SUBGROUP = "pdpTypeA";

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

    /**
     * Deletes the deployed group.
     */
    @After
    public void tearDown() {
        // delete the group that was inserted
        try {
            sendRequest(DELETE_GROUP_ENDPOINT + "/createGroups").delete();
        } catch (Exception e) {
            logger.warn("cannot delete group: createGroups", e);
        }

        super.tearDown();
    }

    @Test
    public void testDeployGroup() throws Exception {

        addGroups("deployGroups.json");

        PdpStatus status11 = new PdpStatus();
        status11.setName("pdpAA_1");
        status11.setState(PdpState.ACTIVE);
        status11.setPdpGroup("deployPolicies");
        status11.setPdpType(DEPLOY_SUBGROUP);
        status11.setPdpSubgroup(DEPLOY_SUBGROUP);

        List<ToscaPolicyIdentifier> idents = Arrays.asList(new ToscaPolicyIdentifier("onap.restart.tca", "1.0.0"));
        status11.setPolicies(idents);

        PdpStatus status12 = new PdpStatus();
        status12.setName("pdpAA_2");
        status12.setState(PdpState.ACTIVE);
        status12.setPdpGroup("deployPolicies");
        status12.setPdpType(DEPLOY_SUBGROUP);
        status12.setPdpSubgroup(DEPLOY_SUBGROUP);
        status12.setPolicies(idents);

        context.addPdp("pdpAA_1", DEPLOY_SUBGROUP).addReply(status11);
        context.addPdp("pdpAA_2", DEPLOY_SUBGROUP).addReply(status12);
        context.addPdp("pdpAB_1", "pdpTypeA");

        context.startThreads();

        Invocation.Builder invocationBuilder = sendRequest(DEPLOY_GROUP_ENDPOINT);

        PdpGroups groups = loadJsonFile("deployGroupsReq.json", PdpGroups.class);
        Entity<PdpGroups> entity = Entity.entity(groups, MediaType.APPLICATION_JSON);
        Response rawresp = invocationBuilder.post(entity);
        PdpGroupDeployResponse resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        context.await();

        // one of the PDPs should not have handled any requests
        assertEquals(1, context.getPdps().stream().filter(pdp -> pdp.getHandled().isEmpty()).count());

        // repeat - should be OK
        rawresp = invocationBuilder.post(entity);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        // repeat with unknown group - should fail
        PdpGroup group = groups.getGroups().get(0);
        group.setName("unknown-group");
        group.setProperties(null);
        rawresp = invocationBuilder.post(entity);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        assertTrue(resp.getErrorDetails().contains("unknown group"));
    }

    @Test
    public void testDeployPolicies() throws Exception {
        addGroups("deployPolicies.json");

        PdpStatus status11 = new PdpStatus();
        status11.setName("pdpBA_1");
        status11.setState(PdpState.ACTIVE);
        status11.setPdpGroup("deployPolicies");
        status11.setPdpType(DEPLOY_SUBGROUP);
        status11.setPdpSubgroup(DEPLOY_SUBGROUP);

        List<ToscaPolicyIdentifier> idents = Arrays.asList(new ToscaPolicyIdentifier("onap.restart.tca", "1.0.0"));
        status11.setPolicies(idents);

        PdpStatus status12 = new PdpStatus();
        status12.setName("pdpBA_2");
        status12.setState(PdpState.ACTIVE);
        status12.setPdpGroup("deployPolicies");
        status12.setPdpType(DEPLOY_SUBGROUP);
        status12.setPdpSubgroup(DEPLOY_SUBGROUP);
        status12.setPolicies(idents);

        context.addPdp("pdpBA_1", DEPLOY_SUBGROUP).addReply(status11);
        context.addPdp("pdpBA_2", DEPLOY_SUBGROUP).addReply(status12);
        context.addPdp("pdpBB_1", "pdpTypeB");

        context.startThreads();

        Invocation.Builder invocationBuilder = sendRequest(DEPLOY_POLICIES_ENDPOINT);

        PdpDeployPolicies policies = loadJsonFile("deployPoliciesReq.json", PdpDeployPolicies.class);
        Entity<PdpDeployPolicies> entity = Entity.entity(policies, MediaType.APPLICATION_JSON);
        Response rawresp = invocationBuilder.post(entity);
        PdpGroupDeployResponse resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        System.out.println(resp.getErrorDetails());
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        context.await();

        // one of the PDPs should not have handled any requests
        assertEquals(1, context.getPdps().stream().filter(pdp -> pdp.getHandled().isEmpty()).count());

        // repeat - should be OK
        rawresp = invocationBuilder.post(entity);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());
    }
}
