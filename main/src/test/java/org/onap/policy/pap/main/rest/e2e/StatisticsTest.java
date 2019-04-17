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
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.rest.PapStatisticsManager;
import org.onap.policy.pap.main.rest.StatisticsReport;

public class StatisticsTest extends End2EndBase {
    private static final String STATISTICS_ENDPOINT = "statistics";


    @Test
    public void test() throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(STATISTICS_ENDPOINT);
        StatisticsReport report = invocationBuilder.get(StatisticsReport.class);
        assertEquals(HttpURLConnection.HTTP_OK, report.getCode());

        updateDistributionStatistics();

        invocationBuilder = sendRequest(STATISTICS_ENDPOINT);
        StatisticsReport report2 = invocationBuilder.get(StatisticsReport.class);

        assertEquals(HttpURLConnection.HTTP_OK, report.getCode());
        assertEquals(report.getTotalPdpCount() + 1, report2.getTotalPdpCount());
        assertEquals(report.getTotalPdpGroupCount() + 1, report2.getTotalPdpGroupCount());
        assertEquals(report.getTotalPolicyDeployCount() + 1, report2.getTotalPolicyDeployCount());
        assertEquals(report.getPolicyDeploySuccessCount() + 1, report2.getPolicyDeploySuccessCount());
        assertEquals(report.getPolicyDeployFailureCount() + 1, report2.getPolicyDeployFailureCount());
        assertEquals(report.getTotalPolicyDownloadCount() + 1, report2.getTotalPolicyDownloadCount());
        assertEquals(report.getPolicyDeploySuccessCount() + 1, report2.getPolicyDeploySuccessCount());
        assertEquals(report.getPolicyDeployFailureCount() + 1, report2.getPolicyDeployFailureCount());
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
}
