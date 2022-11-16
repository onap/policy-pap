/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.pap.main.rest.e2e;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.Getter;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicListener;
import org.onap.policy.common.endpoints.event.comm.bus.NoopTopicSource;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpResponseDetails;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpMessageType;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context for end-to-end tests.
 */
public class End2EndContext {
    private static final Logger logger = LoggerFactory.getLogger(End2EndContext.class);

    /**
     * Message placed onto a queue to indicate that a PDP has nothing more to do.
     */
    private static final String DONE = "";

    /**
     * Time, in milliseconds, to wait for everything to complete.
     */
    private static final long WAIT_MS = 10000;

    /**
     * Messages to be sent to PAP. Messages are removed from the queue by the ToPapThread
     * and directly handed off to the NOOP source.
     */
    private final BlockingQueue<String> toPap = new LinkedBlockingQueue<>();

    /**
     * Messages to be sent to the PDPs. Messages are removed from the queue by the
     * ToPdpThread and are given to each PDP to handle.
     */
    private final BlockingQueue<String> toPdps = new LinkedBlockingQueue<>();

    /**
     * List of simulated PDPs.
     */
    @Getter
    private final List<PseudoPdp> pdps = new ArrayList<>();

    /**
     * PAP's topic source.
     */
    private final NoopTopicSource toPapTopic;

    /**
     * Decodes messages read from the {@link #toPdps} queue and dispatches them to the
     * appropriate handler.
     */
    private final MessageTypeDispatcher dispatcher;

    /**
     * Thread that passes messages to PAP.
     */
    private final ToPapThread toPapThread;

    /**
     * Thread that passes messages to PDPs.
     */
    private final ToPdpsThread toPdpsThread;

    /**
     * {@code True} if started, {@code false} if stopped.
     */
    private boolean running = false;

    /**
     * Exception thrown by a coder. Should be {@code null} if all is OK.
     */
    private volatile CoderException exception = null;

    /**
     * Listener for messages written to the PDP-PAP topic.
     */
    private TopicListener topicListener = (infra, topic, text) -> toPdps.add(text);

    private String topicPolicyPdpPap = "pdp-pap-topic";

    /**
     * Constructs the object.
     */
    public End2EndContext() {
        toPapTopic = TopicEndpointManager.getManager().getNoopTopicSource(topicPolicyPdpPap);

        TopicEndpointManager.getManager().getNoopTopicSink(topicPolicyPdpPap).register(topicListener);

        dispatcher = new MessageTypeDispatcher("messageName");
        dispatcher.register(PdpMessageType.PDP_UPDATE.name(), new UpdateListener());
        dispatcher.register(PdpMessageType.PDP_STATE_CHANGE.name(), new ChangeListener());

        toPapThread = new ToPapThread();
        toPdpsThread = new ToPdpsThread();
    }

    /**
     * Starts the threads that read the "DMaaP" queues..
     */
    public void startThreads() {
        if (running) {
            throw new IllegalStateException("already running");
        }

        for (Thread thread : new Thread[] {toPapThread, toPdpsThread}) {
            thread.setDaemon(true);
            thread.start();
        }

        running = true;
    }

    /**
     * Waits for the threads to shut down.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    public void await() throws InterruptedException {
        toPapThread.join(WAIT_MS);
        assertFalse(toPapThread.isAlive());

        PdpModifyRequestMap map = Registry.get(PapConstants.REG_PDP_MODIFY_MAP);
        assertTrue(map.isEmpty());

        // no more requests, thus we can tell the other thread to stop
        toPdps.add(DONE);

        toPdpsThread.join(WAIT_MS);
        assertFalse(toPapThread.isAlive());

        // nothing new should have been added to the PAP queue
        assertTrue(toPap.isEmpty());

        assertNull(exception);
    }

    /**
     * Stops the threads and shuts down the PAP Activator, rest services, and topic end
     * points.
     */
    public void stop() {
        if (!running) {
            throw new IllegalStateException("not running");
        }

        running = false;

        // queue up a "done" message for each PDP
        toPdps.clear();
        pdps.forEach(pdp -> toPdps.add(DONE));

        // queue up a "done" message for each PDP
        toPap.clear();
        pdps.forEach(pdp -> toPap.add(DONE));

        TopicEndpointManager.getManager().getNoopTopicSink(topicPolicyPdpPap).unregister(topicListener);
    }

    /**
     * Adds a simulated PDP. This must be called before {@link #startThreads()} is
     * invoked.
     *
     * @param pdpName PDP name
     * @param pdpType PDP type
     * @return a new, simulated PDP
     * @throws IllegalStateException if {@link #startThreads()} has already been invoked
     */
    public PseudoPdp addPdp(String pdpName, String pdpType) {
        if (running) {
            throw new IllegalStateException("not running");
        }

        PseudoPdp pdp = new PseudoPdp(pdpName);
        pdps.add(pdp);

        return pdp;
    }

    /**
     * Thread that reads messages from the {@link End2EndContext#toPdps} queue and
     * dispatches them to each PDP. This thread terminates as soon as it sees a
     * {@link End2EndContext#DONE} message.
     */
    private class ToPdpsThread extends Thread {
        @Override
        public void run() {
            for (;;) {
                String text;
                try {
                    text = toPdps.take();
                } catch (InterruptedException e) {
                    logger.warn("{} interrupted", ToPdpsThread.class.getName(), e);
                    Thread.currentThread().interrupt();
                    break;
                }

                if (DONE.equals(text)) {
                    break;
                }

                dispatcher.onTopicEvent(CommInfrastructure.NOOP, topicPolicyPdpPap, text);
            }
        }
    }

    /**
     * Thread that reads messages from the {@link End2EndContext#toPap} queue and passes
     * them to the PAP's topic source. This thread terminates once it sees a
     * {@link End2EndContext#DONE} message <i>for each PDP</i>.
     */
    private class ToPapThread extends Thread {

        @Override
        public void run() {
            // pretend we received DONE from PDPs that are already finished
            long ndone = pdps.stream().filter(pdp -> pdp.finished).count();

            while (ndone < pdps.size()) {
                String text;
                try {
                    text = toPap.take();
                } catch (InterruptedException e) {
                    logger.warn("{} interrupted", ToPapThread.class.getName(), e);
                    Thread.currentThread().interrupt();
                    break;
                }

                if (DONE.equals(text)) {
                    ++ndone;

                } else {
                    toPapTopic.offer(text);
                }
            }
        }
    }

    /**
     * Listener for PdpUpdate messages received from PAP. Invokes
     * {@link PseudoPdp#handle(PdpUpdate)} for each PDP.
     */
    private class UpdateListener extends ScoListener<PdpUpdate> {
        public UpdateListener() {
            super(PdpUpdate.class);
        }

        @Override
        public void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco, PdpUpdate update) {
            pdps.forEach(pdp -> pdp.handle(update));
        }
    }

    /**
     * Listener for PdpStateChange messages received from PAP. Invokes
     * {@link PseudoPdp#handle(PdpStateChange)} for each PDP.
     */
    private class ChangeListener extends ScoListener<PdpStateChange> {
        public ChangeListener() {
            super(PdpStateChange.class);
        }

        @Override
        public void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco,
                        PdpStateChange change) {
            pdps.forEach(pdp -> pdp.handle(change));
        }
    }

    /**
     * Simulated PDP. Each PDP handles messages from the PAP and can return replies in
     * response to those messages. The replies must be queued up before
     * {@link End2EndContext#startThreads()} is invoked.
     */
    public class PseudoPdp {
        private final String name;

        private final Coder coder = new StandardCoder();
        private final Queue<PdpStatus> replies = new LinkedList<>();

        /**
         * Messages that this PDP has handled.
         */
        @Getter
        private final Queue<PdpMessage> handled = new ConcurrentLinkedQueue<>();

        private volatile String group = null;
        private volatile String subgroup = null;

        private volatile boolean finished = true;

        /**
         * Constructs the object.
         *
         * @param name PDP name
         */
        private PseudoPdp(String name) {
            this.name = name;
        }

        public PseudoPdp setGroup(String group) {
            this.group = group;
            return this;
        }

        public PseudoPdp setSubgroup(String subgroup) {
            this.subgroup = subgroup;
            return this;
        }

        /**
         * Adds a reply to the list of replies that will be returned in response to
         * messages from the PAP.
         *
         * @param reply reply to be added to the list
         * @return this PDP
         */
        public PseudoPdp addReply(PdpStatus reply) {
            replies.add(reply);
            finished = false;
            return this;
        }

        /**
         * Handles an UPDATE message, recording the information extracted from the message
         * and queuing up a reply, if any.
         *
         * @param message message that was received from PAP
         */
        private void handle(PdpUpdate message) {
            if (message.appliesTo(name, group, subgroup)) {
                handled.add(message);
                group = message.getPdpGroup();
                subgroup = message.getPdpSubgroup();
                reply(message);
            }
        }

        /**
         * Handles a STAT-CHANGE message. Queues up a reply, if any.
         *
         * @param message message that was received from PAP
         */
        private void handle(PdpStateChange message) {
            if (message.appliesTo(name, group, subgroup)) {
                handled.add(message);
                reply(message);
            }
        }

        /**
         * Queues up the next reply. If there are no more replies, then it queues up a
         * {@link End2EndContext#DONE} message.
         *
         * @param message the message to which a reply should be sent
         */
        private void reply(PdpMessage message) {
            PdpStatus status = replies.poll();
            if (status == null) {
                return;
            }

            PdpResponseDetails response = new PdpResponseDetails();
            response.setResponseTo(message.getRequestId());
            status.setResponse(response);

            toPap.add(toJson(status));

            if (replies.isEmpty()) {
                finished = true;
                toPap.add(DONE);
            }
        }

        /**
         * Converts a message to JSON.
         *
         * @param status message to be converted
         * @return JSON representation of the message
         */
        private String toJson(PdpStatus status) {
            try {
                return coder.encode(status);

            } catch (CoderException e) {
                exception = e;
                return DONE;
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
