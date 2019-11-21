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
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PdpGroupStateChangeResponse;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;

public class PdpGroupStateChangeTest extends End2EndBase {
    private static final String PDP1 = "pdpAA_1";
    private static final String PDP2 = "pdpAA_2";
    private static final String PDP3 = "pdpAB_1";
    private static final String SUBGROUP1 = "pdpTypeA";
    private static final String SUBGROUP2 = "pdpTypeB";
    private static final String DEACT_GROUP = "stateChangeGroupDeactivate";
    private static final String GROUP_ENDPOINT = "pdps/groups";

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
    public void testMakePassive() throws Exception {
        addGroups("stateChangeGroupDeactivate.json");

        ToscaPolicyIdentifier policy =
                        new ToscaPolicyIdentifier("onap.restart.tca", "1.0.0");
        List<ToscaPolicyIdentifier> policies = Collections.singletonList(policy);

        PdpStatus status11 = new PdpStatus();
        status11.setName(PDP1);
        status11.setState(PdpState.ACTIVE);
        status11.setPdpGroup(DEACT_GROUP);
        status11.setPdpType(SUBGROUP1);
        status11.setPdpSubgroup(SUBGROUP1);
        status11.setPolicies(policies);

        PdpStatus status12 = makeCopy(status11);
        status12.setState(PdpState.PASSIVE);

        PdpStatus status21 = new PdpStatus();
        status21.setName(PDP2);
        status21.setState(PdpState.ACTIVE);
        status21.setPdpGroup(DEACT_GROUP);
        status21.setPdpType(SUBGROUP1);
        status21.setPdpSubgroup(SUBGROUP1);
        status21.setPolicies(policies);

        PdpStatus status22 = makeCopy(status21);
        status22.setState(PdpState.PASSIVE);

        PdpStatus status31 = new PdpStatus();
        status31.setName(PDP3);
        status31.setState(PdpState.ACTIVE);
        status31.setPdpGroup(DEACT_GROUP);
        status31.setPdpType(SUBGROUP2);
        status31.setPdpSubgroup(SUBGROUP2);
        status31.setPolicies(Collections.emptyList());

        PdpStatus status32 = makeCopy(status31);
        status32.setState(PdpState.PASSIVE);

        context.addPdp(PDP1, SUBGROUP1).addReply(status11).addReply(status12);
        context.addPdp(PDP2, SUBGROUP1).addReply(status21).addReply(status22);
        context.addPdp(PDP3, SUBGROUP2).addReply(status31).addReply(status32);

        context.startThreads();

        String uri = GROUP_ENDPOINT + "/" + DEACT_GROUP + "?state=PASSIVE";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.put(Entity.json(""));
        PdpGroupStateChangeResponse resp = rawresp.readEntity(PdpGroupStateChangeResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        context.await();
    }

    private PdpStatus makeCopy(PdpStatus source) {
        PdpStatus status = new PdpStatus();

        status.setHealthy(source.getHealthy());
        status.setName(source.getName());
        status.setPdpGroup(source.getPdpGroup());
        status.setPdpSubgroup(source.getPdpSubgroup());
        status.setPdpType(source.getPdpType());
        status.setPolicies(source.getPolicies());

        return status;
    }
}
