/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019-2021 AT&T Intellectual Property.
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.http.client.HttpClientFactoryInstance;
import org.onap.policy.common.endpoints.http.server.RestServer;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpMessageType;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.PdpHeartbeatListener;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.TimerManager;
import org.onap.policy.pap.main.notification.PolicyNotifier;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;
import org.onap.policy.pap.main.rest.HealthCheckRestControllerV1;
import org.onap.policy.pap.main.rest.PapAafFilter;
import org.onap.policy.pap.main.rest.PapStatisticsManager;
import org.onap.policy.pap.main.rest.PdpGroupCreateOrUpdateControllerV1;
import org.onap.policy.pap.main.rest.PdpGroupDeleteControllerV1;
import org.onap.policy.pap.main.rest.PdpGroupDeployControllerV1;
import org.onap.policy.pap.main.rest.PdpGroupHealthCheckControllerV1;
import org.onap.policy.pap.main.rest.PdpGroupQueryControllerV1;
import org.onap.policy.pap.main.rest.PdpGroupStateChangeControllerV1;
import org.onap.policy.pap.main.rest.PolicyComponentsHealthCheckControllerV1;
import org.onap.policy.pap.main.rest.PolicyComponentsHealthCheckProvider;
import org.onap.policy.pap.main.rest.PolicyStatusControllerV1;
import org.onap.policy.pap.main.rest.PolicyUndeployerImpl;
import org.onap.policy.pap.main.rest.StatisticsRestControllerV1;

/**
 * This class activates Policy Administration (PAP) as a complete service together with all its controllers, listeners &
 * handlers.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapActivator extends ServiceManagerContainer {
    private static final String[] MSG_TYPE_NAMES = { "messageName" };
    private static final String[] REQ_ID_NAMES = { "response", "responseTo" };

    /**
     * Max number of heat beats that can be missed before PAP removes a PDP.
     */
    private static final int MAX_MISSED_HEARTBEATS = 3;

    private final PapParameterGroup papParameterGroup;

    /**
     * Listens for messages on the topic, decodes them into a {@link PdpStatus} message, and then dispatches them to
     * {@link #reqIdDispatcher}.
     */
    private final MessageTypeDispatcher msgDispatcher;

    /**
     * Listens for {@link PdpStatus} messages and then routes them to the listener associated with the ID of the
     * originating request.
     */
    private final RequestIdDispatcher<PdpStatus> reqIdDispatcher;

    /**
     * Listener for anonymous {@link PdpStatus} messages either for registration or heartbeat.
     */
    private final PdpHeartbeatListener pdpHeartbeatListener;

    /**
     * Instantiate the activator for policy pap as a complete service.
     *
     * @param papParameterGroup the parameters for the pap service
     */
    public PapActivator(final PapParameterGroup papParameterGroup) {
        super("Policy PAP");

        TopicEndpointManager.getManager().addTopics(papParameterGroup.getTopicParameterGroup());

        try {
            this.papParameterGroup = papParameterGroup;
            this.msgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
            this.reqIdDispatcher = new RequestIdDispatcher<>(PdpStatus.class, REQ_ID_NAMES);
            this.pdpHeartbeatListener = new PdpHeartbeatListener(papParameterGroup.getPdpParameters());

        } catch (final RuntimeException e) {
            throw new PolicyPapRuntimeException(e);
        }

        papParameterGroup.getRestServerParameters().setName(papParameterGroup.getName());

        final var pdpUpdateLock = new Object();
        final var pdpParams = papParameterGroup.getPdpParameters();
        final AtomicReference<Publisher<PdpMessage>> pdpPub = new AtomicReference<>();
        final AtomicReference<Publisher<PolicyNotification>> notifyPub = new AtomicReference<>();
        final AtomicReference<TimerManager> pdpUpdTimers = new AtomicReference<>();
        final AtomicReference<TimerManager> pdpStChgTimers = new AtomicReference<>();
        final AtomicReference<ScheduledExecutorService> pdpExpirationTimer = new AtomicReference<>();
        final AtomicReference<PolicyModelsProviderFactoryWrapper> daoFactory = new AtomicReference<>();
        final AtomicReference<PdpModifyRequestMap> requestMap = new AtomicReference<>();
        final AtomicReference<RestServer> restServer = new AtomicReference<>();
        final AtomicReference<PolicyNotifier> notifier = new AtomicReference<>();

        // @formatter:off
        addAction("PAP parameters",
            () -> ParameterService.register(papParameterGroup),
            () -> ParameterService.deregister(papParameterGroup.getName()));

        addAction("DAO Factory",
            () -> daoFactory.set(new PolicyModelsProviderFactoryWrapper(
                                    papParameterGroup.getDatabaseProviderParameters())),
            () -> daoFactory.get().close());

        addAction("DAO Factory registration",
            () -> Registry.register(PapConstants.REG_PAP_DAO_FACTORY, daoFactory.get()),
            () -> Registry.unregister(PapConstants.REG_PAP_DAO_FACTORY));

        addAction("Pdp Heartbeat Listener",
            () -> reqIdDispatcher.register(pdpHeartbeatListener),
            () -> reqIdDispatcher.unregister(pdpHeartbeatListener));

        addAction("Request ID Dispatcher",
            () -> msgDispatcher.register(PdpMessageType.PDP_STATUS.name(), this.reqIdDispatcher),
            () -> msgDispatcher.unregister(PdpMessageType.PDP_STATUS.name()));

        addAction("Message Dispatcher",
            this::registerMsgDispatcher,
            this::unregisterMsgDispatcher);

        addAction("topics",
            TopicEndpointManager.getManager()::start,
            TopicEndpointManager.getManager()::shutdown);

        addAction("PAP statistics",
            () -> Registry.register(PapConstants.REG_STATISTICS_MANAGER, new PapStatisticsManager()),
            () -> Registry.unregister(PapConstants.REG_STATISTICS_MANAGER));

        addAction("PDP publisher",
            () -> {
                pdpPub.set(new Publisher<>(PapConstants.TOPIC_POLICY_PDP_PAP));
                startThread(pdpPub.get());
            },
            () -> pdpPub.get().stop());

        addAction("Policy Notification publisher",
            () -> {
                notifyPub.set(new Publisher<>(PapConstants.TOPIC_POLICY_NOTIFICATION));
                startThread(notifyPub.get());
                notifier.set(new PolicyNotifier(notifyPub.get(), daoFactory.get()));
            },
            () -> notifyPub.get().stop());

        addAction("Policy Notifier",
            () -> Registry.register(PapConstants.REG_POLICY_NOTIFIER, notifier.get()),
            () -> Registry.unregister(PapConstants.REG_POLICY_NOTIFIER));

        addAction("PDP update timers",
            () -> {
                pdpUpdTimers.set(new TimerManager("update", pdpParams.getUpdateParameters().getMaxWaitMs()));
                startThread(pdpUpdTimers.get());
            },
            () -> pdpUpdTimers.get().stop());

        addAction("PDP state-change timers",
            () -> {
                pdpStChgTimers.set(new TimerManager("state-change", pdpParams.getUpdateParameters().getMaxWaitMs()));
                startThread(pdpStChgTimers.get());
            },
            () -> pdpStChgTimers.get().stop());

        addAction("PDP modification lock",
            () -> Registry.register(PapConstants.REG_PDP_MODIFY_LOCK, pdpUpdateLock),
            () -> Registry.unregister(PapConstants.REG_PDP_MODIFY_LOCK));

        addAction("PDP modification requests",
            () -> {
                requestMap.set(new PdpModifyRequestMap(
                            PdpModifyRequestMapParams.builder()
                                    .maxPdpAgeMs(MAX_MISSED_HEARTBEATS * pdpParams.getHeartBeatMs())
                                    .daoFactory(daoFactory.get())
                                    .modifyLock(pdpUpdateLock)
                                    .params(pdpParams)
                                    .policyNotifier(notifier.get())
                                    .pdpPublisher(pdpPub.get())
                                    .responseDispatcher(reqIdDispatcher)
                                    .stateChangeTimers(pdpStChgTimers.get())
                                    .updateTimers(pdpUpdTimers.get())
                                    .build()));
                Registry.register(PapConstants.REG_PDP_MODIFY_MAP, requestMap.get());

                // now that it's registered, we can attach a "policy undeploy" provider
                requestMap.get().setPolicyUndeployer(new PolicyUndeployerImpl());
            },
            () -> Registry.unregister(PapConstants.REG_PDP_MODIFY_MAP));

        addAction("PDP expiration timer",
            () -> {
                long frequencyMs = pdpParams.getHeartBeatMs();
                pdpExpirationTimer.set(Executors.newScheduledThreadPool(1));
                pdpExpirationTimer.get().scheduleWithFixedDelay(
                    requestMap.get()::removeExpiredPdps,
                    frequencyMs,
                    frequencyMs,
                    TimeUnit.MILLISECONDS);
            },
            () -> pdpExpirationTimer.get().shutdownNow());

        addAction("PAP client executor",
            () ->
                PolicyComponentsHealthCheckProvider.initializeClientHealthCheckExecutorService(papParameterGroup,
                                HttpClientFactoryInstance.getClientFactory()),
                PolicyComponentsHealthCheckProvider::cleanup);

        addAction("REST server",
            () -> {
                var server = new RestServer(papParameterGroup.getRestServerParameters(), PapAafFilter.class,
                                HealthCheckRestControllerV1.class,
                                StatisticsRestControllerV1.class,
                                PdpGroupCreateOrUpdateControllerV1.class,
                                PdpGroupDeployControllerV1.class,
                                PdpGroupDeleteControllerV1.class,
                                PdpGroupStateChangeControllerV1.class,
                                PdpGroupQueryControllerV1.class,
                                PdpGroupHealthCheckControllerV1.class,
                                PolicyStatusControllerV1.class,
                                PolicyComponentsHealthCheckControllerV1.class);
                restServer.set(server);
                restServer.get().start();
            },
            () -> restServer.get().stop());
        // @formatter:on
    }

    /**
     * Starts a background thread.
     *
     * @param runner function to run in the background
     */
    private void startThread(final Runnable runner) {
        final var thread = new Thread(runner);
        thread.setDaemon(true);

        thread.start();
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
    private void registerMsgDispatcher() {
        for (final TopicSource source : TopicEndpointManager.getManager()
                .getTopicSources(Arrays.asList(PapConstants.TOPIC_POLICY_PDP_PAP))) {
            source.register(msgDispatcher);
        }
    }

    /**
     * Unregisters the dispatcher from the topic source(s).
     */
    private void unregisterMsgDispatcher() {
        for (final TopicSource source : TopicEndpointManager.getManager()
                .getTopicSources(Arrays.asList(PapConstants.TOPIC_POLICY_PDP_PAP))) {
            source.unregister(msgDispatcher);
        }
    }
}
