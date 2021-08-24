/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2020 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 *  Modifications Copyright (C) 2020-2021 Bell Canada. All rights reserved.
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientConfigException;
import org.onap.policy.common.endpoints.http.client.HttpClientFactory;
import org.onap.policy.common.endpoints.parameters.RestClientParameters;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
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
    private static List<HttpClient> clients = new ArrayList<>();
    private static ExecutorService clientHealthCheckExecutorService;

    private PapParameterGroup papParameterGroup = ParameterService.get(PAP_GROUP_PARAMS_NAME);

    /**
     * This method is used to initialize clients and executor.
     * @param papParameterGroup
     *     @{link PapParameterGroup} contains the Pap Parameters set during startup
     * @param clientFactory
     *     @{link HttpClientFactory} contains the client details
     */
    public static void initializeClientHealthCheckExecutorService(PapParameterGroup papParameterGroup,
        HttpClientFactory clientFactory) throws HttpClientConfigException {
        for (RestClientParameters params : papParameterGroup.getHealthCheckRestClientParameters()) {
            params.setManaged(false);
            clients.add(clientFactory.build(params));
        }
        clientHealthCheckExecutorService = Executors.newFixedThreadPool(clients.isEmpty() ? 1 : clients.size());
    }

    /**
     * Returns health status of all Policy components.
     *
     * @return a pair containing the status and the response
     */
    public Pair<Status, Map<String, Object>> fetchPolicyComponentsHealthStatus() {
        boolean isHealthy;
        Map<String, Object> result;

        // Check remote components
        List<Callable<Entry<String, Object>>> tasks = new ArrayList<>(clients.size());

        for (HttpClient client : clients) {
            tasks.add(() -> new AbstractMap.SimpleEntry<>(client.getName(), fetchPolicyComponentHealthStatus(client)));
        }

        try {
            List<Future<Entry<String, Object>>> futures = clientHealthCheckExecutorService.invokeAll(tasks);
            result = futures.stream().map(entryFuture -> {
                try {
                    return entryFuture.get();
                } catch (ExecutionException e) {
                    throw new PfModelRuntimeException(Status.BAD_REQUEST, "Client Health check Failed ", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new PfModelRuntimeException(Status.BAD_REQUEST, "Client Health check interrupted ", e);
                }
            }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            //true when all the clients health status is true
            isHealthy = result.values().stream().allMatch(o -> ((HealthCheckReport) o).isHealthy());
        } catch (InterruptedException exp) {
            Thread.currentThread().interrupt();
            throw new PfModelRuntimeException(Status.BAD_REQUEST, "Client Health check interrupted ", exp);
        }

        // Check PAP itself
        HealthCheckReport papReport = new HealthCheckProvider().performHealthCheck();
        var restServerParameters = papParameterGroup.getRestServerParameters();
        papReport.setUrl(
            (restServerParameters.isHttps() ? "https://" : "http://") + papReport.getUrl() + ":" + restServerParameters
                .getPort() + POLICY_PAP_HEALTHCHECK_URI);
        if (!papReport.isHealthy()) {
            isHealthy = false;
        }
        result.put(PapConstants.POLICY_PAP, papReport);

        // Check PDPs, read status from DB
        try {
            List<PdpGroup> groups = fetchPdpGroups();
            Map<String, List<Pdp>> pdpListWithType = fetchPdpsHealthStatus(groups);
            if (isHealthy && (!verifyNumberOfPdps(groups) || pdpListWithType.values().stream().flatMap(List::stream)
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
        return Pair.of(Status.OK, result);
    }

    private Map<String, List<Pdp>> fetchPdpsHealthStatus(List<PdpGroup> groups) {
        Map<String, List<Pdp>> pdpListWithType = new HashMap<>();
        for (final PdpGroup group : groups) {
            for (final PdpSubGroup subGroup : group.getPdpSubgroups()) {
                List<Pdp> pdpList = new ArrayList<>(subGroup.getPdpInstances());
                pdpListWithType.computeIfAbsent(subGroup.getPdpType(), k -> new ArrayList<>()).addAll(pdpList);
            }
        }
        return pdpListWithType;
    }

    private boolean verifyNumberOfPdps(List<PdpGroup> groups) {
        var flag = true;
        for (final PdpGroup group : groups) {
            for (final PdpSubGroup subGroup : group.getPdpSubgroups()) {
                if (subGroup.getCurrentInstanceCount() < subGroup.getDesiredInstanceCount()) {
                    flag = false;
                    break;
                }
            }
        }
        return flag;
    }

    private List<PdpGroup> fetchPdpGroups() throws PfModelException {
        List<PdpGroup> groups = new ArrayList<>();
        final PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                        Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            groups = databaseProvider.getPdpGroups(null);
        }
        return groups;
    }

    private HealthCheckReport fetchPolicyComponentHealthStatus(HttpClient httpClient) {
        HealthCheckReport clientReport;
        try {
            Response resp = httpClient.get();
            if (httpClient.getName().equalsIgnoreCase("dmaap")) {
                clientReport = verifyDmaapClient(httpClient, resp);
            } else {
                clientReport = replaceIpWithHostname(resp.readEntity(HealthCheckReport.class), httpClient.getBaseUrl());
            }

            // A health report is read successfully when HTTP status is not OK, it is also
            // not healthy
            // even in the report it says healthy.
            if (resp.getStatus() != HttpURLConnection.HTTP_OK) {
                clientReport.setHealthy(false);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("{} connection error", httpClient.getName());
            clientReport = createHealthCheckReport(httpClient.getName(), httpClient.getBaseUrl(),
                            HttpURLConnection.HTTP_INTERNAL_ERROR, false, e.getMessage());
        }
        return clientReport;
    }

    private HealthCheckReport createHealthCheckReport(String name, String url, int code, boolean status,
                    String message) {
        var report = new HealthCheckReport();
        report.setName(name);
        report.setUrl(url);
        report.setHealthy(status);
        report.setCode(code);
        report.setMessage(message);
        return report;
    }

    private HealthCheckReport replaceIpWithHostname(HealthCheckReport report, String baseUrl) {
        var matcher = IP_REPLACEMENT_PATTERN.matcher(baseUrl);
        if (matcher.find()) {
            var ip = matcher.group(1);
            report.setUrl(baseUrl.replace(ip, report.getUrl()));
        }
        return report;
    }

    private HealthCheckReport verifyDmaapClient(HttpClient httpClient, Response resp) {
        HealthCheckReport clientReport;
        DmaapGetTopicResponse dmaapResponse = resp.readEntity(DmaapGetTopicResponse.class);
        var topicVerificationStatus = (dmaapResponse.getTopics() != null
                        && dmaapResponse.getTopics().contains(PapConstants.TOPIC_POLICY_PDP_PAP));
        String message = (topicVerificationStatus ? "PAP to DMaaP connection check is successfull"
                        : "PAP to DMaaP connection check failed");
        int code = (topicVerificationStatus ? resp.getStatus() : 503);
        clientReport = createHealthCheckReport(httpClient.getName(), httpClient.getBaseUrl(), code,
                        topicVerificationStatus, message);
        return clientReport;
    }

    /**
     * This method clears clients {@link List} and clientHealthCheckExecutorService {@link ExecutorService}.
     */
    public static void cleanup() {
        clients.clear();
        clientHealthCheckExecutorService.shutdown();
    }
}
