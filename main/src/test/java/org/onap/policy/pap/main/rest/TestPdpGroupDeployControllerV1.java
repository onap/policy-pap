/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2022 Nordix Foundation.
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

package org.onap.policy.pap.main.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.DeploymentGroup;
import org.onap.policy.models.pdp.concepts.DeploymentGroups;
import org.onap.policy.models.pdp.concepts.DeploymentSubGroup;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.springframework.test.context.ActiveProfiles;

/**
 * Note: this tests failure cases; success cases are tested by tests in the "e2e" package.
 */
@ActiveProfiles({ "test", "default" })
public class TestPdpGroupDeployControllerV1 extends CommonPapRestServer {

    private static final String DEPLOY_GROUP_ENDPOINT = "pdps/deployments/batch";
    private static final String DEPLOY_POLICIES_ENDPOINT = "pdps/policies";

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(DEPLOY_GROUP_ENDPOINT);
        super.testSwagger(DEPLOY_POLICIES_ENDPOINT);
    }

    @Test
    public void testUpdateGroupPolicies() throws Exception {
        Entity<DeploymentGroups> entgrp = makeDeploymentGroupsEntity();

        Invocation.Builder invocationBuilder = sendRequest(DEPLOY_GROUP_ENDPOINT);
        Response rawresp = invocationBuilder.post(entgrp);
        PdpGroupDeployResponse resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp.getErrorDetails());

        rawresp = invocationBuilder.post(entgrp);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
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
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp.getErrorDetails());

        rawresp = invocationBuilder.post(entgrp);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp.getErrorDetails());

        // verify it fails when no authorization info is included
        checkUnauthRequest(DEPLOY_POLICIES_ENDPOINT, req -> req.post(entgrp));
    }

    private Entity<DeploymentGroups> makeDeploymentGroupsEntity() {
        DeploymentSubGroup subgrp = new DeploymentSubGroup();
        subgrp.setPdpType("drools");

        DeploymentGroup group = new DeploymentGroup();
        group.setName("drools-group");
        group.setDeploymentSubgroups(List.of(subgrp));

        DeploymentGroups groups = new DeploymentGroups();
        groups.setGroups(List.of(group));

        return Entity.entity(groups, MediaType.APPLICATION_JSON);
    }

    private Entity<PdpDeployPolicies> makePdpPoliciesEntity() {
        ToscaConceptIdentifierOptVersion pol1 = new ToscaConceptIdentifierOptVersion("policy-a", "1");
        ToscaConceptIdentifierOptVersion pol2 = new ToscaConceptIdentifierOptVersion("policy-b", null);

        PdpDeployPolicies policies = new PdpDeployPolicies();
        policies.setPolicies(Arrays.asList(pol1, pol2));

        return Entity.entity(policies, MediaType.APPLICATION_JSON);
    }
}
