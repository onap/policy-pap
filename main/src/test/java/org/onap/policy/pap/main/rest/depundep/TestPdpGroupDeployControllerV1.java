/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property.
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

package org.onap.policy.pap.main.rest.depundep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.pap.main.rest.CommonPapRestServer;

public class TestPdpGroupDeployControllerV1 extends CommonPapRestServer {

    private static final String DEPLOY_GROUP_ENDPOINT = "pdps";
    private static final String DEPLOY_POLICIES_ENDPOINT = "pdps/policies";

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(DEPLOY_GROUP_ENDPOINT);
        super.testSwagger(DEPLOY_POLICIES_ENDPOINT);
    }

    @Test
    public void testDeployGroup() throws Exception {
        Entity<PdpGroup> entgrp = makePdpGroupEntity();

        Invocation.Builder invocationBuilder = sendRequest(DEPLOY_GROUP_ENDPOINT);
        Response rawresp = invocationBuilder.post(entgrp);
        PdpGroupDeployResponse resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp.getErrorDetails());

        rawresp = invocationBuilder.post(entgrp);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp.getErrorDetails());

        // verify it fails when no authorization info is included
        checkUnauthRequest(DEPLOY_GROUP_ENDPOINT, req -> req.post(entgrp));
    }

    @Test
    public void testDeployPolicies() throws Exception {
        Entity<PdpDeployPolicies> entgrp = makePdpPoliciesEntity();

        Invocation.Builder invocationBuilder = sendRequest(DEPLOY_POLICIES_ENDPOINT);
        Response rawresp = invocationBuilder.post(entgrp);
        PdpGroupDeployResponse resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp.getErrorDetails());

        rawresp = invocationBuilder.post(entgrp);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp.getErrorDetails());

        // verify it fails when no authorization info is included
        checkUnauthRequest(DEPLOY_POLICIES_ENDPOINT, req -> req.post(entgrp));
    }

    private Entity<PdpGroup> makePdpGroupEntity() {
        PdpSubGroup subgrp = new PdpSubGroup();
        subgrp.setPdpType("drools");

        PdpGroup group = new PdpGroup();
        group.setName("drools-group");
        group.setDescription("my description");
        group.setVersion("my-version");
        group.setPdpSubgroups(Arrays.asList(subgrp));

        return Entity.entity(group, MediaType.APPLICATION_JSON);
    }

    private Entity<PdpDeployPolicies> makePdpPoliciesEntity() {
        ToscaPolicyIdentifierOptVersion pol1 = new ToscaPolicyIdentifierOptVersion("policy-a", "1");
        ToscaPolicyIdentifierOptVersion pol2 = new ToscaPolicyIdentifierOptVersion("policy-b", null);

        PdpDeployPolicies policies = new PdpDeployPolicies();
        policies.setPolicies(Arrays.asList(pol1, pol2));

        return Entity.entity(policies, MediaType.APPLICATION_JSON);
    }
}
