/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2020 Nordix Foundation.
 * Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 * Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.utils.resources.MessageConstants;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.parameters.CommonTestData;

/**
 * Class to perform unit test of {@link Main}}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class TestMain {
    private Main main;

    /**
     * Set up.
     */
    @Before
    public void setUp() {
        Registry.newRegistry();
        HttpServletServerFactoryInstance.getServerFactory().destroy();
    }

    /**
     * Shuts "main" down.
     *
     */
    @After
    public void tearDown() {
        // shut down activator
        PapActivator activator = Registry.getOrDefault(PapConstants.REG_PAP_ACTIVATOR, PapActivator.class, null);
        if (activator != null && activator.isAlive()) {
            activator.stop();
        }
    }

    private void testMainBody(String[] papConfigParameters) {
        main = new Main(papConfigParameters);
        assertTrue(main.getParameters().isValid());
        assertEquals(CommonTestData.PAP_GROUP_NAME, main.getParameters().getName());

        // ensure items were added to the registry
        assertNotNull(Registry.get(PapConstants.REG_PAP_ACTIVATOR, PapActivator.class));
        main.shutdown();
    }

    @Test
    public void testMain() {
        final String[] papConfigParameters = {"-c", "parameters/PapConfigParameters.json"};
        testMainBody(papConfigParameters);
    }

    @Test
    public void testMainCustomGroup() {
        final String[] papConfigParameters = {
            "-c",
            "parameters/PapConfigParameters.json",
            "-g",
            "parameters/PapDbGroup1.json"
        };
        testMainBody(papConfigParameters);
    }

    @Test
    public void testMainPapDb() {
        final String[] papConfigParameters = {
            "-c",
            "parameters/PapConfigParameters.json",
            "-g",
            "PapDb.json"
        };
        testMainBody(papConfigParameters);
    }

    @Test
    public void testMain_NoArguments() {
        final String[] papConfigParameters = {};
        assertThatThrownBy(() -> new Main(papConfigParameters)).isInstanceOf(PolicyPapRuntimeException.class)
            .hasMessage(String.format(MessageConstants.START_FAILURE_MSG, MessageConstants.POLICY_PAP));
    }

    @Test
    public void testMain_InvalidArguments() {
        final String[] papConfigParameters = {"parameters/PapConfigParameters.json"};
        assertThatThrownBy(() -> new Main(papConfigParameters)).isInstanceOf(PolicyPapRuntimeException.class)
            .hasMessage(String.format(MessageConstants.START_FAILURE_MSG, MessageConstants.POLICY_PAP));
    }

    @Test
    public void testMain_Help() {
        final String[] papConfigParameters = {"-h"};
        main = new Main(papConfigParameters);
        assertNull(main.getParameters());
    }

    @Test
    public void testMain_InvalidParameters() {
        final String[] papConfigParameters = {"-c", "parameters/PapConfigParameters_InvalidName.json"};
        assertThatThrownBy(() -> new Main(papConfigParameters)).isInstanceOf(PolicyPapRuntimeException.class)
            .hasMessage(String.format(MessageConstants.START_FAILURE_MSG, MessageConstants.POLICY_PAP));
    }
}
