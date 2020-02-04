/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.parameters.RequestParams;


/**
 * Wraps an UPDATE.
 */
public class UpdateReq extends RequestImpl {

    /**
     * Policies to be undeployed if the request fails.
     */
    @Getter
    private Collection<ToscaPolicyIdentifier> undeployPolicies = Collections.emptyList();

    /**
     * Constructs the object, and validates the parameters.
     *
     * @param params configuration parameters
     * @param name the request name, used for logging purposes
     * @param message the initial message
     *
     * @throws IllegalArgumentException if a required parameter is not set
     */
    public UpdateReq(RequestParams params, String name, PdpUpdate message) {
        super(params, name, message);
    }

    @Override
    public PdpUpdate getMessage() {
        return (PdpUpdate) super.getMessage();
    }

    @Override
    public String checkResponse(PdpStatus response) {
        // reset the list
        undeployPolicies = Collections.emptyList();

        String reason = super.checkResponse(response);
        if (reason != null) {
            // response isn't for this PDP - don't generate notifications
            return reason;
        }

        Set<ToscaPolicyIdentifier> actualSet = new HashSet<>(alwaysList(response.getPolicies()));
        getNotifier().processResponse(response.getName(), actualSet);

        PdpUpdate message = getMessage();

        if (!StringUtils.equals(message.getPdpGroup(), response.getPdpGroup())) {
            return "group does not match";
        }

        if (!StringUtils.equals(message.getPdpSubgroup(), response.getPdpSubgroup())) {
            return "subgroup does not match";
        }

        if (message.getPdpSubgroup() == null) {
            return null;
        }

        // see if the policies match

        Set<ToscaPolicyIdentifier> expectedSet = new HashSet<>(alwaysList(message.getPolicies()).stream()
                        .map(ToscaPolicy::getIdentifier).collect(Collectors.toSet()));

        if (!actualSet.equals(expectedSet)) {
            // need to undeploy the policies that are expected, but missing from the
            // response
            undeployPolicies = expectedSet;
            undeployPolicies.removeAll(actualSet);

            return "policies do not match";
        }

        return null;
    }

    @Override
    public boolean reconfigure(PdpMessage newMessage) {
        if (!(newMessage instanceof PdpUpdate)) {
            // not an update - no change to this request
            return false;
        }

        PdpUpdate update = (PdpUpdate) newMessage;

        if (isSameContent(update)) {
            // content hasn't changed - nothing more to do
            return true;
        }

        reconfigure2(newMessage);
        return true;
    }

    protected final boolean isSameContent(PdpUpdate second) {
        PdpUpdate first = getMessage();

        if (!StringUtils.equals(first.getPdpGroup(), second.getPdpGroup())) {
            return false;
        }

        if (!StringUtils.equals(first.getPdpSubgroup(), second.getPdpSubgroup())) {
            return false;
        }

        // see if the policies are the same
        Set<ToscaPolicy> set1 = new HashSet<>(alwaysList(first.getPolicies()));
        Set<ToscaPolicy> set2 = new HashSet<>(alwaysList(second.getPolicies()));

        return set1.equals(set2);
    }

    /**
     * Always get a list, even if the original is {@code null}.
     *
     * @param list the original list, or {@code null}
     * @return the list, or an empty list if the original was {@code null}
     */
    private <T> List<T> alwaysList(List<T> list) {
        return (list != null ? list : Collections.emptyList());
    }
}
