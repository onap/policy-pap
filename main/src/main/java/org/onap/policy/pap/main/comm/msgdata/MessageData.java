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

package org.onap.policy.pap.main.comm.msgdata;

import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.comm.TimerManager;


/**
 * Wraps a message, providing methods appropriate to the message type.
 */
public abstract class MessageData {
    private final PdpMessage message;
    private final int maxRetries;
    private final TimerManager timers;

    /**
     * Constructs the object.
     *
     * @param message message to be wrapped by this
     * @param maxRetries max number of retries
     * @param timers the timer manager for messages of this type
     */
    public MessageData(PdpMessage message, int maxRetries, TimerManager timers) {
        this.message = message;
        this.maxRetries = maxRetries;
        this.timers = timers;
    }

    /**
     * Gets the wrapped message.
     *
     * @return the wrapped message
     */
    public PdpMessage getMessage() {
        return message;
    }

    /**
     * Gets a string, suitable for logging, identifying the message type.
     *
     * @return the message type
     */
    public String getType() {
        return message.getClass().getSimpleName();
    }

    /**
     * Gets the maximum retry count for the particular message type.
     *
     * @return the maximum retry count
     */
    public int getMaxRetryCount() {
        return maxRetries;
    }

    /**
     * Gets the timer manager for the particular message type.
     *
     * @return the timer manager
     */
    public TimerManager getTimers() {
        return timers;
    }

    /**
     * Indicates that the response did not match what was expected.
     *
     * @param reason the reason for the mismatch
     */
    public abstract void mismatch(String reason);

    /**
     * Indicates that processing of this particular message has completed successfully.
     */
    public abstract void completed();

    /**
     * Checks the response to ensure it is as expected.
     *
     * @param response the response to check
     * @return an error message, if a fatal error has occurred, {@code null} otherwise
     */
    public abstract String checkResponse(PdpStatus response);
}
