/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2020, 2022 Nordix Foundation.
 *  Modifications Copyright (C) 2020-2021 AT&T Inc.
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.onap.policy.common.endpoints.parameters.RestClientParameters;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/**
 * Class to perform unit test of {@link PolicyComponentsHealthCheckControllerV1}.
 *
 * @author Yehui Wang (yehui.wang@est.tech)
 */
@ActiveProfiles({ "test", "default" })
public class TestPolicyComponentsHealthCheckControllerV1 extends CommonPapRestServer {

    private static final String ENDPOINT = "components/healthcheck";
    private List<RestClientParameters> savedRestClientParameters;

    @Autowired
    private PapParameterGroup papParameterGroup;

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(ENDPOINT);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPolicyComponentsHealthCheck() throws Exception {
        // take out the other components for healthcheck
        savedRestClientParameters = papParameterGroup.getHealthCheckRestClientParameters();
        papParameterGroup.setHealthCheckRestClientParameters(null);

        Invocation.Builder invocationBuilder = sendRequest(ENDPOINT);
        Response response = invocationBuilder.get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Map<String, Object> result = new HashMap<>();
        result = (Map<String, Object>) response.readEntity(GenericType.forInstance(result));
        // No PDP configured, healthy is false
        assertFalse((Boolean) result.get("healthy"));

        // put back the other components
        papParameterGroup.setHealthCheckRestClientParameters(savedRestClientParameters);
    }
}
