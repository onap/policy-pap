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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.pap.main.comm.MultiPdpStatusListener;
import org.onap.policy.pdp.common.models.PdpResponseDetails;
import org.onap.policy.pdp.common.models.PdpStatus;

public class MultiPdpStatusListenerTest {
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";
    private static final String ID1 = "request-1";
    private static final String ID2 = "request-2";
    private static final List<String> ID_LIST = Arrays.asList(ID1, ID2);

    private MultiPdpStatusListener listener;
    private PdpStatus status;

    @Test
    public void testMultiPdpStatusListenerString() throws Exception {
        listener = new MyListener(ID1);
        assertEquals(Arrays.asList(ID1).toString(), listener.getUnseenIds().toString());

        // an ID is in the queue - not done yet
        assertFalse(doWait(0));
    }

    @Test
    public void testMultiPdpStatusListenerCollectionOfString() throws Exception {
        List<String> lst = ID_LIST;

        listener = new MyListener(lst);
        assertEquals(lst.toString(), listener.getUnseenIds().toString());

        // an ID is in the queue - not done yet
        assertFalse(doWait(0));

        /*
         * Try with an empty list - should already be complete.
         */
        listener = new MyListener(new LinkedList<>());
        assertTrue(listener.getUnseenIds().isEmpty());
        assertTrue(doWait(0));
    }

    @Test
    public void testGetUnseenIds() {
        List<String> lst = ID_LIST;

        listener = new MyListener(lst);
        assertEquals(lst.toString(), listener.getUnseenIds().toString());

        // receive message from one PDP
        status = new PdpStatus();
        status.setResponse(makeResponse(ID2));
        listener.onTopicEvent(INFRA, TOPIC, status);
        assertEquals(Arrays.asList(ID1).toString(), listener.getUnseenIds().toString());

        // receive message from the other PDP
        status = new PdpStatus();
        status.setResponse(makeResponse(ID1));
        listener.onTopicEvent(INFRA, TOPIC, status);
        assertTrue(listener.getUnseenIds().isEmpty());
    }

    @Test
    public void testAwait() throws Exception {
        // try with an empty list - should already be complete
        listener = new MyListener(new LinkedList<>());
        assertTrue(doWait(0));

        // try it with something in the list
        listener = new MyListener(ID_LIST);
        assertFalse(doWait(0));

        // process a message from one PDP - wait should block the entire time
        status = new PdpStatus();
        status.setResponse(makeResponse(ID1));
        listener.onTopicEvent(INFRA, TOPIC, status);
        long tbeg = System.currentTimeMillis();
        assertFalse(doWait(50));
        assertTrue(System.currentTimeMillis() - tbeg >= 49);

        // process a message from the other PDP - wait should NOT block
        status = new PdpStatus();
        status.setResponse(makeResponse(ID2));
        listener.onTopicEvent(INFRA, TOPIC, status);
        tbeg = System.currentTimeMillis();
        assertTrue(doWait(4000));
        assertTrue(System.currentTimeMillis() - tbeg < 3000);
    }

    @Test
    public void testOnTopicEvent() throws Exception {
        listener = new MyListener(ID_LIST);

        // not done yet
        assertFalse(doWait(0));

        // process a message - still not done as have another ID to go
        status = new PdpStatus();
        status.setResponse(makeResponse(ID1));
        listener.onTopicEvent(INFRA, TOPIC, status);
        assertFalse(doWait(0));

        // process a message from the same PDP - still not done
        status = new PdpStatus();
        status.setResponse(makeResponse(ID1));
        listener.onTopicEvent(INFRA, TOPIC, status);
        assertFalse(doWait(0));

        // process another message - now we're done
        status = new PdpStatus();
        status.setResponse(makeResponse(ID2));
        listener.onTopicEvent(INFRA, TOPIC, status);
        assertTrue(doWait(0));

        // ensure "handleEvent" is called
        AtomicBoolean handled = new AtomicBoolean(false);
        listener = new MyListener(ID1) {
            @Override
            protected void handleEvent(CommInfrastructure infra, String topic, PdpStatus message) {
                handled.set(true);
            }
        };
        status = new PdpStatus();
        status.setResponse(makeResponse(ID1));
        listener.onTopicEvent(INFRA, TOPIC, status);
        assertTrue(handled.get());

        // handleEvent throws an exception - doWait still returns true
        listener = new MyListener(ID1) {
            @Override
            protected void handleEvent(CommInfrastructure infra, String topic, PdpStatus message) {
                throw new RuntimeException("expected exception");
            }
        };
        status = new PdpStatus();
        status.setResponse(makeResponse(ID1));
        listener.onTopicEvent(INFRA, TOPIC, status);
        assertTrue(doWait(0));

        // extract returns null - doWait does not return true
        listener = new MyListener(ID1) {
            @Override
            protected String extractId(PdpStatus message) {
                return null;
            }
        };
        status = new PdpStatus();
        status.setResponse(makeResponse(ID1));
        listener.onTopicEvent(INFRA, TOPIC, status);
        assertFalse(doWait(0));
    }

    /**
     * Waits for the listener to complete. Spawns a background thread to do the waiting so
     * we can limit how long we wait.
     *
     * @param millisec milliseconds to wait
     * @return {@code true} if the wait completed successfully, {@code false} otherwise
     * @throws InterruptedException if this thread is interrupted while waiting for the
     *         background thread to complete
     */
    private boolean doWait(long millisec) throws InterruptedException {
        AtomicBoolean done = new AtomicBoolean(false);

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    done.set(listener.await(millisec, TimeUnit.MILLISECONDS));

                } catch (InterruptedException expected) {
                    return;
                }
            }
        };

        thread.start();
        thread.join(5000);
        thread.interrupt();

        return done.get();
    }

    /**
     * Makes a response for the given request ID.
     *
     * @param id ID of the request
     * @return a new response
     */
    private PdpResponseDetails makeResponse(String id) {
        PdpResponseDetails resp = new PdpResponseDetails();
        resp.setResponseTo(id);

        return resp;
    }

    private static class MyListener extends MultiPdpStatusListener {

        public MyListener(String id) {
            super(id);
        }

        public MyListener(List<String> lst) {
            super(lst);
        }

        @Override
        protected void handleEvent(CommInfrastructure infra, String topic, PdpStatus message) {
            // do nothing
        }

        @Override
        protected String extractId(PdpStatus message) {
            return (message.getResponse().getResponseTo());
        }

    }
}
