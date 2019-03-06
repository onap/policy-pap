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
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.RestServerParameters;
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
    public static final String HTTPS_PREFIX = "https://localhost:6969/";
    public static final String ENDPOINT_PREFIX = "policy/pap/v1/";

    private static String KEYSTORE = System.getProperty("user.dir") + "/src/test/resources/ssl/policy-keystore";

    private static Main main;

    private PapRestServer restServer;

    /**
     * Starts Main.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        startMain();
    }

    /**
     * Stops Main.
     */
    @AfterClass
    public static void teardownAfterClass() {
        try {
            stopMain();

        } catch (PolicyPapException exp) {
            LOGGER.error("cannot stop main", exp);
        }
    }

    /**
     * Set up.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        restServer = null;

        // restart, if not currently running
        if (main == null) {
            startMain();
        }
    }

    /**
     * Stops any running rest server.
     */
    @After
    public void tearDown() {
        if (restServer != null && restServer.isAlive()) {
            restServer.shutdown();
        }
    }

    /**
     * Verifies that an endpoint appears within the swagger response.
     *
     * @param endpoint the endpoint of interest
     * @throws Exception if an error occurs
     */
    protected void testSwagger(final String endpoint) throws Exception {
        final Invocation.Builder invocationBuilder = sendFqeRequest(HTTPS_PREFIX + "swagger.yaml");
        final String resp = invocationBuilder.get(String.class);

        assertTrue(resp.contains(ENDPOINT_PREFIX + endpoint + ":"));
    }

    /**
     * Starts the "Main".
     *
     * @throws Exception if an error occurs
     */
    private static void startMain() throws Exception {
        // make sure port is available
        if (NetworkUtil.isTcpPortOpen("localhost", 6969, 1, 1L)) {
            throw new IllegalStateException("port 6969 is still in use");
        }

        final Properties systemProps = System.getProperties();
        systemProps.put("javax.net.ssl.keyStore", KEYSTORE);
        systemProps.put("javax.net.ssl.keyStorePassword", "Pol1cy_0nap");
        System.setProperties(systemProps);

        final String[] papConfigParameters = new String[2];
        papConfigParameters[0] = "-c";
        papConfigParameters[1] = "parameters/PapConfigParameters.json";

        main = new Main(papConfigParameters);

        if (!NetworkUtil.isTcpPortOpen("localhost", 6969, 6, 10000L)) {
            throw new IllegalStateException("server is not listening on port 6969");
        }
    }

    /**
     * Stops the "Main".
     * @throws Exception if an error occurs
     */
    protected static void stopMain() throws PolicyPapException {
        if (main != null) {
            Main main2 = main;
            main = null;

            main2.shutdown();
        }
    }

    /**
     * Starts a REST server, without main/activator.
     */
    protected void startRestOnly() {
        final RestServerParameters restServerParams = new CommonTestData().getRestServerParameters(false);
        restServerParams.setName(CommonTestData.PAP_GROUP_NAME);
        restServer = new PapRestServer(restServerParams);
        restServer.start();
    }

    /**
     * Sends a request to an endpoint.
     *
     * @param endpoint the target endpoint
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendRequest(final String endpoint) throws Exception {
        return sendFqeRequest(HTTPS_PREFIX + ENDPOINT_PREFIX + endpoint);
    }

    /**
     * Sends a request to a fully qualified endpoint.
     *
     * @param endpoint the fully qualified target endpoint
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendFqeRequest(final String fullyQualifiedEndpoint) throws Exception {

        // @formatter:off
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
        // @formatter:on

        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, noopTrustManager, new SecureRandom());
        final ClientBuilder clientBuilder =
                        ClientBuilder.newBuilder().sslContext(sc).hostnameVerifier((host, session) -> true);
        final Client client = clientBuilder.build();
        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        client.register(feature);

        final WebTarget webTarget = client.target(fullyQualifiedEndpoint);

        return webTarget.request(MediaType.APPLICATION_JSON);
    }
}
