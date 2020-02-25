/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2020 Nordix Foundation.
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

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientConfigException;
import org.onap.policy.common.endpoints.http.client.HttpClientFactory;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PAP to fetch health status of all Policy components, including PAP, API, Distribution, and PDPs. 
 *
 * @author Yehui Wang (yehui.wang@est.tech)
 */
public class PolicyComponentsHealthCheckProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyComponentsHealthCheckProvider.class);
    private static final String PAP_GROUP_PARAMS_NAME = "PapGroup";
    private static final String HEALTH_STATUS = "healthy";
    private static final Pattern IP_REPLACEMENT_PATTERN = Pattern.compile("//(\\S+):");
    private static final String POLICY_PAP_HEALTHCHECK_URI = "/policy/pap/v1/healthcheck";
    private PapParameterGroup papParameterGroup = ParameterService.get(PAP_GROUP_PARAMS_NAME);
    private List<HttpClient> clients = new ArrayList<>();

    /**
     * Constructs the object.
     *
     * @throws HttpClientConfigException if creating http client failed
     */
    public PolicyComponentsHealthCheckProvider() throws HttpClientConfigException {
        this(HttpClientFactoryInstance.getClientFactory());
    }

    /**
     * Constructs the object with provided http client factory.
     *
     * <p>This constructor is for unit test to use a mock {@link HttpClientFactory}.
     *
     * @param clientFactory factory used to construct http client
     * @throws HttpClientConfigException if creating http client failed
     */
    PolicyComponentsHealthCheckProvider(HttpClientFactory clientFactory) throws HttpClientConfigException {
        for (BusTopicParams params : papParameterGroup.getHealthCheckRestClientParameters()) {
            params.setManaged(false);
            clients.add(clientFactory.build(params));
        }
    }

    /**
     * Returns health status of all Policy components.
     *
     * @return a pair containing the status and the response
     */
    public Pair<Status, Map<String, Object>> fetchPolicyComponentsHealthStatus() {
        boolean isHealthy = true;
        Map<String, Object> result = new HashMap<>();

        // Check remote components
        for (HttpClient client : clients) {
            HealthCheckReport report = fetchPolicyComponentHealthStatus(client);
            if (!report.isHealthy()) {
                isHealthy = false;
            }
            result.put(client.getName(), report);
        }

        // Check PAP itself
        HealthCheckReport papReport = new HealthCheckProvider().performHealthCheck();
        RestServerParameters restServerParameters = papParameterGroup.getRestServerParameters();
        papReport.setUrl((restServerParameters.isHttps() ? "https://" : "http://") + papReport.getUrl() + ":"
            + restServerParameters.getPort() + POLICY_PAP_HEALTHCHECK_URI);
        if (!papReport.isHealthy()) {
            isHealthy = false;
        }
        result.put(PapConstants.POLICY_PAP, papReport);

        // Check PDPs, read status from DB
        try {
            Map<String, List<Pdp>> pdpListWithType = fetchPdpsHealthStatus();
            if (isHealthy && (pdpListWithType.isEmpty() || pdpListWithType.values().stream().flatMap(List::stream)
                .anyMatch(pdp -> !PdpHealthStatus.HEALTHY.equals(pdp.getHealthy())))) {
                isHealthy = false;
            }
            result.put(PapConstants.POLICY_PDPS, pdpListWithType);
        } catch (final PfModelException exp) {
            result.put(PapConstants.POLICY_PDPS, exp.getErrorResponse());
            isHealthy = false;
        }

        result.put(HEALTH_STATUS, isHealthy);
        LOGGER.debug("Policy Components HealthCheck Response - {}", result);
        return Pair.of(Response.Status.OK, result);
    }

    private Map<String, List<Pdp>> fetchPdpsHealthStatus() throws PfModelException {
        Map<String, List<Pdp>> pdpListWithType = new HashMap<>();
        final PolicyModelsProviderFactoryWrapper modelProviderWrapper =
            Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            final List<PdpGroup> groups = databaseProvider.getPdpGroups(null);
            for (final PdpGroup group : groups) {
                for (final PdpSubGroup subGroup : group.getPdpSubgroups()) {
                    List<Pdp> pdpList = new ArrayList<>(subGroup.getPdpInstances());
                    pdpListWithType.computeIfAbsent(subGroup.getPdpType(), k -> new ArrayList<>()).addAll(pdpList);
                }
            }
        }
        return pdpListWithType;
    }

    private HealthCheckReport fetchPolicyComponentHealthStatus(HttpClient httpClient) {
        HealthCheckReport clientReport;
        try {
            Response resp = httpClient.get();
            clientReport = replaceIpWithHostname(
                resp.readEntity(HealthCheckReport.class), httpClient.getBaseUrl());

            // A health report is read successfully when HTTP status is not OK, it is also not healthy
            // even in the report it says healthy.
            if (resp.getStatus() != HttpURLConnection.HTTP_OK) {
                clientReport.setHealthy(false);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("{} connection error", httpClient.getName());
            clientReport = createUnHealthCheckReport(httpClient.getName(), httpClient.getBaseUrl(),
                HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage());
        }
        return clientReport;
    }

    private HealthCheckReport createUnHealthCheckReport(String name, String url, int code, String message) {
        HealthCheckReport report = new HealthCheckReport();
        report.setName(name);
        report.setUrl(url);
        report.setHealthy(false);
        report.setCode(code);
        report.setMessage(message);
        return report;
    }

    private HealthCheckReport replaceIpWithHostname(HealthCheckReport report, String baseUrl) {
        Matcher matcher = IP_REPLACEMENT_PATTERN.matcher(baseUrl);
        String ip = "";
        if (matcher.find()) {
            ip = matcher.group(1);
            report.setUrl(baseUrl.replace(ip, report.getUrl()));
        }
        return report;
    }
}
