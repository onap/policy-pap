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

import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

/**
 * This class wraps a distributor so that it can be activated as a complete service together with all its pap and
 * forwarding handlers.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapActivator {
    // The logger for this class
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(PapActivator.class);

    // The parameters of this policy pap activator
    private final PapParameterGroup papParameterGroup;

    private static boolean alive = false;

    /**
     * Instantiate the activator for policy pap as a complete service.
     *
     * @param papParameterGroup the parameters for the pap service
     */
    public PapActivator(final PapParameterGroup papParameterGroup) {
        this.papParameterGroup = papParameterGroup;
    }

    /**
     * Initialize pap as a complete service.
     *
     * @throws PolicyPapException on errors in initializing the service
     */
    public void initialize() throws PolicyPapException {
        try {
            LOGGER.debug("Policy pap starting as a service . . .");
            registerToParameterService(papParameterGroup);
            PapActivator.setAlive(true);
            LOGGER.debug("Policy pap started as a service");
        } catch (final Exception exp) {
            LOGGER.error("Policy pap service startup failed", exp);
            throw new PolicyPapException(exp.getMessage(), exp);
        }
    }

    /**
     * Terminate policy pap.
     *
     * @throws PolicyPapException on errors in terminating the service
     */
    public void terminate() throws PolicyPapException {
        try {
            deregisterToParameterService(papParameterGroup);
            PapActivator.setAlive(false);

        } catch (final Exception exp) {
            LOGGER.error("Policy pap service termination failed", exp);
            throw new PolicyPapException(exp.getMessage(), exp);
        }
    }

    /**
     * Get the parameters used by the activator.
     *
     * @return the parameters of the activator
     */
    public PapParameterGroup getParameterGroup() {
        return papParameterGroup;
    }

    /**
     * Method to register the parameters to Common Parameter Service.
     *
     * @param papParameterGroup the pap parameter group
     */
    public void registerToParameterService(final PapParameterGroup papParameterGroup) {
        ParameterService.register(papParameterGroup);
    }

    /**
     * Method to deregister the parameters from Common Parameter Service.
     *
     * @param papParameterGroup the pap parameter group
     */
    public void deregisterToParameterService(final PapParameterGroup papParameterGroup) {
        ParameterService.deregister(papParameterGroup.getName());
    }

    /**
     * Returns the alive status of pap service.
     *
     * @return the alive
     */
    public static boolean isAlive() {
        return alive;
    }

    /**
     * Change the alive status of pap service.
     *
     * @param status the status
     */
    public static void setAlive(final boolean status) {
        alive = status;
    }
}
