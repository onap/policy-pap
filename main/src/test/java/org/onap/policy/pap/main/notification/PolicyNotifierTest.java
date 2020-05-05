/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.QueueToken;

public class PolicyNotifierTest extends PolicyCommonSupport {

    @Mock
    private Publisher<PolicyNotification> publisher;

    @Mock
    private PolicyModelsProviderFactoryWrapper daoFactory;

    @Mock
    private PolicyModelsProvider dao;

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

        try {
            when(daoFactory.create()).thenReturn(dao);
            when(dao.getPdpGroups(null)).thenReturn(Collections.emptyList());

            notifier = new MyNotifier(publisher);

        } catch (PfModelException e) {
            throw new PolicyPapRuntimeException(e);
        }
    }

    @Test
    public void testLoadPoliciesPolicyModelsProviderFactoryWrapper() throws PfModelException {
        final PdpGroup group1 = makeGroup("my group #1", makeSubGroup("sub #1 A", 2, policy1, policy4),
                        makeSubGroup("sub #1 B", 1, policy2));

        // one policy is a duplicate
        final PdpGroup group2 = makeGroup("my group #2", makeSubGroup("sub #2 A", 1, policy1, policy3));

        when(dao.getPdpGroups(null)).thenReturn(Arrays.asList(group1, group2));

        ToscaPolicyTypeIdentifier type2 = new ToscaPolicyTypeIdentifier("my other type", "8.8.8");

        // note: no mapping for policy4
        when(dao.getFilteredPolicyList(any())).thenReturn(Arrays.asList(makePolicy(policy1, type),
                        makePolicy(policy2, type2), makePolicy(policy3, type)));

        // load it
        notifier = new MyNotifier(publisher);

        ArgumentCaptor<PolicyPdpNotificationData> captor = ArgumentCaptor.forClass(PolicyPdpNotificationData.class);

        // should have added policy1, policy2, policy1 (duplicate), policy3, but not
        // policy4
        verify(deploy, times(4)).addData(captor.capture());

        Iterator<PolicyPdpNotificationData> iter = captor.getAllValues().iterator();
        PolicyPdpNotificationData data = iter.next();
        assertEquals(policy1, data.getPolicyId());
        assertEquals(type, data.getPolicyType());
        assertEquals("[sub #1 A 0, sub #1 A 1]", data.getPdps().toString());

        data = iter.next();
        assertEquals(policy2, data.getPolicyId());
        assertEquals(type2, data.getPolicyType());
        assertEquals("[sub #1 B 0]", data.getPdps().toString());

        data = iter.next();
        assertEquals(policy1, data.getPolicyId());
        assertEquals(type, data.getPolicyType());
        assertEquals("[sub #2 A 0]", data.getPdps().toString());

        data = iter.next();
        assertEquals(policy3, data.getPolicyId());
        assertEquals(type, data.getPolicyType());
        assertEquals("[sub #2 A 0]", data.getPdps().toString());
    }

    private ToscaPolicy makePolicy(ToscaPolicyIdentifier policyId, ToscaPolicyTypeIdentifier type) {
        ToscaPolicy policy = new ToscaPolicy();

        policy.setName(policyId.getName());
        policy.setVersion(policyId.getVersion());
        policy.setType(type.getName());
        policy.setTypeVersion(type.getVersion());

        return policy;
    }

    private PdpGroup makeGroup(String name, PdpSubGroup... subgrps) {
        final PdpGroup group = new PdpGroup();
        group.setName(name);

        group.setPdpSubgroups(Arrays.asList(subgrps));

        return group;
    }

    private PdpSubGroup makeSubGroup(String name, int numPdps, ToscaPolicyIdentifier... policies) {
        final PdpSubGroup subgrp = new PdpSubGroup();
        subgrp.setPdpType(name);
        subgrp.setPdpInstances(new ArrayList<>(numPdps));

        for (int x = 0; x < numPdps; ++x) {
            Pdp pdp = new Pdp();
            pdp.setInstanceId(name + " " + x);

            subgrp.getPdpInstances().add(pdp);
        }

        subgrp.setPolicies(Arrays.asList(policies));

        return subgrp;
    }

    @Test
    public void testGetStatus() {
        List<PolicyStatus> statusList = Arrays.asList(status1);
        when(deploy.getStatus()).thenReturn(statusList);

        assertSame(statusList, notifier.getStatus());
    }

    @Test
    public void testGetStatusString() {
        List<PolicyStatus> statusList = Arrays.asList(status1);
        when(deploy.getStatus("a policy")).thenReturn(statusList);

        assertSame(statusList, notifier.getStatus("a policy"));
    }

    @Test
    public void testGetStatusToscaPolicyIdentifier() {
        Optional<PolicyStatus> status = Optional.of(status1);
        when(deploy.getStatus(policy1)).thenReturn(status);

        assertSame(status, notifier.getStatus(policy1));
    }

    @Test
    public void testAddDeploymentData() {
        PolicyPdpNotificationData data = makeData(policy1, PDP1, PDP2);
        notifier.addDeploymentData(data);

        verify(deploy).addData(data);
        verify(undeploy).removeData(eq(data));
    }

    @Test
    public void testAddUndeploymentData() {
        PolicyPdpNotificationData data = makeData(policy1, PDP1, PDP2);
        notifier.addUndeploymentData(data);

        verify(undeploy).addData(data);
        verify(deploy).removeData(eq(data));
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
    public void testMakeDeploymentTracker_testMakeUndeploymentTracker() throws PfModelException {
        // make real object, which will invoke the real makeXxx() methods
        new PolicyNotifier(publisher, daoFactory).removePdp(PDP1);

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
            List<PolicyStatus> statusList = invocation.getArgument(listIndex, List.class);
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

        public MyNotifier(Publisher<PolicyNotification> publisher) throws PfModelException {
            super(publisher, daoFactory);
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
