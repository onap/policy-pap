/*-
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
import static org.junit.Assert.assertNull;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PdpGroupStateChangeResponse;
import org.springframework.test.context.ActiveProfiles;

/**
 * Class to perform unit test of {@link PdpGroupStateChangeControllerV1}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@ActiveProfiles("test")
public class TestPdpGroupStateChangeControllerV1 extends CommonPapRestServer {

    private static final String GROUP_ENDPOINT = "pdps/groups";

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(GROUP_ENDPOINT + "/{name}");
    }

    @Test
    public void testChangeGroupState() throws Exception {
        final String uri = GROUP_ENDPOINT + "/my-name?state=ACTIVE";

        final Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.put(Entity.json(""));
        PdpGroupStateChangeResponse resp = rawresp.readEntity(PdpGroupStateChangeResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        rawresp = invocationBuilder.put(Entity.json(""));
        resp = rawresp.readEntity(PdpGroupStateChangeResponse.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNull(resp.getErrorDetails());

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, req -> req.put(Entity.json("")));
    }
}
