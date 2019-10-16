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

public class PolicyUndeployTrackerTest extends PolicyCommonSupport {

    @Mock
    private PolicyTrackerData data;

    private PolicyUndeployTracker tracker;

    /**
     * Creates various objects, including {@link #tracker}.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        super.setUp();

        tracker = new PolicyUndeployTracker();
    }

    @Test
    public void test() {
        tracker.addData(makeData(policy1, PDP1, PDP2));

        // indicate that PDP2 has been undeployed
        tracker.processResponse(PDP2, Collections.emptyList(), new ArrayList<>(0));

        // indicate that PDP1 has been undeployed
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.processResponse(PDP1, Collections.emptyList(), statusList);

        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals("[2, 0, 0]", getCounts(statusList.get(0)).toString());

        // indicate that PDP1 has been re-deployed - should not get a notification,
        // because policy
        // is gone
        statusList.clear();
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);
        assertTrue(statusList.isEmpty());
    }

    @Test
    public void testUpdateData() {
        // when success returns false
        assertFalse(tracker.updateData(PDP1, data, false));
        verify(data).success(PDP1);
        verify(data, never()).fail(any());

        // when inactive
        assertFalse(tracker.updateData(PDP1, data, true));
        verify(data).success(PDP1);
        verify(data).fail(any());

        // when success & fail return true
        when(data.success(PDP1)).thenReturn(true);
        when(data.fail(PDP1)).thenReturn(true);
        assertTrue(tracker.updateData(PDP1, data, false));
        verify(data, times(2)).success(PDP1);
        verify(data, times(1)).fail(PDP1);

        // when inactive
        assertTrue(tracker.updateData(PDP1, data, true));
        verify(data, times(2)).success(PDP1);
        verify(data, times(2)).fail(PDP1);
    }

    @Test
    public void testShouldRemove() {
        // when data is not complete
        assertFalse(tracker.shouldRemove(data));

        // when data is complete
        when(data.isComplete()).thenReturn(true);
        assertTrue(tracker.shouldRemove(data));
    }
}
