/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

import java.util.Arrays;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.parameters.PapParameterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class initiates ONAP Policy Framework PAP component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class Main {

    private static final String START_FAILED = "start of policy pap service failed";

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private PapActivator activator;
    private PapParameterGroup parameterGroup;

    /**
     * Instantiates the policy pap service.
     *
     * @param args the command line arguments
     */
    public Main(final String[] args) {
        final String argumentString = Arrays.toString(args);
        LOGGER.info("Starting policy pap service with arguments - {}", argumentString);

        // Check the arguments
        final PapCommandLineArguments arguments = new PapCommandLineArguments();
        try {
            // The arguments return a string if there is a message to print and we should exit
            final String argumentMessage = arguments.parse(args);
            if (argumentMessage != null) {
                LOGGER.info(argumentMessage);
                return;
            }
            // Validate that the arguments are sane
            arguments.validate();

            // Read the parameters
            parameterGroup = new PapParameterHandler().getParameters(arguments);

            // Initialize database
            new PapDatabaseInitializer().initializePapDatabase(parameterGroup.getDatabaseProviderParameters());

            // Now, create the activator for the policy pap service
            activator = new PapActivator(parameterGroup);
            Registry.register(PapConstants.REG_PAP_ACTIVATOR, activator);

            // Start the activator
            activator.start();
        } catch (Exception exp) {
            if (null != activator) {
                Registry.unregister(PapConstants.REG_PAP_ACTIVATOR);
            }
            LOGGER.error(START_FAILED + ", used parameters are {}", Arrays.toString(args), exp);
            System.exit(1);
        }

        // Add a shutdown hook to shut everything down in an orderly manner
        Runtime.getRuntime().addShutdownHook(new PolicyPapShutdownHookClass());
        LOGGER.info("Started policy pap service");
    }

    /**
     * Get the parameters specified in JSON.
     *
     * @return the parameters
     */
    public PapParameterGroup getParameters() {
        return parameterGroup;
    }

    /**
     * Shut down Execution.
     *
     * @throws PolicyPapException on shutdown errors
     */
    public void shutdown() throws PolicyPapException {
        // clear the parameterGroup variable
        parameterGroup = null;

        // clear the pap activator
        if (activator != null) {
            activator.stop();
        }
    }

    /**
     * The Class PolicyPapShutdownHookClass terminates the policy pap service when its run method is called.
     */
    private class PolicyPapShutdownHookClass extends Thread {
        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            if (!activator.isAlive()) {
                return;
            }

            try {
                // Shutdown the policy pap service and wait for everything to stop
                activator.stop();
            } catch (final RuntimeException e) {
                LOGGER.warn("error occured during shut down of the policy pap service", e);
            }
        }
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(final String[] args) {      // NOSONAR
        /*
         * NOTE: arguments are validated by the constructor, thus sonar is disabled.
         */

        new Main(args);
    }
}
