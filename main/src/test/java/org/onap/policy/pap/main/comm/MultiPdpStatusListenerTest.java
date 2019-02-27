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
import org.onap.policy.pap.main.comm.PdpStatus;

public class MultiPdpStatusListenerTest {
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";
    private static final String NAME1 = "pdp_1";
    private static final String NAME2 = "pdp_2";
    private static final List<String> NAME_LIST = Arrays.asList(NAME1, NAME2);

    private MultiPdpStatusListener listener;

    @Test
    public void testMultiPdpStatusListenerString() throws Exception {
        listener = new MultiPdpStatusListener(NAME1);
        assertEquals(Arrays.asList(NAME1).toString(), listener.getUnseenPdpNames().toString());

        // a name is in the queue - not done yet
        assertFalse(doWait(0));
    }

    @Test
    public void testMultiPdpStatusListenerCollectionOfString() throws Exception {
        List<String> lst = NAME_LIST;

        listener = new MultiPdpStatusListener(lst);
        assertEquals(lst.toString(), listener.getUnseenPdpNames().toString());

        // a name is in the queue - not done yet
        assertFalse(doWait(0));

        /*
         * Try with an empty list - should already be complete.
         */
        listener = new MultiPdpStatusListener(new LinkedList<>());
        assertTrue(listener.getUnseenPdpNames().isEmpty());
        assertTrue(doWait(0));
    }

    @Test
    public void testGetUnseenPdpNames() {
        List<String> lst = NAME_LIST;

        listener = new MultiPdpStatusListener(lst);
        assertEquals(lst.toString(), listener.getUnseenPdpNames().toString());

        // receive message from one PDP
        listener.onTopicEvent(INFRA, TOPIC, new PdpStatus(NAME2));
        assertEquals(Arrays.asList(NAME1).toString(), listener.getUnseenPdpNames().toString());

        // receive message from the other PDP
        listener.onTopicEvent(INFRA, TOPIC, new PdpStatus(NAME1));
        assertTrue(listener.getUnseenPdpNames().isEmpty());
    }

    @Test
    public void testAwait() throws Exception {
        // try with an empty list - should already be complete
        listener = new MultiPdpStatusListener(new LinkedList<>());
        assertTrue(doWait(0));

        // try it with something in the list
        listener = new MultiPdpStatusListener(NAME_LIST);
        assertFalse(doWait(0));

        // process a message from one PDP - wait should block the entire time
        listener.onTopicEvent(INFRA, TOPIC, new PdpStatus(NAME1));
        long tbeg = System.currentTimeMillis();
        assertFalse(doWait(50));
        assertTrue(System.currentTimeMillis() - tbeg >= 49);

        // process a message from the other PDP - wait should NOT block
        listener.onTopicEvent(INFRA, TOPIC, new PdpStatus(NAME2));
        tbeg = System.currentTimeMillis();
        assertTrue(doWait(4000));
        assertTrue(System.currentTimeMillis() - tbeg < 3000);
    }

    @Test
    public void testOnTopicEvent() throws Exception {
        listener = new MultiPdpStatusListener(NAME_LIST);

        // not done yet
        assertFalse(doWait(0));

        // process a message - still not done as have another name to go
        listener.onTopicEvent(INFRA, TOPIC, new PdpStatus(NAME1));
        assertFalse(doWait(0));

        // process a message from the same PDP - still not done
        listener.onTopicEvent(INFRA, TOPIC, new PdpStatus(NAME1));
        assertFalse(doWait(0));

        // process another message - now we're done
        listener.onTopicEvent(INFRA, TOPIC, new PdpStatus(NAME2));
        assertTrue(doWait(0));
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
}
