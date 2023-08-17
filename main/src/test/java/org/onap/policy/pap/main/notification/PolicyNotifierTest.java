/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023 Nordix Foundation.
 * Modifications Copyright (C) 2022 Bell Canada. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.QueueToken;
import org.onap.policy.pap.main.service.PolicyStatusService;

@RunWith(MockitoJUnitRunner.class)
class PolicyNotifierTest {
    private static final String GROUP_A = "groupA";
    private static final String PDP1 = "pdp-1";
    private static final ToscaConceptIdentifier policy1 = new ToscaConceptIdentifier("policy1", "1.2.3");
    private static final ToscaConceptIdentifier policy2 = new ToscaConceptIdentifier("policy2", "1.2.3");

    @Mock
    private Publisher<PolicyNotification> publisher;

    @Mock
    private PolicyStatusService policyStatusService;

    @Mock
    private DeploymentStatus tracker;

    @Captor
    ArgumentCaptor<QueueToken<PolicyNotification>> notifyCaptor;

    private MyNotifier notifier;

    AutoCloseable closeable;

    /**
     * Creates various objects, including {@link #notifier}.
     */
    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        try {
            when(policyStatusService.getGroupPolicyStatus(anyString())).thenReturn(Collections.emptyList());
            Registry.registerOrReplace(PapConstants.REG_METER_REGISTRY, new SimpleMeterRegistry());
            notifier = new MyNotifier(publisher);

        } catch (PfModelException e) {
            throw new PolicyPapRuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testProcessResponseString() {
        Set<ToscaConceptIdentifier> expected = Set.of(policy1);
        Set<ToscaConceptIdentifier> actual = Set.of(policy2);

        // add a status to the notification when tracker.flush(notif) is called
        doAnswer(invocation -> {
            PolicyNotification notif = invocation.getArgument(0);
            notif.getAdded().add(new PolicyStatus());
            return null;
        }).when(tracker).flush(any());

        notifier.processResponse(PDP1, GROUP_A, expected, actual);

        verify(tracker).loadByGroup(GROUP_A);
        verify(tracker).completeDeploy(PDP1, expected, actual);
        verify(tracker).flush(any());

        verify(publisher).enqueue(any());
    }

    @Test
    void testProcessResponseString_Ex() {
        doThrow(new PfModelRuntimeException(Status.BAD_REQUEST, "expected exception")).when(tracker)
            .loadByGroup(anyString());

        assertThatCode(() -> notifier.processResponse(PDP1, GROUP_A, Set.of(), Set.of())).doesNotThrowAnyException();
    }

    /**
     * Tests publish(), when the notification is empty.
     */
    @Test
    void testPublishEmpty() {
        notifier.publish(new PolicyNotification());
        verify(publisher, never()).enqueue(any());
    }

    /**
     * Tests publish(), when the notification is NOT empty.
     */
    @Test
    void testPublishNotEmpty() {
        PolicyNotification notif = new PolicyNotification();
        notif.getAdded().add(new PolicyStatus());

        notifier.publish(notif);

        verify(publisher).enqueue(any());
    }

    @Test
    void testMakeDeploymentTracker() {
        // make real object, which will invoke the real makeXxx() methods
        PolicyNotifier policyNotifier = new PolicyNotifier(policyStatusService);
        policyNotifier.setPublisher(publisher);
        policyNotifier.processResponse(PDP1, GROUP_A, Set.of(), Set.of());

        verify(policyStatusService).getGroupPolicyStatus(GROUP_A);
    }


    private class MyNotifier extends PolicyNotifier {

        public MyNotifier(Publisher<PolicyNotification> publisher) throws PfModelException {
            super(policyStatusService);
            super.setPublisher(publisher);
        }

        @Override
        protected DeploymentStatus makeDeploymentTracker() {
            return tracker;
        }
    }
}
