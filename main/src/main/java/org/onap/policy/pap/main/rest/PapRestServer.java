/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.common.endpoints.http.server.internal.JettyServletServer;
import org.onap.policy.common.endpoints.properties.PolicyEndPointProperties;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.onap.policy.pap.main.parameters.RestServerParameters;
import org.onap.policy.pap.main.rest.depundep.PdpGroupDeleteControllerV1;
import org.onap.policy.pap.main.rest.depundep.PdpGroupDeployControllerV1;

/**
 * Class to manage life cycle of PAP rest server.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapRestServer extends ServiceManagerContainer {

    private List<HttpServletServer> servers = new ArrayList<>();

    private RestServerParameters restServerParameters;

    /**
     * Constructor for instantiating PapRestServer.
     *
     * @param restServerParameters the rest server parameters
     */
    public PapRestServer(final RestServerParameters restServerParameters) {
        this.restServerParameters = restServerParameters;
        this.servers = HttpServletServer.factory.build(getServerProperties());

        // configure servers, but don't actually start them
        for (final HttpServletServer server : servers) {
            if (server.isAaf()) {
                server.addFilterClass(null, PapAafFilter.class.getName());
            }

            // arrange to start/stop server when this.start() is invoked
            String name = "rest server " + ((JettyServletServer) server).getName();
            addAction(name, server::start, server::stop);
        }
    }

    /**
     * Creates the server properties object using restServerParameters.
     *
     * @return the properties object
     */
    private Properties getServerProperties() {
        final Properties props = new Properties();
        props.setProperty(PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES, restServerParameters.getName());

        final String svcpfx =
                        PolicyEndPointProperties.PROPERTY_HTTP_SERVER_SERVICES + "." + restServerParameters.getName();

        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HOST_SUFFIX, restServerParameters.getHost());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_PORT_SUFFIX,
                        Integer.toString(restServerParameters.getPort()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_REST_CLASSES_SUFFIX,
                        String.join(",", HealthCheckRestControllerV1.class.getName(),
                                        StatisticsRestControllerV1.class.getName(),
                                        PdpGroupDeployControllerV1.class.getName(),
                                        PdpGroupDeleteControllerV1.class.getName(),
                                        PdpGroupStateChangeControllerV1.class.getName(),
                                        PdpGroupQueryControllerV1.class.getName()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_MANAGED_SUFFIX, "false");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_SWAGGER_SUFFIX, "true");
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_AUTH_USERNAME_SUFFIX,
                        restServerParameters.getUserName());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_AUTH_PASSWORD_SUFFIX,
                        restServerParameters.getPassword());
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_HTTPS_SUFFIX,
                        String.valueOf(restServerParameters.isHttps()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_AAF_SUFFIX,
                        String.valueOf(restServerParameters.isAaf()));
        props.setProperty(svcpfx + PolicyEndPointProperties.PROPERTY_HTTP_SERIALIZATION_PROVIDER,
                        GsonMessageBodyHandler.class.getName());
        return props;
    }

    @Override
    public String toString() {
        return "PapRestServer [servers=" + servers + "]";
    }
}
