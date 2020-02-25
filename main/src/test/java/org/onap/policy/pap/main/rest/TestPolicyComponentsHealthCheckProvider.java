/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation.
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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientFactory;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.startstop.PapActivator;

public class TestPolicyComponentsHealthCheckProvider {

    private static final String CLIENT_1 = "client1";
    private static final String PDP_GROUP_DATA_FILE = "rest/pdpGroup.json";
    private static final String PAP_GROUP_PARAMS_NAME = "PapGroup";

    @Mock
    private PolicyModelsProvider dao;

    @Mock
    private PolicyModelsProviderFactoryWrapper daofact;

    @Mock
    private HttpClientFactory clientFactory;

    @Mock
    PapActivator papActivator;

    @Mock
    private HttpClient client1;

    @Mock
    private HttpClient client2;

    @Mock
    private Response response1;

    @Mock
    private Response response2;

    private List<PdpGroup> groups;

    private PapParameterGroup savedPapParameterGroup;

    /**
     * Configures mocks and objects.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        groups = loadPdpGroupsFromFile().getGroups();
        when(dao.getPdpGroups(any())).thenReturn(groups);

        when(daofact.create()).thenReturn(dao);
        Registry.newRegistry();
        Registry.register(PapConstants.REG_PAP_DAO_FACTORY, daofact);

        when(papActivator.isAlive()).thenReturn(true);
        Registry.register(PapConstants.REG_PAP_ACTIVATOR, papActivator);

        if (ParameterService.contains(PAP_GROUP_PARAMS_NAME)) {
            savedPapParameterGroup = ParameterService.get(PAP_GROUP_PARAMS_NAME);
        }
        CommonTestData testData = new CommonTestData();
        ParameterService.register(testData.getPapParameterGroup(0), true);

        when(client1.getName()).thenReturn(CLIENT_1);
        when(client1.getBaseUrl()).thenReturn("url1");
        when(response1.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);
        when(response1.readEntity(HealthCheckReport.class))
            .thenReturn(createReport(HttpURLConnection.HTTP_OK, true));
        when(client1.get()).thenReturn(response1);

        when(client2.getName()).thenReturn("client2");
        when(client2.getBaseUrl()).thenReturn("url2");
        when(response2.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);
        when(response2.readEntity(HealthCheckReport.class))
            .thenReturn(createReport(HttpURLConnection.HTTP_OK, true));
        when(client2.get()).thenReturn(response2);

        List<HttpClient> clients = new ArrayList<>(List.of(client1, client2));
        when(clientFactory.inventory()).thenReturn(clients);
    }

    /**
     * Tear down.
     */
    @After
    public void tearDown() {
        if (savedPapParameterGroup != null) {
            ParameterService.register(savedPapParameterGroup, true);
        } else {
            ParameterService.deregister(PAP_GROUP_PARAMS_NAME);
        }
    }

    @Test
    public void testFetchPolicyComponentsHealthStatus_allHealthy() {
        PolicyComponentsHealthCheckProvider provider = new PolicyComponentsHealthCheckProvider(clientFactory);
        Pair<Status, Map<String, Object>> ret = provider.fetchPolicyComponentsHealthStatus();
        assertEquals(ret.getLeft(), Response.Status.OK);
        assertTrue((Boolean) ret.getRight().get("healthy"));
    }

    @Test
    public void testFetchPolicyComponentsHealthStatus_unhealthyClient() {
        when(response1.getStatus()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(response1.readEntity(HealthCheckReport.class))
            .thenReturn(createReport(HttpURLConnection.HTTP_INTERNAL_ERROR, false));
        Map<String, Object> result = callFetchPolicyComponentsHealthStatus();
        assertFalse((Boolean) result.get("healthy"));
        HealthCheckReport report = (HealthCheckReport) result.get(CLIENT_1);
        assertFalse(report.isHealthy());

        when(response1.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);
        when(response1.readEntity(HealthCheckReport.class))
            .thenReturn(createReport(HttpURLConnection.HTTP_OK, false));
        Map<String, Object> result2 = callFetchPolicyComponentsHealthStatus();
        assertFalse((Boolean) result2.get("healthy"));
        HealthCheckReport report2 = (HealthCheckReport) result.get(CLIENT_1);
        assertFalse(report.isHealthy());
    }

    @Test
    public void testFetchPolicyComponentsHealthStatus_unhealthyPdps() {
        //Get a PDP and set it unhealthy
        groups.get(0).getPdpSubgroups().get(0)
            .getPdpInstances().get(0).setHealthy(PdpHealthStatus.NOT_HEALTHY);
        Map<String, Object> result = callFetchPolicyComponentsHealthStatus();
        Map<String, List<Pdp>> pdpListWithType = (Map<String, List<Pdp>>) result.get(PapConstants.POLICY_PDPS);
        assertEquals(2, pdpListWithType.size());
        assertFalse((Boolean) result.get("healthy"));
    }

    @Test
    public void testFetchPolicyComponentsHealthStatus_unhealthyPap() {
        when(papActivator.isAlive()).thenReturn(false);
        Map<String, Object> result = callFetchPolicyComponentsHealthStatus();
        assertFalse((Boolean) result.get("healthy"));
        HealthCheckReport report = (HealthCheckReport) result.get(PapConstants.POLICY_PAP);
        assertFalse(report.isHealthy());
    }

    private Map<String, Object> callFetchPolicyComponentsHealthStatus() {
        PolicyComponentsHealthCheckProvider provider = new PolicyComponentsHealthCheckProvider(clientFactory);
        return provider.fetchPolicyComponentsHealthStatus().getRight();
    }

    private HealthCheckReport createReport(int code, boolean healthy) {
        HealthCheckReport report = new HealthCheckReport();
        report.setName("name");
        report.setUrl("url");
        report.setCode(code);
        report.setHealthy(healthy);
        report.setMessage("message");
        return report;
    }

    private PdpGroups loadPdpGroupsFromFile() {
        final File propFile = new File(ResourceUtils.getFilePath4Resource(PDP_GROUP_DATA_FILE));
        try {
            Coder coder = new StandardCoder();
            return coder.decode(propFile, PdpGroups.class);
        } catch (final CoderException e) {
            throw new RuntimeException(e);
        }
    }
}