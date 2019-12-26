/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Nordix Foundation.
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.rest.PapStatisticsManager;
import org.onap.policy.pap.main.rest.StatisticsReport;

public class StatisticsTest extends End2EndBase {
    private static final String STATISTICS_ENDPOINT = "statistics";
    private List<PdpStatistics> recordList = new ArrayList<>();


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

    @Test
    public void testDb() throws Exception {
        setupEnv();
        Invocation.Builder invocationBuilder = sendRequest("pdps/statistics");
        Response testResponse = invocationBuilder.get();
        verifyResponse(testResponse);
    }

    @Test
    public void testDbWithGroup() throws Exception {
        setupEnv();
        Invocation.Builder invocationBuilder = sendRequest("pdps/statistics/defaultGroup");
        Response testResponse = invocationBuilder.get();
        verifyResponse(testResponse);

    }

    @Test
    public void testDbWithSubGroup() throws Exception {
        setupEnv();
        Invocation.Builder invocationBuilder = sendRequest("pdps/statistics/defaultGroup/apex");
        Response testResponse = invocationBuilder.get();
        verifyResponse(testResponse);
    }

    @Test
    public void testDbWithPdp() throws Exception {
        setupEnv();
        Invocation.Builder invocationBuilder = sendRequest("pdps/statistics/defaultGroup/apex/pdp1");
        Response testResponse = invocationBuilder.get();
        verifyResponse(testResponse);
    }

    @Test
    public void testDbWithPdpLatest() throws Exception {
        setupEnv();
        Invocation.Builder invocationBuilder = sendRequest("pdps/statistics/defaultGroup/apex/pdp1?recordCount=5");
        Response testResponse = invocationBuilder.get();
        verifyResponse(testResponse);
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

    private void setupEnv() {
        PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);

        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            PdpStatistics pdpStatisticsRecord = new PdpStatistics();
            pdpStatisticsRecord.setPdpGroupName("defaultGroup");
            pdpStatisticsRecord.setPdpSubGroupName("apex");
            pdpStatisticsRecord.setPdpInstanceId("pdp1");
            pdpStatisticsRecord.setTimeStamp(new Date());
            pdpStatisticsRecord.setPolicyDeployCount(1);
            pdpStatisticsRecord.setPolicyDeployFailCount(0);
            pdpStatisticsRecord.setPolicyDeploySuccessCount(1);
            pdpStatisticsRecord.setPolicyExecutedCount(1);
            pdpStatisticsRecord.setPolicyExecutedFailCount(0);
            pdpStatisticsRecord.setPolicyExecutedSuccessCount(1);
            recordList.add(pdpStatisticsRecord);
            databaseProvider.createPdpStatistics(recordList);
        } catch (final PfModelException exp) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, exp.getMessage());
        }
    }

    private void verifyResponse(Response testResponse) {
        assertEquals(Response.Status.OK.getStatusCode(), testResponse.getStatus());
        Map<String, Map<String, List<PdpStatistics>>> map =
                testResponse.readEntity(new GenericType<Map<String, Map<String, List<PdpStatistics>>>>() {});
        Map<String, List<PdpStatistics>> subMap = map.get("defaultGroup");
        List<PdpStatistics> resRecord = subMap.get("apex");
        assertEquals("pdp1", resRecord.get(0).getPdpInstanceId());
        assertEquals("apex", resRecord.get(0).getPdpSubGroupName());
        assertEquals("defaultGroup", resRecord.get(0).getPdpGroupName());
    }
}
