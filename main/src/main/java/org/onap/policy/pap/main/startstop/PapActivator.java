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

import java.util.Arrays;
import java.util.Properties;
import lombok.Getter;
import org.onap.policy.common.endpoints.event.comm.TopicEndpoint;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.common.utils.services.ServiceManager;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.PdpCommObjects;
import org.onap.policy.pap.main.comm.PdpCommObjectsImpl;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.rest.PapRestServer;
import org.onap.policy.pdp.common.enums.PdpMessageType;
import org.onap.policy.pdp.common.models.PdpStatus;

/**
 * This class wraps a distributor so that it can be activated as a complete service
 * together with all its pap and forwarding handlers.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapActivator {
    private static final String[] MSG_TYPE_NAMES = {"messageName"};
    private static final String[] REQ_ID_NAMES = {"response", "responseTo"};

    private final PapParameterGroup papParameterGroup;

    /**
     * The current activator.
     */
    @Getter
    private static volatile PapActivator current = null;

    /**
     * Used to stop the services.
     */
    private final ServiceManager manager;

    /**
     * The PAP REST API server.
     */
    private PapRestServer restServer;

    /**
     * Objects used to communicate with the PDPs.
     */
    @Getter
    private final PdpCommObjectsImpl pdpComm;

    /**
     * Listens for messages on the topic, decodes them into a {@link PdpStatus} message,
     * and then dispatches them to {@link #reqIdDispatcher}.
     */
    private final MessageTypeDispatcher msgDispatcher;

    /**
     * Listens for {@link PdpStatus} messages and then routes them to the listener
     * associated with the ID of the originating request.
     */
    @Getter
    private final RequestIdDispatcher<PdpStatus> reqIdDispatcher;

    /**
     * Prevents more than one thread from updating the PDPs at the same time.
     */
    @Getter
    private final Object pdpUpdateLock = new Object();

    /**
     * Instantiate the activator for policy pap as a complete service.
     *
     * @param papParameterGroup the parameters for the pap service
     * @param topicProperties properties used to configure the topics
     */
    public PapActivator(final PapParameterGroup papParameterGroup, Properties topicProperties) {
        TopicEndpoint.manager.addTopicSinks(topicProperties);
        TopicEndpoint.manager.addTopicSources(topicProperties);

        try {
            this.papParameterGroup = papParameterGroup;
            this.msgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
            this.reqIdDispatcher = new RequestIdDispatcher<>(PdpStatus.class, REQ_ID_NAMES);
            this.pdpComm = new PdpCommObjectsImpl(papParameterGroup.getPdpParameters(), reqIdDispatcher, pdpUpdateLock);

        } catch (PolicyPapException e) {
            throw new PolicyPapRuntimeException(e);
        }

        this.msgDispatcher.register(PdpMessageType.PDP_STATUS.toString(), this.reqIdDispatcher);

        // @formatter:off
        this.manager = new ServiceManager("Policy PAP")
                        .addAction("dispatcher",
                            () -> registerDispatcher(),
                            () -> deregisterDispatcher())
                        .addAction("topics",
                            () -> TopicEndpoint.manager.start(),
                            () -> TopicEndpoint.manager.shutdown())
                        .addAction("register parameters",
                            () -> registerToParameterService(papParameterGroup),
                            () -> deregisterToParameterService(papParameterGroup))
                        .addAction("REST server",
                            () -> startPapRestServer(),
                            () -> restServer.stop());
        // @formatter:on

        current = this;
    }

    /**
     * Determines if this activator is alive.
     *
     * @return {@code true} if this activator is alive, {@code false} otherwise
     */
    public boolean isAlive() {
        return manager.isAlive();
    }

    /**
     * Initialize pap as a complete service.
     *
     * @throws PolicyPapException on errors in initializing the service
     */
    public void initialize() throws PolicyPapException {
        manager.start();
    }

    /**
     * Terminate policy pap.
     *
     * @throws PolicyPapException on errors in terminating the service
     */
    public void terminate() throws PolicyPapException {
        manager.stop();
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
     * Registers the dispatcher with the topic source(s).
     */
    private void registerDispatcher() {
        for (TopicSource source : TopicEndpoint.manager.getTopicSources(Arrays.asList(PdpCommObjects.POLICY_PDP_PAP))) {
            source.register(msgDispatcher);
        }
    }

    /**
     * Unregisters the dispatcher from the topic source(s).
     */
    private void deregisterDispatcher() {
        for (TopicSource source : TopicEndpoint.manager.getTopicSources(Arrays.asList(PdpCommObjects.POLICY_PDP_PAP))) {
            source.unregister(msgDispatcher);
        }
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
