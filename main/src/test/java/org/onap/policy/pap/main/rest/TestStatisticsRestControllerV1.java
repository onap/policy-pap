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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import javax.ws.rs.client.Invocation;
import org.junit.After;
import org.junit.Test;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.RestServerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to perform unit test of {@link PapRestServer}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class TestStatisticsRestControllerV1 extends CommonPapRestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestStatisticsRestControllerV1.class);

    private static final String STATISTICS_ENDPOINT = "statistics";

    /**
     * Method for cleanup after each test.
     */
    @After
    public void teardown() {
        PapStatisticsManager.getInstance().resetAllStatistics();

        super.teardown();
    }

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(STATISTICS_ENDPOINT);
    }

    @Test
    public void testPapStatistics_200() {
        try {
            startPapService(true);
            Invocation.Builder invocationBuilder = sendHttpRequest(STATISTICS_ENDPOINT);
            StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
            validateStatisticsReport(report, 0, 200);
            updateDistributionStatistics();
            invocationBuilder = sendHttpRequest(STATISTICS_ENDPOINT);
            report = invocationBuilder.get(StatisticsReport.class);
            validateStatisticsReport(report, 1, 200);
        } catch (final Exception exp) {
            LOGGER.error("testPapStatistics_200 failed", exp);
            fail("Test should not throw an exception");
        }
    }

    @Test
    public void testPapStatistics_500() {
        final RestServerParameters restServerParams = new CommonTestData().getRestServerParameters(false);
        restServerParams.setName(CommonTestData.PAP_GROUP_NAME);
        restServer = new PapRestServer(restServerParams);
        try {
            restServer.start();
            final Invocation.Builder invocationBuilder = sendHttpRequest(STATISTICS_ENDPOINT);
            final StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
            validateStatisticsReport(report, 0, 500);
        } catch (final Exception exp) {
            LOGGER.error("testPapStatistics_500 failed", exp);
            fail("Test should not throw an exception");
        }
    }

    @Test
    public void testHttpsPapStatistic() {
        try {
            startPapService(false);
            final Invocation.Builder invocationBuilder = sendHttpsRequest(STATISTICS_ENDPOINT);
            final StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
            validateStatisticsReport(report, 0, 200);
        } catch (final Exception exp) {
            LOGGER.error("testHttpsDistributionStatistic failed", exp);
            fail("Test should not throw an exception");
        }
    }

    @Test
    public void testPapStatisticsConstructorIsProtected() throws Exception {
        final Constructor<PapStatisticsManager> constructor = PapStatisticsManager.class.getDeclaredConstructor();
        assertTrue(Modifier.isProtected(constructor.getModifiers()));
    }

    private void updateDistributionStatistics() {
        PapStatisticsManager mgr = PapStatisticsManager.getInstance();

        mgr.updateTotalPdpCount();
        mgr.updateTotalPdpGroupCount();
        mgr.updateTotalPolicyDeployCount();
        mgr.updatePolicyDeploySuccessCount();
        mgr.updatePolicyDeployFailureCount();
        mgr.updateTotalPolicyDownloadCount();
        mgr.updatePolicyDownloadSuccessCount();
        mgr.updatePolicyDownloadFailureCount();
    }

    private void validateStatisticsReport(final StatisticsReport report, final int count, final int code) {
        assertEquals(code, report.getCode());
        assertEquals(count, report.getTotalPdpCount());
        assertEquals(count, report.getTotalPdpGroupCount());
        assertEquals(count, report.getTotalPolicyDeployCount());
        assertEquals(count, report.getPolicyDeploySuccessCount());
        assertEquals(count, report.getPolicyDeployFailureCount());
        assertEquals(count, report.getTotalPolicyDownloadCount());
        assertEquals(count, report.getPolicyDeploySuccessCount());
        assertEquals(count, report.getPolicyDeployFailureCount());
    }
}
