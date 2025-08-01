/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2022-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2023 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.startstop;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.message.bus.event.TopicEndpointManager;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.comm.PdpHeartbeatListener;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.notification.PolicyNotifier;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.PapParameterGroup;


/**
 * Class to perform unit test of {@link PapActivator}}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
class TestPapActivator {
    private static final String CONFIG_FILE = "src/test/resources/parameters/TestConfigParams.json";

    private PapActivator activator;

    /**
     * Allocates a new DB name, server port, and creates a config file.
     */
    @BeforeAll
    static void setUpBeforeClass() {
        Registry.newRegistry();
        CommonTestData.newDb();
    }

    /**
     * Initializes an activator.
     *
     * @throws Exception if an error occurs
     */
    @BeforeEach
    void setUp() throws Exception {
        Registry.newRegistry();
        TopicEndpointManager.getManager().shutdown();
        HttpServletServerFactoryInstance.getServerFactory().destroy();

        int port = NetworkUtil.allocPort();

        String json = new CommonTestData().getPapParameterGroupAsString(port);

        File file = new File(CONFIG_FILE);
        file.deleteOnExit();

        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(json.getBytes(StandardCharsets.UTF_8));
        }

        final PapParameterGroup parGroup = new CommonTestData().getPapParameterGroup(6969);

        activator = new PapActivator(parGroup, new PolicyNotifier(null), new PdpHeartbeatListener(),
            new PdpModifyRequestMap(null, null, null, null, null), new SimpleMeterRegistry());

    }

    /**
     * Method for cleanup after each test.
     *
     * @throws Exception if an error occurs
     */
    @AfterEach
    void teardown() throws Exception {
        if (activator != null && activator.isAlive()) {
            activator.stop();
        }
    }

    @AfterAll
    static void afterClass() {
        Registry.newRegistry();
    }

    @Test
    void testPapActivator() {
        assertFalse(activator.isAlive());
        activator.start();
        assertTrue(activator.isAlive());
        assertTrue(activator.getParameterGroup().isValid());
        assertEquals(CommonTestData.PAP_GROUP_NAME, activator.getParameterGroup().getName());

        // ensure items were added to the registry
        assertNotNull(Registry.get(PapConstants.REG_PDP_MODIFY_LOCK, Object.class));
        assertNotNull(Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class));

        // repeat - should throw an exception
        assertThatIllegalStateException().isThrownBy(() -> activator.start());
        assertTrue(activator.isAlive());
        assertTrue(activator.getParameterGroup().isValid());
    }

    @Test
    void testTerminate() {
        activator.start();
        activator.stop();
        assertFalse(activator.isAlive());

        // ensure items have been removed from the registry
        assertNull(Registry.getOrDefault(PapConstants.REG_PDP_MODIFY_LOCK, Object.class, null));
        assertNull(Registry.getOrDefault(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class, null));

        // repeat - should throw an exception
        assertThatIllegalStateException().isThrownBy(() -> activator.stop());
        assertFalse(activator.isAlive());
    }
}
