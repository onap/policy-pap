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

package org.onap.policy.pap.client.monitoring.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onap.policy.common.endpoints.event.comm.bus.internal.BusTopicParams;
import org.onap.policy.common.endpoints.http.client.HttpClient;
import org.onap.policy.common.endpoints.http.client.HttpClientConfigException;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.models.pdp.concepts.PdpEngineWorkerStatistics;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

/**
 * The class represents the root resource exposed at the base URL<br>
 * The url to access this resource would be in the form {@code <baseURL>/rest/....} <br>
 * For example: a GET request to the following URL
 * {@code http://localhost:18989/papservices/rest/?hostName=localhost&port=12345}
 *
 * <b>Note:</b> An allocated {@code hostName} and {@code port} query parameter must be included in
 * all requests. Datasets for different {@code hostName} are completely isolated from one another.
 *
 */
@Path("monitoring/")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class PapMonitoringRestResource {
    // Get a reference to the logger
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(PapMonitoringRestResource.class);
    // Set up a map separated by host and engine for the data
    private static final HashMap<String, HashMap<String, List<Counter>>> cache = new HashMap<>();

    // Set the maximum number of stored data entries to be stored for each engine
    private static final int MAX_CACHED_ENTITIES = 50;

    /**
     * Query the engine service for data.
     *
     * @param hostName the host name of the engine service to connect to.
     * @param port the port number of the engine service to connect to.
     * @return a Response object containing the engines service, status and context data in JSON
     * @throws HttpClientConfigException exception
     */
    @GET
    public Response createSession(@QueryParam("hostName") final String hostName, @QueryParam("port") final int port)
            throws HttpClientConfigException {
        BusTopicParams busParams = new BusTopicParams();// TODO, make it configurable
        busParams.setClientName("pap-monitoring");
        busParams.setHostname(hostName);
        busParams.setManaged(false);
        busParams.setPassword("zb!XztG34");
        busParams.setPort(port);
        busParams.setUseHttps(false);
        busParams.setUserName("healthcheck");

        HttpClient httpClient = HttpClientFactoryInstance.getClientFactory().build(busParams);

        return Response.ok(httpClient.get("policy/pap/v1/pdps").getEntity(), MediaType.APPLICATION_JSON).build();
    }

    /**
     * Query the engine service for data.
     *
     * @param hostName the host name of the engine service to connect to.
     * @param port the port number of the engine service to connect to.
     * @return a Response object containing the engines service, status and context data in JSON
     * @throws HttpClientConfigException exception
     */
    @GET
    @Path("statistics/")
    public Response getStatistics(@QueryParam("hostName") final String hostName, @QueryParam("port") final int port,
            @QueryParam("id") final String id) throws HttpClientConfigException {

        BusTopicParams busParams = new BusTopicParams();// TODO, make it configurable
        busParams.setClientName("pap-monitoring");
        busParams.setHostname(hostName);
        busParams.setManaged(false);
        busParams.setPassword("zb!XztG34");
        busParams.setPort(port);
        busParams.setUseHttps(false);
        busParams.setUserName("healthcheck");
        HttpClient httpClient = HttpClientFactoryInstance.getClientFactory().build(busParams);

        String entity = httpClient.get("policy/pap/v1/pdps/statistics/" + id + "/latest").readEntity(String.class);
        final Gson gson = new Gson();
        List<PdpStatistics> lists = gson.fromJson(entity, new TypeToken<List<PdpStatistics>>() {}.getType());
        PdpStatistics pdpStatistics = lists.get(0);
        final JsonObject responseObject = new JsonObject();

        // Engine Service data
        responseObject.addProperty("engine_id", pdpStatistics.getPdpInstanceId());
        responseObject.addProperty("server", hostName);
        responseObject.addProperty("port", Integer.toString(port));
        responseObject.addProperty("timeStamp", pdpStatistics.getTimeStamp().toString());
        responseObject.addProperty("policyDeployCount", pdpStatistics.getPolicyDeployCount());
        responseObject.addProperty("policyDeploySuccessCount", pdpStatistics.getPolicyDeploySuccessCount());
        responseObject.addProperty("policyDeployFailCount", pdpStatistics.getPolicyDeployFailCount());
        responseObject.addProperty("policyExecutedCount", pdpStatistics.getPolicyExecutedCount());
        responseObject.addProperty("policyExecutedSuccessCount", pdpStatistics.getPolicyExecutedSuccessCount());
        responseObject.addProperty("policyExecutedFailCount", pdpStatistics.getPolicyExecutedFailCount());

        // Engine Status data
        final JsonArray engineStatusList = new JsonArray();

        for (final PdpEngineWorkerStatistics engineStats : pdpStatistics.getEngineStats()) {
            try {
                final JsonObject engineStatusObject = new JsonObject();
                engineStatusObject.addProperty("timestamp", pdpStatistics.getTimeStamp().toString());
                engineStatusObject.addProperty("id", engineStats.getEngineId());
                engineStatusObject.addProperty("status", "Ready");
                engineStatusObject.addProperty("last_message", new Date(engineStats.getEngineTimeStamp()).toString());
                engineStatusObject.addProperty("up_time", engineStats.getUpTime());
                engineStatusObject.addProperty("policy_executions", engineStats.getEventCount());
                engineStatusObject.addProperty("last_policy_duration",
                        gson.toJson(
                                getValuesFromCache(id, engineStats.getEngineId() + "_last_policy_duration",
                                        pdpStatistics.getTimeStamp().getTime(), engineStats.getLastExecutionTime()),
                                List.class));
                engineStatusObject.addProperty("average_policy_duration",
                        gson.toJson(getValuesFromCache(id, engineStats.getEngineId() + "_average_policy_duration",
                                pdpStatistics.getTimeStamp().getTime(), (long) engineStats.getAverageExecutionTime()),
                                List.class));
                engineStatusList.add(engineStatusObject);
            } catch (final RuntimeException e) {
                LOGGER.warn("Error getting status of engine with ID " + engineStats.getEngineId() + "<br>", e);
            }
        }
        responseObject.add("status", engineStatusList);
        return Response.ok(responseObject.toString(), MediaType.APPLICATION_JSON).build();
    }

    /**
     * This method takes in the latest data entry for an engine, adds it to an existing data set and
     * returns the full map for that host and engine.
     *
     * @param host the pdp uri
     * @param id the engines id
     * @param timestamp the timestamp of the latest data entry
     * @param latestValue the value of the latest data entry
     * @return a list of {@code Counter} objects for that engine
     */
    private List<Counter> getValuesFromCache(final String uri, final String id, final long timestamp,
            final long latestValue) {
        SlidingWindowList<Counter> valueList;

        if (!cache.containsKey(uri)) {
            cache.put(uri, new HashMap<String, List<Counter>>());
        }

        if (cache.get(uri).containsKey(id)) {
            valueList = (SlidingWindowList<Counter>) cache.get(uri).get(id);
        } else {
            valueList = new SlidingWindowList<>(MAX_CACHED_ENTITIES);
        }
        valueList.add(new Counter(timestamp, latestValue));

        cache.get(uri).put(id, valueList);

        return valueList;
    }

    /**
     * A list of values that uses a FIFO sliding window of a fixed size.
     */
    public class SlidingWindowList<V> extends LinkedList<V> {
        private static final long serialVersionUID = -7187277916025957447L;

        private final int maxEntries;

        public SlidingWindowList(final int maxEntries) {
            this.maxEntries = maxEntries;
        }

        @Override
        public boolean add(final V elm) {
            if (this.size() > (maxEntries - 1)) {
                this.removeFirst();
            }
            return super.add(elm);
        }

        private PapMonitoringRestResource getOuterType() {
            return PapMonitoringRestResource.this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + getOuterType().hashCode();
            result = prime * result + maxEntries;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!super.equals(obj)) {
                return false;
            }

            if (getClass() != obj.getClass()) {
                return false;
            }

            @SuppressWarnings("unchecked")
            SlidingWindowList<V> other = (SlidingWindowList<V>) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }

            return maxEntries == other.maxEntries;
        }
    }

    /**
     * A class used to storing a single data entry for an engine.
     */
    public class Counter {
        private long timestamp;
        private long value;

        public Counter(final long timestamp, final long value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getValue() {
            return value;
        }
    }

}
