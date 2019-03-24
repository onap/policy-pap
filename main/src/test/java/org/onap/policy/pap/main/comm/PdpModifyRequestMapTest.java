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

package org.onap.policy.pap.main.comm;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.common.endpoints.listeners.TypedMessageListener;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.simple.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap.ModifyReqData;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.onap.policy.pap.main.parameters.PdpStateChangeParameters;
import org.onap.policy.pap.main.parameters.PdpUpdateParameters;
import org.powermock.reflect.Whitebox;

public class PdpModifyRequestMapTest {
    private static final String DIFFERENT = "-diff";
    private static final String PDP1 = "pdp_1";

    private static final int UPDATE_RETRIES = 2;
    private static final int STATE_RETRIES = 1;

    private PdpModifyRequestMap map;
    private Publisher pub;
    private RequestIdDispatcher<PdpStatus> disp;
    private Object lock;
    private TimerManager updTimers;
    private TimerManager stateTimers;
    private TimerManager.Timer timer;
    private Queue<QueueToken<PdpMessage>> queue;
    private PdpStatus response;
    private PdpParameters pdpParams;
    private PdpUpdateParameters updParams;
    private PdpStateChangeParameters stateParams;
    private PdpUpdate update;
    private PdpStateChange state;
    private String mismatchReason;

    /**
     * Sets up.
     */
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        pub = mock(Publisher.class);
        disp = mock(RequestIdDispatcher.class);
        lock = new Object();
        updTimers = mock(TimerManager.class);
        stateTimers = mock(TimerManager.class);
        timer = mock(TimerManager.Timer.class);
        queue = new LinkedList<>();
        response = new PdpStatus();
        pdpParams = mock(PdpParameters.class);
        updParams = mock(PdpUpdateParameters.class);
        stateParams = mock(PdpStateChangeParameters.class);
        update = makeUpdate();
        state = makeStateChange();
        mismatchReason = null;

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                queue.add(invocation.getArgumentAt(0, QueueToken.class));
                return null;
            }
        }).when(pub).enqueue(any());

        when(updTimers.register(any(), any())).thenReturn(timer);
        when(stateTimers.register(any(), any())).thenReturn(timer);

        when(pdpParams.getUpdateParameters()).thenReturn(updParams);
        when(pdpParams.getStateChangeParameters()).thenReturn(stateParams);

        when(updParams.getMaxRetryCount()).thenReturn(UPDATE_RETRIES);
        when(updParams.getMaxWaitMs()).thenReturn(1000L);

        when(stateParams.getMaxRetryCount()).thenReturn(STATE_RETRIES);
        when(stateParams.getMaxWaitMs()).thenReturn(1000L);

        response.setName(PDP1);
        response.setState(PdpState.SAFE);
        response.setPdpGroup(update.getPdpGroup());
        response.setPdpSubgroup(update.getPdpSubgroup());
        response.setPolicies(update.getPolicies());

        map = new PdpModifyRequestMap(makeParameters()) {

            @Override
            protected ModifyReqData makeRequestData(PdpUpdate update, PdpStateChange stateChange) {
                return new ModifyReqData(update, stateChange) {
                    @Override
                    protected void mismatch(String reason) {
                        mismatchReason = reason;
                        super.mismatch(reason);
                    }
                };
            }
        };

        map = spy(map);
    }

    @Test
    public void testAdd_DifferentPdps() {
        map.addRequest(update);

        state.setName(DIFFERENT);
        map.addRequest(state);

        assertNotNull(getReqData(PDP1));
        assertNotNull(getReqData(DIFFERENT));

        assertQueueContains("testAdd_DifferentPdps", update, state);
    }

    @Test
    public void testAddRequestPdpUpdate() {
        map.addRequest(update);

        assertQueueContains("testAddRequestPdpUpdate", update);
    }

    @Test
    public void testAddRequestPdpStateChange() {
        map.addRequest(state);

        assertQueueContains("testAddRequestPdpStateChange", state);
    }

    @Test
    public void testAddRequestPdpUpdatePdpStateChange_Both() {
        map.addRequest(update, state);

        assertQueueContains("testAddRequestPdpUpdatePdpStateChange_Both", update);
    }

    @Test
    public void testAddRequestPdpUpdatePdpStateChange_BothNull() {
        map.addRequest(null, null);

        // nothing should have been added to the queue
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testGetPdpName_SameNames() {
        // should be no exception
        map.addRequest(update, state);
    }

    @Test
    public void testGetPdpName_DifferentNames() {
        // should be no exception
        state.setName(update.getName() + "X");
        assertThatIllegalArgumentException().isThrownBy(() -> map.addRequest(update, state))
                        .withMessageContaining("does not match");
    }

    @Test
    public void testGetPdpName_NullUpdateName() {
        update.setName(null);
        assertThatIllegalArgumentException().isThrownBy(() -> map.addRequest(update)).withMessageContaining("update");

        assertThatIllegalArgumentException().isThrownBy(() -> map.addRequest(update, state))
                        .withMessageContaining("update");

        // both names are null
        state.setName(null);
        assertThatIllegalArgumentException().isThrownBy(() -> map.addRequest(update, state));
    }

    @Test
    public void testGetPdpName_NullStateName() {
        state.setName(null);
        assertThatIllegalArgumentException().isThrownBy(() -> map.addRequest(state)).withMessageContaining("state");

        assertThatIllegalArgumentException().isThrownBy(() -> map.addRequest(update, state))
                        .withMessageContaining("state");

        // both names are null
        update.setName(null);
        assertThatIllegalArgumentException().isThrownBy(() -> map.addRequest(update, state));
    }

    @Test
    public void testIsSamePdpUpdatePdpUpdate() {
        map.addRequest(update);

        // queue a similar request
        PdpUpdate update2 = makeUpdate();
        map.addRequest(update2);

        // token should still have original message
        assertQueueContains("testIsSamePdpUpdatePdpUpdate", update);
    }

    @Test
    public void testIsSamePdpUpdatePdpUpdate_DifferentPolicyCount() {
        map.addRequest(update);

        PdpUpdate update2 = makeUpdate();
        update2.setPolicies(Arrays.asList(update.getPolicies().get(0)));
        map.addRequest(update2);

        // should have replaced the message in the token
        assertQueueContains("testIsSamePdpUpdatePdpUpdate_DifferentPolicyCount", update2);
    }

    @Test
    public void testIsSamePdpUpdatePdpUpdate_DifferentGroup() {
        map.addRequest(update);

        // queue a similar request
        PdpUpdate update2 = makeUpdate();
        update2.setPdpGroup(update.getPdpGroup() + DIFFERENT);
        map.addRequest(update2);

        // should have replaced the message in the token
        assertQueueContains("testIsSamePdpUpdatePdpUpdate_DifferentGroup", update2);
    }

    @Test
    public void testIsSamePdpUpdatePdpUpdate_DifferentSubGroup() {
        map.addRequest(update);

        PdpUpdate update2 = makeUpdate();
        update2.setPdpSubgroup(update.getPdpSubgroup() + DIFFERENT);
        map.addRequest(update2);

        // should have replaced the message in the token
        assertQueueContains("testIsSamePdpUpdatePdpUpdate_DifferentSubGroup", update2);
    }

    @Test
    public void testIsSamePdpUpdatePdpUpdate_DifferentPolicies() {
        map.addRequest(update);

        ArrayList<ToscaPolicy> policies = new ArrayList<>(update.getPolicies());
        policies.set(0, new ToscaPolicy(new PfConceptKey("policy-3-x", "2.0.0")));

        PdpUpdate update2 = makeUpdate();
        update2.setPolicies(policies);
        map.addRequest(update2);

        // should have replaced the message in the token
        assertQueueContains("testIsSamePdpUpdatePdpUpdate_DifferentPolicies", update2);
    }

    @Test
    public void testIsSamePdpStateChangePdpStateChange() {
        map.addRequest(state);

        // queue a similar request
        PdpStateChange state2 = makeStateChange();
        map.addRequest(state2);

        // token should still have original message
        assertQueueContains("testIsSamePdpStateChangePdpStateChange", state);
    }

    @Test
    public void testIsSamePdpStateChangePdpStateChange_DifferentState() {
        map.addRequest(state);

        // queue a similar request
        PdpStateChange state2 = makeStateChange();
        state2.setState(PdpState.TERMINATED);
        map.addRequest(state2);

        // should have replaced the message in the token
        assertQueueContains("testIsSamePdpStateChangePdpStateChange_DifferentState", state2);
    }

    @Test
    public void testModifyReqDataIsActive() {
        map.addRequest(update);

        invokeProcessResponse();

        // name should have been removed
        assertNull(getReqData(PDP1));
    }

    @Test
    public void testModifyReqDataAddPdpUpdate() {
        map.addRequest(state);

        map.addRequest(update);

        // update should have replaced the state-change in the queue
        assertQueueContains("testModifyReqDataAddPdpUpdate", update);
    }

    @Test
    public void testModifyReqDataAddPdpStateChange() {
        map.addRequest(update);

        map.addRequest(state);

        // update should still be in the queue
        assertQueueContains("testModifyReqDataAddPdpStateChange", update);
    }

    @Test
    public void testModifyReqDataRetryCountExhausted() {
        map.addRequest(state);

        // timeout twice so that retry count is exhausted
        invokeTimeoutHandler(stateTimers, STATE_RETRIES + 1);

        // name should have been removed
        assertNull(getReqData(PDP1));
    }

    @Test
    public void testModifyReqDataMismatch() {
        map.addRequest(state);

        // set up a response with incorrect info
        response.setName(state.getName() + DIFFERENT);

        invokeProcessResponse();

        assertNotNull(mismatchReason);

        // name should have been removed
        assertNull(getReqData(PDP1));
    }

    @Test
    public void testUpdateDataGetMaxRetryCount() {
        map.addRequest(update);
        ModifyReqData reqdata = getReqData(PDP1);

        for (int count = 0; count < UPDATE_RETRIES; ++count) {
            assertTrue("update bump " + count, reqdata.bumpRetryCount());
        }

        assertFalse("update bump final", reqdata.bumpRetryCount());
    }

    @Test
    public void testUpdateCheckResponse() {
        map.addRequest(update);

        invokeProcessResponse();

        assertNull(mismatchReason);
    }

    @Test
    public void testUpdateDataCheckResponse_MismatchedName() {
        map.addRequest(update);

        response.setName(DIFFERENT);
        invokeProcessResponse();

        assertEquals("name does not match", mismatchReason);
    }

    @Test
    public void testUpdateDataCheckResponse_MismatchedGroup() {
        map.addRequest(update);

        response.setPdpGroup(DIFFERENT);
        invokeProcessResponse();

        assertEquals("group does not match", mismatchReason);
    }

    @Test
    public void testUpdateDataCheckResponse_MismatchedSubGroup() {
        map.addRequest(update);

        response.setPdpSubgroup(DIFFERENT);
        invokeProcessResponse();

        assertEquals("subgroup does not match", mismatchReason);
    }

    @Test
    public void testUpdateDataCheckResponse_MismatchedPoliciesLength() {
        map.addRequest(update);

        response.setPolicies(Arrays.asList(update.getPolicies().get(0)));
        invokeProcessResponse();

        assertEquals("policies do not match", mismatchReason);
    }

    @Test
    public void testUpdateDataCheckResponse_MismatchedPolicies() {
        map.addRequest(update);

        ArrayList<ToscaPolicy> policies = new ArrayList<>(update.getPolicies());
        policies.set(0, new ToscaPolicy(new PfConceptKey(DIFFERENT, "10.0.0")));

        response.setPolicies(policies);
        invokeProcessResponse();

        assertEquals("policies do not match", mismatchReason);
    }

    @Test
    public void testUpdateDataCheckResponseCompleted() {
        map.addRequest(update);

        invokeProcessResponse();

        assertNull(getReqData(PDP1));
    }

    @Test
    public void testUpdateDataCheckResponseCompleted_MoreToGo() {
        map.addRequest(update, state);

        invokeProcessResponse();

        assertNotNull(getReqData(PDP1));

        assertSame(state, queue.poll().get());
    }

    @Test
    public void testStateChangeDataGetMaxRetryCount() {
        map.addRequest(state);
        ModifyReqData reqdata = getReqData(PDP1);

        for (int count = 0; count < STATE_RETRIES; ++count) {
            assertTrue("state bump " + count, reqdata.bumpRetryCount());
        }

        assertFalse("state bump final", reqdata.bumpRetryCount());
    }

    @Test
    public void testStateChangeCheckResponse() {
        map.addRequest(state);

        invokeProcessResponse();

        assertNull(mismatchReason);
    }

    @Test
    public void testStateChangeCheckResponse_MismatchedName() {
        map.addRequest(state);

        response.setName(DIFFERENT);
        invokeProcessResponse();

        assertEquals("name does not match", mismatchReason);
    }

    @Test
    public void testStateChangeCheckResponse_MismatchedState() {
        map.addRequest(state);

        response.setState(PdpState.TERMINATED);
        invokeProcessResponse();

        assertEquals("state is TERMINATED, but expected SAFE", mismatchReason);
    }

    @Test
    public void testMakeRequestData() {
        // need a map that doesn't override the method
        map = new PdpModifyRequestMap(makeParameters());

        // this will invoke makeRequestData() - should not throw an exception
        map.addRequest(update);

        assertNotNull(getReqData(PDP1));
    }

    /**
     * Asserts that the queue contains the specified messages.
     *
     * @param testName the test name
     * @param messages messages that are expected in the queue
     */
    private void assertQueueContains(String testName, PdpMessage... messages) {
        assertEquals(testName, messages.length, queue.size());

        int count = 0;
        for (PdpMessage msg : messages) {
            ++count;

            QueueToken<PdpMessage> token = queue.remove();
            assertSame(testName + "-" + count, msg, token.get());
        }
    }

    /**
     * Makes parameters to configure a map.
     *
     * @return new parameters
     */
    private PdpModifyRequestMapParams makeParameters() {
        return new PdpModifyRequestMapParams().setModifyLock(lock).setParams(pdpParams).setPublisher(pub)
                        .setResponseDispatcher(disp).setStateChangeTimers(stateTimers).setUpdateTimers(updTimers);
    }

    /**
     * Gets the listener that was registered with the dispatcher and invokes it.
     *
     * @return the response processor
     */
    @SuppressWarnings("unchecked")
    private TypedMessageListener<PdpStatus> invokeProcessResponse() {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<TypedMessageListener> processResp = ArgumentCaptor.forClass(TypedMessageListener.class);

        // indicate that is has been published
        queue.remove().replaceItem(null);

        verify(disp).register(any(), processResp.capture());

        TypedMessageListener<PdpStatus> func = processResp.getValue();
        func.onTopicEvent(CommInfrastructure.NOOP, PapConstants.TOPIC_POLICY_PDP_PAP, response);

        return func;
    }

    /**
     * Gets the timeout handler that was registered with the timer manager and invokes it.
     *
     * @param timers the timer manager whose handler is to be invoked
     * @param ntimes number of times to invoke the timeout handler
     * @return the timeout handler
     */
    @SuppressWarnings("unchecked")
    private void invokeTimeoutHandler(TimerManager timers, int ntimes) {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Consumer> timeoutHdlr = ArgumentCaptor.forClass(Consumer.class);

        for (int count = 1; count <= ntimes; ++count) {
            // indicate that is has been published
            queue.remove().replaceItem(null);

            verify(timers, times(count)).register(any(), timeoutHdlr.capture());

            @SuppressWarnings("rawtypes")
            List<Consumer> lst = timeoutHdlr.getAllValues();

            Consumer<String> hdlr = lst.get(lst.size() - 1);
            hdlr.accept(PDP1);
        }
    }

    /**
     * Gets the request data from the map.
     *
     * @param pdpName name of the PDP whose data is desired
     * @return the request data, or {@code null} if the PDP is not in the map
     */
    private ModifyReqData getReqData(String pdpName) {
        Map<String, ModifyReqData> name2data = Whitebox.getInternalState(map, "name2data");
        return name2data.get(pdpName);
    }

    /**
     * Makes an update message.
     *
     * @return a new update message
     */
    private PdpUpdate makeUpdate() {
        PdpUpdate upd = new PdpUpdate();

        upd.setDescription("update-description");
        upd.setName(PDP1);
        upd.setPdpGroup("group1-a");
        upd.setPdpSubgroup("sub1-a");
        upd.setPdpType("drools");

        ToscaPolicy policy1 = new ToscaPolicy(new PfConceptKey("policy-1-a", "1.0.0"));
        ToscaPolicy policy2 = new ToscaPolicy(new PfConceptKey("policy-2-a", "1.1.0"));

        upd.setPolicies(Arrays.asList(policy1, policy2));

        return upd;
    }

    /**
     * Makes a state-change message.
     *
     * @return a new state-change message
     */
    private PdpStateChange makeStateChange() {
        PdpStateChange cng = new PdpStateChange();

        cng.setName(PDP1);
        cng.setState(PdpState.SAFE);

        return cng;
    }
}
