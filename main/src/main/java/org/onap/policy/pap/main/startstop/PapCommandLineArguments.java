/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021 Nordix Foundation.
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

import org.apache.commons.cli.Option;
import org.onap.policy.common.utils.cmd.CommandLineArgumentsHandler;
import org.onap.policy.common.utils.cmd.CommandLineException;
import org.onap.policy.common.utils.resources.MessageConstants;
import org.onap.policy.pap.main.PolicyPapRuntimeException;

/**
 * This class reads and handles command line parameters for the policy pap service.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapCommandLineArguments extends CommandLineArgumentsHandler {

    /**
     * Construct the options for the CLI editor.
     */
    public PapCommandLineArguments() {
        super(Main.class.getName(), MessageConstants.POLICY_PAP, customOption());
    }

    /**
     * Builds the extra property-file option to be declared on constructor.
     *
     * @return property-file option
     */
    private static Option customOption() {
        return Option.builder("p").longOpt("property-file")
                .desc("the full path to the topic property file to use, "
                        + "the property file contains the policy pap topic properties")
                .hasArg().argName("PROP_FILE").required(false).type(String.class).build();
    }

    /**
     * Construct the options for the CLI editor and parse in the given arguments.
     *
     * @param args The command line arguments
     */
    public PapCommandLineArguments(final String[] args) {
        this();

        try {
            parse(args);
        } catch (final CommandLineException e) {
            throw new PolicyPapRuntimeException("parse error on policy pap parameters", e);
        }
    }
}
