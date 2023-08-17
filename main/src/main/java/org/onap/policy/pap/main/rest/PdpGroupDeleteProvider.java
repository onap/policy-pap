/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020-2021, 2023 Nordix Foundation.
 * Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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

import jakarta.ws.rs.core.Response.Status;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Provider for PAP component to delete PDP groups.
 */
@Service
public class PdpGroupDeleteProvider extends ProviderBase {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupDeleteProvider.class);

    /**
     * Deletes a PDP group.
     *
     * @param groupName name of the PDP group to be deleted
     * @throws PfModelException if an error occurred
     */
    public void deleteGroup(String groupName) throws PfModelException {
        process(groupName, this::deleteGroup);
    }

    /**
     * Deletes a PDP group.
     *
     * @param data session data
     * @param groupName name of the PDP group to be deleted
     * @throws PfModelException if an error occurred
     */
    private void deleteGroup(SessionData data, String groupName) throws PfModelException {
        try {
            PdpGroup group = data.getGroup(groupName);
            if (group == null) {
                throw new PfModelException(Status.NOT_FOUND, "group not found");
            }

            if (group.getPdpGroupState() == PdpState.ACTIVE) {
                throw new PfModelException(Status.BAD_REQUEST, "group is still " + PdpState.ACTIVE);
            }

            data.deleteGroupFromDb(group);

        } catch (PfModelException | RuntimeException e) {
            // no need to log the error object here, as it will be logged by the invoker
            logger.warn("failed to delete group: {}", groupName);
            throw e;
        }
    }

    /**
     * Undeploys a policy.
     *
     * @param policyIdent identifier of the policy to be undeployed
     * @throws PfModelException if an error occurred
     */
    public void undeploy(ToscaConceptIdentifierOptVersion policyIdent, String user) throws PfModelException {
        process(user, policyIdent, this::undeployPolicy);
    }

    /**
     * Undeploys a policy from its groups.
     *
     * @param data session data
     * @param ident identifier of the policy to be deleted
     * @throws PfModelException if an error occurred
     */
    private void undeployPolicy(SessionData data, ToscaConceptIdentifierOptVersion ident) throws PfModelException {
        try {
            processPolicy(data, ident);

            if (data.isUnchanged()) {
                throw new PfModelException(Status.BAD_REQUEST, "policy does not appear in any PDP group: "
                                + ident.getName() + " " + ident.getVersion());
            }

        } catch (PfModelException | RuntimeException e) {
            // no need to log the error object here, as it will be logged by the invoker
            logger.warn("failed to undeploy policy: {}", ident);
            throw e;
        }
    }

    /**
     * Returns a function that will remove the desired policy from a subgroup.
     */
    @Override
    protected Updater makeUpdater(SessionData data, ToscaPolicy policy,
                    ToscaConceptIdentifierOptVersion desiredIdent) {

        // construct a matcher based on whether or not the version was specified
        Predicate<ToscaConceptIdentifier> matcher;

        if (desiredIdent.getVersion() != null) {
            // version was specified - match the whole identifier
            matcher = policy.getIdentifier()::equals;

        } else {
            // version was not specified - match the name only
            String desnm = desiredIdent.getName();
            matcher = ident -> ident.getName().equals(desnm);
        }


        // return a function that will remove the policy from the subgroup
        return (group, subgroup) -> {

            Set<String> pdps = subgroup.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toSet());

            var result = false;

            Iterator<ToscaConceptIdentifier> iter = subgroup.getPolicies().iterator();
            while (iter.hasNext()) {
                ToscaConceptIdentifier ident = iter.next();

                if (matcher.test(ident)) {
                    result = true;
                    iter.remove();
                    logger.info("remove policy {} from subgroup {} {} count={}", ident,
                                    group.getName(), subgroup.getPdpType(), subgroup.getPolicies().size());

                    data.trackUndeploy(ident, pdps, group.getName(), subgroup.getPdpType());
                }
            }

            return result;
        };
    }
}
