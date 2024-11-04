/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2021, 2024 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.comm.msgdata;

import java.util.Collection;
import java.util.Collections;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.utils.services.ServiceManager;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.comm.QueueToken;
import org.onap.policy.pap.main.comm.TimerManager;
import org.onap.policy.pap.main.notification.PolicyNotifier;
import org.onap.policy.pap.main.parameters.RequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request data implementation.
 */
public abstract class RequestImpl implements Request {
    private static final Logger logger = LoggerFactory.getLogger(RequestImpl.class);

    /**
     * Name with which this data is associated, used for logging purposes.
     */
    @Getter
    private final String name;

    /**
     * The configuration parameters.
     */
    @Getter(AccessLevel.PROTECTED)
    private final RequestParams params;

    /**
     * Used to register/unregister the listener and the timer.
     */
    private final ServiceManager svcmgr;

    /**
     * Handles events associated with the request.
     */
    @Setter
    private RequestListener listener;

    /**
     * Notifier for policy update completions.
     */
    @Getter
    @Setter
    private PolicyNotifier notifier;

    /**
     * Current retry count.
     */
    @Getter
    private int retryCount = 0;

    /**
     * The current message.
     */
    @Getter
    private PdpMessage message;

    /**
     * The currently running timer.
     */
    private TimerManager.Timer timer;

    /**
     * Token that has been placed on the queue.
     */
    private QueueToken<PdpMessage> token = null;


    /**
     * Constructs the object, and validates the parameters.
     *
     * @param params configuration parameters
     * @param name the request name, used for logging purposes
     * @param message the initial message
     *
     * @throws IllegalArgumentException if a required parameter is not set
     */
    protected RequestImpl(@NonNull RequestParams params, @NonNull String name, @NonNull PdpMessage message) {
        params.validate();

        this.name = name;
        this.params = params;
        this.message = message;

        // @formatter:off
        this.svcmgr = new ServiceManager(name)
                        .addAction("listener",
                            () -> params.getResponseDispatcher()
                                            .register(this.message.getRequestId(), this::processResponse),
                            () -> params.getResponseDispatcher().unregister(this.message.getRequestId()))
                        .addAction("timer",
                            () -> timer = params.getTimers().register(this.message.getRequestId(), this::handleTimeout),
                            () -> timer.cancel())
                        .addAction("enqueue",
                            this::enqueue,
                            () -> {
                                // do not remove from the queue - token may be re-used if re-started
                            });
        // @formatter:on
    }

    /**
     * Reconfigures the current request with a new message. If it's currently publishing a
     * message, then it stops publishing, replaces the message, and then starts publishing
     * the new message.
     *
     * @param newMessage the new message
     */
    protected void reconfigure2(PdpMessage newMessage) {
        if (newMessage.getClass() != message.getClass()) {
            throw new IllegalArgumentException("expecting " + message.getClass().getSimpleName() + " instead of "
                            + newMessage.getClass().getSimpleName());
        }

        logger.info("reconfiguring {} with new message", getName());

        synchronized (params.getModifyLock()) {
            if (svcmgr.isAlive()) {
                stopPublishing(false);
                message = newMessage;
                startPublishing();

            } else {
                message = newMessage;
            }
        }
    }

    @Override
    public boolean isPublishing() {
        return svcmgr.isAlive();
    }

    @Override
    public void startPublishing() {
        if (listener == null) {
            throw new IllegalStateException("listener has not been set");
        }

        synchronized (params.getModifyLock()) {
            if (svcmgr.isAlive()) {
                logger.info("{} is already publishing", getName());

            } else {
                resetRetryCount();
                svcmgr.start();
            }
        }
    }

    @Override
    public void stopPublishing() {
        stopPublishing(true);
    }

    private void stopPublishing(boolean removeFromQueue) {
        synchronized (params.getModifyLock()) {
            if (svcmgr.isAlive()) {
                svcmgr.stop();

                if (removeFromQueue) {
                    token.replaceItem(null);
                    token = null;
                }
            }
        }
    }

    /**
     * Enqueues the current message with the publisher, putting it into the queue token,
     * if possible. Otherwise, it adds a new token to the queue.
     */
    private void enqueue() {
        if (token != null && token.replaceItem(message) != null) {
            // took the other's place in the queue - continue using the token
            return;
        }

        // couldn't take the other's place - add our own token to the queue
        token = new QueueToken<>(message);
        params.getPdpPublisher().enqueue(token);
    }

    /**
     * Resets the retry count.
     */
    public void resetRetryCount() {
        retryCount = 0;
    }

    /**
     * Bumps the retry count.
     *
     * @return {@code true} if successful, {@code false} if the limit has been reached
     */
    public boolean bumpRetryCount() {
        if (retryCount >= params.getMaxRetryCount()) {
            return false;
        }

        retryCount++;
        return true;
    }

    /**
     * Processes a response received from the PDP.
     *
     * @param infra infrastructure on which the response was received
     * @param topic topic on which the response was received
     * @param response the response
     */
    private void processResponse(CommInfrastructure infra, String topic, PdpStatus response) {

        synchronized (params.getModifyLock()) {
            String pdpName = response.getName();

            if (!svcmgr.isAlive()) {
                // this particular request must have been discarded
                return;
            }

            /*
             * Note: don't have to verify that getResponse() != null, as this code won't
             * even be reached if that's the case.
             */
            if (!message.getRequestId().equals(response.getResponse().getResponseTo())) {
                logger.info("{} ignore old response via {} {}: {}", getName(), infra, topic,
                                response.getResponse().getResponseTo());
                return;
            }

            svcmgr.stop();

            String reason = checkResponse(response);
            if (reason != null) {
                logger.info("{} PDP data mismatch via {} {}: {}", getName(), infra, topic, reason);
                listener.failure(pdpName, reason);
                return;
            }

            logger.info("{} successful", getName());
            listener.success(pdpName, response);
        }
    }

    /**
     * Handles a timeout.
     *
     * @param timerName the timer timer
     */
    private void handleTimeout(String timerName) {

        synchronized (params.getModifyLock()) {
            if (!svcmgr.isAlive()) {
                // this particular request must have been discarded
                return;
            }

            stopPublishing();

            if (!bumpRetryCount()) {
                logger.info("{} timeout {} - retry count {} exhausted", getName(), timerName, retryCount);
                listener.retryCountExhausted(this);
                return;
            }

            // re-publish
            logger.info("{} timeout - re-publish count {}", getName(), retryCount);

            // startPublishing() resets the count, so save & restore it here
            int count = retryCount;
            startPublishing();
            retryCount = count;
        }
    }

    /**
     * Verifies that the name is not null. Also verifies that it matches the name in the
     * message, if the message has a name.
     */
    @Override
    public String checkResponse(PdpStatus response) {
        if (response.getName() == null) {
            return "null PDP name";
        }

        if (message.getName() != null && !message.getName().equals(response.getName())) {
            return "PDP name does not match";
        }

        return null;
    }

    /**
     * Just returns an empty list.
     */
    @Override
    public Collection<ToscaConceptIdentifier> getUndeployPolicies() {
        return Collections.emptyList();
    }
}
