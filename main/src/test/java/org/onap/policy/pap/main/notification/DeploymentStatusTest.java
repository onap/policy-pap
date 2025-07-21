/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2022 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2023, 2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.NonNull;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.PdpPolicyStatusBuilder;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.State;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.notification.StatusAction.Action;
import org.onap.policy.pap.main.service.PolicyStatusService;

class DeploymentStatusTest {

    private static final String VERSION = "1.2.3";
    private static final @NonNull String GROUP_A = "groupA";
    private static final String PDP_A = "pdpA";
    private static final String PDP_B = "pdpB";
    private static final String PDP_C = "pdpC";
    private static final String PDP_D = "pdpD";
    private static final String PDP_TYPE = "MyPdpType";
    private static final ToscaConceptIdentifier POLICY_A = new ToscaConceptIdentifier("MyPolicyA", VERSION);
    private static final ToscaConceptIdentifier POLICY_B = new ToscaConceptIdentifier("MyPolicyB", VERSION);
    private static final ToscaConceptIdentifier POLICY_C = new ToscaConceptIdentifier("MyPolicyC", VERSION);
    private static final ToscaConceptIdentifier POLICY_D = new ToscaConceptIdentifier("MyPolicyD", VERSION);
    private static final ToscaConceptIdentifier POLICY_TYPE = new ToscaConceptIdentifier("MyPolicyType", VERSION);

    private PdpPolicyStatusBuilder builder;

    @Captor
    private ArgumentCaptor<List<PdpPolicyStatus>> created;
    @Captor
    private ArgumentCaptor<List<PdpPolicyStatus>> updated;
    @Captor
    private ArgumentCaptor<List<PdpPolicyStatus>> deleted;

    @Mock
    private PolicyStatusService policyStatusService;

    private DeploymentStatus tracker;

    AutoCloseable autoCloseable;

    /**
     * Set up the meter registry for tests.
     */
    @BeforeAll
    static void setUpBeforeClass() {
        Registry.registerOrReplace(PapConstants.REG_METER_REGISTRY, new SimpleMeterRegistry());
    }

    /**
     * Tear down the meter registry after tests.
     */
    @AfterAll
    static void tearDownAfterClass() {
        Registry.unregister(PapConstants.REG_METER_REGISTRY);
    }

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        tracker = new DeploymentStatus(policyStatusService);

        // @formatter:off
        builder = PdpPolicyStatus.builder()
                        .pdpGroup(GROUP_A)
                        .pdpId(PDP_A)
                        .pdpType(PDP_TYPE)
                        .policy(POLICY_A)
                        .policyType(POLICY_TYPE)
                        .deploy(true)
                        .state(State.SUCCESS);
        // @formatter:on

    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void testAddNotifications() {
        PdpPolicyStatus create = builder.pdpId("created").state(State.FAILURE).build();
        PdpPolicyStatus update = builder.pdpId("updated").state(State.SUCCESS).build();
        PdpPolicyStatus delete = builder.pdpId("deleted").state(State.SUCCESS).build();
        PdpPolicyStatus unchange = builder.pdpId("unchanged").state(State.FAILURE).build();

        // @formatter:off
        tracker.getRecordMap().putAll(makeMap(
                        Action.CREATED, create,
                        Action.UPDATED, update,
                        Action.DELETED, delete,
                        Action.UNCHANGED, unchange
                        ));
        // @formatter:on

        PolicyNotification notif = new PolicyNotification();

        tracker.addNotifications(notif);
        assertThat(notif.getAdded()).hasSize(1);
        assertThat(notif.getDeleted()).isEmpty();

        PolicyStatus status = notif.getAdded().get(0);
        assertThat(status.getFailureCount()).isEqualTo(2);
        assertThat(status.getIncompleteCount()).isZero();
        assertThat(status.getSuccessCount()).isEqualTo(1);
        assertThat(status.getPolicy()).isEqualTo(POLICY_A);
        assertThat(status.getPolicyType()).isEqualTo(POLICY_TYPE);

        /*
         * repeat - should be no notifications
         */
        notif = new PolicyNotification();
        tracker.addNotifications(notif);
        assertThat(notif.getAdded()).isEmpty();
        assertThat(notif.getDeleted()).isEmpty();
    }

    @Test
    void testLoadByGroup() {
        PdpPolicyStatus status1 = builder.build();
        PdpPolicyStatus status2 = builder.policy(POLICY_B).build();
        PdpPolicyStatus status3 = builder.policy(POLICY_A).pdpId(PDP_B).build();

        when(policyStatusService.getGroupPolicyStatus(GROUP_A)).thenReturn(List.of(status1, status2, status3));

        tracker.loadByGroup(GROUP_A);

        // @formatter:off
        assertThat(tracker.getRecordMap()).isEqualTo(makeMap(
            Action.UNCHANGED, status1,
            Action.UNCHANGED, status2,
            Action.UNCHANGED, status3
            ));
        // @formatter:on

        // try again - should not reload
        tracker.loadByGroup(GROUP_A);
        verify(policyStatusService).getGroupPolicyStatus(anyString());
    }

    @Test
    void testFlushPdpNotification() {
        PdpPolicyStatus create = builder.pdpId("created").state(State.FAILURE).build();
        tracker.getRecordMap().putAll(makeMap(Action.CREATED, create));

        PolicyNotification notif = new PolicyNotification();

        tracker.flush(notif);

        assertThat(notif.getAdded()).hasSize(1);
        assertThat(notif.getDeleted()).isEmpty();
    }

    @Test
    void testFlush() {
        PdpPolicyStatus create1 = builder.pdpId("createA").build();
        PdpPolicyStatus create2 = builder.pdpId("createB").build();
        PdpPolicyStatus update1 = builder.pdpId("updateA").build();
        PdpPolicyStatus update2 = builder.pdpId("updateB").build();
        PdpPolicyStatus delete1 = builder.pdpId("deleteA").build();
        PdpPolicyStatus delete2 = builder.pdpId("deleteB").build();
        PdpPolicyStatus unchange1 = builder.pdpId("unchangeA").build();
        PdpPolicyStatus unchange2 = builder.pdpId("unchangeB").build();

        // @formatter:off
        tracker.getRecordMap().putAll(makeMap(
                        Action.CREATED, create1,
                        Action.CREATED, create2,
                        Action.UPDATED, update1,
                        Action.UPDATED, update2,
                        Action.DELETED, delete1,
                        Action.DELETED, delete2,
                        Action.UNCHANGED, unchange1,
                        Action.UNCHANGED, unchange2
                        ));
        // @formatter:on

        tracker.flush();

        verify(policyStatusService).cudPolicyStatus(created.capture(), updated.capture(), deleted.capture());

        assertThat(sort(created.getValue())).isEqualTo(List.of(create1, create2));
        assertThat(sort(updated.getValue())).isEqualTo(List.of(update1, update2));
        assertThat(sort(deleted.getValue())).isEqualTo(List.of(delete1, delete2));

        // @formatter:off
        assertThat(tracker.getRecordMap()).isEqualTo(makeMap(
                        Action.UNCHANGED, create1,
                        Action.UNCHANGED, create2,
                        Action.UNCHANGED, update1,
                        Action.UNCHANGED, update2,
                        Action.UNCHANGED, unchange1,
                        Action.UNCHANGED, unchange2
                        ));
        // @formatter:on
    }

    @Test
    void testDeleteUndeployments() {
        builder.deploy(true);
        PdpPolicyStatus delete = builder.policy(POLICY_A).build();
        PdpPolicyStatus deployedComplete = builder.policy(POLICY_B).build();

        builder.deploy(false);
        PdpPolicyStatus undepComplete1 = builder.policy(POLICY_C).build();
        PdpPolicyStatus undepIncomplete1 = builder.policy(POLICY_D).build();

        builder.pdpId(PDP_B);
        PdpPolicyStatus undepComplete2 = builder.policy(POLICY_C).build();
        PdpPolicyStatus undepIncomplete2 = builder.policy(POLICY_D).state(State.WAITING).build();

        // @formatter:off
        Map<StatusKey, StatusAction> map = makeMap(
                        Action.DELETED, delete,
                        Action.UNCHANGED, deployedComplete,
                        Action.UNCHANGED, undepComplete1,
                        Action.UNCHANGED, undepComplete2,
                        Action.UNCHANGED, undepIncomplete1,
                        Action.UNCHANGED, undepIncomplete2
                        );
        // @formatter:on

        tracker.getRecordMap().putAll(map);

        tracker.deleteUndeployments();

        // the completed undeployments should now be marked DELETED

        // @formatter:off
        assertThat(tracker.getRecordMap()).isEqualTo(makeMap(
                        Action.DELETED, delete,
                        Action.UNCHANGED, deployedComplete,
                        Action.DELETED, undepComplete1,
                        Action.DELETED, undepComplete2,
                        Action.UNCHANGED, undepIncomplete1,
                        Action.UNCHANGED, undepIncomplete2
                        ));
        // @formatter:on
    }

    @Test
    void testDeleteDeploymentString() {
        PdpPolicyStatus statusaa = builder.pdpId(PDP_A).policy(POLICY_A).build();
        PdpPolicyStatus statusab = builder.pdpId(PDP_A).policy(POLICY_B).build();
        PdpPolicyStatus statusba = builder.pdpId(PDP_B).policy(POLICY_A).build();
        PdpPolicyStatus statuscb = builder.pdpId(PDP_C).policy(POLICY_B).build();

        // @formatter:off
        tracker.getRecordMap().putAll(makeMap(
                        Action.UNCHANGED, statusaa,
                        Action.UNCHANGED, statusab,
                        Action.UNCHANGED, statusba,
                        Action.UNCHANGED, statuscb
                        ));
        // @formatter:on

        tracker.deleteDeployment(PDP_A);

        // @formatter:off
        assertThat(tracker.getRecordMap()).isEqualTo(makeMap(
                        Action.DELETED, statusaa,
                        Action.DELETED, statusab,
                        Action.UNCHANGED, statusba,
                        Action.UNCHANGED, statuscb
                        ));
        // @formatter:on
    }

    @Test
    void testDeleteDeploymentToscaConceptIdentifierBoolean() {
        PdpPolicyStatus deploy1A = builder.policy(POLICY_A).build();
        PdpPolicyStatus deploy2A = builder.policy(POLICY_A).pdpId(PDP_B).build();
        PdpPolicyStatus deployB = builder.policy(POLICY_B).pdpId(PDP_A).build();

        builder.deploy(false);
        PdpPolicyStatus undeployA = builder.policy(POLICY_A).build();
        PdpPolicyStatus undeployB = builder.policy(POLICY_B).build();

        // @formatter:off
        tracker.getRecordMap().putAll(makeMap(
                        Action.UNCHANGED, deploy1A,
                        Action.UNCHANGED, deploy2A,
                        Action.UNCHANGED, deployB,
                        Action.UNCHANGED, undeployA,
                        Action.UNCHANGED, undeployB
                        ));
        // @formatter:on

        tracker.deleteDeployment(POLICY_A, true);

        // @formatter:off
        assertThat(tracker.getRecordMap()).isEqualTo(makeMap(
                        Action.DELETED, deploy1A,
                        Action.DELETED, deploy2A,
                        Action.UNCHANGED, deployB,
                        Action.UNCHANGED, undeployA,
                        Action.UNCHANGED, undeployB
                        ));
        // @formatter:on

        tracker.deleteDeployment(POLICY_B, false);

        // @formatter:off
        assertThat(tracker.getRecordMap()).isEqualTo(makeMap(
                        Action.DELETED, deploy1A,
                        Action.DELETED, deploy2A,
                        Action.UNCHANGED, deployB,
                        Action.UNCHANGED, undeployA,
                        Action.DELETED, undeployB
                        ));
        // @formatter:on
    }

    @Test
    void testDeleteDeploymentBiPredicateOfStatusKeyStatusAction() {
        PdpPolicyStatus create1 = builder.pdpId(PDP_A).build();
        PdpPolicyStatus delete = builder.pdpId(PDP_B).build();
        PdpPolicyStatus update = builder.pdpId(PDP_C).build();
        PdpPolicyStatus unchange = builder.pdpId(PDP_D).build();

        PdpPolicyStatus create2 = builder.pdpId(PDP_B).build();

        // @formatter:off
        tracker.getRecordMap().putAll(makeMap(
                        Action.CREATED, create1,
                        Action.CREATED, create2,
                        Action.DELETED, delete,
                        Action.UPDATED, update,
                        Action.UNCHANGED, unchange
                        ));
        // @formatter:on

        tracker.deleteDeployment(POLICY_A, true);

        // @formatter:off
        assertThat(tracker.getRecordMap()).isEqualTo(makeMap(
                        Action.DELETED, delete,
                        Action.DELETED, update,
                        Action.DELETED, unchange
                        ));
        // @formatter:on
    }

    @Test
    void testDeploy() {
        tracker.deploy(PDP_A, POLICY_A, POLICY_TYPE, GROUP_A, PDP_TYPE, true);

        assertThat(tracker.getRecordMap()).hasSize(1);

        StatusAction status2 = tracker.getRecordMap().values().iterator().next();

        assertThat(status2.getAction()).isEqualTo(Action.CREATED);
        assertThat(status2.getStatus().getState()).isEqualTo(State.WAITING);
        assertThat(status2.getStatus().isDeploy()).isTrue();

        /*
         * repeat - should be the same status
         */
        tracker.deploy(PDP_A, POLICY_A, POLICY_TYPE, GROUP_A, PDP_TYPE, true);

        assertThat(tracker.getRecordMap()).hasSize(1);
        assertThat(tracker.getRecordMap().values().iterator().next()).isSameAs(status2);
        assertThat(status2.getAction()).isEqualTo(Action.CREATED);
        assertThat(status2.getStatus().getState()).isEqualTo(State.WAITING);
        assertThat(status2.getStatus().isDeploy()).isTrue();

        /*
         * repeat, with different values - should be unchanged
         */
        status2.setAction(Action.UNCHANGED);
        status2.getStatus().setDeploy(true);
        status2.getStatus().setState(State.SUCCESS);

        tracker.deploy(PDP_A, POLICY_A, POLICY_TYPE, GROUP_A, PDP_TYPE, true);

        assertThat(tracker.getRecordMap()).hasSize(1);
        assertThat(tracker.getRecordMap().values().iterator().next()).isSameAs(status2);
        assertThat(status2.getAction()).isEqualTo(Action.UNCHANGED);
        assertThat(status2.getStatus().getState()).isEqualTo(State.SUCCESS);
        assertThat(status2.getStatus().isDeploy()).isTrue();

        /*
         * incorrect "deploy" value - should update it
         */
        status2.setAction(Action.UNCHANGED);
        status2.getStatus().setDeploy(true);

        tracker.deploy(PDP_A, POLICY_A, POLICY_TYPE, GROUP_A, PDP_TYPE, false);

        assertThat(status2.getAction()).isEqualTo(Action.UPDATED);
        assertThat(status2.getStatus().getState()).isEqualTo(State.WAITING);
        assertThat(status2.getStatus().isDeploy()).isFalse();

        /*
         * marked for deletion - should reinstate it
         */
        status2.setAction(Action.DELETED);
        status2.getStatus().setState(State.FAILURE);
        status2.getStatus().setDeploy(false);

        tracker.deploy(PDP_A, POLICY_A, POLICY_TYPE, GROUP_A, PDP_TYPE, false);

        assertThat(status2.getAction()).isEqualTo(Action.UPDATED);
        assertThat(status2.getStatus().getState()).isEqualTo(State.FAILURE);
        assertThat(status2.getStatus().isDeploy()).isFalse();
    }

    @Test
    void testCompleteDeploy() {
        tracker.deploy(PDP_A, POLICY_A, POLICY_TYPE, GROUP_A, PDP_TYPE, true);
        assertThat(tracker.getRecordMap()).hasSize(1);

        // deployed, but not expected to be deployed - record should be left as is
        checkCompleteDeploy(true, Set.of(), Set.of(), Action.UNCHANGED, State.WAITING);
        checkCompleteDeploy(true, Set.of(), Set.of(POLICY_A), Action.UNCHANGED, State.WAITING);

        // expected, but not actually deployed - failure
        checkCompleteDeploy(true, Set.of(POLICY_A), Set.of(), Action.UPDATED, State.FAILURE);

        // expected and actually deployed - success
        checkCompleteDeploy(true, Set.of(POLICY_A), Set.of(POLICY_A), Action.UPDATED, State.SUCCESS);
        checkCompleteDeploy(true, Set.of(POLICY_A, POLICY_B), Set.of(POLICY_A), Action.UPDATED, State.SUCCESS);

        // not expected and not actually deployed - success
        checkCompleteDeploy(false, Set.of(), Set.of(), Action.UPDATED, State.SUCCESS);

        // not expected, but actually deployed - failure
        checkCompleteDeploy(false, Set.of(), Set.of(POLICY_A), Action.UPDATED, State.FAILURE);

        // undeployed, but expected to be deployed - record should be left as is
        checkCompleteDeploy(false, Set.of(POLICY_A), Set.of(), Action.UNCHANGED, State.WAITING);
        checkCompleteDeploy(false, Set.of(POLICY_A), Set.of(POLICY_A), Action.UNCHANGED, State.WAITING);
        checkCompleteDeploy(false, Set.of(POLICY_A, POLICY_B), Set.of(POLICY_A), Action.UNCHANGED, State.WAITING);

        /*
         * Try a case where the state is already correct.
         */
        StatusAction status = tracker.getRecordMap().values().iterator().next();
        status.getStatus().setDeploy(false);
        status.setAction(Action.UNCHANGED);
        status.getStatus().setState(State.SUCCESS);

        tracker.completeDeploy(PDP_A, Set.of(), Set.of());

        assertThat(status.getAction()).isEqualTo(Action.UNCHANGED);
        assertThat(status.getStatus().getState()).isEqualTo(State.SUCCESS);

        /*
         * Try a case where the PDP does not match the record.
         */
        status.getStatus().setDeploy(false);
        status.setAction(Action.UNCHANGED);
        status.getStatus().setState(State.WAITING);

        tracker.completeDeploy(PDP_B, Set.of(), Set.of());

        assertThat(status.getAction()).isEqualTo(Action.UNCHANGED);
        assertThat(status.getStatus().getState()).isEqualTo(State.WAITING);
    }

    private void checkCompleteDeploy(boolean deploy, Set<ToscaConceptIdentifier> expected,
                                     Set<ToscaConceptIdentifier> actual, Action action, State state) {

        StatusAction status = tracker.getRecordMap().values().iterator().next();
        status.getStatus().setDeploy(deploy);
        status.setAction(Action.UNCHANGED);
        status.getStatus().setState(State.WAITING);

        tracker.completeDeploy(PDP_A, expected, actual);

        assertThat(status.getAction()).isEqualTo(action);
        assertThat(status.getStatus().getState()).isEqualTo(state);
    }

    private List<PdpPolicyStatus> sort(List<PdpPolicyStatus> list) {

        list.sort((rec1, rec2) -> {

            // @formatter:off
            return new CompareToBuilder()
                            .append(rec1.getPdpId(), rec2.getPdpId())
                            .append(rec1.getPolicy(), rec2.getPolicy())
                            .toComparison();
            // @formatter:on
        });

        return list;
    }

    /**
     * Makes a map.
     *
     * @param data pairs of (Action, PdpPolicyStatus)
     * @return a new map containing the given data
     */
    private Map<StatusKey, StatusAction> makeMap(Object... data) {
        Map<StatusKey, StatusAction> map = new HashMap<>();

        assert (data.length % 2 == 0);

        for (int idata = 0; idata < data.length; idata += 2) {
            Action action = (Action) data[idata];
            PdpPolicyStatus status = (PdpPolicyStatus) data[idata + 1];
            map.put(new StatusKey(status), new StatusAction(action, status));
        }

        return map;
    }
}
