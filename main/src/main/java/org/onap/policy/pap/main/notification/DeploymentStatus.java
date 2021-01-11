/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.State;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.notification.StatusAction.Action;

/**
 * Collection of Policy Deployment Status records. The sequence of method invocations
 * should be as follows:
 * <ol>
 * <li>{@link #loadByGroup(String)}</li>
 * <li>various other methods</li>
 * <li>repeat the previous steps as appropriate</li>
 * <li>{@link #flush(PolicyNotification)}</li>
 * </ol>
 */
public class DeploymentStatus {
    /**
     * Tracks the groups that have been loaded.
     */
    private final Set<String> pdpGroupLoaded = new HashSet<>();

    /**
     * Records, mapped by PDP/Policy pair.
     */
    @Getter(AccessLevel.PROTECTED)
    private final Map<StatusKey, StatusAction> recordMap = new HashMap<>();

    /**
     * Records the policy status so that notifications can be generated. When
     * {@link #loadByGroup(String)} is invoked, records are added to this. Other than
     * that, this is not updated until {@link #addNotifications(PolicyNotification)} is
     * invoked.
     */
    private DeploymentTracker tracker = new DeploymentTracker();

    private PolicyModelsProvider provider;


    /**
     * Constructs the object.
     *
     * @param provider the provider to use to access the DB
     */
    public DeploymentStatus(PolicyModelsProvider provider) {
        this.provider = provider;
    }

    /**
     * Adds new policy status to a notification.
     *
     * @param notif notification to which to add policy status
     */
    protected void addNotifications(PolicyNotification notif) {
        DeploymentTracker newTracker = new DeploymentTracker();
        recordMap.values().forEach(newTracker::add);

        tracker.addNotifications(notif, newTracker);

        tracker = newTracker;
    }

    /**
     * Loads policy deployment status associated with the given PDP group.
     *
     * @param pdpGroup group whose records are to be loaded
     * @throws PfModelException if an error occurs
     */
    public void loadByGroup(String pdpGroup) throws PfModelException {
        if (pdpGroupLoaded.contains(pdpGroup)) {
            return;
        }

        pdpGroupLoaded.add(pdpGroup);

        for (PdpPolicyStatus status : provider.getGroupPolicyStatus(pdpGroup)) {
            StatusAction status2 = new StatusAction(Action.UNCHANGED, status);
            recordMap.put(new StatusKey(status), status2);
            tracker.add(status2);
        }
    }

    /**
     * Flushes changes to the DB, adding policy status to the notification.
     *
     * @param notif notification to which to add policy status
     */
    public void flush(PolicyNotification notif) {
        // must add notifications BEFORE deleting undeployments
        addNotifications(notif);
        deleteUndeployments();
        flush();
    }

    /**
     * Flushes changes to the DB.
     */
    protected void flush() {
        // categorize the records
        List<PdpPolicyStatus> created = new ArrayList<>();
        List<PdpPolicyStatus> updated = new ArrayList<>();
        List<PdpPolicyStatus> deleted = new ArrayList<>();

        for (StatusAction status : recordMap.values()) {
            switch (status.getAction()) {
                case CREATED:
                    created.add(status.getStatus());
                    break;
                case UPDATED:
                    updated.add(status.getStatus());
                    break;
                case DELETED:
                    deleted.add(status.getStatus());
                    break;
                default:
                    break;
            }
        }

        provider.cudPolicyStatus(created, updated, deleted);

        /*
         * update the records to indicate everything is now unchanged (i.e., matches what
         * is in the DB)
         */

        Iterator<StatusAction> iter = recordMap.values().iterator();
        while (iter.hasNext()) {
            StatusAction status = iter.next();

            if (status.getAction() == Action.DELETED) {
                iter.remove();
            } else {
                status.setAction(Action.UNCHANGED);
            }
        }
    }

    /**
     * Deletes records for any policies that have been completely undeployed.
     */
    protected void deleteUndeployments() {
        // identify the incomplete policies

        // @formatter:off
        Set<ToscaConceptIdentifier> incomplete = recordMap.values().stream()
            .filter(status -> status.getAction() != Action.DELETED)
            .map(StatusAction::getStatus)
            .filter(status -> status.getState() == State.WAITING)
            .map(PdpPolicyStatus::getPolicy)
            .collect(Collectors.toSet());
        // @formatter:on

        // delete if UNDEPLOYED and not incomplete
        deleteDeployment((key, status) -> !status.getStatus().isDeploy() && !incomplete.contains(key.getPolicy()));
    }

    /**
     * Delete deployment records for a PDP.
     *
     * @param pdpId PDP whose records are to be deleted
     */
    public void deleteDeployment(String pdpId) {
        deleteDeployment((key, status) -> key.getPdpId().equals(pdpId));
    }

    /**
     * Delete deployment records for a policy.
     *
     * @param policy policy whose records are to be deleted
     * @param deploy {@code true} to delete deployment records, {@code false} to delete
     *        undeployment records
     */
    public void deleteDeployment(ToscaConceptIdentifier policy, boolean deploy) {
        deleteDeployment((key, status) -> status.getStatus().isDeploy() == deploy && key.getPolicy().equals(policy));
    }

    /**
     * Delete deployment records for a policy.
     *
     * @param filter filter to identify records to be deleted
     */
    private void deleteDeployment(BiPredicate<StatusKey, StatusAction> filter) {
        Iterator<Entry<StatusKey, StatusAction>> iter = recordMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<StatusKey, StatusAction> entry = iter.next();
            StatusKey key = entry.getKey();
            StatusAction value = entry.getValue();

            if (filter.test(key, value)) {
                if (value.getAction() == Action.CREATED) {
                    // it's a new record - just remove it
                    iter.remove();
                } else {
                    // it's an existing record - mark it for deletion
                    value.setAction(Action.DELETED);
                }
            }
        }
    }

    /**
     * Deploys/undeploys a policy to a PDP. Assumes that
     * {@link #deleteDeployment(ToscaConceptIdentifier, boolean)} has already been invoked
     * to delete any records having the wrong "deploy" value.
     *
     * @param pdpId PDP to which the policy is to be deployed
     * @param policy policy to be deployed
     * @param policyType policy's type
     * @param pdpGroup PdpGroup containing the PDP of interest
     * @param pdpType PDP type (i.e., PdpSubGroup) containing the PDP of interest
     * @param deploy {@code true} if the policy is being deployed, {@code false} if
     *        undeployed
     */
    public void deploy(String pdpId, ToscaConceptIdentifier policy, ToscaConceptIdentifier policyType, String pdpGroup,
                    String pdpType, boolean deploy) {

        recordMap.compute(new StatusKey(pdpId, policy), (key, status) -> {

            if (status == null) {
                // no record yet - create one

                // @formatter:off
                return new StatusAction(Action.CREATED, PdpPolicyStatus.builder()
                                    .pdpGroup(pdpGroup)
                                    .pdpId(pdpId)
                                    .pdpType(pdpType)
                                    .policy(policy)
                                    .policyType(policyType)
                                    .deploy(deploy)
                                    .state(State.WAITING)
                                    .build());
                // @formatter:on
            }

            PdpPolicyStatus status2 = status.getStatus();

            // record already exists - see if the deployment flag should be changed

            if (status2.isDeploy() != deploy) {
                // deployment flag has changed
                status.setChanged();
                status2.setDeploy(deploy);
                status2.setState(State.WAITING);


            } else if (status.getAction() == Action.DELETED) {
                // deployment flag is unchanged
                status.setAction(Action.UPDATED);
            }

            return status;
        });
    }

    /**
     * Indicates the deployment/undeployment of a set of policies to a PDP has completed.
     *
     * @param pdpId PDP of interest
     * @param expectedPolicies policies that we expected to be deployed to the PDP
     * @param actualPolicies policies that were actually deployed to the PDP
     */
    public void completeDeploy(String pdpId, Set<ToscaConceptIdentifier> expectedPolicies,
                    Set<ToscaConceptIdentifier> actualPolicies) {

        for (StatusAction status : recordMap.values()) {
            PdpPolicyStatus status2 = status.getStatus();

            if (!status.getStatus().getPdpId().equals(pdpId)
                            || expectedPolicies.contains(status2.getPolicy()) != status2.isDeploy()) {
                /*
                 * The policy is "expected" to be deployed, but the record is not marked
                 * for deployment (or vice versa), which means the expected policy is out
                 * of date with the DB, thus we'll ignore this policy for now.
                 */
                continue;
            }

            State state;
            if (actualPolicies.contains(status2.getPolicy())) {
                state = (status.getStatus().isDeploy() ? State.SUCCESS : State.FAILURE);
            } else {
                state = (status.getStatus().isDeploy() ? State.FAILURE : State.SUCCESS);
            }

            if (status2.getState() != state) {
                status.setChanged();
                status2.setState(state);
            }
        }
    }
}
