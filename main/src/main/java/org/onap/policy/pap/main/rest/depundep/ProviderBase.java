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
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.common.utils.validation.Version;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.SimpleResponse;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
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
 *
 * @param <R> the response type
 */
public abstract class ProviderBase<R extends SimpleResponse> {
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
     * @return a pair containing the status and the response
     */
    protected <T> Pair<Response.Status, R> process(T request, BiConsumer<SessionData, T> processor) {

        synchronized (updateLock) {
            // list of requests to be published to the PDPs
            Collection<PdpUpdate> requests = Collections.emptyList();

            try (PolicyModelsProvider dao = daoFactory.create()) {

                SessionData data = new SessionData(dao);
                processor.accept(data, request);

                // make all of the DB updates
                data.updateDb();

                requests = data.getPdpUpdates();

            } catch (PfModelException e) {
                logger.warn(DEPLOY_FAILED, e);
                return Pair.of(Response.Status.INTERNAL_SERVER_ERROR, makeResponse(DB_ERROR_MSG));

            } catch (PolicyPapRuntimeException e) {
                logger.warn(DEPLOY_FAILED, e);
                return Pair.of(Response.Status.INTERNAL_SERVER_ERROR, makeResponse(e.getMessage()));

            } catch (RuntimeException e) {
                logger.warn(DEPLOY_FAILED, e);
                return Pair.of(Response.Status.INTERNAL_SERVER_ERROR, makeResponse("request failed"));
            }


            // publish the requests
            requests.forEach(requestMap::addRequest);
        }

        return Pair.of(Response.Status.OK, makeResponse(null));
    }

    /**
     * Makes a response.
     *
     * @param errorMsg error message, or {@code null} if there was no error
     * @return a new response
     */
    public abstract R makeResponse(String errorMsg);

    /**
     * Finds a Policy having the given name and version. If the specified version is
     * {@code null}, then it finds the matching Policy with the latest version.
     *
     * @param data session data
     * @param desiredPolicy the policy desired, with the "name" and optional
     *        "policyVersion" populated
     * @return the matching Policy type
     * @throws PfModelException if a DAO error occurred
     * @throws PolicyPapRuntimeException if there is no matching policy type
     */
    private ToscaPolicy getPolicy(SessionData data, ToscaPolicyIdentifierOptVersion desiredPolicy)
                    throws PfModelException {

        if (desiredPolicy.isNullVersion()) {
            return data.getPolicyMaxVersion(desiredPolicy.getName());

        } else {
            return data.getPolicy(new ToscaPolicyIdentifier(desiredPolicy.getName(), desiredPolicy.getVersion()));
        }
    }

    /**
     * Process a single policy from the request.
     *
     * @param data session data
     * @param desiredPolicy request policy
     * @throws PolicyPapRuntimeException if an error occurs
     * @throws PfModelException if a DAO error occurred
     */
    protected void processPolicy(SessionData data, ToscaPolicyIdentifierOptVersion desiredPolicy)
                    throws PfModelException {

        ToscaPolicy policy = getPolicy(data, desiredPolicy);

        Collection<PdpGroup> groups = getGroups(data, policy.getTypeIdentifier());
        if (groups.isEmpty()) {
            throw new PolicyPapRuntimeException("policy not supported by any PDP group: " + desiredPolicy.getName()
                            + " " + desiredPolicy.getVersion());
        }

        BiFunction<PdpGroup, PdpSubGroup, Boolean> updater = makeUpdater(policy);

        for (PdpGroup group : groups) {
            upgradeGroup(data, policy, group, updater);
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
     * Finds the active PDP group(s) with the highest version that supports the given
     * policy type.
     *
     * @param data session data
     * @param policyType the policy type of interest
     * @return the matching PDP group, or {@code null} if no active group supports the
     *         given PDP types
     * @throws PfModelException if an error occurs
     */
    private Collection<PdpGroup> getGroups(SessionData data, ToscaPolicyTypeIdentifier policyType)
                    throws PfModelException {
        // build a map containing the group with the highest version for each name
        Map<String, GroupVersion> name2data = new HashMap<>();

        for (PdpGroup group : data.getActivePdpGroupsByPolicyType(policyType)) {
            Version vers = Version.makeVersion("PdpGroup", group.getName(), group.getVersion());
            if (vers == null) {
                continue;
            }

            GroupVersion grpdata = name2data.get(group.getName());

            if (grpdata == null) {
                // not in the map yet
                name2data.put(group.getName(), new GroupVersion(group, vers));

            } else if (vers.compareTo(grpdata.version) >= 0) {
                // higher version
                grpdata.replace(group, vers);
            }
        }

        return name2data.values().stream().map(grpdata -> grpdata.group).collect(Collectors.toList());
    }

    /**
     * Updates a group, assigning a new version number, if it actually changes.
     *
     * @param data session data
     * @param policy policy to be added to or removed from the group
     * @param oldGroup the original group, to be updated
     * @param updater function to update a group
     * @throws PfModelException if a DAO error occurred
     */
    private void upgradeGroup(SessionData data, ToscaPolicy policy, PdpGroup oldGroup,
                    BiFunction<PdpGroup, PdpSubGroup, Boolean> updater) throws PfModelException {

        PdpGroup newGroup = new PdpGroup(oldGroup);
        boolean updated = false;

        for (PdpSubGroup subgroup : newGroup.getPdpSubgroups()) {

            if (!updater.apply(newGroup, subgroup)) {
                continue;
            }

            updated = true;

            /*
             * generate an UPDATE for each PDP instance. Since the group is active, we
             * assume that the PDP is, too, thus no need for a STATE-CHANGE.
             */
            for (Pdp pdpInstance : subgroup.getPdpInstances()) {
                data.addUpdate(makeUpdate(data, newGroup, subgroup, pdpInstance));
            }
        }


        if (updated) {
            // something changed
            data.setNewGroup(newGroup);
        }
    }

    /**
     * Makes an UPDATE message for a particular PDP.
     *
     * @param data session data
     * @param group group to which the PDP should belong
     * @param subgroup subgroup to which the PDP should belong
     * @param pdpInstance identifies the PDP of interest
     * @return a new UPDATE message
     */
    private PdpUpdate makeUpdate(SessionData data, PdpGroup group, PdpSubGroup subgroup, Pdp pdpInstance) {

        PdpUpdate update = new PdpUpdate();

        update.setName(pdpInstance.getInstanceId());
        update.setDescription(group.getDescription());
        update.setPdpGroup(group.getName());
        update.setPdpSubgroup(subgroup.getPdpType());
        update.setPolicies(subgroup.getPolicies().stream().map(data::getPolicy).collect(Collectors.toList()));

        return update;
    }

    /**
     * Data associated with a group. Used to find the maximum version for a given group.
     */
    private static class GroupVersion {
        private PdpGroup group;
        private Version version;

        public GroupVersion(PdpGroup group, Version version) {
            this.group = group;
            this.version = version;
        }

        public void replace(PdpGroup group, Version version) {
            this.group = group;
            this.version = version;
        }
    }
}
