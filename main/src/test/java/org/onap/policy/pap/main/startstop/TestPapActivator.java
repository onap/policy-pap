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

package org.onap.policy.pap.main.startstop;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.parameters.PapParameterHandler;
import org.onap.policy.pap.main.rest.PapStatisticsManager;


/**
 * Class to perform unit test of {@link PapActivator}}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class TestPapActivator {

    private PapActivator activator;

    /**
     * Initializes an activator.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        Registry.newRegistry();
        HttpServletServerFactoryInstance.getServerFactory().destroy();

        final String[] papConfigParameters = {"-c", "parameters/PapConfigParameters.json"};
        final PapCommandLineArguments arguments = new PapCommandLineArguments(papConfigParameters);
        final PapParameterGroup parGroup = new PapParameterHandler().getParameters(arguments);

        activator = new PapActivator(parGroup);
    }

    /**
     * Method for cleanup after each test.
     *
     * @throws Exception if an error occurs
     */
    @After
    public void teardown() throws Exception {
        if (activator != null && activator.isAlive()) {
            activator.stop();
        }
    }

    @Test
    public void testPapActivator() throws PolicyPapException {
        assertFalse(activator.isAlive());
        activator.start();
        assertTrue(activator.isAlive());
        assertTrue(activator.getParameterGroup().isValid());
        assertEquals(CommonTestData.PAP_GROUP_NAME, activator.getParameterGroup().getName());

        // ensure items were added to the registry
        assertNotNull(Registry.get(PapConstants.REG_PDP_MODIFY_LOCK, Object.class));
        assertNotNull(Registry.get(PapConstants.REG_STATISTICS_MANAGER, PapStatisticsManager.class));
        assertNotNull(Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class));

        // repeat - should throw an exception
        assertThatIllegalStateException().isThrownBy(() -> activator.start());
        assertTrue(activator.isAlive());
        assertTrue(activator.getParameterGroup().isValid());
    }

    @Test
    public void testTerminate() throws Exception {
        activator.start();
        activator.stop();
        assertFalse(activator.isAlive());

        // ensure items have been removed from the registry
        assertNull(Registry.getOrDefault(PapConstants.REG_PDP_MODIFY_LOCK, Object.class, null));
        assertNull(Registry.getOrDefault(PapConstants.REG_STATISTICS_MANAGER, PapStatisticsManager.class, null));
        assertNull(Registry.getOrDefault(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class, null));

        // repeat - should throw an exception
        assertThatIllegalStateException().isThrownBy(() -> activator.stop());
        assertFalse(activator.isAlive());
    }
}
