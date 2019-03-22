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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClientException;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.pap.main.PolicyPapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes messages to a topic. Maintains a queue of references to data that is to be
 * published. Once the publisher removes a reference from a queue, it sets it to
 * {@link null} to indicate that it is being processed. Until it has been set to
 * {@link null}, clients are free to atomically update the reference to new values, thus
 * maintaining their place in the queue.
 */
public class Publisher implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(Publisher.class);

    private static Coder coder = new StandardCoder();

    /**
     * Name of the topic to which this publishes.
     */
    private final String topic;

    /**
     * Used to send to the topic.
     */
    private final TopicSinkClient client;

    /**
     * Request queue. The references may contain {@code null}.
     */
    private final BlockingQueue<QueueToken<PdpMessage>> queue = new LinkedBlockingQueue<>();

    /**
     * Set to {@code true} to cause the publisher to stop running.
     */
    private volatile boolean stopNow = false;

    /**
     * Constructs the object.
     *
     * @param topic name of the topic to which to publish
     * @throws PolicyPapException if the topic sink does not exist
     */
    public Publisher(String topic) throws PolicyPapException {
        this.topic = topic;

        try {
            this.client = new TopicSinkClient(topic);
        } catch (TopicSinkClientException e) {
            throw new PolicyPapException(e);
        }
    }

    /**
     * Stops the publisher, if it's running.
     */
    public void stop() {
        stopNow = true;

        // add an empty reference so the thread doesn't block on the queue
        queue.add(new QueueToken<>(null));
    }

    /**
     * Adds an item to the queue. The referenced objects are assumed to be POJOs and will
     * be converted to JSON via the {@link StandardCoder} prior to publishing.
     *
     * @param ref reference to the message to be published
     */
    public void enqueue(QueueToken<PdpMessage> ref) {
        queue.add(ref);
    }

    /**
     * Continuously publishes items in the queue until {@link #stop()} is invoked.
     */
    @Override
    public void run() {
        for (;;) {
            QueueToken<PdpMessage> token = getNext();

            if (stopNow) {
                // unblock any other publisher threads
                queue.offer(new QueueToken<>(null));
                break;
            }

            PdpMessage data = token.replaceItem(null);
            if (data == null) {
                continue;
            }

            process(data);
        }
    }

    /**
     * Gets the next item from the queue. If the thread is interrupted, then it sets
     * {@link #stopNow}.
     *
     * @return the next item, or a reference containing {@code null} if this is
     *         interrupted
     */
    private QueueToken<PdpMessage> getNext() {
        try {
            return queue.take();

        } catch (InterruptedException e) {
            logger.warn("Publisher stopping due to interrupt");
            stopNow = true;
            Thread.currentThread().interrupt();
            return new QueueToken<>(null);
        }
    }

    /**
     * Processes a request.
     *
     * @param req the request to process
     */
    private void process(PdpMessage req) {
        // convert to json
        String json;
        try {
            json = coder.encode(req);
        } catch (CoderException e) {
            logger.warn("could not encode request: {}", req, e);
            return;
        }

        // publish it
        try {
            client.send(json);
        } catch (RuntimeException e) {
            logger.warn("could not publish message to {}: {}", topic, json, e);
            return;
        }
    }
}
