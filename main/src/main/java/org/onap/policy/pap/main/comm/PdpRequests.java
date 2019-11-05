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

import java.util.ArrayDeque;
import java.util.Queue;
import lombok.Getter;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.pap.main.comm.msgdata.Request;
import org.onap.policy.pap.main.notification.PolicyNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks requests associated with a particular PDP. Requests may be broadcast requests or
 * singleton requests (i.e., destined for a single PDP).
 */
public class PdpRequests {
    private static final Logger logger = LoggerFactory.getLogger(PdpRequests.class);

    /**
     * Name of the PDP with which the requests are associated.
     */
    @Getter
    private final String pdpName;

    /**
     * Notifier for policy update completions.
     */
    @Getter
    private final PolicyNotifier notifier;

    /**
     * Queue of requests to be published. The first item in the queue is currently being
     * published. Currently, there will be at most three messages in the queue: PASSIVE,
     * ACTIVE, and UPDATE.
     */
    private final Queue<Request> requests = new ArrayDeque<>(3);


    /**
     * Constructs the object.
     *
     * @param pdpName name of the PDP with which the requests are associated
     */
    public PdpRequests(String pdpName, PolicyNotifier notifier) {
        this.pdpName = pdpName;
        this.notifier = notifier;
    }

    /**
     * Adds a singleton request.
     *
     * @param request the request to be added
     */
    public void addSingleton(Request request) {

        request.setNotifier(notifier);

        if (request.getMessage().getName() == null) {
            throw new IllegalArgumentException("unexpected broadcast for " + pdpName);
        }

        // try to reconfigure an existing request with the new message
        PdpMessage newMessage = request.getMessage();
        for (Request req : requests) {
            if (req.reconfigure(newMessage)) {
                return;
            }
        }

        // couldn't reconfigure an existing request - must add the new one

        requests.add(request);

        if (requests.peek() == request) {
            // this is the first request in the queue - publish it
            request.startPublishing();
        }
    }

    /**
     * Stops all publishing and removes this PDP from any broadcast messages.
     */
    public void stopPublishing() {
        Request request = requests.peek();
        if (request != null) {
            request.stopPublishing();
        }
    }

    /**
     * Starts publishing the next request in the queue.
     *
     * @param request the request that just completed
     * @return {@code true} if there is another request in the queue, {@code false} if all
     *         requests for this PDP have been processed
     */
    public boolean startNextRequest(Request request) {
        if (request != requests.peek()) {
            // not the request we're looking for
            return !requests.isEmpty();
        }

        // remove the completed request
        requests.remove();

        // start publishing next request, but don't remove it from the queue
        Request nextRequest = requests.peek();
        if (nextRequest == null) {
            logger.info("{} has no more requests", pdpName);
            return false;
        }

        logger.info("{} start publishing next request", pdpName);

        nextRequest.startPublishing();
        return true;
    }
}
