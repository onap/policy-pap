/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2024 Nordix Foundation.
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.parameters.RequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Wraps an UPDATE.
 */
@Getter
public class UpdateReq extends RequestImpl {

    private static final Logger logger = LoggerFactory.getLogger(UpdateReq.class);

    /**
     * Policies to be undeployed if the request fails.
     */
    private Collection<ToscaConceptIdentifier> undeployPolicies = Collections.emptyList();

    /**
     * Constructs the object, and validates the parameters.
     *
     * @param params  configuration parameters
     * @param name    the request name, used for logging purposes
     * @param message the initial message
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

        Set<ToscaConceptIdentifier> actualSet = new HashSet<>(alwaysList(response.getPolicies()));
        Set<ToscaConceptIdentifier> expectedSet = alwaysList(message.getPoliciesToBeDeployed()).stream()
            .map(ToscaPolicy::getIdentifier).collect(Collectors.toSet());

        getNotifier().processResponse(response.getName(), message.getPdpGroup(), expectedSet, actualSet);

        Set<ToscaConceptIdentifier> expectedUndeploySet =
            new HashSet<>(alwaysList(message.getPoliciesToBeUndeployed()));

        if (actualSet.stream().anyMatch(expectedUndeploySet::contains)) {
            logger.info("some policies have failed to undeploy");
        }

        if (!actualSet.containsAll(expectedSet)) {
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
        if (!(newMessage instanceof PdpUpdate update)) {
            // not an update - no change to this request
            return false;
        }

        // ensure lists are never null
        update.setPoliciesToBeDeployed(alwaysList(update.getPoliciesToBeDeployed()));
        update.setPoliciesToBeUndeployed(alwaysList(update.getPoliciesToBeUndeployed()));

        if (isSameContent(update)) {
            // content hasn't changed - nothing more to do
            return true;
        }

        Map<ToscaConceptIdentifier, ToscaPolicy> newDeployMap = update.getPoliciesToBeDeployed().stream()
            .collect(Collectors.toMap(ToscaPolicy::getIdentifier, policy -> policy));

        // Merge undeploy lists
        var tempUndeploy = alwaysList(getMessage().getPoliciesToBeUndeployed());
        Set<ToscaConceptIdentifier> policiesToBeUndeployedSet = new HashSet<>(tempUndeploy);
        policiesToBeUndeployedSet.removeAll(newDeployMap.keySet());
        policiesToBeUndeployedSet.addAll(update.getPoliciesToBeUndeployed());
        final List<ToscaConceptIdentifier> policiesToBeUndeployed = new LinkedList<>(policiesToBeUndeployedSet);

        // Merge deploy lists
        var tempDeploy = alwaysList(getMessage().getPoliciesToBeDeployed());
        Map<ToscaConceptIdentifier, ToscaPolicy> policiesToBeDeployedMap = tempDeploy.stream()
            .collect(Collectors.toMap(ToscaPolicy::getIdentifier, policy -> policy));

        // cross-check policy in deploy and undeploy map; remove from deploy map.
        update.getPoliciesToBeUndeployed().forEach(policiesToBeDeployedMap.keySet()::remove);
        policiesToBeDeployedMap.putAll(newDeployMap);
        final List<ToscaPolicy> policiesToBeDeployed = new LinkedList<>(policiesToBeDeployedMap.values());

        // Set lists in update
        update.setPoliciesToBeDeployed(policiesToBeDeployed);
        update.setPoliciesToBeUndeployed(policiesToBeUndeployed);

        reconfigure2(update);
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
        Set<ToscaPolicy> set1 = new HashSet<>(alwaysList(first.getPoliciesToBeDeployed()));
        Set<ToscaPolicy> set2 = new HashSet<>(alwaysList(second.getPoliciesToBeDeployed()));

        if (!(set1.equals(set2))) {
            return false;
        }

        Set<ToscaConceptIdentifier> undep1 = new HashSet<>(alwaysList(first.getPoliciesToBeUndeployed()));
        Set<ToscaConceptIdentifier> undep2 = new HashSet<>(alwaysList(second.getPoliciesToBeUndeployed()));

        return undep1.equals(undep2);
    }

    /**
     * Always get a list, even if the original is {@code null}.
     *
     * @param list the original list, or {@code null}
     * @return the list, or an empty list if the original was {@code null}
     */
    private <T> List<T> alwaysList(List<T> list) {
        return Optional.ofNullable(list).orElse(Collections.emptyList());
    }
}
