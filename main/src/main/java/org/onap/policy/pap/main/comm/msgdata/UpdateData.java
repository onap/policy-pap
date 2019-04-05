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

import java.util.ArrayList;
import java.util.List;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;


/**
 * Wraps an UPDATE.
 */
public abstract class UpdateData extends MessageData {
    private PdpUpdate update;

    /**
     * Constructs the object.
     *
     * @param message message to be wrapped by this
     * @param params the parameters
     */
    public UpdateData(PdpUpdate message, PdpModifyRequestMapParams params) {
        super(message, params.getParams().getUpdateParameters().getMaxRetryCount(), params.getUpdateTimers());

        update = message;
    }

    @Override
    public String checkResponse(PdpStatus response) {
        if (!update.getName().equals(response.getName())) {
            return "name does not match";
        }

        if (!update.getPdpGroup().equals(response.getPdpGroup())) {
            return "group does not match";
        }

        if (!update.getPdpSubgroup().equals(response.getPdpSubgroup())) {
            return "subgroup does not match";
        }

        // see if the other has any policies that this does not have
        ArrayList<ToscaPolicyIdentifier> lst = new ArrayList<>(response.getPolicies());
        List<ToscaPolicy> mypolicies = update.getPolicies();

        if (mypolicies.size() != lst.size()) {
            return "policies do not match";
        }

        lst.removeAll(convertToscaPolicyToToscaPolicyIndentifier(update.getPolicies()));
        if (!lst.isEmpty()) {
            return "policies do not match";
        }

        return null;
    }

    /**
     * Converts a ToscaPolicy list to ToscaPolicyIdentifier list.
     *
     * @param toscaPolicies the list of ToscaPolicy
     * @return the ToscaPolicyIdentifier list
     */
    private List<ToscaPolicyIdentifier> convertToscaPolicyToToscaPolicyIndentifier(List<ToscaPolicy> toscaPolicies) {
        final List<ToscaPolicyIdentifier> toscaPolicyIdentifiers = new ArrayList<>();
        for (final ToscaPolicy toscaPolicy : toscaPolicies) {
            toscaPolicyIdentifiers.add(new ToscaPolicyIdentifier(toscaPolicy.getName(), toscaPolicy.getVersion()));
        }
        return toscaPolicyIdentifiers;
    }
}
