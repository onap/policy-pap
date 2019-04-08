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

import lombok.Getter;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.pap.main.comm.msgdata.Request;

/**
 * Tracks requests associated with a particular PDP. Requests may be broadcast requests or
 * singleton requests (i.e., destined for a single PDP).
 */
public class PdpRequests {

    /**
     * The maximum request priority + 1.
     */
    private static final int MAX_PRIORITY = 2;

    /**
     * Name of the PDP with which the requests are associated.
     */
    @Getter
    private final String pdpName;

    /**
     * Index of request currently being published.
     */
    private int curIndex = 0;

    /**
     * Singleton requests. Items may be {@code null}.
     */
    private Request[] singletons = new Request[MAX_PRIORITY];

    /**
     * Last group name to which the associated PDP was assigned.
     */
    @Getter
    private String lastGroupName;


    /**
     * Constructs the object.
     *
     * @param pdpName name of the PDP with which the requests are associated
     */
    public PdpRequests(String pdpName) {
        this.pdpName = pdpName;
    }

    /**
     * Records the group information from the request.
     *
     * @param request the request from which to extract the group information
     */
    private void recordGroup(Request request) {
        PdpMessage message = request.getMessage();
        if (message instanceof PdpUpdate) {
            lastGroupName = message.getPdpGroup();
        }
    }

    /**
     * Adds a singleton request.
     *
     * @param request the request to be added
     */
    public void addSingleton(Request request) {

        if (request.getMessage().getName() == null) {
            throw new IllegalArgumentException("unexpected broadcast for " + pdpName);
        }

        recordGroup(request);

        if (checkExisting(request)) {
            // have an existing request that's similar - discard this request
            return;
        }

        // no existing request of this type

        int priority = request.getPriority();
        singletons[priority] = request;

        // stop publishing anything of a lower priority
        QueueToken<PdpMessage> token = stopPublishingLowerPriority(priority);

        // start publishing if nothing of higher priority
        if (higherPrioritySingleton(priority)) {
            return;
        }

        curIndex = priority;
        request.startPublishing(token);
    }

    /**
     * Checks for an existing request.
     *
     * @param request the request of interest
     * @return {@code true} if a similar request already exists, {@code false} otherwise
     */
    private boolean checkExisting(Request request) {

        return checkExistingSingleton(request);
    }

    /**
     * Checks for an existing singleton request.
     *
     * @param request the request of interest
     * @return {@code true} if a similar singleton request already exists, {@code false}
     *         otherwise
     */
    private boolean checkExistingSingleton(Request request) {

        Request exsingle = singletons[request.getPriority()];

        if (exsingle == null) {
            return false;
        }

        if (exsingle.isSameContent(request)) {
            // unchanged from existing request
            return true;
        }

        // reconfigure the existing request
        PdpMessage message = request.getMessage();
        exsingle.reconfigure(message, null);

        // still have a singleton in the queue for this request
        return true;
    }

    /**
     * Stops all publishing and removes this PDP from any broadcast messages.
     */
    public void stopPublishing() {
        // stop singletons
        for (int x = 0; x < MAX_PRIORITY; ++x) {
            Request single = singletons[x];

            if (single != null) {
                singletons[x] = null;
                single.stopPublishing();
            }
        }
    }

    /**
     * Stops publishing requests of a lower priority than the specified priority.
     *
     * @param priority priority of interest
     * @return the token that was being used to publish a lower priority request
     */
    private QueueToken<PdpMessage> stopPublishingLowerPriority(int priority) {

        // stop singletons
        for (int x = 0; x < priority; ++x) {
            Request single = singletons[x];

            if (single != null) {
                QueueToken<PdpMessage> token = single.stopPublishing(false);
                if (token != null) {
                    // found one that was publishing
                    return token;
                }
            }
        }

        return null;
    }

    /**
     * Starts publishing the next request in the queue.
     *
     * @param request the request that just completed
     * @return {@code true} if there is another request in the queue, {@code false} if all
     *         requests for this PDP have been processed
     */
    public boolean startNextRequest(Request request) {
        if (!zapRequest(curIndex, request)) {
            // not at curIndex - look for it in other indices
            for (int x = 0; x < MAX_PRIORITY; ++x) {
                if (zapRequest(x, request)) {
                    break;
                }
            }
        }

        // find/start the highest priority request
        for (curIndex = MAX_PRIORITY - 1; curIndex >= 0; --curIndex) {
            if (singletons[curIndex] != null) {
                singletons[curIndex].startPublishing();
                return true;
            }
        }

        curIndex = 0;

        return false;
    }

    /**
     * Zaps request pointers, if the request appears at the given index.
     *
     * @param index index to examine
     * @param request request of interest
     * @return {@code true} if a request pointer was zapped, {@code false} if the request
     *         did not appear at the given index
     */
    private boolean zapRequest(int index, Request request) {
        if (singletons[index] == request) {
            singletons[index] = null;
            return true;
        }

        return false;
    }

    /**
     * Determines if any singleton request, with a higher priority, is associated with the
     * PDP.
     *
     * @param priority priority of interest
     *
     * @return {@code true} if the PDP has a singleton, {@code false} otherwise
     */
    private boolean higherPrioritySingleton(int priority) {
        for (int x = priority + 1; x < MAX_PRIORITY; ++x) {
            if (singletons[x] != null) {
                return true;
            }
        }

        return false;
    }
}
