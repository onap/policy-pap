/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2022 Nordix Foundation.
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

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PdpGroupDeleteResponse;
import org.springframework.test.context.ActiveProfiles;

/**
 * Note: this tests failure cases; success cases are tested by tests in the "e2e" package.
 */
@ActiveProfiles("test")
public class TestPdpGroupDeleteControllerV1 extends CommonPapRestServer {

    private static final String GROUP_NOT_FOUND = "group not found";
    private static final String DELETE_GROUP_ENDPOINT = "pdps/groups";
    private static final String DELETE_POLICIES_ENDPOINT = "pdps/policies";

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(DELETE_GROUP_ENDPOINT + "/{name}");

        super.testSwagger(DELETE_POLICIES_ENDPOINT + "/{name}");
        super.testSwagger(DELETE_POLICIES_ENDPOINT + "/{name}/versions/{version}");
    }

    @Test
    public void testDeleteGroup() throws Exception {
        String uri = DELETE_GROUP_ENDPOINT + "/my-name";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.delete();
        PdpGroupDeleteResponse resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());
        assertEquals(GROUP_NOT_FOUND, resp.getErrorDetails());

        rawresp = invocationBuilder.delete();
        resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());
        assertEquals(GROUP_NOT_FOUND, resp.getErrorDetails());

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, SyncInvoker::delete);
    }

    @Test
    public void testDeletePolicy() throws Exception {
        String uri = DELETE_POLICIES_ENDPOINT + "/my-name";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.delete();
        PdpGroupDeleteResponse resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());
        assertEquals("cannot find policy: my-name null", resp.getErrorDetails());

        rawresp = invocationBuilder.delete();
        resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());
        assertEquals("cannot find policy: my-name null", resp.getErrorDetails());

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, SyncInvoker::delete);
    }

    @Test
    public void testDeletePolicyVersion() throws Exception {
        String uri = DELETE_POLICIES_ENDPOINT + "/my-name/versions/3";

        Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.delete();
        PdpGroupDeleteResponse resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());
        assertEquals("cannot find policy: my-name 3", resp.getErrorDetails());

        rawresp = invocationBuilder.delete();
        resp = rawresp.readEntity(PdpGroupDeleteResponse.class);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), rawresp.getStatus());
        assertEquals("cannot find policy: my-name 3", resp.getErrorDetails());

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, SyncInvoker::delete);
    }
}
