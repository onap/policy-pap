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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.parameters.PapParameterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class to perform unit test of {@link PapActivator}}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class TestPapActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestPapActivator.class);
    private PapActivator activator;

    /**
     * Initializes an activator.
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        final String[] papConfigParameters = { "-c", "parameters/PapConfigParameters.json" };
        final PapCommandLineArguments arguments = new PapCommandLineArguments(papConfigParameters);
        final PapParameterGroup parGroup = new PapParameterHandler().getParameters(arguments);
        activator = new PapActivator(parGroup);
    }

    /**
     * Method for cleanup after each test.
     */
    @After
    public void teardown() {
        try {
            if (activator != null) {
                activator.terminate();
            }
        } catch (final PolicyPapException exp) {
            LOGGER.error("teardown failed", exp);
        }
    }

    @Test
    public void testPapActivator() throws PolicyPapException {
        try {
            assertFalse(activator.isAlive());
            activator.initialize();
            assertTrue(activator.isAlive());
            assertTrue(activator.getParameterGroup().isValid());
            assertEquals(CommonTestData.PAP_GROUP_NAME, activator.getParameterGroup().getName());
        } catch (final Exception exp) {
            LOGGER.error("testPapActivator failed", exp);
            fail("Test should not throw an exception");
        }
    }

    @Test(expected = PolicyPapException.class)
    public void testPapActivatorError() throws PolicyPapException {
        activator.initialize();
        assertTrue(activator.getParameterGroup().isValid());
        activator.initialize();
    }

    @Test
    public void testGetCurrent_testSetCurrent() {
        assertNotNull(PapActivator.getCurrent());

        PapActivator.setCurrent(activator);

        assertSame(activator, PapActivator.getCurrent());
    }
}
