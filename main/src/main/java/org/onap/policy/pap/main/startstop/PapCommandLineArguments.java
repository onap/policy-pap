/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021 Nordix Foundation.
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

    protected static final String GROUP_FILE_OPTION = "g";
    protected static final String GROUP_FILE_LONG_OPTION = "groups-file";
    public static final String GROUP_FILE_ARG_NAME = "GROUP_FILE";

    protected static final String DEFAULT_GROUP_RESOURCE = "PapDb.json";

    /**
     * Construct the options for the CLI editor.
     */
    public PapCommandLineArguments() {
        super(Main.class.getName(), MessageConstants.POLICY_PAP, customOptionG());
    }

    private static Option customOptionG() {
        return Option.builder(GROUP_FILE_OPTION).longOpt(GROUP_FILE_LONG_OPTION)
                       .desc("the full path to the groups file to use, "
                                     + "the groups file contains the group configuration added to the DB")
                       .hasArg().argName(GROUP_FILE_ARG_NAME).required(false).type(String.class).build();
    }

    protected String getPdpGroupsConfiguration() {
        return this.getCommandLine()
                       .getOptionValue(GROUP_FILE_OPTION, DEFAULT_GROUP_RESOURCE);
    }

    @Override
    public void validate() throws CommandLineException {
        super.validate();
        String groupConfig = getPdpGroupsConfiguration();
        if (!groupConfig.equals(DEFAULT_GROUP_RESOURCE)) {
            validateReadableFile(MessageConstants.POLICY_PAP, groupConfig);
        }
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
