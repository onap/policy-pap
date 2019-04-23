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

package org.onap.policy.pap.main.rest.depundep;

import java.util.function.BiFunction;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PAP component to delete PDP groups.
 */
public class PdpGroupDeleteProvider extends ProviderBase {
    private static final Logger logger = LoggerFactory.getLogger(PdpGroupDeleteProvider.class);


    /**
     * Constructs the object.
     */
    public PdpGroupDeleteProvider() {
        super();
    }

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
    public void undeploy(ToscaPolicyIdentifierOptVersion policyIdent) throws PfModelException {
        process(policyIdent, this::undeployPolicy);
    }

    /**
     * Undeploys a policy from its groups.
     *
     * @param data session data
     * @param ident identifier of the policy to be deleted
     * @throws PfModelException if an error occurred
     */
    private void undeployPolicy(SessionData data, ToscaPolicyIdentifierOptVersion ident) throws PfModelException {
        try {
            processPolicy(data, ident);

        } catch (PfModelException | RuntimeException e) {
            // no need to log the error object here, as it will be logged by the invoker
            logger.warn("failed to undeploy policy: {}", ident);
            throw e;
        }
    }

    /**
     * Returns a function that will remove the specified policy from a subgroup.
     */
    @Override
    protected BiFunction<PdpGroup, PdpSubGroup, Boolean> makeUpdater(ToscaPolicy policy) {
        ToscaPolicyIdentifier desiredIdent = policy.getIdentifier();

        // remove the policy from the subgroup
        return (group, subgroup) -> subgroup.getPolicies().remove(desiredIdent);
    }
}
