/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.PdpPolicyStatusBuilder;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.State;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class DeploymentTrackerTest {
    private static final String VERSION = "1.2.3";
    private static final String MY_GROUP = "MyGroup";
    private static final String MY_PDP_TYPE = "MyPdpType";
    private static final ToscaConceptIdentifier POLICY_A = new ToscaConceptIdentifier("PolicyA", VERSION);
    private static final ToscaConceptIdentifier POLICY_B = new ToscaConceptIdentifier("PolicyB", VERSION);
    private static final ToscaConceptIdentifier POLICY_TYPE = new ToscaConceptIdentifier("MyPolicyType", VERSION);
    private static final String PDP_A = "pdpA";
    private static final String PDP_B = "pdpB";

    private DeploymentTracker tracker;
    private PdpPolicyStatusBuilder builder;

    /**
     * Sets up test objects.
     */
    @BeforeEach
    public void setUp() {
        tracker = new DeploymentTracker();
        builder = PdpPolicyStatus.builder().deploy(true).state(State.SUCCESS).pdpGroup(MY_GROUP).pdpType(MY_PDP_TYPE)
            .policy(POLICY_A).policyType(POLICY_TYPE).pdpId(PDP_A);
    }

    @Test
    void testGetDeploymentStatus() {
        assertThat(tracker.getDeploymentStatus()).isEmpty();

        tracker.add(builder.build());
        tracker.add(builder.policy(POLICY_B).build());
        assertThat(tracker.getDeploymentStatus()).hasSize(2);

        assertThat(tracker.getUndeploymentStatus()).isEmpty();
    }

    @Test
    void testGetUndeploymentStatus() {
        builder.deploy(false);

        assertThat(tracker.getUndeploymentStatus()).isEmpty();

        tracker.add(builder.build());
        tracker.add(builder.policy(POLICY_B).build());
        assertThat(tracker.getUndeploymentStatus()).hasSize(2);

        assertThat(tracker.getDeploymentStatus()).isEmpty();
    }

    @Test
    void testAddNotifications() {
        DeploymentTracker newTracker = new DeploymentTracker();

        newTracker.add(builder.build());
        newTracker.add(builder.policy(POLICY_B).deploy(false).build());

        PolicyNotification notif = new PolicyNotification();
        tracker.addNotifications(notif, newTracker);

        assertThat(notif.getAdded()).hasSize(1);
        assertThat(notif.getAdded().get(0).getPolicy()).isEqualTo(POLICY_A);

        assertThat(notif.getDeleted()).hasSize(1);
        assertThat(notif.getDeleted().get(0).getPolicy()).isEqualTo(POLICY_B);
    }

    @Test
    void testMerge() {
        DeploymentTracker newTracker = new DeploymentTracker();

        // appears in both
        tracker.add(builder.build());
        newTracker.add(builder.build());

        // only appears in the new tracker
        newTracker.add(builder.policy(POLICY_B).build());

        PolicyNotification notif = new PolicyNotification();
        tracker.addNotifications(notif, newTracker);

        assertThat(notif.getDeleted()).isEmpty();

        // only policy B should appear
        assertThat(notif.getAdded()).hasSize(1);
        assertThat(notif.getAdded().get(0).getPolicy()).isEqualTo(POLICY_B);
    }

    @Test
    void testNeedNotification() {
        final PolicyStatus oldStat = new PolicyStatus();
        final PolicyStatus newStat = new PolicyStatus();

        // new, complete policy - notify
        assertThat(tracker.needNotification(null, newStat)).isTrue();

        // new, incomplete policy - don't notify
        newStat.setIncompleteCount(1);
        assertThat(tracker.needNotification(null, newStat)).isFalse();
        newStat.setIncompleteCount(0);

        // unchanged - don't notify
        assertThat(tracker.needNotification(oldStat, newStat)).isFalse();

        // was incomplete, now complete - notify
        oldStat.setIncompleteCount(1);
        assertThat(tracker.needNotification(oldStat, newStat)).isTrue();
        oldStat.setIncompleteCount(0);

        // was failed, now ok - notify
        oldStat.setFailureCount(1);
        assertThat(tracker.needNotification(oldStat, newStat)).isTrue();
        oldStat.setFailureCount(0);

        // was failed & incomplete, now complete - notify
        oldStat.setIncompleteCount(1);
        oldStat.setFailureCount(1);
        assertThat(tracker.needNotification(oldStat, newStat)).isTrue();
        oldStat.setIncompleteCount(0);
        oldStat.setFailureCount(0);

        // was complete, now incomplete - notify
        newStat.setIncompleteCount(1);
        assertThat(tracker.needNotification(oldStat, newStat)).isTrue();
        newStat.setIncompleteCount(0);

        // was incomplete, still incomplete - don't notify
        newStat.setIncompleteCount(1);
        oldStat.setIncompleteCount(1);
        assertThat(tracker.needNotification(oldStat, newStat)).isFalse();
        newStat.setIncompleteCount(0);
        oldStat.setIncompleteCount(0);
    }

    @Test
    void testAddMissing() {
        DeploymentTracker newTracker = new DeploymentTracker();

        // appears in both, not waiting
        tracker.add(builder.build());
        newTracker.add(builder.build());

        // appears only in the tracker, not waiting
        tracker.add(builder.policy(POLICY_B).build());

        // appears in both, waiting
        tracker.add(builder.policy(new ToscaConceptIdentifier("PolicyX", VERSION)).state(State.WAITING).build());
        newTracker.add(builder.build());

        // appears only in the tracker, waiting
        tracker.add(builder.policy(new ToscaConceptIdentifier("PolicyY", VERSION)).state(State.WAITING).build());

        // now extract the notifications
        PolicyNotification notif = new PolicyNotification();
        tracker.addNotifications(notif, newTracker);

        assertThat(notif.getDeleted()).isEmpty();

        // only policy B should appear
        assertThat(notif.getAdded()).hasSize(1);
        assertThat(notif.getAdded().get(0).getPolicy()).isEqualTo(POLICY_B);
    }

    @Test
    void testAddStatusAction() {
        tracker.add(new StatusAction(StatusAction.Action.DELETED, builder.build()));
        assertThat(tracker.getDeploymentStatus()).isEmpty();

        tracker.add(new StatusAction(StatusAction.Action.UNCHANGED, builder.build()));
        tracker.add(new StatusAction(StatusAction.Action.CREATED, builder.build()));
        tracker.add(new StatusAction(StatusAction.Action.UPDATED, builder.build()));

        Collection<PolicyStatus> result = tracker.getDeploymentStatus();
        assertThat(result).hasSize(1);
        PolicyStatus status = result.iterator().next();
        assertThat(status.getSuccessCount()).isEqualTo(3);
    }

    @Test
    void testAddPdpPolicyStatus() {
        tracker.add(builder.build());
        Collection<PolicyStatus> result = tracker.getDeploymentStatus();
        assertThat(result).hasSize(1);
        PolicyStatus status = result.iterator().next();

        assertThat(status.getFailureCount()).isZero();
        assertThat(status.getIncompleteCount()).isZero();
        assertThat(status.getSuccessCount()).isEqualTo(1);
        assertThat(status.getPolicy()).isEqualTo(POLICY_A);
        assertThat(status.getPolicyType()).isEqualTo(POLICY_TYPE);

        // add another, failed
        tracker.add(builder.pdpId(PDP_B).state(State.FAILURE).build());
        result = tracker.getDeploymentStatus();
        assertThat(result).hasSize(1);
        status = result.iterator().next();

        assertThat(status.getFailureCount()).isEqualTo(1);
        assertThat(status.getIncompleteCount()).isZero();
        assertThat(status.getSuccessCount()).isEqualTo(1);

        // add another, waiting
        tracker.add(builder.pdpId(PDP_A).state(State.WAITING).build());
        result = tracker.getDeploymentStatus();
        assertThat(result).hasSize(1);
        status = result.iterator().next();

        assertThat(status.getFailureCount()).isEqualTo(1);
        assertThat(status.getIncompleteCount()).isEqualTo(1);
        assertThat(status.getSuccessCount()).isEqualTo(1);

        // different policy
        tracker.add(builder.policy(POLICY_B).pdpId(PDP_A).state(State.WAITING).build());
        result = tracker.getDeploymentStatus();
        assertThat(result).hasSize(2);

        List<PolicyStatus> list = new ArrayList<>(result);
        list.sort(Comparator.comparing(PolicyStatus::getPolicy));
        Iterator<PolicyStatus> iter = list.iterator();

        status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_A);
        assertThat(status.getFailureCount()).isEqualTo(1);
        assertThat(status.getIncompleteCount()).isEqualTo(1);
        assertThat(status.getSuccessCount()).isEqualTo(1);

        status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_B);
        assertThat(status.getFailureCount()).isZero();
        assertThat(status.getIncompleteCount()).isEqualTo(1);
        assertThat(status.getSuccessCount()).isZero();

        // add undeployment record
        tracker.add(builder.deploy(false).build());
        assertThat(tracker.getDeploymentStatus()).hasSize(2);
        assertThat(tracker.getUndeploymentStatus()).hasSize(1);
    }
}
