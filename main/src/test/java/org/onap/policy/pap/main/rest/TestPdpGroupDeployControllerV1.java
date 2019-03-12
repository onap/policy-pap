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

package org.onap.policy.pap.main.rest;

import static org.junit.Assert.assertNull;

import java.util.Arrays;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import org.junit.Test;
import org.onap.policy.models.pap.PdpGroup;
import org.onap.policy.models.pap.PdpGroupDeployResponse;
import org.onap.policy.models.pap.PdpSubGroup;

public class TestPdpGroupDeployControllerV1 extends CommonPapRestServer {

    private static final String DEPLOY_ENDPOINT = "pdps";

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(DEPLOY_ENDPOINT);
    }

    @Test
    public void testDeploy() throws Exception {
        Entity<PdpGroup> entgrp = makePdpGroupEntity();

        Invocation.Builder invocationBuilder = sendRequest(DEPLOY_ENDPOINT);
        PdpGroupDeployResponse resp = invocationBuilder.post(entgrp, PdpGroupDeployResponse.class);
        assertNull(resp.getErrorDetails());

        invocationBuilder = sendRequest(DEPLOY_ENDPOINT);
        resp = invocationBuilder.post(entgrp, PdpGroupDeployResponse.class);
        assertNull(resp.getErrorDetails());
    }

    private Entity<PdpGroup> makePdpGroupEntity() {
        PdpSubGroup subgrp = new PdpSubGroup();
        subgrp.setPdpType("drools");

        PdpGroup group = new PdpGroup();
        group.setName("drools-group");
        group.setDescription("my description");
        group.setVersion("my-version");
        group.setPdpSubgroups(Arrays.asList(subgrp));

        Entity<PdpGroup> entgrp = Entity.entity(group, MediaType.APPLICATION_JSON);
        return entgrp;
    }
}
