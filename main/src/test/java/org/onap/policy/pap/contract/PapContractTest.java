/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
 * Modifications Copyright (C) 2023 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.contract;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.pap.main.rest.CommonPapRestServer;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({ "test", "stub" })
public class PapContractTest extends CommonPapRestServer {

    @Test
    public void testStubsHealthcheck() throws Exception {
        checkStubJsonGet("healthcheck");
        checkStubJsonGet("pdps/healthcheck");
        checkStubJsonGet("components/healthcheck");
    }

    @Test
    public void testStubsPolicies() throws Exception {
        checkStubJsonGet("policies/audit");
        checkStubJsonGet("policies/audit/group");
        checkStubJsonGet("policies/audit/group/name/version");
        checkStubJsonGet("policies/audit/name,version");
        checkStubJsonGet("policies/deployed");
        checkStubJsonGet("policies/deployed/name");
        checkStubJsonGet("policies/deployed/name/version");
        checkStubJsonGet("policies/status");
        checkStubJsonGet("policies/status/group");
        checkStubJsonGet("policies/status/group/name");
        checkStubJsonGet("policies/status/group/name/version");
    }

    @Test
    public void testStubsPdps() throws Exception {
        checkStubJsonGet("pdps");

        checkStubJsonPost("pdps/groups/batch");
        checkStubJsonPost("pdps/deployments/batch");
        checkStubJsonPost("pdps/policies");

        checkStubJsonPut("pdps/groups/my-name?state=ACTIVE");

        checkStubJsonDelete("pdps/groups/name");
        checkStubJsonDelete("pdps/policies/name");
        checkStubJsonDelete("pdps/policies/name/versions/version");
    }


    private void checkStubJsonGet(String url) throws Exception {
        var response = super.sendRequest(url);
        assertEquals(Response.Status.OK.getStatusCode(), response.get().getStatus());
    }

    private void checkStubJsonPost(String url) throws Exception {
        var response = super.sendRequest(url);
        PdpGroups groups = new PdpGroups();
        assertEquals(Response.Status.OK.getStatusCode(), response
                .post(Entity.entity(groups, MediaType.APPLICATION_JSON))
                .getStatus());
    }

    private void checkStubJsonPut(String url) throws Exception {
        var response = super.sendRequest(url);
        assertEquals(Response.Status.OK.getStatusCode(), response.put(Entity.json("")).getStatus());
    }

    private void checkStubJsonDelete(String url) throws Exception {
        var response = super.sendRequest(url);
        assertEquals(Response.Status.OK.getStatusCode(), response.delete().getStatus());
    }

}
