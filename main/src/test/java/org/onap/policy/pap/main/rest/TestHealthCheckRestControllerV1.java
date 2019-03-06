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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ws.rs.client.Invocation;
import org.junit.After;
import org.junit.Test;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.RestServerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to perform unit test of {@link PapRestServer}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class TestHealthCheckRestControllerV1 extends CommonPapRestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestHealthCheckRestControllerV1.class);

    private static final String HEALTHCHECK_ENDPOINT = "healthcheck";

    /**
     * Method for cleanup after each test.
     */
    @After
    public void teardown() {
        super.teardown();
    }

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(HEALTHCHECK_ENDPOINT);
    }

    @Test
    public void testHealthCheckSuccess() {
        try {
            startPapService(true);
            final Invocation.Builder invocationBuilder = sendHttpRequest(HEALTHCHECK_ENDPOINT);
            final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
            validateHealthCheckReport(NAME, SELF, true, 200, ALIVE, report);
        } catch (final Exception exp) {
            LOGGER.error("testHealthCheckSuccess failed", exp);
            fail("Test should not throw an exception");
        }
    }

    @Test
    public void testHealthCheckFailure() throws Exception {
        final RestServerParameters restServerParams = new CommonTestData().getRestServerParameters(false);
        restServerParams.setName(CommonTestData.PAP_GROUP_NAME);
        restServer = new PapRestServer(restServerParams);
        restServer.start();
        final Invocation.Builder invocationBuilder = sendHttpRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        validateHealthCheckReport(NAME, SELF, false, 500, NOT_ALIVE, report);
        assertTrue(restServer.isAlive());
        assertTrue(restServer.toString().startsWith("PapRestServer [servers="));
    }

    @Test
    public void testHttpsHealthCheckSuccess() {
        try {
            startPapService(false);
            final Invocation.Builder invocationBuilder = sendHttpsRequest(HEALTHCHECK_ENDPOINT);
            final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
            validateHealthCheckReport(NAME, SELF, true, 200, ALIVE, report);
        } catch (final Exception exp) {
            LOGGER.error("testHttpsHealthCheckSuccess failed", exp);
            fail("Test should not throw an exception");
        }
    }

    private void validateHealthCheckReport(final String name, final String url, final boolean healthy, final int code,
            final String message, final HealthCheckReport report) {
        assertEquals(name, report.getName());
        assertEquals(url, report.getUrl());
        assertEquals(healthy, report.isHealthy());
        assertEquals(code, report.getCode());
        assertEquals(message, report.getMessage());
    }
}
