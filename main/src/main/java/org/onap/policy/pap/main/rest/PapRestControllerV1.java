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

import io.swagger.annotations.Api;
import io.swagger.annotations.BasicAuthDefinition;
import io.swagger.annotations.Info;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.pap.main.comm.PdpClient;
import org.onap.policy.pap.main.comm.PdpClientException;
import org.onap.policy.pdp.common.enums.PdpMessageType;
import org.onap.policy.pdp.common.models.PdpStatus;

/**
 * Version v1 common superclass to provide REST endpoints for PAP component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
// @formatter:off
@Path("/policy/pap/v1")
@Api(value = "Policy Administration (PAP) API")
@Produces(MediaType.APPLICATION_JSON)
@SwaggerDefinition(
    info = @Info(description =
                    "Policy Administration is responsible for the deployment life cycle of policies as well as "
                    + "interworking with the mechanisms required to orchestrate the nodes and containers on which "
                    + "policies run. It is also responsible for the administration of policies at run time;"
                    + " ensuring that policies are available to users, that policies are executing correctly,"
                    + " and that the state and status of policies is monitored", version = "v1.0",
                    title = "Policy Administration"),
    consumes = {MediaType.APPLICATION_JSON}, produces = {MediaType.APPLICATION_JSON},
    schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
    tags = {@Tag(name = "policy-administration", description = "Policy Administration Service Operations")},
    securityDefinition = @SecurityDefinition(
                    basicAuthDefinitions = {@BasicAuthDefinition(key = "basicAuth")}))
// @formatter:on
public class PapRestControllerV1 {
    public static final String PDP_PAP_TOPIC = "POLICY-PDP-PAP";

    public static final String AUTHORIZATION_TYPE = "basicAuth";

    public static final int AUTHENTICATION_ERROR_CODE = HttpURLConnection.HTTP_UNAUTHORIZED;
    public static final int AUTHORIZATION_ERROR_CODE = HttpURLConnection.HTTP_FORBIDDEN;
    public static final int SERVER_ERROR_CODE = HttpURLConnection.HTTP_INTERNAL_ERROR;

    public static final String AUTHENTICATION_ERROR_MESSAGE = "Authentication Error";
    public static final String AUTHORIZATION_ERROR_MESSAGE = "Authorization Error";
    public static final String SERVER_ERROR_MESSAGE = "Internal Server Error";

    private static final Object lockit = new Object();
    private static RequestIdDispatcher<PdpStatus> dispatcher;
    private static PdpClient client;

    /**
     * Gets the PDP client singleton.
     *
     * @return the PDP client singleton
     * @throws PdpClientException if the client cannot be created
     */
    public static PdpClient getPdpClientInstance() throws PdpClientException {
        synchronized (lockit) {
            if (client == null) {
                client = new PdpClient(PDP_PAP_TOPIC);
            }

            return client;
        }
    }

    /**
     * Gets the dispatcher singleton.
     *
     * @return the dispatcher singleton
     * @throws PdpClientException if the dispatcher cannot be created
     */
    public static RequestIdDispatcher<PdpStatus> getRequestIdDispatcherInstance() throws PdpClientException {
        synchronized (lockit) {
            if (dispatcher == null) {
                dispatcher = new RequestIdDispatcher<PdpStatus>(PdpStatus.class, "response", "responseTo");

                MessageTypeDispatcher mtd = new MessageTypeDispatcher("messageName");
                mtd.register(PdpMessageType.PDP_STATUS.name(), dispatcher);

                List<TopicSource> sources = TopicEndpoint.manager.getTopicSources(Arrays.asList(PDP_PAP_TOPIC));
                if (sources.isEmpty()) {
                    throw new PdpClientException("no sources for topic: " + PDP_PAP_TOPIC);
                }

                sources.forEach(src -> src.register(mtd));
            }

            return dispatcher;
        }
    }
}
