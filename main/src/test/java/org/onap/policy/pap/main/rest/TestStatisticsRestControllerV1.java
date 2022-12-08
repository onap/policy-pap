/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2020, 2022 Nordix Foundation.
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
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PapConstants;
import org.springframework.test.context.ActiveProfiles;

/**
 * Class to perform unit test of {@link StatisticsRestControllerV1}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@ActiveProfiles("test")
public class TestStatisticsRestControllerV1 extends CommonPapRestServer {

    private static final String STATISTICS_ENDPOINT = "statistics";
    private static final String STATISTICS_DB_ENDPOINT = "pdps/statistics";

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(STATISTICS_ENDPOINT);
        super.testSwagger(STATISTICS_DB_ENDPOINT);
        super.testSwagger(STATISTICS_DB_ENDPOINT + "/{group}");
        super.testSwagger(STATISTICS_DB_ENDPOINT + "/{group}" + "/{type}");
        super.testSwagger(STATISTICS_DB_ENDPOINT + "/{group}" + "/{type}" + "/{pdp}");
    }

    @Test
    public void testPapStatistics_200() throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(STATISTICS_ENDPOINT);
        StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        validateStatisticsReport(report, 0, 200);
        updateDistributionStatistics();

        invocationBuilder = sendRequest(STATISTICS_ENDPOINT);
        report = invocationBuilder.get(StatisticsReport.class);
        validateStatisticsReport(report, 1, 200);

        // verify it fails when no authorization info is included
        checkUnauthRequest(STATISTICS_ENDPOINT, req -> req.get());
    }

    @Test
    public void testPapStatistics_500() throws Exception {

        markActivatorDead();

        Registry.get(PapConstants.REG_STATISTICS_MANAGER, PapStatisticsManager.class).resetAllStatistics();

        Invocation.Builder invocationBuilder = sendRequest(STATISTICS_ENDPOINT);
        StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        validateStatisticsReport(report, 0, 500);
    }

    private void updateDistributionStatistics() {
        PapStatisticsManager mgr = Registry.get(PapConstants.REG_STATISTICS_MANAGER, PapStatisticsManager.class);

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
