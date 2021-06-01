/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.PolicyPapRuntimeException;


/**
 * This class reads and handles command line parameters for the policy pap service.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapCommandLineArguments {
    private static final String FILE_MESSAGE_PREAMBLE = " file \"";
    private static final int HELP_LINE_LENGTH = 120;

    private final Options options;
    private String configurationFilePath = null;
    private String propertyFilePath = null;
    private String groupFilePath = null;

    /**
     * Construct the options for the CLI editor.
     */
    public PapCommandLineArguments() {
        //@formatter:off
        options = new Options();
        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("outputs the usage of this command")
                .required(false)
                .type(Boolean.class)
                .build());
        options.addOption(Option.builder("v")
                .longOpt("version")
                .desc("outputs the version of policy pap")
                .required(false)
                .type(Boolean.class)
                .build());
        options.addOption(Option.builder("c")
                .longOpt("config-file")
                .desc("the full path to the configuration file to use, "
                        + "the configuration file must be a Json file containing the policy pap parameters")
                .hasArg()
                .argName("CONFIG_FILE")
                .required(false)
                .type(String.class)
                .build());
        options.addOption(Option.builder("p")
                .longOpt("property-file")
                .desc("the full path to the topic property file to use, "
                        + "the property file contains the policy pap topic properties")
                .hasArg()
                .argName("PROP_FILE")
                .required(false)
                .type(String.class)
                .build());
        options.addOption(Option.builder("g")
                .longOpt("groups-file")
                .desc("the full path to the groups file to use, "
                        + "the groups file contains the group configuration")
                .hasArg()
                .argName("GROUP_FILE")
                .required(false)
                .type(String.class)
                .build());
        //@formatter:on
    }

    /**
     * Construct the options for the CLI editor and parse in the given arguments.
     *
     * @param args The command line arguments
     */
    public PapCommandLineArguments(final String[] args) {
        // Set up the options with the default constructor
        this();

        // Parse the arguments
        try {
            parse(args);
        } catch (final PolicyPapException e) {
            throw new PolicyPapRuntimeException("parse error on policy pap parameters", e);
        }
    }

    /**
     * Parse the command line options.
     *
     * @param args The command line arguments
     * @return a string with a message for help and version, or null if there is no message
     * @throws PolicyPapException on command argument errors
     */
    public String parse(final String[] args) throws PolicyPapException {
        // Clear all our arguments
        setConfigurationFilePath(null);
        setPropertyFilePath(null);
        setGroupFilePath(null);

        CommandLine commandLine;
        try {
            commandLine = new DefaultParser().parse(options, args);
        } catch (final ParseException e) {
            throw new PolicyPapException("invalid command line arguments specified : " + e.getMessage());
        }

        // Arguments left over after Commons CLI does its stuff
        final String[] remainingArgs = commandLine.getArgs();

        if (remainingArgs.length > 0 && commandLine.hasOption('c') || remainingArgs.length > 0) {
            throw new PolicyPapException("too many command line arguments specified : " + Arrays.toString(args));
        }

        if (commandLine.hasOption('h')) {
            return help(Main.class.getName());
        }

        if (commandLine.hasOption('v')) {
            return version();
        }

        if (commandLine.hasOption('c')) {
            setConfigurationFilePath(commandLine.getOptionValue('c'));
        }

        if (commandLine.hasOption('p')) {
            setPropertyFilePath(commandLine.getOptionValue('p'));
        }

        if (commandLine.hasOption('g')) {
            setGroupFilePath(commandLine.getOptionValue('g'));
        }

        return null;
    }

    /**
     * Validate the command line options.
     *
     * @throws PolicyPapException on command argument validation errors
     */
    public void validate() throws PolicyPapException {
        validateReadableFile("policy pap configuration", configurationFilePath);
        if (groupFilePath != null) {
            validateReadableFile("policy pap groups", groupFilePath);
        }
    }

    /**
     * Print version information for policy pap.
     *
     * @return the version string
     */
    public String version() {
        return ResourceUtils.getResourceAsString("version.txt");
    }

    /**
     * Print help information for policy pap.
     *
     * @param mainClassName the main class name
     * @return the help string
     */
    public String help(final String mainClassName) {
        final var helpFormatter = new HelpFormatter();
        final var stringWriter = new StringWriter();
        final var printWriter = new PrintWriter(stringWriter);

        helpFormatter.printHelp(printWriter, HELP_LINE_LENGTH, mainClassName + " [options...]", "options", options, 0,
                0, "");

        return stringWriter.toString();
    }

    /**
     * Gets the configuration file path.
     *
     * @return the configuration file path
     */
    public String getConfigurationFilePath() {
        return configurationFilePath;
    }

    /**
     * Gets the full expanded configuration file path.
     *
     * @return the configuration file path
     */
    public String getFullConfigurationFilePath() {
        return ResourceUtils.getFilePath4Resource(getConfigurationFilePath());
    }

    /**
     * Sets the configuration file path.
     *
     * @param configurationFilePath the configuration file path
     */
    public void setConfigurationFilePath(final String configurationFilePath) {
        this.configurationFilePath = configurationFilePath;

    }

    /**
     * Check set configuration file path.
     *
     * @return true, if check set configuration file path
     */
    public boolean checkSetConfigurationFilePath() {
        return configurationFilePath != null && configurationFilePath.length() > 0;
    }

    /**
     * Gets the property file path.
     *
     * @return the property file path
     */
    public String getPropertyFilePath() {
        return propertyFilePath;
    }

    /**
     * Gets the full expanded property file path.
     *
     * @return the property file path
     */
    public String getFullPropertyFilePath() {
        return ResourceUtils.getFilePath4Resource(getPropertyFilePath());
    }

    /**
     * Sets the property file path.
     *
     * @param propertyFilePath the property file path
     */
    public void setPropertyFilePath(final String propertyFilePath) {
        this.propertyFilePath = propertyFilePath;

    }

    /**
     * Check set property file path.
     *
     * @return true, if check set property file path
     */
    public boolean checkSetPropertyFilePath() {
        return propertyFilePath != null && propertyFilePath.length() > 0;
    }

    /**
     * Gets the group file path or the default file group resource if not present.
     *
     * @return the group file path
     */
    public String getGroupFilePath() {
        return groupFilePath;
    }

    /**
     * Sets the group file path.
     *
     * @param groupFilePath the property file path
     */
    public void setGroupFilePath(final String groupFilePath) {
        this.groupFilePath = groupFilePath;
    }

    /**
     * Validate readable file.
     *
     * @param fileTag the file tag
     * @param fileName the file name
     * @throws PolicyPapException on the file name passed as a parameter
     */
    private void validateReadableFile(final String fileTag, final String fileName) throws PolicyPapException {
        if (fileName == null || fileName.length() == 0) {
            throw new PolicyPapException(fileTag + " file was not specified as an argument");
        }

        // The file name refers to a resource on the local file system
        final var fileUrl = ResourceUtils.getUrl4Resource(fileName);
        if (fileUrl == null) {
            throw new PolicyPapException(fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" does not exist");
        }

        final var theFile = new File(fileUrl.getPath());
        if (!theFile.exists()) {
            throw new PolicyPapException(fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" does not exist");
        }
        if (!theFile.isFile()) {
            throw new PolicyPapException(fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" is not a normal file");
        }
        if (!theFile.canRead()) {
            throw new PolicyPapException(fileTag + FILE_MESSAGE_PREAMBLE + fileName + "\" is unreadable");
        }
    }
}
