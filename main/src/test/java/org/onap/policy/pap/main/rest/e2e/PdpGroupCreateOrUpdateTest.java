/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2023 Nordix Foundation.
 * Modifications Copyright (C) 2022 Bell Canada. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.pap.concepts.PdpGroupUpdateResponse;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.enums.PdpState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdpGroupCreateOrUpdateTest extends End2EndBase {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupCreateOrUpdateTest.class);

    private static final String CREATEORUPDATE_GROUPS_ENDPOINT = "pdps/groups/batch";
    private static final String DELETE_GROUP_ENDPOINT = "pdps/groups/";
    private static final String CREATE_SUBGROUP = "pdpTypeA";
    private static final String GROUP_ENDPOINT = "pdps";

    /**
     * Sets up.
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        addToscaPolicyTypes("monitoring.policy-type.yaml");
        super.setUp();

        context = new End2EndContext();
    }

    /**
     * Deletes the deployed group.
     */
    @Override
    @AfterEach
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
    public void testCreateGroupsJson() throws Exception {

        createPdpGroups("createGroups.json", MediaType.APPLICATION_JSON);
    }

    @Test
    public void testCreateGroupsYaml() throws Exception {

        createPdpGroups("createGroups.yaml", "application/yaml");
    }

    private void createPdpGroups(String fileName, String mediaType) throws Exception, InterruptedException {
        context.addPdp("pdpAA_1", CREATE_SUBGROUP);
        context.addPdp("pdpAA_2", CREATE_SUBGROUP);
        context.addPdp("pdpAB_1", "pdpTypeB");

        context.startThreads();

        Invocation.Builder invocationBuilder = sendRequest(CREATEORUPDATE_GROUPS_ENDPOINT, mediaType);

        PdpGroups groups = (mediaType.equalsIgnoreCase(MediaType.APPLICATION_JSON)
                        ? loadJsonFile(fileName, PdpGroups.class)
                        : loadYamlFile(fileName, PdpGroups.class));
        Entity<PdpGroups> entity = Entity.entity(groups, mediaType);
        Response rawresp = invocationBuilder.post(entity);
        PdpGroupUpdateResponse resp = rawresp.readEntity(PdpGroupUpdateResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        context.await();

        // none of the PDPs should have handled any requests
        assertEquals(context.getPdps().size(),
            context.getPdps().stream().filter(pdp -> pdp.getHandled().isEmpty()).count());

        // repeat - should be OK
        rawresp = invocationBuilder.post(entity);
        resp = rawresp.readEntity(PdpGroupUpdateResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        // repeat with different properties - should fail
        groups.getGroups().get(0).setProperties(null);
        rawresp = invocationBuilder.post(entity);
        resp = rawresp.readEntity(PdpGroupUpdateResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        assertTrue(resp.getErrorDetails().contains("cannot change properties"));
    }

    @Test
    public void testCreateAndUpdate_MultipleGroups() throws Exception {

        Invocation.Builder invocationBuilderForGroupUpdate = sendRequest(CREATEORUPDATE_GROUPS_ENDPOINT);

        PdpGroups groups = loadJsonFile("createGroups.json", PdpGroups.class);
        Entity<PdpGroups> entity = Entity.entity(groups, MediaType.APPLICATION_JSON);
        Response rawPdpGroupUpdateResponse = invocationBuilderForGroupUpdate.post(entity);
        PdpGroupUpdateResponse pdpGroupUpdateResponse =
            rawPdpGroupUpdateResponse.readEntity(PdpGroupUpdateResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawPdpGroupUpdateResponse.getStatus());
        assertNull(pdpGroupUpdateResponse.getErrorDetails());

        Invocation.Builder invocationBuilderForGroupQuery = sendRequest(GROUP_ENDPOINT);
        Response pdpGroupQueryResponse = invocationBuilderForGroupQuery.get();
        assertEquals(Response.Status.OK.getStatusCode(), pdpGroupQueryResponse.getStatus());
        PdpGroups pdpGroupsInDb = pdpGroupQueryResponse.readEntity(PdpGroups.class);
        assertEquals(1, pdpGroupsInDb.getGroups().size());
        assertEquals("createGroups", pdpGroupsInDb.getGroups().get(0).getName());
        assertEquals(PdpState.PASSIVE, pdpGroupsInDb.getGroups().get(0).getPdpGroupState());

        // creating 2 new groups
        PdpGroups newGroups = loadJsonFile("createNewGroups.json", PdpGroups.class);
        entity = Entity.entity(newGroups, MediaType.APPLICATION_JSON);
        rawPdpGroupUpdateResponse = invocationBuilderForGroupUpdate.post(entity);
        pdpGroupUpdateResponse = rawPdpGroupUpdateResponse.readEntity(PdpGroupUpdateResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawPdpGroupUpdateResponse.getStatus());
        assertNull(pdpGroupUpdateResponse.getErrorDetails());

        invocationBuilderForGroupQuery = sendRequest(GROUP_ENDPOINT);
        pdpGroupQueryResponse = invocationBuilderForGroupQuery.get();
        assertEquals(Response.Status.OK.getStatusCode(), pdpGroupQueryResponse.getStatus());
        pdpGroupsInDb = pdpGroupQueryResponse.readEntity(PdpGroups.class);
        assertEquals(3, pdpGroupsInDb.getGroups().size());

        // updating a group, changing state of old group to active
        groups.getGroups().get(0).setPdpGroupState(PdpState.ACTIVE);
        entity = Entity.entity(groups, MediaType.APPLICATION_JSON);
        rawPdpGroupUpdateResponse = invocationBuilderForGroupUpdate.post(entity);
        pdpGroupUpdateResponse = rawPdpGroupUpdateResponse.readEntity(PdpGroupUpdateResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawPdpGroupUpdateResponse.getStatus());
        assertNull(pdpGroupUpdateResponse.getErrorDetails());

        invocationBuilderForGroupQuery = sendRequest(GROUP_ENDPOINT);
        pdpGroupQueryResponse = invocationBuilderForGroupQuery.get();
        assertEquals(Response.Status.OK.getStatusCode(), pdpGroupQueryResponse.getStatus());
        pdpGroupsInDb = pdpGroupQueryResponse.readEntity(PdpGroups.class);
        assertEquals(3, pdpGroupsInDb.getGroups().size());
        Optional<PdpGroup> oldGroupInDb =
            pdpGroupsInDb.getGroups().stream().filter(group -> group.getName().equals("createGroups")).findAny();
        assertEquals(PdpState.ACTIVE, oldGroupInDb.get().getPdpGroupState());
    }
}
