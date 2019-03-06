/*
 * ============LICENSE_START=======================================================
 *  Copyright Copyright (C) 2019 AT&T Intellectual Property.
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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.TreeMap;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.pap.main.model.PdpGroup;
import org.onap.policy.pap.main.model.PdpGroupDeploymentResponse;
import org.onap.policy.pap.main.model.PdpSubgroup;

/**
 * Class to perform unit test of {@link PapRestServer}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class TestPdpGroupDeploymentControllerV1 extends CommonPapRestServer {

    private static final String PDP_GROUP_DEPLOYMENT_ENDPOINT = "pdps";
    private static final String POLICY1 = "policy-a";
    private static final String POLICY2 = "policy-b";

    private Entity<PdpGroup> req;
    private PdpGroup group;
    private PdpSubgroup subgrp;

    /**
     * Creates request structure.
     */
    @Before
    public void setUp() {
        group = new PdpGroup();
        group.setDescription("my description");
        group.setName("my-name");
        group.setVersion("my-version");

        subgrp = new PdpSubgroup();
        subgrp.setMinInstanceCount(0);
        subgrp.setPdpType("my-type");
        subgrp.setProperties(new TreeMap<>());
        subgrp.setPolicies(Arrays.asList(POLICY1, POLICY2));

        group.setSubgroups(Arrays.asList(subgrp));

        req = Entity.entity(group, MediaType.APPLICATION_JSON);
    }

    @After
    public void teardown() {
        super.teardown();
    }

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(PDP_GROUP_DEPLOYMENT_ENDPOINT);
    }

    @Test
    public void testDeploy() throws Exception {
        startPapService(false);
        final Invocation.Builder invocationBuilder = sendHttpsRequest(PDP_GROUP_DEPLOYMENT_ENDPOINT);
        final PdpGroupDeploymentResponse resp = invocationBuilder.post(req, PdpGroupDeploymentResponse.class);

        assertTrue(resp.isSuccess());
        assertEquals(0, resp.getPdps().size());
    }
}
