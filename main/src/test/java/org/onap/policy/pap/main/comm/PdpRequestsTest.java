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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.pap.main.comm.msgdata.StateChangeReq;
import org.onap.policy.pap.main.comm.msgdata.UpdateReq;

public class PdpRequestsTest extends CommonRequestBase {

    private PdpRequests data;
    private UpdateReq update;
    private StateChangeReq change;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        update = makeUpdateReq(PDP1, MY_GROUP, MY_SUBGROUP);
        change = makeStateChangeReq(PDP1, MY_STATE);

        data = new PdpRequests(PDP1, notifier);
    }

    @Test
    public void testPdpRequests_testGetLastGroupName() {
        assertEquals(PDP1, data.getPdpName());
    }

    @Test
    public void testAddSingleton() {
        data.addSingleton(update);

        verify(update).setNotifier(notifier);
        verify(update).startPublishing(any());
    }

    @Test
    public void testAddSingleton_SameAsExisting() {
        data.addSingleton(update);

        // add duplicate update
        UpdateReq req2 = makeUpdateReq(PDP1, MY_GROUP, MY_SUBGROUP);
        data.addSingleton(req2);

        // should not publish duplicate
        verify(req2, never()).startPublishing(any());
    }

    @Test
    public void testAddSingleton_LowerPriority() {
        data.addSingleton(update);

        // add lower priority request
        data.addSingleton(change);

        // should not publish lower priority request
        verify(change, never()).startPublishing(any());
    }

    @Test
    public void testAddSingleton_HigherPriority() {
        data.addSingleton(change);

        QueueToken<PdpMessage> token = new QueueToken<>(change.getMessage());
        when(change.stopPublishing(false)).thenReturn(token);

        // add higher priority request
        data.addSingleton(update);

        // should stop publishing lower priority request
        verify(change).stopPublishing(false);

        // should start publishing higher priority request
        verify(update).startPublishing(token);
    }

    @Test
    public void testAddSingleton_Broadcast() {
        UpdateReq req = makeUpdateReq(null, MY_GROUP, MY_SUBGROUP);
        assertThatIllegalArgumentException().isThrownBy(() -> data.addSingleton(req))
                        .withMessage("unexpected broadcast for pdp_1");
    }

    @Test
    public void testCheckExistingSingleton_DoesNotExist() {
        data.addSingleton(update);
        verify(update).startPublishing(any());
    }

    @Test
    public void testCheckExistingSingleton_SameContent() {
        data.addSingleton(update);

        // add duplicate update
        UpdateReq req2 = makeUpdateReq(PDP1, MY_GROUP, MY_SUBGROUP);
        when(update.isSameContent(req2)).thenReturn(true);
        data.addSingleton(req2);

        // should not publish duplicate
        verify(req2, never()).startPublishing(any());
    }

    @Test
    public void testCheckExistingSingleton_DifferentContent() {
        data.addSingleton(update);

        // add different update
        UpdateReq req2 = makeUpdateReq(PDP1, MY_GROUP, MY_SUBGROUP);
        when(req2.isSameContent(update)).thenReturn(false);
        data.addSingleton(req2);

        // should not publish duplicate
        verify(req2, never()).startPublishing(any());

        // should have re-configured the original
        verify(update).reconfigure(req2.getMessage(), null);

        // should not have started publishing again
        verify(update).startPublishing(any());
    }

    @Test
    public void testStopPublishing() {
        data.addSingleton(update);
        data.addSingleton(change);

        data.stopPublishing();

        verify(update).stopPublishing();
        verify(change).stopPublishing();

        // repeat, but with only one request in the queue
        data.addSingleton(update);
        data.stopPublishing();
        verify(update, times(2)).stopPublishing();

        // should not have been invoked again
        verify(change).stopPublishing();
    }

    @Test
    public void testStopPublishingLowerPriority() {
        data.addSingleton(change);

        QueueToken<PdpMessage> token = new QueueToken<>(change.getMessage());
        when(change.stopPublishing(false)).thenReturn(token);

        // add higher priority request
        data.addSingleton(update);

        // should stop publishing lower priority request
        verify(change).stopPublishing(false);

        // should start publishing higher priority request, with the old token
        verify(update).startPublishing(token);
    }

    @Test
    public void testStopPublishingLowerPriority_NothingPublishing() {
        data.addSingleton(change);

        // change will return a null token when stopPublishing(false) is called

        data.addSingleton(update);

        // should stop publishing lower priority request
        verify(change).stopPublishing(false);

        // should start publishing higher priority request
        verify(update).startPublishing(null);
    }

    @Test
    public void testStartNextRequest_NothingToStart() {
        assertFalse(data.startNextRequest(update));
    }

    @Test
    public void testStartNextRequest_ZapCurrent() {
        data.addSingleton(update);
        assertFalse(data.startNextRequest(update));

        // invoke again
        assertFalse(data.startNextRequest(update));
    }

    @Test
    public void testStartNextRequest_ZapOther() {
        data.addSingleton(update);
        data.addSingleton(change);

        // update is still active - should return true
        assertTrue(data.startNextRequest(change));

        // invoke again
        assertTrue(data.startNextRequest(change));

        // nothing more once update completes
        assertFalse(data.startNextRequest(update));

        assertFalse(data.startNextRequest(change));
    }

    @Test
    public void testStartNextRequest_StartOther() {
        data.addSingleton(update);
        data.addSingleton(change);

        assertTrue(data.startNextRequest(change));

        // should have published update twice, with and without a token
        verify(update).startPublishing(any());
        verify(update).startPublishing();
    }

    @Test
    public void testStartNextRequest_NoOther() {
        data.addSingleton(update);

        // nothing else to start
        assertFalse(data.startNextRequest(update));

        verify(update).startPublishing(any());
        verify(update, never()).startPublishing();
    }

    @Test
    public void testHigherPrioritySingleton_True() {
        data.addSingleton(update);
        data.addSingleton(change);

        verify(update).startPublishing(any());

        verify(update, never()).startPublishing();
        verify(change, never()).startPublishing();
        verify(change, never()).startPublishing(any());
    }

    @Test
    public void testHigherPrioritySingleton_FalseWithUpdate() {
        data.addSingleton(update);

        verify(update).startPublishing(any());
        verify(update, never()).startPublishing();
    }

    @Test
    public void testHigherPrioritySingleton_FalseWithStateChange() {
        data.addSingleton(change);

        verify(change).startPublishing(any());
        verify(change, never()).startPublishing();
    }

    @Test
    public void testGetPdpName() {
        assertEquals(PDP1, data.getPdpName());
    }
}
