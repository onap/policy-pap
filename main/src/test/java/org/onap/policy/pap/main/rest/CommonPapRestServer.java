/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.common.utils.security.SelfSignedKeyStore;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PolicyPapApplication;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.startstop.PapActivator;
import org.powermock.reflect.Whitebox;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Class to perform unit test of {@link PapRestControllerV1}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PolicyPapApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"pap.activator.initialize=false"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class CommonPapRestServer {

    protected static final String CONFIG_FILE = "src/test/resources/parameters/TestConfigParams.json";

    public static final String NOT_ALIVE = "not alive";
    public static final String ALIVE = "alive";
    public static final String SELF = NetworkUtil.getHostname();
    public static final String NAME = "Policy PAP";
    public static final String ENDPOINT_PREFIX = "policy/pap/v1/";

    private static SelfSignedKeyStore keystore;

    private boolean activatorWasAlive;

    @LocalServerPort
    private int port;

    private static PapActivator papActivator;

    /**
     * Allocates a port for the server, writes a config file, and then starts Main.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass(true);
    }

    /**
     * Allocates a port for the server, writes a config file, and then starts Main, if
     * specified.
     *
     * @param shouldStart {@code true} if Main should be started, {@code false} otherwise
     * @throws Exception if an error occurs
     */
    public static void setUpBeforeClass(boolean shouldStart) throws Exception {
        keystore = new SelfSignedKeyStore();
        makeConfigFile();

        HttpServletServerFactoryInstance.getServerFactory().destroy();
        TopicEndpointManager.getManager().shutdown();

        CommonTestData.newDb();

        if (shouldStart) {
            startMain();
        }
    }

    /**
     * Stops Main.
     */
    @AfterClass
    public static void teardownAfterClass() {
        papActivator.stopService();
    }

    /**
     * Set up.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        // restart, if not currently running
        if (papActivator != null) {
            papActivator.startService();
        }

        activatorWasAlive = papActivator.isAlive();
    }

    /**
     * Restores the activator's "alive" state.
     */
    @After
    public void tearDown() {
        markActivator(activatorWasAlive);
    }

    /**
     * Verifies that an endpoint appears within the swagger response.
     *
     * @param endpoint the endpoint of interest
     * @throws Exception if an error occurs
     */
    protected void testSwagger(final String endpoint) throws Exception {
        final Invocation.Builder invocationBuilder =
            sendFqeRequest("http://localhost:" + port + "/" + "swagger.yaml", true);
        // TODO: Fix swagger endpoints in next phase of spring boot migration
        // final String resp = invocationBuilder.get(String.class);
        // assertTrue(resp.contains(ENDPOINT_PREFIX + endpoint + ":"));
        assertThatThrownBy(() -> invocationBuilder.get(String.class)).isInstanceOfAny(NotFoundException.class);
    }

    /**
     * Makes a parameter configuration file.
     *
     * @throws Exception if an error occurs
     */
    private static void makeConfigFile() throws Exception {
        String json = new CommonTestData().getPapParameterGroupAsString(6969);

        File file = new File(CONFIG_FILE);
        file.deleteOnExit();

        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Starts the "Main".
     *
     * @throws Exception if an error occurs
     */
    protected static void startMain() throws Exception {
        Registry.newRegistry();

        final Properties systemProps = System.getProperties();
        systemProps.put("javax.net.ssl.keyStore", keystore.getKeystoreName());
        systemProps.put("javax.net.ssl.keyStorePassword", SelfSignedKeyStore.KEYSTORE_PASSWORD);
        System.setProperties(systemProps);

        final PapParameterGroup params = new StandardCoder().decode(new File(CONFIG_FILE), PapParameterGroup.class);
        papActivator = new PapActivator(params);
        papActivator.startService();
    }

    /**
     * Mark the activator as dead, but leave its REST server running.
     */
    protected void markActivatorDead() {
        markActivator(false);
    }

    private void markActivator(boolean wasAlive) {
        Object manager = Whitebox.getInternalState(papActivator, "serviceManager");
        AtomicBoolean running = Whitebox.getInternalState(manager, "running");
        running.set(wasAlive);
    }

    /**
     * Verifies that unauthorized requests fail.
     *
     * @param endpoint the target end point
     * @param sender function that sends the requests to the target
     * @throws Exception if an error occurs
     */
    protected void checkUnauthRequest(final String endpoint, Function<Invocation.Builder, Response> sender)
                    throws Exception {
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(),
                        sender.apply(sendNoAuthRequest(endpoint)).getStatus());
    }

    /**
     * Sends a request to an endpoint.
     *
     * @param endpoint the target endpoint
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendRequest(final String endpoint) throws Exception {
        return sendFqeRequest("http://localhost:" + port + "/" + ENDPOINT_PREFIX + endpoint, true);
    }

    /**
     * Sends a request to an endpoint, without any authorization header.
     *
     * @param endpoint the target endpoint
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendNoAuthRequest(final String endpoint) throws Exception {
        return sendFqeRequest("http://localhost:" + port + "/" + ENDPOINT_PREFIX + endpoint, false);
    }

    /**
     * Sends a request to a fully qualified endpoint.
     *
     * @param fullyQualifiedEndpoint the fully qualified target endpoint
     * @param includeAuth if authorization header should be included
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendFqeRequest(final String fullyQualifiedEndpoint, boolean includeAuth)
                    throws Exception {
        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, NetworkUtil.getAlwaysTrustingManager(), new SecureRandom());
        final ClientBuilder clientBuilder =
                        ClientBuilder.newBuilder().sslContext(sc).hostnameVerifier((host, session) -> true);
        final Client client = clientBuilder.build();

        client.property(ClientProperties.METAINF_SERVICES_LOOKUP_DISABLE, "true");
        client.register(GsonMessageBodyHandler.class);

        if (includeAuth) {
            final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("policyadmin", "zb!XztG34");
            client.register(feature);
        }

        final WebTarget webTarget = client.target(fullyQualifiedEndpoint);

        return webTarget.request(MediaType.APPLICATION_JSON);
    }
}
