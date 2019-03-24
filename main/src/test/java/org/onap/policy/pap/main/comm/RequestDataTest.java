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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
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
import org.onap.policy.common.utils.services.ServiceManager;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.parameters.RequestDataParams;
import org.powermock.reflect.Whitebox;

public class RequestDataTest {
    private static final String PDP1 = "pdp_1";
    private static final String MY_MSG_TYPE = "my-type";

    private MyRequestData reqdata;
    private Publisher pub;
    private RequestIdDispatcher<PdpStatus> disp;
    private Object lock;
    private TimerManager timers;
    private TimerManager.Timer timer;
    private MyMessageData msgdata;
    private Queue<QueueToken<PdpMessage>> queue;
    private PdpStatus response;

    /**
     * Sets up.
     */
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        pub = mock(Publisher.class);
        disp = mock(RequestIdDispatcher.class);
        lock = new Object();
        timers = mock(TimerManager.class);
        timer = mock(TimerManager.Timer.class);
        msgdata = new MyMessageData(PDP1);
        queue = new LinkedList<>();
        response = new PdpStatus();

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                queue.add(invocation.getArgumentAt(0, QueueToken.class));
                return null;
            }
        }).when(pub).enqueue(any());

        when(timers.register(any(), any())).thenReturn(timer);

        reqdata = new MyRequestData(
                        new RequestDataParams().setModifyLock(lock).setPublisher(pub).setResponseDispatcher(disp));

        reqdata.setName(PDP1);

        msgdata = spy(msgdata);
        reqdata = spy(reqdata);
    }

    @Test
    public void testRequestData_Invalid() {
        // null params
        assertThatThrownBy(() -> new MyRequestData(null)).isInstanceOf(NullPointerException.class);

        // invalid params
        assertThatIllegalArgumentException().isThrownBy(() -> new MyRequestData(new RequestDataParams()));
    }

    @Test
    public void testStartPublishing() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        verify(disp).register(eq(msgdata.getMessage().getRequestId()), any());
        verify(timers).register(eq(PDP1), any());
        verify(pub).enqueue(any());

        QueueToken<PdpMessage> token = queue.poll();
        assertNotNull(token);
        assertSame(msgdata.getMessage(), token.get());


        // invoking start() again has no effect - invocation counts remain the same
        reqdata.startPublishing();
        verify(disp, times(1)).register(eq(msgdata.getMessage().getRequestId()), any());
        verify(timers, times(1)).register(eq(PDP1), any());
        verify(pub, times(1)).enqueue(any());
    }

    @Test
    public void testStopPublishing() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();
        reqdata.stopPublishing();

        verify(disp).unregister(msgdata.getMessage().getRequestId());
        verify(timer).cancel();


        // invoking stop() again has no effect - invocation counts remain the same
        reqdata.stopPublishing();

        verify(disp, times(1)).unregister(msgdata.getMessage().getRequestId());
        verify(timer, times(1)).cancel();
    }

    @Test
    public void testConfigure() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        verify(disp).register(eq(msgdata.getMessage().getRequestId()), any());
        verify(timers).register(eq(PDP1), any());
        verify(pub).enqueue(any());

        ServiceManager svcmgr = Whitebox.getInternalState(reqdata, "svcmgr");
        assertEquals(PDP1 + " " + MY_MSG_TYPE, svcmgr.getName());


        // bump this so we can verify that it is reset by configure()
        reqdata.bumpRetryCount();

        reqdata.configure(msgdata);
        assertEquals(0, getRetryCount());
    }

    @Test
    public void testEnqueue() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        // replace the message with a new message
        reqdata.stopPublishing();
        MyMessageData msgdata2 = new MyMessageData(PDP1);
        reqdata.configure(msgdata2);
        reqdata.startPublishing();

        // should still only be one token in the queue
        QueueToken<PdpMessage> token = queue.poll();
        assertNull(queue.poll());
        assertNotNull(token);
        assertSame(msgdata2.getMessage(), token.get());

        // null out the token
        token.replaceItem(null);

        // enqueue a new message
        reqdata.stopPublishing();
        MyMessageData msgdata3 = new MyMessageData(PDP1);
        reqdata.configure(msgdata3);
        reqdata.startPublishing();

        // a new token should have been placed in the queue
        QueueToken<PdpMessage> token2 = queue.poll();
        assertTrue(token != token2);
        assertNull(queue.poll());
        assertNotNull(token2);
        assertSame(msgdata3.getMessage(), token2.get());
    }

    @Test
    public void testResetRetryCount_testBumpRetryCount() {
        when(msgdata.getMaxRetryCount()).thenReturn(2);

        reqdata.configure(msgdata);

        assertEquals(0, getRetryCount());
        assertTrue(reqdata.bumpRetryCount());
        assertTrue(reqdata.bumpRetryCount());

        // limit should now be reached and it should go no further
        assertFalse(reqdata.bumpRetryCount());
        assertFalse(reqdata.bumpRetryCount());

        assertEquals(2, getRetryCount());

        reqdata.resetRetryCount();
        assertEquals(0, getRetryCount());
    }

    @Test
    public void testRetryCountExhausted() {
        reqdata.configure(msgdata);

        reqdata.retryCountExhausted();

        verify(reqdata).allCompleted();
    }

    @Test
    public void testProcessResponse() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        invokeProcessResponse();

        verify(reqdata).stopPublishing();
        verify(msgdata).checkResponse(response);
        verify(msgdata).completed();
    }

    @Test
    public void testProcessResponse_NotPublishing() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        reqdata.stopPublishing();

        invokeProcessResponse();

        // only invocation should have been the one before calling invokeProcessResponse()
        verify(reqdata, times(1)).stopPublishing();

        verify(msgdata, never()).checkResponse(response);
        verify(msgdata, never()).completed();
    }

    @Test
    public void testProcessResponse_NotActive() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        when(reqdata.isActive()).thenReturn(false);

        invokeProcessResponse();

        // it should still stop publishing
        verify(reqdata).stopPublishing();

        verify(msgdata, never()).checkResponse(response);
        verify(msgdata, never()).completed();
    }

    @Test
    public void testProcessResponse_ResponseFailed() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        when(msgdata.checkResponse(response)).thenReturn("failed");

        invokeProcessResponse();

        verify(reqdata).stopPublishing();
        verify(msgdata).checkResponse(response);

        verify(msgdata, never()).completed();
        verify(msgdata).mismatch("failed");
    }

    @Test
    public void testHandleTimeout() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        // remove it from the queue
        queue.poll().replaceItem(null);

        invokeTimeoutHandler();

        // count should have been bumped
        assertEquals(1, getRetryCount());

        // should have invoked startPublishing() a second time
        verify(reqdata, times(2)).startPublishing();
    }

    @Test
    public void testHandleTimeout_NotPublishing() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        reqdata.stopPublishing();

        invokeTimeoutHandler();

        // should NOT have invoked startPublishing() a second time
        verify(reqdata, times(1)).startPublishing();
        verify(reqdata, never()).retryCountExhausted();
    }

    @Test
    public void testHandleTimeout_NotActive() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        when(reqdata.isActive()).thenReturn(false);

        invokeTimeoutHandler();

        // should NOT have invoked startPublishing() a second time
        verify(reqdata, times(1)).startPublishing();
        verify(reqdata, never()).retryCountExhausted();
    }

    @Test
    public void testHandleTimeout_StillInQueue() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        reqdata.bumpRetryCount();

        invokeTimeoutHandler();

        // count should reset the count
        assertEquals(0, getRetryCount());

        // should have invoked startPublishing() a second time
        verify(reqdata, times(2)).startPublishing();
    }

    @Test
    public void testHandleTimeout_RetryExhausted() {
        reqdata.configure(msgdata);
        reqdata.startPublishing();

        // exhaust the count
        reqdata.bumpRetryCount();
        reqdata.bumpRetryCount();
        reqdata.bumpRetryCount();

        // remove it from the queue
        queue.poll().replaceItem(null);

        invokeTimeoutHandler();

        // should NOT have invoked startPublishing() a second time
        verify(reqdata, times(1)).startPublishing();

        verify(reqdata).retryCountExhausted();
    }

    @Test
    public void testGetName_testSetName() {
        reqdata.setName("abc");
        assertEquals("abc", reqdata.getName());
    }

    @Test
    public void testGetWrapper() {
        reqdata.configure(msgdata);
        assertSame(msgdata, reqdata.getWrapper());
    }

    /**
     * Gets the retry count from the data.
     * @return the current retry count
     */
    private int getRetryCount() {
        return Whitebox.getInternalState(reqdata, "retryCount");
    }

    /**
     * Gets the listener that was registered with the dispatcher and invokes it.
     */
    @SuppressWarnings("unchecked")
    private void invokeProcessResponse() {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<TypedMessageListener> processResp = ArgumentCaptor.forClass(TypedMessageListener.class);

        verify(disp).register(any(), processResp.capture());

        processResp.getValue().onTopicEvent(CommInfrastructure.NOOP, PapConstants.TOPIC_POLICY_PDP_PAP, response);
    }

    /**
     * Gets the timeout handler that was registered with the timer manager and invokes it.
     */
    @SuppressWarnings("unchecked")
    private void invokeTimeoutHandler() {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Consumer> timeoutHdlr = ArgumentCaptor.forClass(Consumer.class);

        verify(timers).register(any(), timeoutHdlr.capture());

        timeoutHdlr.getValue().accept(PDP1);
    }

    private class MyRequestData extends RequestData {

        public MyRequestData(RequestDataParams params) {
            super(params);
        }

        @Override
        protected boolean isActive() {
            return true;
        }

        @Override
        protected void allCompleted() {
            // do nothing
        }
    }

    private class MyMessageData implements RequestData.MessageData {
        private PdpStateChange msg;

        public MyMessageData(String pdpName) {
            msg = new PdpStateChange();
            msg.setName(pdpName);
            msg.setState(PdpState.ACTIVE);
        }

        @Override
        public PdpMessage getMessage() {
            return msg;
        }

        @Override
        public String getType() {
            return MY_MSG_TYPE;
        }

        @Override
        public void mismatch(String reason) {
            // do nothing
        }

        @Override
        public void completed() {
            // do nothing
        }

        @Override
        public String checkResponse(PdpStatus response) {
            // always valid - return null
            return null;
        }

        @Override
        public int getMaxRetryCount() {
            return 1;
        }

        @Override
        public TimerManager getTimers() {
            return timers;
        }
    }
}
