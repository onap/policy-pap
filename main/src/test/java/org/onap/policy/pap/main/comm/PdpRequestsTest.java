/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2025 OpenInfra Foundation Europe.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.pap.main.comm.msgdata.StateChangeReq;
import org.onap.policy.pap.main.comm.msgdata.UpdateReq;

class PdpRequestsTest extends CommonRequestBase {

    private PdpRequests data;
    private UpdateReq update;
    private StateChangeReq change;

    /**
     * Sets up.
     */
    @Override
    @BeforeEach
    public void setUp() {
        update = makeUpdateReq(PDP1);
        change = makeStateChangeReq();

        data = new PdpRequests(PDP1, notifier);
    }

    @Test
    void testPdpRequests_testGetLastGroupName() {
        assertEquals(PDP1, data.getPdpName());
        assertSame(notifier, data.getNotifier());
    }

    @Test
    void testAddSingleton() {
        data.addSingleton(update);

        verify(update).setNotifier(notifier);
        verify(update).startPublishing();
    }

    @Test
    void testAddSingleton_SameAsExisting() {
        data.addSingleton(update);

        // add duplicate update
        UpdateReq req2 = makeUpdateReq(PDP1);
        data.addSingleton(req2);

        // should not publish duplicate
        verify(req2, never()).startPublishing();

        // should not re-publish original
        verify(update, times(1)).startPublishing();
    }

    @Test
    void testAddSingleton_AnotherRequest() {
        data.addSingleton(update);

        // add another request
        data.addSingleton(change);

        // add duplicate requests
        StateChangeReq change2 = makeStateChangeReq();
        when(change.reconfigure(change2.getMessage())).thenReturn(true);
        data.addSingleton(change2);

        UpdateReq update2 = makeUpdateReq(PDP1);
        when(update.reconfigure(update2.getMessage())).thenReturn(true);
        data.addSingleton(update2);

        // should still only be publishing the first request
        verify(update).startPublishing();

        verify(change, never()).startPublishing();
        verify(change2, never()).startPublishing();
        verify(update2, never()).startPublishing();
    }

    @Test
    void testAddSingleton_Broadcast() {
        UpdateReq req = makeUpdateReq(null);
        assertThatIllegalArgumentException().isThrownBy(() -> data.addSingleton(req))
            .withMessage("unexpected broadcast for pdp_1");
    }

    @Test
    void testStopPublishing() {
        // nothing in the queue - nothing should happen
        data.stopPublishing();

        data.addSingleton(update);
        data.addSingleton(change);

        data.stopPublishing();

        verify(update).stopPublishing();
        verify(change, never()).stopPublishing();

        // repeat, but with only one request in the queue
        data.addSingleton(update);
        data.stopPublishing();
        verify(update, times(2)).stopPublishing();
        verify(change, never()).stopPublishing();
    }

    @Test
    void testStartNextRequest_NothingToStart() {
        assertFalse(data.startNextRequest(update));

        // should not have published it
        verify(update, never()).startPublishing();
    }

    /**
     * Tests addSingleton() when only one request is in the queue.
     */
    @Test
    void testStartNextRequest_OneRequest() {
        data.addSingleton(update);
        assertFalse(data.startNextRequest(update));

        // invoke again
        assertFalse(data.startNextRequest(update));

        // should have only been published once
        verify(update, times(1)).startPublishing();
    }

    /**
     * Tests addSingleton() when more than one request is in the queue.
     */
    @Test
    void testStartNextRequest_MultipleRequests() {
        data.addSingleton(update);
        data.addSingleton(change);

        // update is still active - should return true
        assertTrue(data.startNextRequest(change));

        // invoke again
        assertTrue(data.startNextRequest(change));

        // should not have published yet
        verify(change, never()).startPublishing();

        // should publish "change" once update completes
        assertTrue(data.startNextRequest(update));
        verify(change).startPublishing();

        // nothing more in the queue once it completes
        assertFalse(data.startNextRequest(change));
    }

    @Test
    void testIsFirstInQueue() {
        // test with empty queue
        assertFalse(data.isFirstInQueue(update));

        data.addSingleton(update);
        assertTrue(data.isFirstInQueue(update));

        data.addSingleton(change);
        assertTrue(data.isFirstInQueue(update));
        assertFalse(data.isFirstInQueue(change));
    }

    @Test
    void testGetPdpName() {
        assertEquals(PDP1, data.getPdpName());
    }
}
