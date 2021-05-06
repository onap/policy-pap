/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020-2021 Nordix Foundation.
 * Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

import java.util.Collection;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.notification.PolicyNotifier;

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
    public static final String DB_ERROR_MSG = "DB error";

    /**
     * Lock used when updating PDPs.
     */
    private final Object updateLock;

    /**
     * Used to send UPDATE and STATE-CHANGE requests to the PDPs.
     */
    private final PdpModifyRequestMap requestMap;

    /**
     * Generates policy notifications based on responses from PDPs.
     */
    private final PolicyNotifier notifier;

    /**
     * Factory for PAP DAO.
     */
    private final PolicyModelsProviderFactoryWrapper daoFactory;

    /**
     * Constructs the object.
     */
    protected ProviderBase() {
        this.updateLock = Registry.get(PapConstants.REG_PDP_MODIFY_LOCK, Object.class);
        this.requestMap = Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class);
        this.daoFactory = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        this.notifier = Registry.get(PapConstants.REG_POLICY_NOTIFIER, PolicyNotifier.class);
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
            SessionData data;
            var notif = new PolicyNotification();

            try (PolicyModelsProvider dao = daoFactory.create()) {

                data = new SessionData(dao);
                processor.accept(data, request);

                // make all of the DB updates
                data.updateDb(notif);

            } catch (PfModelException | PfModelRuntimeException e) {
                throw e;

            } catch (RuntimeException e) {
                throw new PfModelException(Status.INTERNAL_SERVER_ERROR, "request failed", e);
            }

            // publish the requests
            data.getPdpRequests().forEach(pair -> requestMap.addRequest(pair.getLeft(), pair.getRight()));

            // publish the notifications
            notifier.publish(notif);
        }
    }

    /**
     * Process a single policy from the request.
     *
     * @param data session data
     * @param desiredPolicy request policy
     * @throws PfModelException if an error occurred
     */
    protected void processPolicy(SessionData data, ToscaConceptIdentifierOptVersion desiredPolicy)
                    throws PfModelException {

        ToscaPolicy policy = getPolicy(data, desiredPolicy);

        Collection<PdpGroup> groups = getGroups(data, policy.getTypeIdentifier());
        if (groups.isEmpty()) {
            throw new PfModelException(Status.BAD_REQUEST, "policy not supported by any PDP group: "
                            + desiredPolicy.getName() + " " + desiredPolicy.getVersion());
        }

        var updater = makeUpdater(data, policy, desiredPolicy);

        for (PdpGroup group : groups) {
            upgradeGroup(data, group, updater);
        }
    }

    /**
     * Makes a function to update a subgroup. The function is expected to return
     * {@code true} if the subgroup was updated, {@code false} if no update was
     * necessary/appropriate.
     *
     * @param data session data
     * @param policy policy to be added to or removed from each subgroup
     * @param desiredPolicy request policy
     * @return a function to update a subgroup
     */
    protected abstract Updater makeUpdater(SessionData data, ToscaPolicy policy,
                    ToscaConceptIdentifierOptVersion desiredPolicy);

    /**
     * Finds the active PDP group(s) that supports the given policy type.
     *
     * @param data session data
     * @param policyType the policy type of interest
     * @return the matching PDP group, or {@code null} if no active group supports the
     *         given PDP types
     * @throws PfModelException if an error occurred
     */
    private Collection<PdpGroup> getGroups(SessionData data, ToscaConceptIdentifier policyType)
                    throws PfModelException {

        return data.getActivePdpGroupsByPolicyType(policyType);
    }

    /**
     * Updates a group, assigning a new version number, if it actually changes.
     *
     * @param data session data
     * @param group the original group, to be updated
     * @param updater function to update a group
     * @throws PfModelException if an error occurred
     */
    private void upgradeGroup(SessionData data, PdpGroup group, Updater updater)
                    throws PfModelException {

        var updated = false;

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

        var update = new PdpUpdate();

        update.setName(pdp.getInstanceId());
        update.setDescription(group.getDescription());
        update.setPdpGroup(group.getName());
        update.setPdpSubgroup(subgroup.getPdpType());
        update.setPolicies(subgroup.getPolicies().stream().map(ToscaConceptIdentifierOptVersion::new)
                        .map(ident -> getPolicy(data, ident)).collect(Collectors.toList()));
        update.setPoliciesToBeDeployed(data.getPoliciesToBeDeployed());
        update.setPoliciesToBeUndeployed(data.getPoliciesToBeUndeployed());

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
    private ToscaPolicy getPolicy(SessionData data, ToscaConceptIdentifierOptVersion ident) {
        try {
            ToscaPolicy policy = data.getPolicy(ident);
            if (policy == null) {
                throw new PfModelRuntimeException(Status.NOT_FOUND,
                                "cannot find policy: " + ident.getName() + " " + ident.getVersion());
            }

            return policy;

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

    @FunctionalInterface
    public static interface Updater {
        boolean apply(PdpGroup group, PdpSubGroup subgroup) throws PfModelException;
    }
}
