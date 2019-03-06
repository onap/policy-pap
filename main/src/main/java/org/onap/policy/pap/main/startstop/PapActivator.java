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

import lombok.Getter;
import lombok.Setter;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.rest.PapRestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wraps a distributor so that it can be activated as a complete service together with all its pap and
 * forwarding handlers.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PapActivator.class);

    private final PapParameterGroup papParameterGroup;

    /**
     * The current activator. This is initialized to a dummy instance used until the real
     * one has been configured.
     */
    @Getter
    @Setter
    private static volatile PapActivator current = new PapActivator(null);

    @Getter
    @Setter(lombok.AccessLevel.PRIVATE)
    private volatile boolean alive = false;

    private PapRestServer restServer;

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
        if (isAlive()) {
            return;
        }

        try {
            LOGGER.debug("Policy pap starting as a service . . .");
            startPapRestServer();
            registerToParameterService(papParameterGroup);
            setAlive(true);
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
        if (!isAlive()) {
            return;
        }

        try {
            deregisterToParameterService(papParameterGroup);
            setAlive(false);

            // Stop the pap rest server
            restServer.stop();
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
     * Starts the pap rest server using configuration parameters.
     *
     * @throws PolicyPapException if server start fails
     */
    private void startPapRestServer() throws PolicyPapException {
        papParameterGroup.getRestServerParameters().setName(papParameterGroup.getName());
        restServer = new PapRestServer(papParameterGroup.getRestServerParameters());
        if (!restServer.start()) {
            throw new PolicyPapException("Failed to start pap rest server. Check log for more details...");
        }
    }
}
