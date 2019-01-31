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

package org.onap.policy.pap.main.startstop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.parameters.PapParameterHandler;


/**
 * Class to perform unit test of {@link PapActivator}}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class TestPapActivator {

    @Test
    public void testPapActivator() throws PolicyPapException {
        final String[] papConfigParameters = { "-c", "parameters/PapConfigParameters.json" };

        final PapCommandLineArguments arguments = new PapCommandLineArguments(papConfigParameters);

        final PapParameterGroup parGroup = new PapParameterHandler().getParameters(arguments);

        final PapActivator activator = new PapActivator(parGroup);
        activator.initialize();
        assertTrue(activator.getParameterGroup().isValid());
        assertEquals(CommonTestData.PAP_GROUP_NAME, activator.getParameterGroup().getName());
        activator.terminate();
    }
}
