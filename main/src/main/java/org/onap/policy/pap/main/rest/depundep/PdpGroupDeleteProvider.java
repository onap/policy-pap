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

import java.util.List;
import java.util.function.BiFunction;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpGroupDeleteResponse;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PAP component to delete PDP groups.
 */
public class PdpGroupDeleteProvider extends ProviderBase<PdpGroupDeleteResponse> {
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
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeleteResponse> deleteGroup(String groupName) {

        return process(groupName, this::deleteGroup);
    }

    /**
     * Deletes a PDP group.
     *
     * @param data session data
     * @param groupName name of the PDP group to be deleted
     */
    private void deleteGroup(SessionData data, String groupName) {
        try {
            PdpGroup group = data.getGroup(groupName);
            if (group == null) {
                throw new PolicyPapRuntimeException("group not found: " + groupName);
            }

            if (group.getPdpGroupState() == PdpState.ACTIVE) {
                group.getPdpSubgroups().forEach(subgrp -> removePdps(data, subgrp.getPdpInstances()));
            }

            data.deleteGroupFromDb(group);

        } catch (PfModelException e) {
            // no need to log the error here, as it will be logged by the invoker
            logger.warn("failed to delete group: {}", groupName);
            throw new PolicyPapRuntimeException(DB_ERROR_MSG, e);

        } catch (RuntimeException e) {
            // no need to log the error here, as it will be logged by the invoker
            logger.warn("failed to delete group: {}", groupName);
            throw e;
        }
    }

    /**
     * Updates each PDP, removing it from its group and subgroup, and then deactivates the
     * PDP.
     *
     * @param data session data
     * @param pdpInstances the PDPs to be removed
     */
    private void removePdps(SessionData data, List<Pdp> pdpInstances) {
        for (Pdp pdp : pdpInstances) {
            PdpUpdate update = new PdpUpdate();
            update.setName(pdp.getInstanceId());
            // Note: removing from the group/subgroup, so don't set those fields

            PdpStateChange change = new PdpStateChange();
            change.setName(pdp.getInstanceId());
            change.setState(PdpState.PASSIVE);

            data.addRequests(update, change);
        }
    }

    /**
     * Undeploys a policy.
     *
     * @param policyIdent identifier of the policy to be undeployed
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, PdpGroupDeleteResponse> undeploy(ToscaPolicyIdentifierOptVersion policyIdent) {

        return process(policyIdent, this::undeployPolicy);
    }

    /**
     * Undeploys a policy from its groups.
     *
     * @param data session data
     * @param ident identifier of the policy to be deleted
     */
    private void undeployPolicy(SessionData data, ToscaPolicyIdentifierOptVersion ident) {
        try {
            processPolicy(data, ident);

        } catch (PfModelException e) {
            // no need to log the error here, as it will be logged by the invoker
            logger.warn("failed to undeploy policy: {}", ident);
            throw new PolicyPapRuntimeException(DB_ERROR_MSG, e);

        } catch (RuntimeException e) {
            // no need to log the error here, as it will be logged by the invoker
            logger.warn("failed to undeploy policy: {}", ident);
            throw e;
        }
    }

    @Override
    public PdpGroupDeleteResponse makeResponse(String errorMsg) {
        PdpGroupDeleteResponse resp = new PdpGroupDeleteResponse();
        resp.setErrorDetails(errorMsg);
        return resp;
    }

    /**
     * Returns a function that will remove the specified policy from a subgroup.
     */
    @Override
    protected BiFunction<PdpGroup, PdpSubGroup, Boolean> makeUpdater(ToscaPolicy policy) {
        ToscaPolicyIdentifier desiredIdent = policy.getIdentifier();

        return (group, subgroup) -> {

            // remove the policy from the subgroup
            return subgroup.getPolicies().remove(desiredIdent);
        };
    }
}
