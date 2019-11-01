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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.powermock.reflect.Whitebox;

public class PolicyCommonTrackerTest extends PolicyCommonSupport {

    private MyTracker tracker;
    private Map<ToscaPolicyIdentifier, PolicyTrackerData> map;

    /**
     * Creates various objects, including {@link #tracker}.
     */
    @Before
    public void setUp() {
        super.setUp();

        tracker = new MyTracker();
    }

    @Test
    public void testGetStatus() {
        tracker.addData(makeData(policy1, PDP1, PDP2));
        tracker.addData(makeData(policy2, PDP2));

        List<PolicyStatus> statusList = tracker.getStatus();
        assertEquals(2, statusList.size());

        Collections.sort(statusList, (left, right) -> left.getPolicyId().compareTo(right.getPolicyId()));

        assertEquals(policy1.getName(), statusList.get(0).getPolicyId());
        assertEquals(policy2.getName(), statusList.get(1).getPolicyId());
    }

    @Test
    public void testGetStatusString() {
        tracker.addData(makeData(policy1, PDP1, PDP2));
        tracker.addData(makeData(policy2, PDP2));

        policy3 = new ToscaPolicyIdentifier(policy1.getName(), policy1.getVersion() + "0");
        tracker.addData(makeData(policy3, PDP3));

        List<PolicyStatus> statusList = tracker.getStatus(policy1.getName());
        assertEquals(2, statusList.size());

        Collections.sort(statusList, (left, right) -> {
            int cmp = left.getPolicyId().compareTo(right.getPolicyId());
            if (cmp != 0) {
                return cmp;
            }

            return left.getPolicyVersion().compareTo(right.getPolicyVersion());
        });

        assertEquals(policy1.getName(), statusList.get(0).getPolicyId());
        assertEquals(policy1.getVersion(), statusList.get(0).getPolicyVersion());

        assertEquals(policy3.getName(), statusList.get(1).getPolicyId());
        assertEquals(policy3.getVersion(), statusList.get(1).getPolicyVersion());
    }

    @Test
    public void testGetStatusToscaPolicyIdentifier() {
        tracker.addData(makeData(policy1, PDP1, PDP2));
        tracker.addData(makeData(policy2, PDP2));

        policy3 = new ToscaPolicyIdentifier(policy1.getName(), policy1.getVersion() + "0");
        tracker.addData(makeData(policy3, PDP3));

        Optional<PolicyStatus> status = tracker.getStatus(policy1);
        assertTrue(status.isPresent());

        assertEquals(policy1.getName(), status.get().getPolicyId());
        assertEquals(policy1.getVersion(), status.get().getPolicyVersion());

        // check not-found case
        status = tracker.getStatus(policy4);
        assertFalse(status.isPresent());
    }

    @Test
    public void testAddData() {
        tracker.addData(makeData(policy1, PDP1, PDP2));
        assertEquals(1, map.size());

        PolicyTrackerData data = map.get(policy1);
        assertNotNull(data);
        assertFalse(data.isComplete());

        // add same policy - should still have the same data object
        tracker.addData(makeData(policy1, PDP1, PDP3));
        assertEquals(1, map.size());
        assertSame(data, map.get(policy1));
        assertFalse(data.isComplete());

        // add another policy
        tracker.addData(makeData(policy2, PDP2));
        assertEquals(2, map.size());

        // data for policy 1 is unchanged
        assertSame(data, map.get(policy1));
        assertFalse(data.isComplete());

        // policy 2 should have its own data
        assertTrue(map.get(policy2) != data);
        data = map.get(policy2);
        assertNotNull(data);
        assertFalse(data.isComplete());
    }

    /**
     * Tests removeData() when the policy isn't in the map.
     */
    @Test
    public void testRemoveDataUnknownPolicy() {
        tracker.addData(makeData(policy1, PDP1, PDP2));
        tracker.addData(makeData(policy2, PDP1, PDP3));

        // remove a policy that isn't in the map
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.removeData(makeData(policy3, PDP1), statusList);
        assertTrue(statusList.isEmpty());
        assertEquals(2, map.size());
    }

    /**
     * Tests removeData() when only some PDPs are removed from the policy.
     */
    @Test
    public void testRemoveDataRemoveSomePdps() {
        tracker.addData(makeData(policy1, PDP1, PDP2));
        tracker.addData(makeData(policy2, PDP1, PDP3));

        // remove some PDPs from a policy - no notifications and no changes to the map
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.removeData(makeData(policy2, PDP1), statusList);
        assertTrue(statusList.isEmpty());
        assertTrue(map.containsKey(policy1));
        assertTrue(map.containsKey(policy2));
    }

    /**
     * Tests removeData() when the subclass indicates that the policy should NOT be
     * removed.
     */
    @Test
    public void testRemoveDataDoNotRemovePolicy() {
        tracker = new MyTracker() {
            @Override
            protected boolean shouldRemove(PolicyTrackerData data) {
                return false;
            }
        };

        tracker.addData(makeData(policy1, PDP1, PDP2));
        tracker.addData(makeData(policy2, PDP1, PDP3));

        // remove all the PDPs from one policy, but do NOT remove the policy
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.removeData(makeData(policy2, PDP1, PDP3), statusList);
        assertEquals(1, statusList.size());
        assertEquals(policy2, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());
        assertTrue(map.containsKey(policy1));
        assertTrue(map.containsKey(policy2));
    }

    /**
     * Tests removeData() when the subclass indicates that the policy should be removed.
     */
    @Test
    public void testRemoveDataRemovePolicy() {
        tracker.addData(makeData(policy1, PDP1, PDP2));
        tracker.addData(makeData(policy2, PDP1, PDP3));

        // remove all the PDPs from one policy, and remove the policy
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.removeData(makeData(policy1, PDP1, PDP2, PDP3), statusList);
        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());
        assertFalse(map.containsKey(policy1));
        assertTrue(map.containsKey(policy2));
    }

    @Test
    public void testRemovePdp() {
        tracker.addData(makeData(policy1, PDP1));
        tracker.addData(makeData(policy2, PDP1, PDP3));
        tracker.addData(makeData(policy3, PDP1, PDP2, PDP3));
        tracker.addData(makeData(policy4, PDP4, PDP2, PDP3));

        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.removePdp(PDP1, statusList);

        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());

        assertEquals(3, map.size());
        assertFalse(map.containsKey(policy1));
        assertEquals("[0, 0, 1]", getCounts(map.get(policy2)).toString());
        assertEquals("[0, 0, 2]", getCounts(map.get(policy3)).toString());
        assertEquals("[0, 0, 3]", getCounts(map.get(policy4)).toString());
    }

    @Test
    public void testProcessResponse() {
        tracker.addData(makeData(policy1, PDP1));
        tracker.addData(makeData(policy2, PDP1, PDP3));
        tracker.addData(makeData(policy3, PDP1, PDP2, PDP3));
        tracker.addData(makeData(policy4, PDP4, PDP2, PDP3));

        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.processResponse(PDP1, Arrays.asList(policy3), statusList);

        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());

        assertEquals(3, map.size());
        assertFalse(map.containsKey(policy1));
        assertEquals("[0, 1, 1]", getCounts(map.get(policy2)).toString());
        assertEquals("[1, 0, 2]", getCounts(map.get(policy3)).toString());
        assertEquals("[0, 0, 3]", getCounts(map.get(policy4)).toString());
    }

    @Test
    public void testUpdateMap() {
        tracker.addData(makeData(policy1, PDP1));
        tracker.addData(makeData(policy2, PDP1, PDP3));
        tracker.addData(makeData(policy3, PDP1, PDP2, PDP3));
        tracker.addData(makeData(policy4, PDP4, PDP2, PDP3));

        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.processResponse(PDP1, Arrays.asList(policy3), statusList);

        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());

        assertEquals(3, map.size());
        assertFalse(map.containsKey(policy1));
        assertEquals("[0, 1, 1]", getCounts(map.get(policy2)).toString());
        assertEquals("[1, 0, 2]", getCounts(map.get(policy3)).toString());
        assertEquals("[0, 0, 3]", getCounts(map.get(policy4)).toString());
    }

    /**
     * Tests updateMap() when the policy should NOT be removed.
     */
    @Test
    public void testUpdateMapDoNotRemove() {
        tracker = new MyTracker() {
            @Override
            protected boolean shouldRemove(PolicyTrackerData data) {
                return false;
            }
        };

        tracker.addData(makeData(policy1, PDP1, PDP2));

        // indicate that PDP2 has succeeded
        tracker.processResponse(PDP2, Arrays.asList(policy1), new ArrayList<>(0));

        // indicate that PDP1 has succeeded
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);

        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());

        assertEquals(1, map.size());
        assertTrue(map.containsKey(policy1));
        assertEquals("[2, 0, 0]", getCounts(map.get(policy1)).toString());
    }

    /**
     * Tests updateMap() when the policy SHOULD be removed.
     */
    @Test
    public void testUpdateMapRemovePolicy() {
        tracker.addData(makeData(policy1, PDP1, PDP2));

        // indicate that PDP2 has succeeded
        tracker.processResponse(PDP2, Arrays.asList(policy1), new ArrayList<>(0));

        // indicate that PDP1 has succeeded
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.processResponse(PDP1, Arrays.asList(policy1), statusList);

        assertEquals(1, statusList.size());
        assertEquals(policy1, statusList.get(0).getPolicy());
        assertEquals(type, statusList.get(0).getPolicyType());
        assertEquals("[2, 0, 0]", getCounts(statusList.get(0)).toString());

        assertTrue(map.isEmpty());
    }

    @Test
    public void testMakeStatus() {
        tracker.addData(makeData(policy1, PDP1, PDP2));

        // indicate that PDP2 has succeeded
        List<PolicyStatus> statusList = new ArrayList<>();
        tracker.processResponse(PDP2, Arrays.asList(policy1), statusList);
        assertTrue(statusList.isEmpty());

        // indicate that PDP1 has failed
        tracker.processResponse(PDP1, Arrays.asList(policy2), statusList);
        assertEquals(1, statusList.size());
        assertEquals("[1, 1, 0]", getCounts(statusList.get(0)).toString());
    }

    private class MyTracker extends PolicyCommonTracker {

        public MyTracker() {
            map = Whitebox.getInternalState(this, MAP_FIELD);
        }

        @Override
        protected boolean updateData(String pdp, PolicyTrackerData data, boolean stillActive) {
            return (stillActive ? data.success(pdp) : data.fail(pdp));
        }

        @Override
        protected boolean shouldRemove(PolicyTrackerData data) {
            return data.isComplete();
        }
    }
}
