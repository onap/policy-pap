/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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

package org.onap.policy.pap.main.rest;

import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.pap.concepts.PdpGroupStateChangeResponse;
import org.onap.policy.models.pdp.enums.PdpState;

/**
 * Provider for PAP component to change state of PDP group.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpGroupStateChangeProvider {

    /**
     * Changes state of a PDP group.
     *
     * @param groupName name of the PDP group
     * @param version version of the PDP group
     * @param pdpGroupState state of the PDP group
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupStateChangeResponse> changeGroupState(final String groupName,
            final String version, final PdpState pdpGroupState) {

        /*
         * TODO Check preconditions - return error if any.
         */

        /*
         * TODO Change state - sending state change messages to PDPs and arranging for listeners to complete the state
         * change actions (in the background).
         */

        /*
         * TODO Return error if unable to send state change to all PDPs.
         */

        return Pair.of(Response.Status.OK, new PdpGroupStateChangeResponse());
    }
}
