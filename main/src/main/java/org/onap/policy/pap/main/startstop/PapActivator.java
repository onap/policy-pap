/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2022 Nordix Foundation.
 *  Modifications Copyright (C) 2019-2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
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
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.PdpHeartbeatListener;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.TimerManager;
import org.onap.policy.pap.main.notification.PolicyNotifier;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;
import org.onap.policy.pap.main.rest.PapStatisticsManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * This class activates Policy Administration (PAP) as a complete service together with all its controllers, listeners &
 * handlers.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@Component
public class PapActivator extends ServiceManagerContainer {

    // topic names
    @Getter
    @Value("${pap.topic.pdp-pap.name}")
    private String topicPolicyPdpPap = "POLICY-PDP-PAP";

    @Getter
    @Value("${pap.topic.notification.name}")
    private String topicPolicyNotification = "POLICY-NOTIFICATION";

    @Getter
    @Value("${pap.topic.heartbeat.name}")
    private String topicPolicyHeartbeat = "POLICY-HEARTBEAT";

    private static final String[] MSG_TYPE_NAMES = { "messageName" };
    private static final String[] REQ_ID_NAMES = { "response", "responseTo" };

    /**
     * Max number of heart beats that can be missed before PAP removes a PDP.
     */
    private static final int MAX_MISSED_HEARTBEATS = 3;

    private final PapParameterGroup papParameterGroup;

    /**
     * Listens for messages on the topic, decodes them into a {@link PdpStatus} message, and then dispatches them to
     * {@link #responseReqIdDispatcher}.
     */
    private final MessageTypeDispatcher responseMsgDispatcher;
    private final MessageTypeDispatcher heartbeatMsgDispatcher;

    /**
     * Listens for {@link PdpStatus} messages and then routes them to the listener associated with the ID of the
     * originating request.
     */
    private final RequestIdDispatcher<PdpStatus> responseReqIdDispatcher;
    private final RequestIdDispatcher<PdpStatus> heartbeatReqIdDispatcher;

    /**
     * Instantiate the activator for policy pap as a complete service.
     *
     * @param papParameterGroup the parameters for the pap service
     */
    public PapActivator(PapParameterGroup papParameterGroup, PolicyNotifier policyNotifier,
        PdpHeartbeatListener pdpHeartbeatListener, PdpModifyRequestMap pdpModifyRequestMap,
        MeterRegistry meterRegistry) {
        super("Policy PAP");
        this.papParameterGroup = papParameterGroup;
        TopicEndpointManager.getManager().addTopics(papParameterGroup.getTopicParameterGroup());

        try {
            this.responseMsgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
            this.heartbeatMsgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
            this.responseReqIdDispatcher = new RequestIdDispatcher<>(PdpStatus.class, REQ_ID_NAMES);
            this.heartbeatReqIdDispatcher = new RequestIdDispatcher<>(PdpStatus.class, REQ_ID_NAMES);

        } catch (final RuntimeException e) {
            throw new PolicyPapRuntimeException(e);
        }


        final var pdpUpdateLock = new Object();
        final var pdpParams = papParameterGroup.getPdpParameters();
        final AtomicReference<Publisher<PdpMessage>> pdpPub = new AtomicReference<>();
        final AtomicReference<Publisher<PolicyNotification>> notifyPub = new AtomicReference<>();
        final AtomicReference<TimerManager> pdpUpdTimers = new AtomicReference<>();
        final AtomicReference<TimerManager> pdpStChgTimers = new AtomicReference<>();
        final AtomicReference<ScheduledExecutorService> pdpExpirationTimer = new AtomicReference<>();
        final AtomicReference<PdpModifyRequestMap> requestMap = new AtomicReference<>();

        // @formatter:off

        // Note: This class is not Thread Safe. If more than one PAP component is started, the code below overwrites
        //       the parameter service and registry entries.

        addAction("Meter Registry",
            () -> Registry.register(PapConstants.REG_METER_REGISTRY, meterRegistry),
            () -> Registry.unregister(PapConstants.REG_METER_REGISTRY));

        addAction("PAP parameters",
            () -> ParameterService.register(papParameterGroup),
            () -> ParameterService.deregister(papParameterGroup.getName()));

        addAction("Pdp Heartbeat Listener",
            () -> heartbeatReqIdDispatcher.register(pdpHeartbeatListener),
            () -> heartbeatReqIdDispatcher.unregister(pdpHeartbeatListener));

        addAction("Response Request ID Dispatcher",
            () -> responseMsgDispatcher.register(PdpMessageType.PDP_STATUS.name(), this.responseReqIdDispatcher),
            () -> responseMsgDispatcher.unregister(PdpMessageType.PDP_STATUS.name()));

        addAction("Heartbeat Request ID Dispatcher",
            () -> heartbeatMsgDispatcher.register(PdpMessageType.PDP_STATUS.name(), this.heartbeatReqIdDispatcher),
            () -> heartbeatMsgDispatcher.unregister(PdpMessageType.PDP_STATUS.name()));

        addAction("Response Message Dispatcher",
            () -> registerMsgDispatcher(responseMsgDispatcher, topicPolicyPdpPap),
            () -> unregisterMsgDispatcher(responseMsgDispatcher, topicPolicyPdpPap));

        addAction("Heartbeat Message Dispatcher",
            () -> registerMsgDispatcher(heartbeatMsgDispatcher, topicPolicyHeartbeat),
            () -> unregisterMsgDispatcher(heartbeatMsgDispatcher, topicPolicyHeartbeat));

        addAction("topics",
            TopicEndpointManager.getManager()::start,
            TopicEndpointManager.getManager()::shutdown);

        addAction("PAP statistics",
            () -> Registry.register(PapConstants.REG_STATISTICS_MANAGER, new PapStatisticsManager()),
            () -> Registry.unregister(PapConstants.REG_STATISTICS_MANAGER));

        addAction("PAP Activator",
            () -> Registry.register(PapConstants.REG_PAP_ACTIVATOR, this),
            () -> Registry.unregister(PapConstants.REG_PAP_ACTIVATOR));

        addAction("PDP publisher",
            () -> {
                pdpPub.set(new Publisher<>(topicPolicyPdpPap));
                startThread(pdpPub.get());
            },
            () -> pdpPub.get().stop());

        addAction("Policy Notification publisher",
            () -> {
                notifyPub.set(new Publisher<>(topicPolicyNotification));
                startThread(notifyPub.get());
                policyNotifier.setPublisher(notifyPub.get());
            },
            () -> notifyPub.get().stop());

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
                pdpModifyRequestMap.initialize(
                    PdpModifyRequestMapParams.builder()
                    .maxPdpAgeMs(MAX_MISSED_HEARTBEATS * pdpParams.getHeartBeatMs())
                    .modifyLock(pdpUpdateLock)
                    .params(pdpParams)
                    .pdpPublisher(pdpPub.get())
                    .responseDispatcher(responseReqIdDispatcher)
                    .stateChangeTimers(pdpStChgTimers.get())
                    .updateTimers(pdpUpdTimers.get())
                    .savePdpStatistics(papParameterGroup.isSavePdpStatisticsInDb())
                    .build());
                requestMap.set(pdpModifyRequestMap);
                Registry.register(PapConstants.REG_PDP_MODIFY_MAP, requestMap.get());
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
            () -> pdpExpirationTimer.get().shutdown());

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
     * @param dispatcher dispatcher to register
     * @param topic topic of interest
     */
    private void registerMsgDispatcher(TopicListener dispatcher, String topic) {
        for (final TopicSource source : TopicEndpointManager.getManager().getTopicSources(List.of(topic))) {
            source.register(dispatcher);
        }
    }

    /**
     * Unregisters the dispatcher from the topic source(s).
     * @param dispatcher dispatcher to unregister
     * @param topic topic of interest
     */
    private void unregisterMsgDispatcher(TopicListener dispatcher, String topic) {
        for (final TopicSource source : TopicEndpointManager.getManager().getTopicSources(List.of(topic))) {
            source.unregister(dispatcher);
        }
    }

    /**
     * Handle ContextRefreshEvent.
     *
     * @param ctxRefreshedEvent the ContextRefreshedEvent
     */
    @EventListener
    public void handleContextRefreshEvent(ContextRefreshedEvent ctxRefreshedEvent) {
        if (!isAlive()) {
            start();
        }
    }

    /**
     * Handle ContextClosedEvent.
     *
     * @param ctxClosedEvent the ContextClosedEvent
     */
    @EventListener
    public void handleContextClosedEvent(ContextClosedEvent ctxClosedEvent) {
        if (isAlive()) {
            stop();
        }
    }
}
