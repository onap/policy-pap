/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2022 Nordix Foundation.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PdpGroupUpdateResponse;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.springframework.test.context.ActiveProfiles;

/**
 * Note: this tests failure cases; success cases are tested by tests in the "e2e" package.
 */
@ActiveProfiles("test")
public class TestPdpGroupCreateOrUpdateControllerV1 extends CommonPapRestServer {

    private static final String CREATEORUPDATE_GROUPS_ENDPOINT = "pdps/groups/batch";

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(CREATEORUPDATE_GROUPS_ENDPOINT);
    }

    @Test
    public void testCreateOrUpdateGroups() throws Exception {
        Entity<PdpGroups> entgrp = makePdpGroupsEntity();

        Invocation.Builder invocationBuilder = sendRequest(CREATEORUPDATE_GROUPS_ENDPOINT);
        Response rawresp = invocationBuilder.post(entgrp);
        PdpGroupUpdateResponse resp = rawresp.readEntity(PdpGroupUpdateResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp.getErrorDetails());

        rawresp = invocationBuilder.post(entgrp);
        resp = rawresp.readEntity(PdpGroupUpdateResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp.getErrorDetails());

        // verify it fails when no authorization info is included
        checkUnauthRequest(CREATEORUPDATE_GROUPS_ENDPOINT, req -> req.post(entgrp));
    }

    private Entity<PdpGroups> makePdpGroupsEntity() {
        PdpSubGroup subgrp = new PdpSubGroup();
        subgrp.setPdpType("drools");

        PdpGroup group = new PdpGroup();
        group.setName("drools-group");
        group.setDescription("my description");
        group.setPdpSubgroups(List.of(subgrp));
        PdpGroups groups = new PdpGroups();
        groups.setGroups(List.of(group));
        return Entity.entity(groups, MediaType.APPLICATION_JSON);
    }
}
