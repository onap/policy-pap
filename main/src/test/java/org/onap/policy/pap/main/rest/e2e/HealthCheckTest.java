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

import java.net.HttpURLConnection;
import javax.ws.rs.client.Invocation;
import org.junit.Test;
import org.onap.policy.common.endpoints.report.HealthCheckReport;

public class HealthCheckTest extends End2EndBase {
    private static final String HEALTHCHECK_ENDPOINT = "healthcheck";

    @Test
    public void testHealthCheckSuccess() throws Exception {
        final Invocation.Builder invocationBuilder = sendRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);

        assertEquals(NAME, report.getName());
        assertEquals(SELF, report.getUrl());
        assertEquals(true, report.isHealthy());
        assertEquals(HttpURLConnection.HTTP_OK, report.getCode());
        assertEquals(ALIVE, report.getMessage());
    }
}
