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
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.onap.policy.models.pdp.concepts.Pdps;
import org.springframework.test.context.ActiveProfiles;

/**
 * Class to perform unit test of {@link PdpGroupHealthCheckControllerV1}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@ActiveProfiles({ "test", "default" })
public class TestPdpGroupHealthCheckControllerV1 extends CommonPapRestServer {

    private static final String ENDPOINT = "pdps/healthcheck";

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(ENDPOINT);
    }

    @Test
    public void testPdpGroupHealthCheck() throws Exception {
        final String uri = ENDPOINT;

        final Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        Pdps resp = rawresp.readEntity(Pdps.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp);

        rawresp = invocationBuilder.get();
        resp = rawresp.readEntity(Pdps.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp);

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, SyncInvoker::get);
    }
}
