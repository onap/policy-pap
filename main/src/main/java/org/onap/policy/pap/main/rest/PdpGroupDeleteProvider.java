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
import org.onap.policy.models.pap.concepts.PdpGroupDeleteResponse;

/**
 * Provider for PAP component to delete PDP groups.
 */
public class PdpGroupDeleteProvider {

    /**
     * Deletes a PDP group.
     *
     * @param groupName name of the PDP group to be deleted
     * @param version group version to delete; may be {@code null} if the group has only
     *        one version
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeleteResponse> deleteGroup(String groupName, String version) {

        /*
         * TODO Lock for updates - return error if already locked.
         */

        /*
         * TODO Make updates - sending initial messages to PDPs and arranging for
         * listeners to complete the deletion actions (in the background). The final step
         * for the listener is to unlock.
         */

        /*
         * TODO Return error if unable to send updates to all PDPs.
         */

        return Pair.of(Response.Status.OK, new PdpGroupDeleteResponse());
    }

    /**
     * Deletes a PDP policy.
     *
     * @param policyId id of the policy to be deleted
     * @param version policy version to delete; may be {@code null} if the policy has only
     *        one version
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeleteResponse> deletePolicy(String policyId, String version) {

        /*
         * TODO Lock for updates - return error if already locked.
         */

        /*
         * TODO Make updates - sending initial messages to PDPs and arranging for
         * listeners to complete the deletion actions (in the background). The final step
         * for the listener is to unlock.
         */

        /*
         * TODO Return error if unable to send updates to all PDPs.
         */

        return Pair.of(Response.Status.OK, new PdpGroupDeleteResponse());
    }
}
