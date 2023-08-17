/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021-2023 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.event.comm.bus.NoopTopicFactories;
import org.onap.policy.common.endpoints.event.comm.bus.NoopTopicSink;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.pap.concepts.PdpGroupDeleteResponse;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.rest.PdpGroupDeployControllerV1;

public class PdpGroupDeleteTest extends End2EndBase {
    private static final String DELETE_GROUP_ENDPOINT = "pdps/groups";
    private static final String DELETE_POLICIES_ENDPOINT = "pdps/policies";

    /**
     * Sets up.
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        addToscaPolicyTypes("monitoring.policy-type.yaml");
        addToscaPolicies("monitoring.policy.yaml");
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
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());
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

        // arrange to catch notifications
        LinkedBlockingQueue<String> notifications = new LinkedBlockingQueue<>();
        NoopTopicSink notifier = NoopTopicFactories.getSinkFactory().get(getTopicPolicyNotification());
        notifier.register((infra, topic, msg) -> notifications.add(msg));

        String uri = DELETE_POLICIES_ENDPOINT + "/onap.restart.tcaB";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.delete();
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
        assertTrue(notify.getAdded().isEmpty());
        assertEquals(1, notify.getDeleted().size());

        PolicyStatus deleted = notify.getDeleted().get(0);
        assertEquals(2, deleted.getSuccessCount());
        assertEquals(0, deleted.getFailureCount());
        assertEquals(0, deleted.getIncompleteCount());
        assertEquals(new ToscaConceptIdentifier("onap.restart.tcaB", "1.0.0"), deleted.getPolicy());

        assertThat(meterRegistry.counter(deploymentsCounterName, unDeploymentSuccessTag).count()).isEqualTo(2);

        rawresp = invocationBuilder.delete();
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        assertEquals("policy does not appear in any PDP group: onap.restart.tcaB null", resp.getErrorDetails());
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
        PdpGroupDeployResponse resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());
        assertEquals(PdpGroupDeployControllerV1.DEPLOYMENT_RESPONSE_MSG, resp.getMessage());
        assertEquals(PdpGroupDeployControllerV1.POLICY_STATUS_URI, resp.getUri());
        context.await();

        rawresp = invocationBuilder.delete();
        resp = rawresp.readEntity(PdpGroupDeployResponse.class);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), rawresp.getStatus());
        assertEquals("policy does not appear in any PDP group: onap.restart.tcaC 1.0.0", resp.getErrorDetails());
    }
}
