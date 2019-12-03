/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 Nordix Foundation.
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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdpGroupCreateOrUpdateTest extends End2EndBase {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupCreateOrUpdateTest.class);

    private static final String CREATEORUPDAE_GROUP_ENDPOINT = "pdps/groups";
    private static final String DELETE_GROUP_ENDPOINT = "pdps/groups/";
    private static final String CREATE_SUBGROUP = "pdpTypeA";

    /**
     * Starts Main and adds policies to the DB.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        End2EndBase.setUpBeforeClass();

        addToscaPolicyTypes("monitoring.policy-type.yaml");
    }

    /**
     * Sets up.
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context = new End2EndContext();
    }

    /**
     * Deletes the deployed group.
     */
    @Override
    @After
    public void tearDown() {
        // delete the group that was inserted
        try {
            sendRequest(DELETE_GROUP_ENDPOINT + "createGroups").delete();
        } catch (Exception e) {
            logger.warn("cannot delete group: createGroups", e);
        }

        super.tearDown();
    }

    @Test
    public void testCreateGroups() throws Exception {

        context.addPdp("pdpAA_1", CREATE_SUBGROUP);
        context.addPdp("pdpAA_2", CREATE_SUBGROUP);
        context.addPdp("pdpAB_1", "pdpTypeB");

        context.startThreads();

        Invocation.Builder invocationBuilder = sendRequest(CREATEORUPDAE_GROUP_ENDPOINT);

        PdpGroups groups = loadJsonFile("createGroups.json", PdpGroups.class);
        Entity<PdpGroups> entity = Entity.entity(groups, MediaType.APPLICATION_JSON);
        Response rawresp = invocationBuilder.post(entity);
        PdpGroupDeployResponse resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        context.await();

        // none of the PDPs should have handled any requests
        assertEquals(context.getPdps().size(),
                        context.getPdps().stream().filter(pdp -> pdp.getHandled().isEmpty()).count());

        // repeat - should be OK
        rawresp = invocationBuilder.post(entity);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        // repeat with different properties - should fail
        groups.getGroups().get(0).setProperties(null);
        rawresp = invocationBuilder.post(entity);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        assertTrue(resp.getErrorDetails().contains("cannot change properties"));
    }

}
