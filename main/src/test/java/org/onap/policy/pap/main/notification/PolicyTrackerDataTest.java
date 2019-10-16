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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.notification.PolicyTrackerData;

public class PolicyTrackerDataTest {

    private static final ToscaPolicyTypeIdentifier TYPE = new ToscaPolicyTypeIdentifier("my-type", "1.2.3");
    private static final String PDP1 = "pdp-1";
    private static final String PDP2 = "pdp-2";
    private static final String PDP3 = "pdp-3";
    private static final String PDP4 = "pdp-4";
    private static final String PDP5 = "pdp-5";
    private static final String PDP6 = "pdp-6";

    private Set<String> fullSet;
    private PolicyTrackerData data;

    @Before
    public void setUp() {
        fullSet = makeSet(PDP1, PDP2, PDP3, PDP4, PDP5, PDP6);
        data = new PolicyTrackerData(TYPE);
    }

    @Test
    public void testPolicyTrackerData_testGetPolicyType() {
        assertSame(TYPE, data.getPolicyType());
    }

    @Test
    public void testIsComplete() {
        assertTrue(data.isComplete());

        data.addPdps(makeSet(PDP1, PDP2));
        assertFalse(data.isComplete());

        data.success(PDP1);
        assertFalse(data.isComplete());

        data.fail(PDP2);
        assertTrue(data.isComplete());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(data.isEmpty());

        data.addPdps(makeSet(PDP1, PDP2));
        assertFalse(data.isEmpty());

        data.success(PDP1);
        assertFalse(data.isEmpty());

        data.fail(PDP2);
        assertFalse(data.isEmpty());

        data.removePdp(PDP1);
        assertFalse(data.isEmpty());

        data.removePdp(PDP2);
        assertTrue(data.isEmpty());
    }

    @Test
    public void testPutValuesInto() {
        data.addPdps(fullSet);
        data.success(PDP1);
        data.fail(PDP2);
        data.fail(PDP3);

        PolicyStatus status = new PolicyStatus();
        data.putValuesInto(status);

        assertEquals(1, status.getSuccessCount());
        assertEquals(2, status.getFailureCount());
        assertEquals(3, status.getIncompleteCount());
    }

    @Test
    public void testAddPdps_testSuccess_testFail() {
        data.addPdps(makeSet(PDP1, PDP2, PDP3, PDP4));
        assertEquals("[0, 0, 4]", getCounts().toString());

        data.success(PDP1);
        assertEquals("[1, 0, 3]", getCounts().toString());

        data.success(PDP2);
        assertEquals("[2, 0, 2]", getCounts().toString());

        // repeat
        data.success(PDP2);
        assertEquals("[2, 0, 2]", getCounts().toString());

        data.fail(PDP3);
        assertEquals("[2, 1, 1]", getCounts().toString());

        // repeat
        data.fail(PDP3);
        assertEquals("[2, 1, 1]", getCounts().toString());

        data.addPdps(makeSet(PDP2, PDP3, PDP4, PDP5));

        // PDP1 is still success
        assertEquals("[1, 0, 4]", getCounts().toString());
    }

    @Test
    public void testRemovePdps() {
        data.addPdps(makeSet(PDP1, PDP2, PDP3, PDP4, PDP5, PDP6));
        data.success(PDP1);
        data.success(PDP2);
        data.fail(PDP3);
        data.fail(PDP4);
        assertFalse(data.removePdps(makeSet(PDP1, PDP3, PDP5)));
        assertEquals("[1, 1, 1]", getCounts().toString());
    }

    /**
     * Tests removePdps(), where nothing is removed from the "incomplete" set.
     */
    @Test
    public void testRemovePdpsNoIncompleteRemove() {
        assertFalse(data.removePdps(makeSet(PDP1, PDP2)));
        assertEquals("[0, 0, 0]", getCounts().toString());
    }

    /**
     * Tests removePdps(), where remaining incomplete items are removed.
     */
    @Test
    public void testRemovePdpsAllComplete() {
        data.addPdps(makeSet(PDP1));
        assertTrue(data.removePdps(makeSet(PDP1)));

        data.addPdps(makeSet(PDP1, PDP2, PDP3));
        assertFalse(data.removePdps(makeSet(PDP1)));
        assertTrue(data.removePdps(makeSet(PDP2, PDP3)));
    }

    @Test
    public void testRemovePdp() {
        data.addPdps(makeSet(PDP1, PDP2, PDP3, PDP4, PDP5, PDP6));
        data.success(PDP1);
        data.success(PDP2);
        data.fail(PDP3);
        data.fail(PDP4);

        assertFalse(data.removePdp(PDP1));
        assertEquals("[1, 2, 2]", getCounts().toString());

        assertFalse(data.removePdp(PDP2));
        assertEquals("[0, 2, 2]", getCounts().toString());

        assertFalse(data.removePdp(PDP3));
        assertEquals("[0, 1, 2]", getCounts().toString());

        assertFalse(data.removePdp(PDP4));
        assertEquals("[0, 0, 2]", getCounts().toString());

        assertFalse(data.removePdp(PDP5));
        assertEquals("[0, 0, 1]", getCounts().toString());

        assertTrue(data.removePdp(PDP6));
        assertEquals("[0, 0, 0]", getCounts().toString());
    }

    /**
     * Tests removePdps(), where nothing is removed from the "incomplete" set.
     */
    @Test
    public void testRemovePdpNoIncompleteRemove() {
        assertFalse(data.removePdp(PDP1));
        assertEquals("[0, 0, 0]", getCounts().toString());
    }

    /**
     * Tests removePdps(), where remaining incomplete items are removed.
     */
    @Test
    public void testRemovePdpAllComplete() {
        data.addPdps(makeSet(PDP1, PDP2));
        assertFalse(data.removePdp(PDP1));

        assertTrue(data.removePdp(PDP2));
    }

    @Test
    public void testComplete() {
        // attempt to remove a PDP that isn't in the data
        assertFalse(data.success(PDP1));

        // remove one that was incomplete
        data.addPdps(makeSet(PDP1));
        assertTrue(data.success(PDP1));

        // move from one set to the other
        assertTrue(data.fail(PDP1));

        // already in the correct set
        assertFalse(data.fail(PDP1));
    }

    private Set<String> makeSet(String... strings) {
        return new HashSet<>(Arrays.asList(strings));
    }

    private List<Integer> getCounts() {
        PolicyStatus status = new PolicyStatus();
        data.putValuesInto(status);

        return Arrays.asList(status.getSuccessCount(), status.getFailureCount(), status.getIncompleteCount());
    }
}
