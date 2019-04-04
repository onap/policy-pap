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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.common.utils.validation.Version;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
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
 * Provider for PAP component to deploy PDP groups. The following items must be in the
 * {@link Registry}:
 * <ul>
 * <li>PDP Modification Lock</li>
 * <li>PDP Modify Request Map</li>
 * <li>PAP DAO Factory</li>
 * </ul>
 */
public class PdpGroupDeployProvider {
    public static final String DB_ERROR_MSG = "DB error";

    private static final Logger logger = LoggerFactory.getLogger(PdpGroupDeployProvider.class);

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
    public PdpGroupDeployProvider() {
        this.updateLock = Registry.get(PapConstants.REG_PDP_MODIFY_LOCK, Object.class);
        this.requestMap = Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class);
        this.daoFactory = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
    }

    /**
     * Deploys or updates PDP groups.
     *
     * @param groups PDP group configurations
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, org.onap.policy.models.pap.concepts.PdpGroupDeployResponse> deployGroup(
                    PdpGroups groups) {

        return deploy(groups, this::deployGroups);
    }

    /**
     * Deploys or updates PDP policies.
     *
     * @param policies PDP policies
     * @return a pair containing the status and the response
     */
    public Pair<Response.Status, org.onap.policy.models.pap.concepts.PdpGroupDeployResponse> deployPolicies(
                    org.onap.policy.models.pap.concepts.PdpDeployPolicies policies) {

        return deploy(policies, this::deploySimplePolicies);
    }

    /**
     * Deploys or updates PDP policies.
     *
     * @param policies PDP policies
     * @param deployer function that deploys the policies and returns a list of PDP
     *        requests
     * @return a pair containing the status and the response
     */
    private <T> Pair<Response.Status, org.onap.policy.models.pap.concepts.PdpGroupDeployResponse> deploy(T policies,
                    BiConsumer<PdpUpdateSessionData, T> deployer) {

        synchronized (updateLock) {
            // list of requests to be published to the PDPs
            Collection<PdpUpdate> requests = Collections.emptyList();


            // deploy all policies within a single transaction
            try (PolicyModelsProvider dao = daoFactory.create()) {

                PdpUpdateSessionData data = new PdpUpdateSessionData(dao);
                deployer.accept(data, policies);

                requests = data.getUpdates();

            } catch (PfModelException e) {
                logger.warn("failed to deploy policies", e);

                PdpGroupDeployResponse resp = new PdpGroupDeployResponse();
                resp.setErrorDetails(DB_ERROR_MSG);

                return Pair.of(Response.Status.INTERNAL_SERVER_ERROR, resp);

            } catch (RuntimeException e) {
                logger.warn("failed to deploy policies", e);

                PdpGroupDeployResponse resp = new PdpGroupDeployResponse();
                resp.setErrorDetails(e instanceof PolicyPapRuntimeException ? e.getMessage() : "request failed");

                return Pair.of(Response.Status.INTERNAL_SERVER_ERROR, resp);
            }


            // publish the requests
            for (PdpUpdate req : requests) {
                requestMap.addRequest(req);
            }
        }

        return Pair.of(Response.Status.OK, new PdpGroupDeployResponse());
    }

    /**
     * Deploys or updates PDP groups.
     *
     * @param data session data
     * @param groups PDP group configurations
     * @return a list of requests that should be sent to configure the PDPs
     */
    private List<PdpUpdate> deployGroups(PdpUpdateSessionData data, PdpGroups groups) {

        throw new PolicyPapRuntimeException("not implemented yet");
    }

    /**
     * Deploys or updates PDP policies using the simple API.
     *
     * @param data session data
     * @param extPolicies external PDP policies
     * @return a list of requests that should be sent to configure the PDPs
     */
    private void deploySimplePolicies(PdpUpdateSessionData data, PdpDeployPolicies policies) {

        for (ToscaPolicyIdentifierOptVersion desiredPolicy : policies.getPolicies()) {

            try {
                processPolicy(data, desiredPolicy);

            } catch (PfModelException e) {
                // no need to log the error here, as it will be logged by the invoker
                logger.warn("failed to deploy policy: {}", desiredPolicy);
                throw new PolicyPapRuntimeException(DB_ERROR_MSG, e);

            } catch (RuntimeException e) {
                // no need to log the error here, as it will be logged by the invoker
                logger.warn("failed to deploy policy: {}", desiredPolicy);
                throw e;
            }
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
    private void processPolicy(PdpUpdateSessionData data, ToscaPolicyIdentifierOptVersion desiredPolicy)
                    throws PolicyPapRuntimeException, PfModelException {

        ToscaPolicy policy = getPolicy(data, desiredPolicy);

        Collection<PdpGroup> groups = getGroups(data, policy.getType(), policy.getTypeVersion());
        if (groups.isEmpty()) {
            throw new PolicyPapRuntimeException("not supported by any PDP group");
        }

        for (PdpGroup group : groups) {
            upgradeGroup(data, policy, group);
        }
    }

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
    private ToscaPolicy getPolicy(PdpUpdateSessionData data, ToscaPolicyIdentifierOptVersion desiredPolicy)
                    throws PfModelException {
        if (desiredPolicy.isNullVersion()) {
            return data.getPolicyMaxVersion(desiredPolicy.getName());
        }

        ToscaPolicy result = null;
        Version maxvers = new Version();

        for (ToscaPolicy policy : data.getPolicies(desiredPolicy)) {
            Version vers = Version.makeVersion("ToscaPolicy", policy.getName(), policy.getVersion());
            if (vers != null && vers.compareTo(maxvers) >= 0) {
                result = policy;
                maxvers = vers;
            }
        }

        if (result == null) {
            throw new PolicyPapRuntimeException("no policy for policy-id=" + desiredPolicy.getName() + " and version="
                            + desiredPolicy.getVersion());
        }

        return result;
    }

    /**
     * Finds the active PDP group(s) with the highest version that supports the given
     * policy type.
     *
     * @param data session data
     * @param policyType the policy type of interest
     * @param policyTypeVersion the version of the policy type of interest
     * @return the matching PDP group, or {@code null} if no active group supports the
     *         given PDP types
     * @throws PfModelException if an error occurs
     */
    private Collection<PdpGroup> getGroups(PdpUpdateSessionData data, String policyType, String policyTypeVersion)
                    throws PfModelException {
        // build a map containing the group with the highest version for each name
        Map<String, GroupData> name2data = new HashMap<>();

        for (PdpGroup group : data.getActivePdpGroupsByPolicy(policyType, policyTypeVersion)) {
            Version vers = Version.makeVersion("PdpGroup", group.getName(), group.getVersion());
            if (vers == null) {
                continue;
            }

            GroupData grpdata = name2data.get(group.getName());

            if (grpdata == null) {
                // not in the map yet
                name2data.put(group.getName(), new GroupData(group, vers));

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
     * @param policy policy to be added to the group
     * @param oldGroup the original group, to be updated
     * @throws PfModelException if a DAO error occurred
     */
    private void upgradeGroup(PdpUpdateSessionData data, ToscaPolicy policy, PdpGroup oldGroup)
                    throws PfModelException {

        ToscaPolicyIdentifier desiredIdent = policy.getIdentifier();
        ToscaPolicyTypeIdentifier desiredType = policy.getTypeIdentifier();

        PdpGroup newGroup = new PdpGroup(oldGroup);
        boolean updated = false;

        for (PdpSubGroup subgroup : newGroup.getPdpSubgroups()) {

            if (!subgroup.getSupportedPolicyTypes().contains(desiredType)) {
                // doesn't support the desired policy type
                continue;
            }

            if (subgroup.getPolicies().contains(desiredIdent)) {
                // already has the desired policy
                continue;
            }

            if (subgroup.getPdpInstances().isEmpty()) {
                throw new PolicyPapRuntimeException("group " + oldGroup.getName() + " subgroup " + subgroup.getPdpType()
                                + " has no active PDPs");
            }

            updated = true;

            // add the policy to the subgroup
            subgroup.getPolicies().add(desiredIdent);

            /*
             * generate an UPDATE for each PDP instance. Since the group is active, we
             * assume that the PDP is, too, thus no need for a STATE-CHANGE.
             */
            for (Pdp pdpInstance : subgroup.getPdpInstances()) {
                data.addUpdate(makeUpdate(data, newGroup, subgroup, pdpInstance));
            }
        }


        if (!updated) {
            return;
        }


        // something changed

        if (data.isNewlyCreated(newGroup.getName())) {
            /*
             * It's already in the list of new groups - update the policies, but not the
             * version.
             */
            data.updatePdpGroup(newGroup);

        } else {
            // haven't seen this group before - update the version
            upgradeGroupVersion(data, oldGroup, newGroup);
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
    private PdpUpdate makeUpdate(PdpUpdateSessionData data, PdpGroup group, PdpSubGroup subgroup, Pdp pdpInstance) {

        PdpUpdate update = new PdpUpdate();

        update.setName(pdpInstance.getInstanceId());
        update.setDescription(group.getDescription());
        update.setPdpGroup(group.getName());
        update.setPdpSubgroup(subgroup.getPdpType());
        update.setPolicies(subgroup.getPolicies().stream().map(data::getPolicy).collect(Collectors.toList()));

        return update;
    }

    /**
     * Upgrades a group's version. Updates the version in the new group, persists the new
     * group, and deactivates the old group.
     *
     * @param data session data
     * @param newGroup the new version of the group
     * @param oldGroup the original group, to be updated
     * @throws PfModelException if a DAO error occurred
     */
    private void upgradeGroupVersion(PdpUpdateSessionData data, PdpGroup newGroup, PdpGroup oldGroup)
                    throws PfModelException {

        // change versions
        newGroup.setVersion(makeNewVersion(data, oldGroup).toString());

        // create it before we update the old group
        newGroup = data.createPdpGroup(newGroup);

        // deactivate the old group
        oldGroup.setPdpGroupState(PdpState.PASSIVE);
        oldGroup = data.updatePdpGroup(oldGroup);
    }

    /**
     * Makes a new version for the PDP group.
     *
     * @param data session data
     * @param group current group
     * @return a new version
     * @throws PfModelException if a DAO error occurred
     */
    private Version makeNewVersion(PdpUpdateSessionData data, PdpGroup group) throws PfModelException {
        PdpGroup group2 = data.getPdpGroupMaxVersion(group.getName());
        Version vers = Version.makeVersion("PdpGroup", group2.getName(), group2.getVersion());
        return vers.newVersion();
    }

    /**
     * Data associated with a group. Used to find the group with the maximum version.
     */
    private static class GroupData {
        private PdpGroup group;
        private Version version;

        public GroupData(PdpGroup group, Version version) {
            this.group = group;
            this.version = version;
        }

        public void replace(PdpGroup group, Version version) {
            this.group = group;
            this.version = version;
        }
    }
}
