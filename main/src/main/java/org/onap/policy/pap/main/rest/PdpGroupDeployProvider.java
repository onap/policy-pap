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

package org.onap.policy.pap.main.rest;

import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.pap.concepts.PdpGroup;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pap.concepts.PdpPolicies;

/**
 * Provider for PAP component to deploy PDP groups.
 */
public class PdpGroupDeployProvider {

    /**
     * Deploys or updates a PDP group.
     *
     * @param group PDP group configuration
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeployResponse> deployGroup(PdpGroup group) {

        /*
         * TODO Lock for updates - return error if already locked.
         */

        /*
         * TODO Make updates - sending initial messages to PDPs and arranging for
         * listeners to complete the deployment actions (in the background). The final
         * step for the listener is to unlock.
         */

        /*
         * TODO Return error if unable to send updates to all PDPs.
         */

        return Pair.of(Response.Status.OK, new PdpGroupDeployResponse());
    }

    /**
     * Deploys or updates PDP policies.
     *
     * @param policies PDP policies
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeployResponse> deployPolicies(PdpPolicies policies) {

        /*
         * TODO Lock for updates - return error if already locked.
         */

        /*
         * TODO Make updates - sending initial messages to PDPs and arranging for
         * listeners to complete the deployment actions (in the background). The final
         * step for the listener is to unlock.
         */

        /*
         * TODO Return error if unable to send updates to all PDPs.
         */

        return Pair.of(Response.Status.OK, new PdpGroupDeployResponse());
    }
}
