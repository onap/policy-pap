/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.endpoints.http.server.YamlMessageBodyHandler;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.common.utils.security.SelfSignedKeyStore;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapApplication;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.startstop.PapActivator;
import org.powermock.reflect.Whitebox;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Class to perform unit test of {@link PapRestControllerV1}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PolicyPapApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"db.initialize=false"})
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
    protected String httpsPrefix;

    @LocalServerPort
    private int port;

    private PapActivator papActivator;

    /**
     * Allocates a new db url, writes a config file.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        keystore = new SelfSignedKeyStore();
        CommonTestData.newDb();
        makeConfigFile();

        HttpServletServerFactoryInstance.getServerFactory().destroy();
        TopicEndpointManager.getManager().shutdown();
        Registry.newRegistry();
    }

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb" + CommonTestData.dbNum);
        registry.add("server.ssl.enabled", () -> "true");
        registry.add("server.ssl.key-store", () -> keystore.getKeystoreName());
        registry.add("server.ssl.key-store-password", () -> SelfSignedKeyStore.KEYSTORE_PASSWORD);
    }

    /**
     * Set up.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        httpsPrefix = "https://localhost:" + port + "/";
        papActivator = Registry.get(PapConstants.REG_PAP_ACTIVATOR, PapActivator.class);
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
                        sendFqeRequest(httpsPrefix + "v3/api-docs", true, MediaType.APPLICATION_JSON);
        final String resp = invocationBuilder.get(String.class);
        assertTrue(resp.contains(ENDPOINT_PREFIX + endpoint));
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
        return sendFqeRequest(httpsPrefix + ENDPOINT_PREFIX + endpoint, true, MediaType.APPLICATION_JSON);
    }

    /**
     * Sends a request to an endpoint.
     *
     * @param endpoint the target endpoint
     * @param mediaType the media type for the request
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendRequest(final String endpoint, String mediaType) throws Exception {
        return sendFqeRequest(httpsPrefix + ENDPOINT_PREFIX + endpoint, true, mediaType);
    }

    /**
     * Sends a request to an endpoint, without any authorization header.
     *
     * @param endpoint the target endpoint
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendNoAuthRequest(final String endpoint) throws Exception {
        return sendFqeRequest(httpsPrefix + ENDPOINT_PREFIX + endpoint, false, MediaType.APPLICATION_JSON);
    }

    /**
     * Sends a request to a fully qualified endpoint.
     *
     * @param fullyQualifiedEndpoint the fully qualified target endpoint
     * @param includeAuth if authorization header should be included
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendFqeRequest(final String fullyQualifiedEndpoint, boolean includeAuth,
                    String mediaType) throws Exception {
        final SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, NetworkUtil.getAlwaysTrustingManager(), new SecureRandom());
        final ClientBuilder clientBuilder =
                        ClientBuilder.newBuilder().sslContext(sc).hostnameVerifier((host, session) -> true);
        final Client client = clientBuilder.build();

        client.property(ClientProperties.METAINF_SERVICES_LOOKUP_DISABLE, "true");
        client.register((mediaType.equalsIgnoreCase(MediaType.APPLICATION_JSON) ? GsonMessageBodyHandler.class
                        : YamlMessageBodyHandler.class));

        if (includeAuth) {
            final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("policyadmin", "zb!XztG34");
            client.register(feature);
        }

        final WebTarget webTarget = client.target(fullyQualifiedEndpoint);

        return webTarget.request(mediaType);
    }
}
