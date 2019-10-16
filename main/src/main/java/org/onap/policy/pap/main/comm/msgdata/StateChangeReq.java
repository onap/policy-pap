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

import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.parameters.RequestParams;

/**
 * Wraps a STATE-CHANGE.
 */
public class StateChangeReq extends RequestImpl {

    /**
     * Constructs the object, and validates the parameters.
     *
     * @param params configuration parameters
     * @param name the request name, used for logging purposes
     * @param message the initial message
     *
     * @throws IllegalArgumentException if a required parameter is not set
     */
    public StateChangeReq(RequestParams params, String name, PdpStateChange message) {
        super(params, name, message);
    }

    @Override
    public PdpStateChange getMessage() {
        return (PdpStateChange) super.getMessage();
    }

    @Override
    public String checkResponse(PdpStatus response) {
        String reason = super.checkResponse(response);
        if (reason != null) {
            return reason;
        }

        PdpStateChange message = getMessage();
        if (response.getState() != message.getState()) {
            return "state is " + response.getState() + ", but expected " + message.getState();
        }

        return null;
    }

    @Override
    public boolean isSameContent(Request other) {
        if (!(other instanceof StateChangeReq)) {
            return false;
        }

        PdpStateChange message = (PdpStateChange) other.getMessage();
        return (getMessage().getState() == message.getState());
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
