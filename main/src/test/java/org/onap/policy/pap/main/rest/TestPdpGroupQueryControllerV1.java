/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2022-2023 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.springframework.test.context.ActiveProfiles;

/**
 * Class to perform unit test of {@link PdpGroupQueryControllerV1}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@ActiveProfiles({"test", "default"})
class TestPdpGroupQueryControllerV1 extends CommonPapRestServer {

    private static final String GROUP_ENDPOINT = "pdps";

    @Test
    void testSwagger() throws Exception {
        super.testSwagger(GROUP_ENDPOINT);
    }

    @Test
    void testChangeGroupState() throws Exception {
        final String uri = GROUP_ENDPOINT;

        final Invocation.Builder invocationBuilder = sendRequest(uri);
        Response rawresp = invocationBuilder.get();
        PdpGroups resp = rawresp.readEntity(PdpGroups.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp);

        rawresp = invocationBuilder.get();
        resp = rawresp.readEntity(PdpGroups.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        assertNotNull(resp);

        // verify it fails when no authorization info is included
        checkUnauthRequest(uri, SyncInvoker::get);
    }
}
