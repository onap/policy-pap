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


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.onap.policy.common.parameters.GroupValidationResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.startstop.PapCommandLineArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles reading, parsing and validating of policy pap parameters from JSON files.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapParameterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PapParameterHandler.class);

    private static final Coder CODER = new StandardCoder();

    /**
     * Read the parameters from the parameter file.
     *
     * @param arguments the arguments passed to policy pap
     * @return the parameters read from the configuration file
     * @throws PolicyPapException on parameter exceptions
     */
    public PapParameterGroup getParameters(final PapCommandLineArguments arguments) throws PolicyPapException {
        PapParameterGroup papParameterGroup = null;

        // Read the parameters
        try {
            byte[] data = Files.readAllBytes(new File(arguments.getFullConfigurationFilePath()).toPath());
            String json = new String(data, StandardCharsets.UTF_8);
            // Read the parameters from JSON
            papParameterGroup = CODER.decode(json, PapParameterGroup.class);
        } catch (final IOException | CoderException e) {
            final String errorMessage = "error reading parameters from \"" + arguments.getConfigurationFilePath()
                    + "\"\n" + "(" + e.getClass().getSimpleName() + "):" + e.getMessage();
            LOGGER.error(errorMessage, e);
            throw new PolicyPapException(errorMessage, e);
        }

        // The JSON processing returns null if there is an empty file
        if (papParameterGroup == null) {
            final String errorMessage = "no parameters found in \"" + arguments.getConfigurationFilePath() + "\"";
            LOGGER.error(errorMessage);
            throw new PolicyPapException(errorMessage);
        }

        // validate the parameters
        final GroupValidationResult validationResult = papParameterGroup.validate();
        if (!validationResult.isValid()) {
            String returnMessage =
                    "validation error(s) on parameters from \"" + arguments.getConfigurationFilePath() + "\"\n";
            returnMessage += validationResult.getResult();

            LOGGER.error(returnMessage);
            throw new PolicyPapException(returnMessage);
        }

        return papParameterGroup;
    }
}
