/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.main.comm;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.common.utils.services.ServiceManager;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request data, the content of which may be changed at any point, possibly triggering a
 * restart of the publishing.
 */
public abstract class RequestData {
    private static final Logger logger = LoggerFactory.getLogger(RequestData.class);

    /**
     * Name with which this data is associated, used for logging purposes.
     */
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private String name;

    /**
     * The configuration parameters.
     */
    private final Params params;

    /**
     * Current retry count.
     */
    private int retryCount = 0;

    /**
     * Used to register/unregister the listener and the timer.
     */
    private ServiceManager svcmgr;

    /**
     * Wrapper for the message that is currently being published (i.e., {@link #update} or
     * {@link #stateChange}.
     */
    @Getter(AccessLevel.PROTECTED)
    private MessageWrapper wrapper;

    /**
     * Used to cancel a timer.
     */
    private TimerManager.Timer timer;

    /**
     * Token that is placed on the queue.
     */
    private QueueToken<PdpMessage> token = null;


    /**
     * Constructs the object.
     *
     * @param params configuration parameters
     *
     * @throws IllegalArgumentException if a required parameter is not set
     */
    public RequestData(Params params) {
        params.validate();

        this.params = params;
    }

    /**
     * Starts the publishing process, registering any listeners or timeout handlers, and
     * adding the request to the publisher queue.
     */
    public void startPublishing() {

        synchronized (params.getModifyLock()) {
            if (!svcmgr.isAlive()) {
                svcmgr.start();
            }
        }
    }

    /**
     * Unregisters the listener and cancels the timer.
     */
    protected void stopPublishing() {
        if (svcmgr.isAlive()) {
            svcmgr.stop();
        }
    }

    /**
     * Configures the fields based on the {@link #message} type.
     *
     * @param newWrapper the new message wrapper
     */
    protected void configure(MessageWrapper newWrapper) {

        wrapper = newWrapper;

        resetRetryCount();

        TimerManager timerManager = wrapper.getTimers();
        String msgType = wrapper.getType();
        String reqid = wrapper.getMessage().getRequestId();

        /*
         * We have to configure the service manager HERE, because it's name changes if the
         * message class changes.
         */

        // @formatter:off
        this.svcmgr = new ServiceManager(name + " " + msgType)
                        .addAction("listener",
                            () -> params.getResponseDispatcher().register(reqid, this::processResponse),
                            () -> params.getResponseDispatcher().unregister(reqid))
                        .addAction("timer",
                            () -> timer = timerManager.register(name, this::handleTimeout),
                            () -> timer.cancel())
                        .addAction("enqueue",
                            () -> enqueue(),
                            () -> {
                                // nothing to "stop"
                            });
        // @formatter:on
    }

    /**
     * Enqueues the current message with the publisher, putting it into the queue token,
     * if possible. Otherwise, it adds a new token to the queue.
     */
    private void enqueue() {
        PdpMessage message = wrapper.getMessage();
        if (token != null && token.replaceItem(message) != null) {
            // took the other's place in the queue - continue using the token
            return;
        }

        // couldn't take the other's place - add our own token to the queue
        token = new QueueToken<>(message);
        params.getPublisher().enqueue(token);
    }

    /**
     * Resets the retry count.
     */
    protected void resetRetryCount() {
        retryCount = 0;
    }

    /**
     * Bumps the retry count.
     *
     * @return {@code true} if successful, {@code false} if the limit has been reached
     */
    protected boolean bumpRetryCount() {
        if (retryCount >= wrapper.getMaxRetryCount()) {
            return false;
        }

        retryCount++;
        return true;
    }

    /**
     * Indicates that the retry count was exhausted. The default method simply invokes
     * {@link #allCompleted()}.
     */
    protected void retryCountExhausted() {
        // remove this request data from the PDP request map
        allCompleted();
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
            if (!svcmgr.isAlive()) {
                // this particular request must have been discarded
                return;
            }

            stopPublishing();

            if (!isActive()) {
                return;
            }

            String reason = wrapper.checkResponse(response);
            if (reason != null) {
                logger.info("{} PDP data mismatch: {}", getName(), reason);
                // TODO comm.stopPdp(response.getName(), reason);
            }

            logger.info("{} {} successful", getName(), wrapper.getType());
            wrapper.completed();
        }
    }

    /**
     * Handles a timeout.
     */
    private void handleTimeout() {

        synchronized (params.getModifyLock()) {
            if (!svcmgr.isAlive()) {
                // this particular request must have been discarded
                return;
            }

            stopPublishing();

            if (!isActive()) {
                return;
            }

            if (isInQueue()) {
                // haven't published yet - just leave it in the queue and reset counts
                logger.info("{} timeout - request still in the queue", getName());
                resetRetryCount();
                startPublishing();
                return;
            }

            if (!bumpRetryCount()) {
                logger.info("{} timeout - retry count exhausted", getName());
                retryCountExhausted();
                return;
            }

            // re-publish
            logger.info("{} timeout - re-publish", getName());
            startPublishing();
        }
    }

    /**
     * Determines if the current message is still in the queue. Assumes that
     * {@link #startPublishing()} has been invoked and thus {@link #token} has been
     * initialized.
     *
     * @return {@code true} if the current message is in the queue, {@code false}
     *         otherwise
     */
    private boolean isInQueue() {
        return (token.get() == wrapper.getMessage());
    }

    /**
     * Determines if this request data is still active.
     *
     * @return {@code true} if this request is active, {@code false} otherwise
     */
    protected abstract boolean isActive();

    /**
     * Indicates that this entire request has completed.
     */
    protected abstract void allCompleted();

    /**
     * Wraps a message, providing methods appropriate to the message type.
     */
    protected static interface MessageWrapper {

        /**
         * Gets the wrapped message.
         *
         * @return the wrapped message
         */
        PdpMessage getMessage();

        /**
         * Gets a string, suitable for logging, identifying the message type.
         *
         * @return the message type
         */
        String getType();

        /**
         * Indicates that processing of this particular message has completed
         * successfully.
         */
        void completed();

        /**
         * Checks the response to ensure it is as expected.
         *
         * @param response the response to check
         * @return an error message, if a fatal error has occurred, {@code null} otherwise
         */
        String checkResponse(PdpStatus response);

        /**
         * Gets the maximum retry count for the particular message type.
         *
         * @return the maximum retry count
         */
        int getMaxRetryCount();

        /**
         * Gets the timer manager for the particular message type.
         *
         * @return the timer manager
         */
        TimerManager getTimers();
    }

    /**
     * Parameters needed to create a {@link RequestData}.
     */
    public static class Params {
        private Publisher publisher;
        private RequestIdDispatcher<PdpStatus> responseDispatcher;
        private Object modifyLock;

        public Publisher getPublisher() {
            return publisher;
        }

        public Params setPublisher(Publisher publisher) {
            this.publisher = publisher;
            return this;
        }

        public RequestIdDispatcher<PdpStatus> getResponseDispatcher() {
            return responseDispatcher;
        }

        public Params setResponseDispatcher(RequestIdDispatcher<PdpStatus> responseDispatcher) {
            this.responseDispatcher = responseDispatcher;
            return this;
        }

        public Object getModifyLock() {
            return modifyLock;
        }

        public Params setModifyLock(Object modifyLock) {
            this.modifyLock = modifyLock;
            return this;
        }

        /**
         * Validates the parameters.
         */
        public void validate() {
            if (publisher == null) {
                throw new IllegalArgumentException("missing publisher");
            }

            if (responseDispatcher == null) {
                throw new IllegalArgumentException("missing responseDispatcher");
            }

            if (modifyLock == null) {
                throw new IllegalArgumentException("missing modifyLock");
            }
        }
    }
}
