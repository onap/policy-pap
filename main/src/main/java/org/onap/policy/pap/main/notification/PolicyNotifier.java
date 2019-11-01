/*-
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

package org.onap.policy.pap.main.notification;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyFilter;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.QueueToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notifier for completion of policy updates.
 */
public class PolicyNotifier {
    private static final Logger logger = LoggerFactory.getLogger(PolicyNotifier.class);

    /**
     * Notification publisher.
     */
    private final Publisher<PolicyNotification> publisher;

    /**
     * Deployment tracker.
     */
    private final PolicyDeployTracker deployTracker = makeDeploymentTracker();

    /**
     * Undeployment tracker.
     */
    private final PolicyUndeployTracker undeployTracker = makeUndeploymentTracker();


    /**
     * Constructs the object. Loads all deployed policies into the internal cache.
     *
     * @param publisher notification publisher
     * @param daoFactory factory used to load policy deployment data from the DB
     * @throws PfModelException if a DB error occurs
     */
    public PolicyNotifier(Publisher<PolicyNotification> publisher, PolicyModelsProviderFactoryWrapper daoFactory)
                    throws PfModelException {

        this.publisher = publisher;

        try (PolicyModelsProvider dao = daoFactory.create()) {
            Map<ToscaPolicyIdentifier, ToscaPolicyTypeIdentifier> id2type = loadPolicyTypes(dao);
            loadPolicies(dao, id2type);
        }
    }

    /**
     * Loads policy types from the DB.
     *
     * @param dao provider used to retrieve policies from the DB
     * @return a mapping from policy id to policy type
     * @throws PfModelException if a DB error occurs
     */
    private Map<ToscaPolicyIdentifier, ToscaPolicyTypeIdentifier> loadPolicyTypes(PolicyModelsProvider dao)
                    throws PfModelException {

        Map<ToscaPolicyIdentifier, ToscaPolicyTypeIdentifier> id2type = new HashMap<>();

        for (ToscaPolicy policy : dao.getFilteredPolicyList(ToscaPolicyFilter.builder().build())) {
            id2type.put(policy.getIdentifier(), policy.getTypeIdentifier());
        }

        return id2type;
    }

    /**
     * Loads deployed policies.
     *
     * @param id2type mapping from policy id to policy type
     * @param dao provider used to retrieve policies from the DB
     * @throws PfModelException if a DB error occurs
     */
    private void loadPolicies(PolicyModelsProvider dao, Map<ToscaPolicyIdentifier, ToscaPolicyTypeIdentifier> id2type)
                    throws PfModelException {
        for (PdpGroup group : dao.getPdpGroups(null)) {
            for (PdpSubGroup subgrp : group.getPdpSubgroups()) {
                loadPolicies(id2type, group, subgrp);
            }
        }
    }

    /**
     * Loads a subgroup's deployed policies.
     *
     * @param id2type maps a policy id to its type
     * @param group group containing the subgroup
     * @param subgrp subgroup whose policies are to be loaded
     */
    private void loadPolicies(Map<ToscaPolicyIdentifier, ToscaPolicyTypeIdentifier> id2type, PdpGroup group,
                    PdpSubGroup subgrp) {

        for (ToscaPolicyIdentifier policyId : subgrp.getPolicies()) {

            ToscaPolicyTypeIdentifier type = id2type.get(policyId);
            if (type == null) {
                logger.error("group {}:{} refers to non-existent policy {}:{}", group.getName(), subgrp.getPdpType(),
                                policyId.getName(), policyId.getVersion());
                continue;
            }

            PolicyPdpNotificationData data = new PolicyPdpNotificationData(policyId, type);
            data.addAll(subgrp.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toList()));
            deployTracker.addData(data);
        }
    }

    /**
     * Gets the status of all deployed policies.
     *
     * @return the status of all deployed policies
     */
    public synchronized List<PolicyStatus> getStatus() {
        return deployTracker.getStatus();
    }

    /**
     * Gets the status of a particular deployed policy.
     *
     * @param policyId ID of the policy of interest, without the version
     * @return the status of all deployed policies matching the given identifier
     */
    public List<PolicyStatus> getStatus(String policyId) {
        return deployTracker.getStatus(policyId);
    }

    /**
     * Gets the status of a particular deployed policy.
     *
     * @param ident identifier of the policy of interest
     * @return the status of the given policy, or empty if the policy is not found
     */
    public Optional<PolicyStatus> getStatus(ToscaPolicyIdentifier ident) {
        return deployTracker.getStatus(ident);
    }

    /**
     * Adds data to the deployment tracker. If a PDP appears within the undeployment
     * tracker, then it's removed from there.
     *
     * @param data data to be added
     */
    public synchronized void addDeploymentData(PolicyPdpNotificationData data) {
        PolicyNotification notification = new PolicyNotification();

        undeployTracker.removeData(data, notification.getDeleted());
        deployTracker.addData(data);

        publish(notification);
    }

    /**
     * Adds data to the undeployment tracker. If a PDP appears within the deployment
     * tracker, then it's removed from there.
     *
     * @param data data to be added
     */
    public synchronized void addUndeploymentData(PolicyPdpNotificationData data) {
        PolicyNotification notification = new PolicyNotification();

        deployTracker.removeData(data, notification.getAdded());
        undeployTracker.addData(data);

        publish(notification);
    }

    /**
     * Processes a response from a PDP.
     *
     * @param pdp PDP of interest
     * @param activePolicies policies that are still active on the PDP, as specified in
     *        the response
     */
    public synchronized void processResponse(String pdp, Collection<ToscaPolicyIdentifier> activePolicies) {
        processResponse(pdp, new HashSet<>(activePolicies));
    }

    /**
     * Processes a response from a PDP.
     *
     * @param pdp PDP of interest
     * @param activePolicies policies that are still active on the PDP, as specified in
     *        the response
     */
    public synchronized void processResponse(String pdp, Set<ToscaPolicyIdentifier> activePolicies) {
        PolicyNotification notification = new PolicyNotification();

        undeployTracker.processResponse(pdp, activePolicies, notification.getDeleted());
        deployTracker.processResponse(pdp, activePolicies, notification.getAdded());

        publish(notification);
    }

    /**
     * Removes a PDP from any policies still awaiting responses from it, generating
     * notifications for any of those policies that become complete as a result.
     *
     * @param pdp PDP to be removed
     */
    public synchronized void removePdp(String pdp) {
        PolicyNotification notification = new PolicyNotification();

        undeployTracker.removePdp(pdp, notification.getDeleted());
        deployTracker.removePdp(pdp, notification.getAdded());

        publish(notification);
    }

    /**
     * Publishes a notification, if it is not empty.
     *
     * @param notification notification to be published
     */
    private void publish(PolicyNotification notification) {
        if (!notification.isEmpty()) {
            publisher.enqueue(new QueueToken<>(notification));
        }
    }


    // the following methods may be overridden by junit tests

    protected PolicyDeployTracker makeDeploymentTracker() {
        return new PolicyDeployTracker();
    }

    protected PolicyUndeployTracker makeUndeploymentTracker() {
        return new PolicyUndeployTracker();
    }
}
