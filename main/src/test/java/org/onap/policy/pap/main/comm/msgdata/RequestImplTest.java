/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020, 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.comm.msgdata;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpResponseDetails;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.pap.main.comm.CommonRequestBase;
import org.onap.policy.pap.main.comm.QueueToken;
import org.onap.policy.pap.main.parameters.RequestParams;

class RequestImplTest extends CommonRequestBase {

    private MyRequest req;
    private PdpStatus response;
    private PdpStateChange msg;

    /**
     * Sets up.
     * @throws Exception if an error occurs
     */
    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        response = new PdpStatus();
        msg = new PdpStateChange();

        response.setName(PDP1);
        response.setResponse(new PdpResponseDetails());
        response.getResponse().setResponseTo(msg.getRequestId());
        msg.setName(PDP1);

        req = new MyRequest(reqParams, MY_REQ_NAME, msg);
        req.setListener(listener);
    }

    @Test
    public void testRequest_InvalidArgs() {
        // null params
        assertThatThrownBy(() -> new MyRequest(null, MY_REQ_NAME, msg)).isInstanceOf(NullPointerException.class);

        // null name
        assertThatThrownBy(() -> new MyRequest(reqParams, null, msg)).isInstanceOf(NullPointerException.class);

        // null message
        assertThatThrownBy(() -> new MyRequest(reqParams, MY_REQ_NAME, null)).isInstanceOf(NullPointerException.class);

        // invalid params
        assertThatIllegalArgumentException().isThrownBy(() -> new MyRequest(new RequestParams(), MY_REQ_NAME, msg));
    }

    @Test
    public void testReconfigure2_WrongMsgClass() {
        assertThatIllegalArgumentException().isThrownBy(() -> req.reconfigure(new PdpUpdate()))
                        .withMessage("expecting PdpStateChange instead of PdpUpdate");
    }

    @Test
    public void testReconfigure2_NotPublishing() {

        // replace the message with a new message
        req.reconfigure(new PdpStateChange());

        // nothing should have been placed in the queue
        assertNull(queue.poll());
    }

    @Test
    public void testRequestImpl_testReconfigure2_Publishing() {
        req.startPublishing();

        // replace the message with a new message
        PdpStateChange msg2 = new PdpStateChange();
        req.reconfigure(msg2);

        // should have cancelled the first timer
        verify(timer).cancel();

        // should only be one token in the queue
        QueueToken<PdpMessage> token = queue.poll();
        org.junit.jupiter.api.Assertions.assertNotNull(token);
        org.junit.jupiter.api.Assertions.assertSame(msg2, token.get());

        verify(dispatcher).register(eq(msg.getRequestId()), any());
        verify(timers).register(eq(msg.getRequestId()), any());
        verify(publisher).enqueue(token);

        verify(dispatcher).unregister(msg.getRequestId());

        verify(dispatcher).register(eq(msg2.getRequestId()), any());
        verify(timers).register(eq(msg2.getRequestId()), any());
        verify(publisher).enqueue(any());
    }

    @Test
    public void testIsPublishing() {
        assertFalse(req.isPublishing());

        req.startPublishing();
        assertTrue(req.isPublishing());

        req.stopPublishing();
        assertFalse(req.isPublishing());
    }

    @Test
    public void testStartPublishingQueueToken_NoListener() {
        req.setListener(null);
        assertThatIllegalStateException().isThrownBy(() -> req.startPublishing())
                        .withMessage("listener has not been set");
    }

    @Test
    public void testStartPublishing() {
        req.startPublishing();

        assertTrue(req.isPublishing());

        verify(dispatcher).register(eq(msg.getRequestId()), any());
        verify(timers).register(eq(msg.getRequestId()), any());
        verify(publisher).enqueue(any());

        QueueToken<PdpMessage> token = queue.poll();
        assertNotNull(token);
        assertSame(msg, token.get());


        // invoking start() again has no effect - invocation counts remain the same
        req.startPublishing();
        verify(dispatcher, times(1)).register(any(), any());
        verify(timers, times(1)).register(any(), any());
        verify(publisher, times(1)).enqueue(any());
        assertNull(queue.poll());
    }

    @Test
    public void testStopPublishing() {
        // not publishing yet
        req.stopPublishing();
        assertFalse(req.isPublishing());

        // now we'll publish
        req.startPublishing();

        req.stopPublishing();
        assertFalse(req.isPublishing());

        // should only be one token in the queue - should be nulled out
        QueueToken<PdpMessage> token = queue.poll();
        assertNotNull(token);
        assertNull(token.get());

        verify(dispatcher).unregister(msg.getRequestId());
        verify(timer).cancel();
    }

    @Test
    public void testStopPublishingBoolean_NotPublishing() {
        // should not throw an exception
        Assertions.assertThatCode(() -> req.stopPublishing()).doesNotThrowAnyException();
    }

    @Test
    public void testStopPublishingBoolean_TruePublishing() {
        req.startPublishing();

        req.stopPublishing();

        // should be nulled out
        QueueToken<PdpMessage> token = queue.poll();
        assertNotNull(token);
        assertNull(token.get());

        verify(dispatcher).unregister(msg.getRequestId());
        verify(timer).cancel();

        // if start publishing again - should use a new token
        req.startPublishing();
        QueueToken<PdpMessage> token2 = queue.poll();
        assertNotNull(token2);
        assertNotSame(token, token2);
        assertSame(msg, token2.get());
    }

    @Test
    public void testEnqueue() {
        req.startPublishing();

        // replace the message with a new message
        PdpStateChange msg2 = new PdpStateChange();
        req.reconfigure(msg2);

        // should still only be one token in the queue
        QueueToken<PdpMessage> token = queue.poll();
        assertNull(queue.poll());
        assertNotNull(token);
        assertSame(msg2, token.get());

        // force the token to be nulled out
        req.stopPublishing();

        // enqueue a new message
        PdpStateChange msg3 = new PdpStateChange();
        req.reconfigure(msg3);
        req.startPublishing();

        // a new token should have been placed in the queue
        QueueToken<PdpMessage> token2 = queue.poll();
        assertNotSame(token, token2);
        assertNull(queue.poll());
        assertNotNull(token2);
        assertSame(msg3, token2.get());

        // zap the token, indicating it's been published
        token2.replaceItem(null);
        PdpStateChange msg4 = new PdpStateChange();
        req.reconfigure(msg4);

        // a new token should have been placed in the queue
        QueueToken<PdpMessage> token3 = queue.poll();
        assertNotSame(token2, token3);
        assertNull(queue.poll());
        assertNotNull(token3);
        assertSame(msg4, token3.get());
    }

    @Test
    public void testResetRetryCount_testBumpRetryCount() {
        req = new MyRequest(new RequestParams().setMaxRetryCount(2).setModifyLock(lock).setPdpPublisher(publisher)
            .setResponseDispatcher(dispatcher).setTimers(timers), MY_REQ_NAME, msg);
        req.setListener(listener);

        assertEquals(0, req.getRetryCount());
        assertTrue(req.bumpRetryCount());
        assertTrue(req.bumpRetryCount());

        // limit should now be reached and it should go no further
        assertFalse(req.bumpRetryCount());
        assertFalse(req.bumpRetryCount());

        assertEquals(2, req.getRetryCount());

        req.resetRetryCount();
        assertEquals(0, req.getRetryCount());
    }

    @Test
    public void testProcessResponse() {
        req.startPublishing();

        invokeProcessResponse(response);

        verify(listener).success(PDP1, response);
        verify(listener, never()).failure(any(), any());
        verify(timer).cancel();
    }

    @Test
    public void testProcessResponse_NotPublishing() {
        // force registration with the dispatcher - needed by invokeProcessResponse(response)
        req.startPublishing();
        req.stopPublishing();

        invokeProcessResponse(response);

        verify(listener, never()).success(any(), any());
        verify(listener, never()).failure(any(), any());
    }

    @Test
    public void testProcessResponse_WrongRequest() {
        req.startPublishing();

        response.getResponse().setResponseTo(DIFFERENT);

        invokeProcessResponse(response);

        verify(listener, never()).success(any(), any());
        verify(listener, never()).failure(any(), any());
        verify(timer, never()).cancel();
    }

    @Test
    public void testProcessResponse_ResponseFailed() {
        req.startPublishing();

        response.setName(DIFFERENT);

        invokeProcessResponse(response);

        verify(listener, never()).success(any(), any());
        verify(listener).failure(DIFFERENT, "PDP name does not match");
        verify(timer).cancel();
    }

    @Test
    public void testHandleTimeout() {
        req.startPublishing();

        // remove it from the queue
        queue.poll().replaceItem(null);

        invokeTimeoutHandler();

        // count should have been bumped
        assertEquals(1, req.getRetryCount());

        // should have invoked startPublishing() a second time
        verify(dispatcher, times(2)).register(eq(msg.getRequestId()), any());
    }

    @Test
    public void testHandleTimeout_NotPublishing() {
        req.startPublishing();

        req.stopPublishing();

        invokeTimeoutHandler();

        // should NOT have invoked startPublishing() a second time
        verify(dispatcher, times(1)).register(eq(msg.getRequestId()), any());
        verify(listener, never()).retryCountExhausted(any());
    }

    @Test
    public void testHandleTimeout_RetryExhausted() {
        req.startPublishing();

        // exhaust the count
        req.bumpRetryCount();
        req.bumpRetryCount();
        req.bumpRetryCount();

        // remove it from the queue
        queue.poll().replaceItem(null);

        invokeTimeoutHandler();

        // should NOT have invoked startPublishing() a second time
        verify(dispatcher, times(1)).register(eq(msg.getRequestId()), any());

        verify(listener).retryCountExhausted(req);
    }

    @Test
    public void testCheckResponse_Matched() {
        req.startPublishing();

        invokeProcessResponse(response);

        verify(listener).success(PDP1, response);
        verify(listener, never()).failure(any(), any());
    }

    @Test
    public void testCheckResponse_NullName() {
        req.startPublishing();

        response.setName(null);

        invokeProcessResponse(response);

        verify(listener, never()).success(any(), any());
        verify(listener).failure(null, "null PDP name");
    }

    @Test
    public void testCheckResponse_MismatchedName() {
        req.startPublishing();

        response.setName(DIFFERENT);

        invokeProcessResponse(response);

        verify(listener, never()).success(any(), any());
        verify(listener).failure(DIFFERENT, "PDP name does not match");
    }

    @Test
    public void testCheckResponse_MismatchedNameWithBroadcast() {
        msg.setName(null);
        req.startPublishing();

        response.setName(DIFFERENT);

        invokeProcessResponse(response);

        verify(listener).success(DIFFERENT, response);
        verify(listener, never()).failure(any(), any());
    }

    @Test
    public void testGetName() {
        assertEquals(MY_REQ_NAME, req.getName());
    }

    @Test
    public void testGetMessage() {
        assertSame(msg, req.getMessage());

        PdpStateChange msg2 = new PdpStateChange();
        req.reconfigure(msg2);
        assertSame(msg2, req.getMessage());
    }

    @Test
    public void testGetParams() {
        assertSame(reqParams, req.getParams());
    }

    @Test
    public void testGetUndeployPolicies() {
        assertTrue(req.getUndeployPolicies().isEmpty());
    }

    private static class MyRequest extends RequestImpl {

        public MyRequest(RequestParams params, String name, PdpMessage message) {
            super(params, name, message);
        }

        @Override
        public boolean reconfigure(PdpMessage newMessage) {
            reconfigure2(newMessage);
            return true;
        }
    }
}
