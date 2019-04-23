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

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super class of providers that deploy and undeploy PDP groups. The following items must
 * be in the {@link Registry}:
 * <ul>
 * <li>PDP Modification Lock</li>
 * <li>PDP Modify Request Map</li>
 * <li>PAP DAO Factory</li>
 * </ul>
 */
public abstract class ProviderBase {
    private static final String DEPLOY_FAILED = "failed to deploy/undeploy policies";
    public static final String DB_ERROR_MSG = "DB error";

    private static final Logger logger = LoggerFactory.getLogger(ProviderBase.class);

    /**
     * Lock used when updating PDPs.
     */
    private final Object updateLock;

    /**
     * Used to send UPDATE and STATE-CHANGE requests to the PDPs.
     */
    private final PdpModifyRequestMap requestMap;

    /**
     * Factory for PAP DAO.
     */
    private final PolicyModelsProviderFactoryWrapper daoFactory;


    /**
     * Constructs the object.
     */
    public ProviderBase() {
        this.updateLock = Registry.get(PapConstants.REG_PDP_MODIFY_LOCK, Object.class);
        this.requestMap = Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class);
        this.daoFactory = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
    }

    /**
     * Processes a policy request.
     *
     * @param request PDP policy request
     * @param processor function that processes the request
     * @throws PfModelException if an error occurred
     */
    protected <T> void process(T request, BiConsumerWithEx<SessionData, T> processor) throws PfModelException {

        synchronized (updateLock) {
            // list of requests to be published to the PDPs
            Collection<Pair<PdpUpdate, PdpStateChange>> requests = Collections.emptyList();

            try (PolicyModelsProvider dao = daoFactory.create()) {

                SessionData data = new SessionData(dao);
                processor.accept(data, request);

                // make all of the DB updates
                data.updateDb();

                requests = data.getPdpRequests();

            } catch (PfModelException | PfModelRuntimeException e) {
                logger.warn(DEPLOY_FAILED, e);
                throw e;

            } catch (RuntimeException e) {
                logger.warn(DEPLOY_FAILED, e);
                throw new PfModelException(Status.INTERNAL_SERVER_ERROR, "request failed", e);
            }


            // publish the requests
            requests.forEach(pair -> requestMap.addRequest(pair.getLeft(), pair.getRight()));
        }
    }

    /**
     * Process a single policy from the request.
     *
     * @param data session data
     * @param desiredPolicy request policy
     * @throws PfModelException if an error occurred
     */
    protected void processPolicy(SessionData data, ToscaPolicyIdentifierOptVersion desiredPolicy)
                    throws PfModelException {

        ToscaPolicy policy = getPolicy(data, desiredPolicy);

        Collection<PdpGroup> groups = getGroups(data, policy.getTypeIdentifier());
        if (groups.isEmpty()) {
            throw new PfModelException(Status.BAD_REQUEST, "policy not supported by any PDP group: "
                            + desiredPolicy.getName() + " " + desiredPolicy.getVersion());
        }

        BiFunction<PdpGroup, PdpSubGroup, Boolean> updater = makeUpdater(policy);

        for (PdpGroup group : groups) {
            upgradeGroup(data, group, updater);
        }
    }

    /**
     * Makes a function to update a subgroup. The function is expected to return
     * {@code true} if the subgroup was updated, {@code false} if no update was
     * necessary/appropriate.
     *
     * @param policy policy to be added to or removed from each subgroup
     * @return a function to update a subgroup
     */
    protected abstract BiFunction<PdpGroup, PdpSubGroup, Boolean> makeUpdater(ToscaPolicy policy);

    /**
     * Finds the active PDP group(s) that supports the given policy type.
     *
     * @param data session data
     * @param policyType the policy type of interest
     * @return the matching PDP group, or {@code null} if no active group supports the
     *         given PDP types
     * @throws PfModelException if an error occurred
     */
    private Collection<PdpGroup> getGroups(SessionData data, ToscaPolicyTypeIdentifier policyType)
                    throws PfModelException {

        return data.getActivePdpGroupsByPolicyType(policyType);
    }

    /**
     * Updates a group, assigning a new version number, if it actually changes.
     *
     * @param data session data
     * @param group the original group, to be updated
     * @param updater function to update a group
     * @throws PfModelRuntimeException if an error occurred
     */
    private void upgradeGroup(SessionData data, PdpGroup group, BiFunction<PdpGroup, PdpSubGroup, Boolean> updater) {

        boolean updated = false;

        for (PdpSubGroup subgroup : group.getPdpSubgroups()) {

            if (!updater.apply(group, subgroup)) {
                continue;
            }

            updated = true;

            makeUpdates(data, group, subgroup);
        }


        if (updated) {
            // something changed
            data.update(group);
        }
    }

    /**
     * Makes UPDATE messages for each PDP in a subgroup.
     *
     * @param data session data
     * @param group group containing the subgroup
     * @param subgroup subgroup whose PDPs should receive messages
     */
    protected void makeUpdates(SessionData data, PdpGroup group, PdpSubGroup subgroup) {
        for (Pdp pdp : subgroup.getPdpInstances()) {
            data.addUpdate(makeUpdate(data, group, subgroup, pdp));
        }
    }

    /**
     * Makes an UPDATE message for a particular PDP.
     *
     * @param data session data
     * @param group group to which the PDP should belong
     * @param subgroup subgroup to which the PDP should belong
     * @param pdp the PDP of interest
     * @return a new UPDATE message
     */
    private PdpUpdate makeUpdate(SessionData data, PdpGroup group, PdpSubGroup subgroup, Pdp pdp) {

        PdpUpdate update = new PdpUpdate();

        update.setName(pdp.getInstanceId());
        update.setDescription(group.getDescription());
        update.setPdpGroup(group.getName());
        update.setPdpSubgroup(subgroup.getPdpType());
        update.setPolicies(subgroup.getPolicies().stream().map(ToscaPolicyIdentifierOptVersion::new)
                        .map(ident -> getPolicy(data, ident)).collect(Collectors.toList()));

        return update;
    }

    /**
     * Gets the specified policy.
     *
     * @param data session data
     * @param ident policy identifier, with an optional version
     * @return the policy of interest
     * @throws PfModelRuntimeException if an error occurred or the policy was not found
     */
    private ToscaPolicy getPolicy(SessionData data, ToscaPolicyIdentifierOptVersion ident) {
        try {
            return data.getPolicy(ident);

        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e.getErrorResponse().getResponseCode(),
                            e.getErrorResponse().getErrorMessage(), e);
        }
    }

    @FunctionalInterface
    public static interface BiConsumerWithEx<F, S> {
        /**
         * Performs this operation on the given arguments.
         *
         * @param firstArg the first input argument
         * @param secondArg the second input argument
         * @throws PfModelException if an error occurred
         */
        void accept(F firstArg, S secondArg) throws PfModelException;
    }
}
