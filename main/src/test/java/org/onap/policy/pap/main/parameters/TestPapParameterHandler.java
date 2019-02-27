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

package org.onap.policy.pap.main.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import org.junit.Test;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.startstop.PapCommandLineArguments;

/**
 * Class to perform unit test of {@link PapParameterHandler}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class TestPapParameterHandler {

    @Test
    public void testParameterHandlerNoParameterFile() throws PolicyPapException {
        final String[] noArgumentString = { "-c", "parameters/NoParameterFile.json" };

        final PapCommandLineArguments noArguments = new PapCommandLineArguments();
        noArguments.parse(noArgumentString);

        try {
            new PapParameterHandler().getParameters(noArguments);
            fail("test should throw an exception here");
        } catch (final Exception e) {
            assertTrue(e.getCause() instanceof CoderException);
            assertTrue(e.getCause().getCause() instanceof FileNotFoundException);
        }
    }

    @Test
    public void testParameterHandlerEmptyParameters() throws PolicyPapException {
        final String[] emptyArgumentString = { "-c", "parameters/EmptyParameters.json" };

        final PapCommandLineArguments emptyArguments = new PapCommandLineArguments();
        emptyArguments.parse(emptyArgumentString);

        try {
            new PapParameterHandler().getParameters(emptyArguments);
            fail("test should throw an exception here");
        } catch (final Exception e) {
            assertEquals("no parameters found in \"parameters/EmptyParameters.json\"", e.getMessage());
        }
    }

    @Test
    public void testParameterHandlerInvalidParameters() throws PolicyPapException {
        final String[] invalidArgumentString = { "-c", "parameters/InvalidParameters.json" };

        final PapCommandLineArguments invalidArguments = new PapCommandLineArguments();
        invalidArguments.parse(invalidArgumentString);

        try {
            new PapParameterHandler().getParameters(invalidArguments);
            fail("test should throw an exception here");
        } catch (final Exception e) {
            assertTrue(e.getMessage().startsWith(
                            "error reading parameters from \"parameters/InvalidParameters.json\""));
            assertTrue(e.getCause() instanceof CoderException);
        }
    }

    @Test
    public void testParameterHandlerNoParameters() throws PolicyPapException {
        final String[] noArgumentString = { "-c", "parameters/NoParameters.json" };

        final PapCommandLineArguments noArguments = new PapCommandLineArguments();
        noArguments.parse(noArgumentString);

        try {
            new PapParameterHandler().getParameters(noArguments);
            fail("test should throw an exception here");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains(
                    "field \"name\" type \"java.lang.String\" value \"null\" INVALID, must be a non-blank string"));
        }
    }

    @Test
    public void testParameterHandlerMinumumParameters() throws PolicyPapException {
        final String[] minArgumentString = { "-c", "parameters/MinimumParameters.json" };

        final PapCommandLineArguments minArguments = new PapCommandLineArguments();
        minArguments.parse(minArgumentString);

        final PapParameterGroup parGroup = new PapParameterHandler().getParameters(minArguments);
        assertEquals(CommonTestData.PAP_GROUP_NAME, parGroup.getName());
    }

    @Test
    public void testPapParameterGroup() throws PolicyPapException {
        final String[] papConfigParameters = { "-c", "parameters/PapConfigParameters.json" };

        final PapCommandLineArguments arguments = new PapCommandLineArguments();
        arguments.parse(papConfigParameters);

        final PapParameterGroup parGroup = new PapParameterHandler().getParameters(arguments);
        assertTrue(arguments.checkSetConfigurationFilePath());
        assertEquals(CommonTestData.PAP_GROUP_NAME, parGroup.getName());
    }

    @Test
    public void testPapParameterGroup_InvalidName() throws PolicyPapException {
        final String[] papConfigParameters = { "-c", "parameters/PapConfigParameters_InvalidName.json" };

        final PapCommandLineArguments arguments = new PapCommandLineArguments();
        arguments.parse(papConfigParameters);

        try {
            new PapParameterHandler().getParameters(arguments);
            fail("test should throw an exception here");
        } catch (final Exception e) {
            assertTrue(e.getMessage().contains(
                    "field \"name\" type \"java.lang.String\" value \" \" INVALID, must be a non-blank string"));
        }
    }

    @Test
    public void testPapVersion() throws PolicyPapException {
        final String[] papConfigParameters = { "-v" };
        final PapCommandLineArguments arguments = new PapCommandLineArguments();
        final String version = arguments.parse(papConfigParameters);
        assertTrue(version.startsWith("ONAP Policy Framework PAP Service"));
    }

    @Test
    public void testPapHelp() throws PolicyPapException {
        final String[] papConfigParameters = { "-h" };
        final PapCommandLineArguments arguments = new PapCommandLineArguments();
        final String help = arguments.parse(papConfigParameters);
        assertTrue(help.startsWith("usage:"));
    }

    @Test
    public void testPapInvalidOption() throws PolicyPapException {
        final String[] papConfigParameters = { "-d" };
        final PapCommandLineArguments arguments = new PapCommandLineArguments();
        try {
            arguments.parse(papConfigParameters);
        } catch (final Exception exp) {
            assertTrue(exp.getMessage().startsWith("invalid command line arguments specified"));
        }
    }
}
