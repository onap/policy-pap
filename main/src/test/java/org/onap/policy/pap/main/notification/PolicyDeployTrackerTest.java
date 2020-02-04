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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.models.pap.concepts.PolicyStatus;

public class PolicyDeployTrackerTest extends PolicyCommonSupport {

    @Mock
    private PolicyTrackerData data;

    private PolicyDeployTracker tracker;

    /**
     * Creates various objects, including {@link #tracker}.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        super.setUp();

        tracker = new PolicyDeployTracker();
    }

    /**
     * Simple test with one PDP that immediately responds with success.
     */
    @Test
    public void testSimpleImmediateSuccess() {
        tracker.addData(makeData(policy1, PDP1));

        // indicate that PDP1 has succeeded
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);

        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());
        assertEquals("[1, 0, 0]", getCounts(statusList.get(0)).toString());

        // indicate that PDP1 has succeeded again - should be no output
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);
        assertEquals(0, statusList.size());

        // indicate failure
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(), statusList);
        assertEquals(1, statusList.size());
        assertEquals("[0, 1, 0]", getCounts(statusList.get(0)).toString());

        // indicate that PDP1 has failed again - should be no output
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(), statusList);
        assertEquals(0, statusList.size());

        // indicate that PDP1 has succeeded again
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);
        assertEquals("[1, 0, 0]", getCounts(statusList.get(0)).toString());
    }

    /**
     * Simple test with one PDP that immediately responds with success.
     */
    @Test
    public void testSimpleImmediateFail() {
        tracker.addData(makeData(policy1, PDP1));

        // indicate that PDP1 has failed
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.processResponse(PDP1, Arrays.asList(), statusList);
        assertEquals(1, statusList.size());
        assertEquals("[0, 1, 0]", getCounts(statusList.get(0)).toString());

        // indicate that PDP1 has failed again - should be no output
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(), statusList);
        assertEquals(0, statusList.size());

        // indicate success
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);

        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());
        assertEquals("[1, 0, 0]", getCounts(statusList.get(0)).toString());

        // indicate that PDP1 has succeeded again - should be no output
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);
        assertEquals(0, statusList.size());
    }

    /**
     * Simple test where PDP is removed and then it responds.
     */
    @Test
    public void testSimpleRemove() {
        tracker.addData(makeData(policy1, PDP1));

        // remove the PDP
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.removePdp(PDP1, statusList);
        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());
        assertEquals("[0, 0, 0]", getCounts(statusList.get(0)).toString());

        /*
         * indicate that PDP1 has succeeded - should be no message since PDP was removed
         * from the policy
         */
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);
        assertEquals(0, statusList.size());

        /*
         * indicate that PDP1 has failed - should be no message since PDP was removed
         * from the policy
         */
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(), statusList);
        assertEquals(0, statusList.size());
    }

    /**
     * Test with multiple PDPs.
     */
    @Test
    public void testMulti() {
        tracker.addData(makeData(policy1, PDP1, PDP2));

        // indicate that PDP2 has succeeded
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.processResponse(PDP2, Arrays.asList(policy1), statusList);
        assertEquals(0, statusList.size());

        // indicate that PDP1 has succeeded
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);

        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());
        assertEquals("[2, 0, 0]", getCounts(statusList.get(0)).toString());

        // indicate that PDP1 has failed - should get a notification
        statusList.clear();
        tracker.processResponse(PDP1, Collections.emptyList(), statusList);
        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());
        assertEquals("[1, 1, 0]", getCounts(statusList.get(0)).toString());

        // indicate that PDP1 has succeeded
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);
        assertEquals(1, statusList.size());
        assertEquals("[2, 0, 0]", getCounts(statusList.get(0)).toString());

        // remove PDP2 - expect message
        statusList.clear();
        tracker.removePdp(PDP2, statusList);
        assertEquals(1, statusList.size());
        assertEquals("[1, 0, 0]", getCounts(statusList.get(0)).toString());

        // re-add PDP2
        tracker.addData(makeData(policy1, PDP2));

        // indicate that PDP2 has succeeded; PDP1 should still be ok
        statusList.clear();
        tracker.processResponse(PDP2, Arrays.asList(policy1), statusList);
        assertEquals(1, statusList.size());
        assertEquals("[2, 0, 0]", getCounts(statusList.get(0)).toString());
    }

    @Test
    public void testUpdateData() {
        // when success returns false
        assertFalse(tracker.updateData(PDP1, data, true));
        verify(data).success(PDP1);
        verify(data, never()).fail(any());

        // when inactive
        assertFalse(tracker.updateData(PDP1, data, false));
        verify(data).success(PDP1);
        verify(data).fail(any());

        // when success & fail return true
        when(data.success(PDP1)).thenReturn(true);
        when(data.fail(PDP1)).thenReturn(true);
        assertTrue(tracker.updateData(PDP1, data, true));
        verify(data, times(2)).success(PDP1);
        verify(data, times(1)).fail(PDP1);

        // when inactive
        assertTrue(tracker.updateData(PDP1, data, false));
        verify(data, times(2)).success(PDP1);
        verify(data, times(2)).fail(PDP1);
    }

    @Test
    public void testShouldRemove() {
        // when data is complete, but not empty
        when(data.isComplete()).thenReturn(true);
        when(data.isEmpty()).thenReturn(false);
        assertFalse(tracker.shouldRemove(data));

        // when data is empty
        when(data.isEmpty()).thenReturn(true);
        assertTrue(tracker.shouldRemove(data));
    }
}
