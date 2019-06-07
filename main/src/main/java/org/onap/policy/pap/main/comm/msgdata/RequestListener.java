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

import org.onap.policy.models.pdp.concepts.PdpStatus;

/**
 * Listener for request events.
 */
public interface RequestListener {

    /**
     * Indicates that an invalid response was received from a PDP.
     *
     * @param pdpName name of the PDP from which the response was received
     * @param reason the reason for the mismatch
     */
    public void failure(String pdpName, String reason);

    /**
     * Indicates that a successful response was received from a PDP.
     *
     * @param response response that was received
     */
    public void success(PdpStatus response);

    /**
     * Indicates that the retry count was exhausted.
     */
    public void retryCountExhausted();
}
