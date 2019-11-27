/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
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
import org.onap.policy.pap.main.startstop.PapActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PAP to fetch health status of all Policy components, including API, Distribution,
 * and PDPs.
 *
 * @author Yehui Wang (yehui.wang@est.tech)
 */
public class PolicyComponentsHealthCheckProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyComponentsHealthCheckProvider.class);
    private PapParameterGroup papParameterGroup =
            Registry.get(PapConstants.REG_PAP_ACTIVATOR, PapActivator.class).getParameterGroup();
    private static final String POLICY_API_HEALTHCHECK_URI = "/policy/api/v1/healthcheck";
    private static final String POLICY_DISTRIBUTION_HEALTHCHECK_URI = "/healthcheck";
    private static final String POLICY_PAP_HEALTHCHECK_URI = "/policy/pap/v1/healthcheck";
    private static final String HEALTH_STATUS = "Health Status";
    private boolean isHealthy = true;
    private final Pattern pattern = Pattern.compile("//(\\S+):");

    /**
     * Returns health status of all Policy components.
     *
     * @return a pair containing the status and the response
     * @throws PfModelException in case of errors
     */
    public Pair<Status, Map<String, Object>> fetchPolicyComponentsHealthStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put(PapConstants.POLICY_API,
                fetchPolicyComponentHealStatus(PapConstants.POLICY_API, POLICY_API_HEALTHCHECK_URI));
        result.put(PapConstants.POLICY_DISTRIBUTION,
                fetchPolicyComponentHealStatus(PapConstants.POLICY_DISTRIBUTION, POLICY_DISTRIBUTION_HEALTHCHECK_URI));
        result.put(PapConstants.POLICY_PAP,
                fetchPolicyComponentHealStatus(PapConstants.POLICY_PAP, POLICY_PAP_HEALTHCHECK_URI));
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
        Map<String, List<Pdp>> result = new HashMap<>();
        final PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            final List<PdpGroup> groups = databaseProvider.getPdpGroups(null);
            for (final PdpGroup group : groups) {
                for (final PdpSubGroup subGroup : group.getPdpSubgroups()) {
                    List<Pdp> pdpList = new ArrayList<>();
                    pdpList.addAll(subGroup.getPdpInstances());
                    result.put(subGroup.getPdpType(), pdpList);
                }
            }
        }
        return result;
    }

    private HealthCheckReport fetchPolicyComponentHealStatus(String clientName, String uri) {
        HealthCheckReport report = new HealthCheckReport();
        try {
            HttpClient httpClient = getHttpClient(clientName);
            Response resp = httpClient.get(uri);
            if (resp.getStatus() != HttpURLConnection.HTTP_OK) {
                report.setName(clientName);
                report.setHealthy(false);
                isHealthy = false;
                report.setCode(resp.getStatus());
                report.setMessage(resp.readEntity(String.class));
                return replaceIpWithHostname(report, httpClient.getBaseUrl(), uri);
            } else {
                return replaceIpWithHostname(resp.readEntity(HealthCheckReport.class), httpClient.getBaseUrl(), uri);
            }
        } catch (HttpClientConfigException | RuntimeException e) {
            LOGGER.warn("{} connection error", clientName);
            report.setName(clientName);
            report.setUrl(uri);
            report.setHealthy(false);
            isHealthy = false;
            report.setCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
            report.setMessage(e.getMessage());
            return report;
        }
    }

    private HealthCheckReport replaceIpWithHostname(HealthCheckReport report, String baseUrl, String uri) {
        Matcher matcher = pattern.matcher(baseUrl);
        String ip = "";
        if (matcher.find()) {
            ip = matcher.group(1);
            report.setUrl(baseUrl.replace(ip, report.getUrl()) + uri.substring(1));
        }
        return report;
    }

    private HttpClient getHttpClient(String clientName) throws HttpClientConfigException {
        RestServerParameters parameters = null;
        if (PapConstants.POLICY_API.equals(clientName)) {
            parameters = papParameterGroup.getApiRestClientParameters();
        } else if (PapConstants.POLICY_DISTRIBUTION.equals(clientName)) {
            parameters = papParameterGroup.getDistributionRestClientParameters();
        } else if (PapConstants.POLICY_PAP.equals(clientName)) {
            parameters = papParameterGroup.getRestServerParameters();
        }
        final BusTopicParams params = BusTopicParams.builder().clientName(clientName).useHttps(parameters.isHttps())
                .hostname(parameters.getHost()).port(parameters.getPort()).userName(parameters.getUserName())
                .password(parameters.getPassword()).build();
        return HttpClientFactoryInstance.getClientFactory().build(params);
    }
}
