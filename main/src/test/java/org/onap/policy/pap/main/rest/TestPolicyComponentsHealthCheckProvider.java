/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020, 2022-2023 Nordix Foundation.
 *  Modifications Copyright (C) 2020-2021 AT&T Corp.
 *  Modifications Copyright (C) 2020-2022 Bell Canada. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
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
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.service.PdpGroupService;
import org.onap.policy.pap.main.startstop.PapActivator;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
class TestPolicyComponentsHealthCheckProvider {

    private static final String CLIENT_1 = "client1";
    private static final String PDP_GROUP_DATA_FILE = "rest/pdpGroup.json";
    private static final String PAP_GROUP_PARAMS_NAME = "PapGroup";
    private static final String HEALTHY = "healthy";

    @Mock
    private PdpGroupService pdpGroupService;

    @Mock
    private HttpClientFactory clientFactory;

    @Mock
    PapActivator papActivator;

    @Mock
    private HttpClient client1;

    @Mock
    private HttpClient client2;

    @Mock
    private HttpClient client3;

    @Mock
    private Response response1;

    @Mock
    private Response response2;

    @Mock
    private Response response3;

    private List<PdpGroup> groups;

    private PapParameterGroup savedPapParameterGroup;

    private PolicyComponentsHealthCheckProvider provider;

    AutoCloseable autoCloseable;

    /**
     * Configures mocks and objects.
     *
     * @throws Exception if an error occurs
     */
    @BeforeEach
    public void setUp() throws Exception {
        autoCloseable = MockitoAnnotations.openMocks(this);
        groups = loadPdpGroupsFromFile().getGroups();
        when(pdpGroupService.getPdpGroups()).thenReturn(groups);

        Registry.newRegistry();

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
        when(response1.readEntity(HealthCheckReport.class)).thenReturn(createReport(HttpURLConnection.HTTP_OK, true));
        when(client1.get()).thenReturn(response1);

        when(client2.getName()).thenReturn("client2");
        when(client2.getBaseUrl()).thenReturn("url2");
        when(response2.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);
        when(response2.readEntity(HealthCheckReport.class)).thenReturn(createReport(HttpURLConnection.HTTP_OK, true));
        when(client2.get()).thenReturn(response2);

        when(client3.getName()).thenReturn("dmaap");
        when(client3.getBaseUrl()).thenReturn("message-router");
        when(response3.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);
        when(response3.readEntity(DmaapGetTopicResponse.class)).thenReturn(createDmaapResponse());
        when(client3.get()).thenReturn(response3);
        List<HttpClient> clients = new ArrayList<>();
        clients.add(client1);
        clients.add(client2);
        clients.add(client3);
        PapParameterGroup papParameterGroup = ParameterService.get(PAP_GROUP_PARAMS_NAME);
        provider = new PolicyComponentsHealthCheckProvider(papParameterGroup, pdpGroupService);
        ReflectionTestUtils.setField(provider, "papParameterGroup", papParameterGroup);
        provider.initializeClientHealthCheckExecutorService();
        ReflectionTestUtils.setField(provider, "clients", clients);
        ReflectionTestUtils.setField(provider, "topicPolicyPdpPap", "POLICY-PDP-PAP");
    }

    /**
     * Tear down.
     */
    @AfterEach
    public void tearDown() throws Exception {
        if (savedPapParameterGroup != null) {
            ParameterService.register(savedPapParameterGroup, true);
        } else {
            ParameterService.deregister(PAP_GROUP_PARAMS_NAME);
        }
        provider.cleanup();
        autoCloseable.close();
    }


    @Test
    void testFetchPolicyComponentsHealthStatus_allHealthy() {
        Pair<HttpStatus, Map<String, Object>> ret = provider.fetchPolicyComponentsHealthStatus();
        assertEquals(HttpStatus.OK, ret.getLeft());
        assertTrue((Boolean) ret.getRight().get(HEALTHY));
    }

    @Test
    void testFetchPolicyComponentsHealthStatus_unhealthyClient() {
        when(response1.getStatus()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(response1.readEntity(HealthCheckReport.class))
            .thenReturn(createReport(HttpURLConnection.HTTP_INTERNAL_ERROR, false));
        Map<String, Object> result = callFetchPolicyComponentsHealthStatus();
        assertFalse((Boolean) result.get(HEALTHY));
        HealthCheckReport report = (HealthCheckReport) result.get(CLIENT_1);
        assertFalse(report.isHealthy());

        when(response1.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);
        when(response1.readEntity(HealthCheckReport.class)).thenReturn(createReport(HttpURLConnection.HTTP_OK, false));
        Map<String, Object> result2 = callFetchPolicyComponentsHealthStatus();
        assertFalse((Boolean) result2.get(HEALTHY));
        HealthCheckReport report2 = (HealthCheckReport) result.get(CLIENT_1);
        assertFalse(report2.isHealthy());

        when(response3.getStatus()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(response3.readEntity(DmaapGetTopicResponse.class)).thenReturn(null);
        Map<String, Object> result3 = callFetchPolicyComponentsHealthStatus();
        assertFalse((Boolean) result3.get(HEALTHY));
        HealthCheckReport report3 = (HealthCheckReport) result3.get("dmaap");
        assertFalse(report3.isHealthy());

        when(response3.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);
        when(response3.readEntity(DmaapGetTopicResponse.class)).thenReturn(new DmaapGetTopicResponse());
        Map<String, Object> result4 = callFetchPolicyComponentsHealthStatus();
        assertFalse((Boolean) result4.get(HEALTHY));
        HealthCheckReport report4 = (HealthCheckReport) result4.get("dmaap");
        assertFalse(report4.isHealthy());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFetchPolicyComponentsHealthStatus_unhealthyPdps() {
        // Get a PDP and set it unhealthy
        groups.get(0).getPdpSubgroups().get(0).getPdpInstances().get(0).setHealthy(PdpHealthStatus.NOT_HEALTHY);
        Map<String, Object> result = callFetchPolicyComponentsHealthStatus();
        Map<String, List<Pdp>> pdpListWithType = (Map<String, List<Pdp>>) result.get(PapConstants.POLICY_PDPS);
        assertEquals(2, pdpListWithType.size());
        assertFalse((Boolean) result.get(HEALTHY));
    }

    @Test
    void testFetchPolicyComponentsHealthStatus_PdpDown() {
        // Set currentInstanceCount as 0 to simulate PDP down
        groups.get(0).getPdpSubgroups().get(0).setCurrentInstanceCount(0);
        Map<String, Object> result = callFetchPolicyComponentsHealthStatus();
        assertFalse((Boolean) result.get(HEALTHY));
    }

    @Test
    void testFetchPolicyComponentsHealthStatus_unhealthyPap() {
        when(papActivator.isAlive()).thenReturn(false);
        Map<String, Object> result = callFetchPolicyComponentsHealthStatus();
        assertFalse((Boolean) result.get(HEALTHY));
        HealthCheckReport report = (HealthCheckReport) result.get(PapConstants.POLICY_PAP);
        assertFalse(report.isHealthy());
    }

    private Map<String, Object> callFetchPolicyComponentsHealthStatus() {

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

    private static PdpGroups loadPdpGroupsFromFile() {
        final File propFile = new File(ResourceUtils.getFilePath4Resource(PDP_GROUP_DATA_FILE));
        try {
            Coder coder = new StandardCoder();
            return coder.decode(propFile, PdpGroups.class);
        } catch (final CoderException e) {
            throw new RuntimeException(e);
        }
    }

    private DmaapGetTopicResponse createDmaapResponse() {
        DmaapGetTopicResponse response = new DmaapGetTopicResponse();
        response.setTopics(List.of("POLICY-PDP-PAP"));
        return response;
    }
}
