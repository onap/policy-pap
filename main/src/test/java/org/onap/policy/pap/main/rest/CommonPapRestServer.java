/*
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

import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.startstop.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to perform unit test of {@link PapRestServer}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class CommonPapRestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonPapRestServer.class);

    public static final String NOT_ALIVE = "not alive";
    public static final String ALIVE = "alive";
    public static final String SELF = "self";
    public static final String NAME = "Policy PAP";
    public static final String HTTP_PREFIX = "http://localhost:6969/";
    public static final String HTTPS_PREFIX = "https://localhost:6969/";
    public static final String ENDPOINT_PREFIX = "policy/pap/v1/";

    private static String KEYSTORE = System.getProperty("user.dir") + "/src/test/resources/ssl/policy-keystore";
    private Main main;
    protected PapRestServer restServer;

    /**
     * Method for cleanup after each test.
     */
    public void teardown() {
        try {
            if (main != null) {
                stopPapService(main);
            }
        } catch (PolicyPapException exp) {
            LOGGER.error("cannot stop main", exp);
        }

        if (restServer != null) {
            restServer.stop();
        }
    }

    protected void testSwagger(final String endpoint) throws Exception {
        startPapService(false);
        final Invocation.Builder invocationBuilder = sendHttpsRequest2(HTTPS_PREFIX + "swagger.yaml");
        final String resp = invocationBuilder.get(String.class);

        assertTrue(resp.contains(ENDPOINT_PREFIX + endpoint + ":"));
    }

    protected void startPapService(final boolean http) {
        final String[] papConfigParameters = new String[2];
        if (http) {
            papConfigParameters[0] = "-c";
            papConfigParameters[1] = "parameters/PapConfigParameters.json";
        } else {
            final Properties systemProps = System.getProperties();
            systemProps.put("javax.net.ssl.keyStore", KEYSTORE);
            systemProps.put("javax.net.ssl.keyStorePassword", "Pol1cy_0nap");
            System.setProperties(systemProps);
            papConfigParameters[0] = "-c";
            papConfigParameters[1] = "parameters/PapConfigParameters_Https.json";
        }

        main = new Main(papConfigParameters);
    }

    protected void stopPapService(final Main main) throws PolicyPapException {
        main.shutdown();
    }

    protected Invocation.Builder sendHttpRequest(final String endpoint) throws Exception {
        final ClientConfig clientConfig = new ClientConfig();

        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        clientConfig.register(feature);

        final Client client = ClientBuilder.newClient(clientConfig);
        final WebTarget webTarget = client.target(HTTP_PREFIX + ENDPOINT_PREFIX + endpoint);

        final Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        if (!NetworkUtil.isTcpPortOpen("localhost", 6969, 6, 10000L)) {
            throw new IllegalStateException("cannot connect to port 6969");
        }
        return invocationBuilder;
    }

    protected Invocation.Builder sendHttpsRequest(final String endpoint) throws Exception {
        return sendHttpsRequest2(HTTPS_PREFIX + ENDPOINT_PREFIX + endpoint);
    }

    protected Invocation.Builder sendHttpsRequest2(final String endpoint) throws Exception {

        final TrustManager[] noopTrustManager = new TrustManager[] {
            new X509TrustManager() {

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(final java.security.cert.X509Certificate[] certs,
                                final String authType) {}

                @Override
                public void checkServerTrusted(final java.security.cert.X509Certificate[] certs,
                                final String authType) {}
            }
        };

        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, noopTrustManager, new SecureRandom());
        final ClientBuilder clientBuilder =
                        ClientBuilder.newBuilder().sslContext(sc).hostnameVerifier((host, session) -> true);
        final Client client = clientBuilder.build();
        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        client.register(feature);

        final WebTarget webTarget = client.target(endpoint);

        final Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        if (!NetworkUtil.isTcpPortOpen("localhost", 6969, 6, 10000L)) {
            throw new IllegalStateException("cannot connect to port 6969");
        }
        return invocationBuilder;
    }
}
