/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021-2022 Nordix Foundation.
 * Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.bus.NoopTopicFactories;
import org.onap.policy.common.endpoints.event.comm.bus.NoopTopicSink;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.DeploymentGroup;
import org.onap.policy.models.pdp.concepts.DeploymentGroups;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.rest.PdpGroupDeployControllerV1;

public class PdpGroupDeployTest extends End2EndBase {
    private static final String DEPLOY_GROUP_ENDPOINT = "pdps/deployments/batch";
    private static final String DEPLOY_POLICIES_ENDPOINT = "pdps/policies";
    private static final String DEPLOY_SUBGROUP = "pdpTypeA";

    /**
     * Sets up.
     */
    @Override
    @Before
    public void setUp() throws Exception {
        addToscaPolicyTypes("monitoring.policy-type.yaml");
        addToscaPolicies("monitoring.policy.yaml");
        super.setUp();

        context = new End2EndContext();
    }

    @Test
    public void testUpdateGroupPolicies() throws Exception {

        addGroups("deployGroups.json");

        PdpStatus status11 = new PdpStatus();
        status11.setName("pdpAA_1");
        status11.setState(PdpState.ACTIVE);
        status11.setPdpGroup("deployGroups");
        status11.setPdpType(DEPLOY_SUBGROUP);
        status11.setPdpSubgroup(DEPLOY_SUBGROUP);

        List<ToscaConceptIdentifier> idents = List.of(new ToscaConceptIdentifier("onap.restart.tca", "1.0.0"));
        status11.setPolicies(idents);

        PdpStatus status12 = new PdpStatus();
        status12.setName("pdpAA_2");
        status12.setState(PdpState.ACTIVE);
        status12.setPdpGroup("deployGroups");
        status12.setPdpType(DEPLOY_SUBGROUP);
        status12.setPdpSubgroup(DEPLOY_SUBGROUP);
        status12.setPolicies(idents);

        context.addPdp("pdpAA_1", DEPLOY_SUBGROUP).addReply(status11);
        context.addPdp("pdpAA_2", DEPLOY_SUBGROUP).addReply(status12);
        context.addPdp("pdpAB_1", "pdpTypeA");

        context.startThreads();

        Invocation.Builder invocationBuilder = sendRequest(DEPLOY_GROUP_ENDPOINT);

        DeploymentGroups groups = loadJsonFile("deployGroupsReq.json", DeploymentGroups.class);
        Entity<DeploymentGroups> entity = Entity.entity(groups, MediaType.APPLICATION_JSON);
        Response rawresp = invocationBuilder.post(entity);
        PdpGroupDeployResponse resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        context.await();

        // one of the PDPs should not have handled any requests
        assertEquals(1, context.getPdps().stream().filter(pdp -> pdp.getHandled().isEmpty()).count());

        // repeat - should be OK
        rawresp = invocationBuilder.post(entity);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), rawresp.getStatus());
        assertEquals(PdpGroupDeployControllerV1.DEPLOYMENT_RESPONSE_MSG, resp.getMessage());
        assertEquals(PdpGroupDeployControllerV1.POLICY_STATUS_URI, resp.getUri());
        assertNull(resp.getErrorDetails());

        assertThat(meterRegistry.counter(deploymentsCounterName, deploymentSuccessTag).count()).isEqualTo(2);

        // repeat with unknown group - should fail
        DeploymentGroup group = groups.getGroups().get(0);
        group.setName("unknown-group");
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

        final ToscaConceptIdentifier identifier = new ToscaConceptIdentifier("onap.restart.tcaB", "1.0.0");

        List<ToscaConceptIdentifier> identifiers = List.of(identifier);
        status11.setPolicies(identifiers);

        PdpStatus status12 = new PdpStatus();
        status12.setName("pdpBA_2");
        status12.setState(PdpState.ACTIVE);
        status12.setPdpGroup("deployPolicies");
        status12.setPdpType(DEPLOY_SUBGROUP);
        status12.setPdpSubgroup(DEPLOY_SUBGROUP);
        status12.setPolicies(identifiers);

        context.addPdp("pdpBA_1", DEPLOY_SUBGROUP).addReply(status11);
        context.addPdp("pdpBA_2", DEPLOY_SUBGROUP).addReply(status12);
        context.addPdp("pdpBB_1", "pdpTypeB");

        context.startThreads();

        // arrange to catch notifications
        LinkedBlockingQueue<String> notifications = new LinkedBlockingQueue<>();
        NoopTopicSink notifier = NoopTopicFactories.getSinkFactory().get(getTopicPolicyNotification());
        notifier.register((infra, topic, msg) -> notifications.add(msg));

        assertThat(meterRegistry.counter(deploymentsCounterName, deploymentSuccessTag).count()).isZero();

        Invocation.Builder invocationBuilder = sendRequest(DEPLOY_POLICIES_ENDPOINT);

        PdpDeployPolicies policies = loadJsonFile("deployPoliciesReq2.json", PdpDeployPolicies.class);
        Entity<PdpDeployPolicies> entity = Entity.entity(policies, MediaType.APPLICATION_JSON);
        Response rawresp = invocationBuilder.post(entity);
        PdpGroupDeployResponse resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), rawresp.getStatus());
        assertEquals(PdpGroupDeployControllerV1.DEPLOYMENT_RESPONSE_MSG, resp.getMessage());
        assertEquals(PdpGroupDeployControllerV1.POLICY_STATUS_URI, resp.getUri());
        assertNull(resp.getErrorDetails());

        context.await();

        // wait for the notification
        String json = notifications.poll(5, TimeUnit.SECONDS);
        PolicyNotification notify = new StandardCoder().decode(json, PolicyNotification.class);
        assertNotNull(notify.getAdded());
        assertNotNull(notify.getDeleted());
        assertTrue(notify.getDeleted().isEmpty());
        assertEquals(1, notify.getAdded().size());

        PolicyStatus added = notify.getAdded().get(0);
        assertEquals(2, added.getSuccessCount());
        assertEquals(0, added.getFailureCount());
        assertEquals(0, added.getIncompleteCount());
        assertEquals(identifier, added.getPolicy());

        // one of the PDPs should not have handled any requests
        assertEquals(1, context.getPdps().stream().filter(pdp -> pdp.getHandled().isEmpty()).count());

        // repeat - should be OK
        rawresp = invocationBuilder.post(entity);
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        assertThat(meterRegistry.counter(deploymentsCounterName, deploymentSuccessTag).count()).isEqualTo(2);
    }
}
