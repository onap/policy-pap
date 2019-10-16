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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.QueueToken;

public class PolicyNotifierTest extends PolicyCommonSupport {

    @Mock
    private Publisher<PolicyNotification> publisher;

    @Mock
    private PolicyDeployTracker deploy;

    @Mock
    private PolicyUndeployTracker undeploy;

    @Mock
    private PolicyStatus status1;

    @Mock
    private PolicyStatus status2;

    @Mock
    private PolicyStatus status3;

    @Mock
    private PolicyStatus status4;

    @Captor
    ArgumentCaptor<QueueToken<PolicyNotification>> notifyCaptor;

    private MyNotifier notifier;

    /**
     * Creates various objects, including {@link #notifier}.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        super.setUp();

        notifier = new MyNotifier(publisher);
    }

    @Test
    public void testAddDeploymentData() {
        doAnswer(addStatus(1, status1, status2)).when(undeploy).removeData(any(), any());

        PolicyPdpNotificationData data = makeData(policy1, PDP1, PDP2);
        notifier.addDeploymentData(data);

        verify(deploy).addData(data);
        verify(undeploy).removeData(eq(data), any());

        PolicyNotification notification = getNotification();
        assertEquals(Arrays.asList(status1, status2), notification.getDeleted());
        assertTrue(notification.getAdded().isEmpty());
    }

    @Test
    public void testAddUndeploymentData() {
        doAnswer(addStatus(1, status1, status2)).when(deploy).removeData(any(), any());

        PolicyPdpNotificationData data = makeData(policy1, PDP1, PDP2);
        notifier.addUndeploymentData(data);

        verify(undeploy).addData(data);
        verify(deploy).removeData(eq(data), any());

        PolicyNotification notification = getNotification();
        assertEquals(Arrays.asList(status1, status2), notification.getAdded());
        assertTrue(notification.getDeleted().isEmpty());
    }

    @Test
    public void testProcessResponseString() {
        doAnswer(addStatus(2, status1, status2)).when(deploy).processResponse(eq(PDP1), any(), any());
        doAnswer(addStatus(2, status3, status4)).when(undeploy).processResponse(eq(PDP1), any(), any());

        List<ToscaPolicyIdentifier> activePolicies = Arrays.asList(policy1, policy2);
        notifier.processResponse(PDP1, activePolicies);

        PolicyNotification notification = getNotification();
        assertEquals(Arrays.asList(status1, status2), notification.getAdded());
        assertEquals(Arrays.asList(status3, status4), notification.getDeleted());
    }

    @Test
    public void testRemovePdp() {
        doAnswer(addStatus(1, status1, status2)).when(deploy).removePdp(eq(PDP1), any());
        doAnswer(addStatus(1, status3, status4)).when(undeploy).removePdp(eq(PDP1), any());

        notifier.removePdp(PDP1);

        PolicyNotification notification = getNotification();
        assertEquals(Arrays.asList(status1, status2), notification.getAdded());
        assertEquals(Arrays.asList(status3, status4), notification.getDeleted());
    }

    /**
     * Tests publish(), when the notification is empty.
     */
    @Test
    public void testPublishEmpty() {
        notifier.removePdp(PDP1);

        verify(publisher, never()).enqueue(any());
    }

    /**
     * Tests publish(), when the notification is NOT empty.
     */
    @Test
    public void testPublishNotEmpty() {
        doAnswer(addStatus(1, status1, status2)).when(deploy).removePdp(eq(PDP1), any());

        notifier.removePdp(PDP1);

        verify(publisher).enqueue(any());
    }

    @Test
    public void testMakeDeploymentTracker_testMakeUndeploymentTracker() {
        // make real object, which will invoke the real makeXxx() methods
        new PolicyNotifier(publisher).removePdp(PDP1);

        verify(publisher, never()).enqueue(any());
    }

    /**
     * Creates an answer that adds status updates to a status list.
     *
     * @param listIndex index of the status list within the argument list
     * @param status status updates to be added
     * @return an answer that adds the given status updates
     */
    private Answer<Void> addStatus(int listIndex, PolicyStatus... status) {
        return invocation -> {
            @SuppressWarnings("unchecked")
            List<PolicyStatus> statusList = invocation.getArgumentAt(listIndex, List.class);
            statusList.addAll(Arrays.asList(status));
            return null;
        };
    }

    /**
     * Gets the notification that was published.
     *
     * @return the notification that was published
     */
    private PolicyNotification getNotification() {
        verify(publisher).enqueue(notifyCaptor.capture());
        return notifyCaptor.getValue().get();
    }


    private class MyNotifier extends PolicyNotifier {

        public MyNotifier(Publisher<PolicyNotification> publisher) {
            super(publisher);
        }

        @Override
        protected PolicyDeployTracker makeDeploymentTracker() {
            return deploy;
        }

        @Override
        protected PolicyUndeployTracker makeUndeploymentTracker() {
            return undeploy;
        }
    }
}
