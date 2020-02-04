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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

public class PolicyTrackerDataTest {

    private static final ToscaPolicyTypeIdentifier TYPE = new ToscaPolicyTypeIdentifier("my-type", "1.2.3");
    private static final String PDP1 = "pdp-1";
    private static final String PDP2 = "pdp-2";
    private static final String PDP3 = "pdp-3";
    private static final String PDP4 = "pdp-4";
    private static final String PDP5 = "pdp-5";
    private static final String PDP6 = "pdp-6";

    private Collection<String> fullSet;
    private PolicyTrackerData data;

    @Before
    public void setUp() {
        fullSet = Arrays.asList(PDP1, PDP2, PDP3, PDP4, PDP5, PDP6);
        data = new PolicyTrackerData(TYPE);
    }

    @Test
    public void testPolicyTrackerData_testGetPolicyType() {
        assertSame(TYPE, data.getPolicyType());
    }

    @Test
    public void testIsComplete() {
        assertTrue(data.isComplete());

        data.addPdps(Arrays.asList(PDP1, PDP2));
        assertFalse(data.isComplete());

        data.success(PDP1);
        assertFalse(data.isComplete());

        data.fail(PDP2);
        assertTrue(data.isComplete());
    }

    @Test
    public void testAllSucceeded() {
        assertTrue(data.allSucceeded());

        data.addPdps(Arrays.asList(PDP1, PDP2));
        assertFalse(data.allSucceeded());

        data.success(PDP1);
        assertFalse(data.allSucceeded());

        data.fail(PDP2);
        assertFalse(data.allSucceeded());

        data.success(PDP2);
        assertTrue(data.allSucceeded());

        data.fail(PDP2);
        assertFalse(data.allSucceeded());

        data.success(PDP2);
        assertTrue(data.allSucceeded());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(data.isEmpty());

        data.addPdps(Arrays.asList(PDP1, PDP2));
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
        data.addPdps(Arrays.asList(PDP1, PDP2, PDP3, PDP4));
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

        data.addPdps(Arrays.asList(PDP2, PDP3, PDP4, PDP5));

        // PDP1 is still success
        assertEquals("[1, 0, 4]", getCounts().toString());
    }

    @Test
    public void testRemovePdps() {
        data.addPdps(Arrays.asList(PDP1, PDP2, PDP3, PDP4, PDP5, PDP6));
        data.success(PDP1);
        data.success(PDP2);
        data.fail(PDP3);
        data.fail(PDP4);
        assertFalse(data.removePdps(Arrays.asList(PDP1, PDP3, PDP5)));
        assertEquals("[1, 1, 1]", getCounts().toString());

        assertTrue(data.removePdps(Arrays.asList(PDP6)));
        assertEquals("[1, 1, 0]", getCounts().toString());
    }

    /**
     * Tests removePdps(), where nothing is removed from the "incomplete" set.
     */
    @Test
    public void testRemovePdpsNoIncompleteRemove() {
        assertFalse(data.removePdps(Arrays.asList(PDP1, PDP2)));
        assertEquals("[0, 0, 0]", getCounts().toString());
    }

    /**
     * Tests removePdps(), where remaining incomplete items are removed.
     */
    @Test
    public void testRemovePdpsAllComplete() {
        data.addPdps(Arrays.asList(PDP1));
        assertTrue(data.removePdps(Arrays.asList(PDP1)));

        data.addPdps(Arrays.asList(PDP1, PDP2, PDP3));
        assertFalse(data.removePdps(Arrays.asList(PDP1)));
        assertTrue(data.removePdps(Arrays.asList(PDP2, PDP3)));
    }

    /**
     * Tests removePdps() with more variations.
     */
    @Test
    public void testRemovePdpsVariations() {
        data.addPdps(Arrays.asList(PDP1, PDP2, PDP3));
        data.success(PDP1);
        data.fail(PDP2);
        assertEquals("[1, 1, 1]", getCounts().toString());

        // remove PDP1, which checks removal from "success" set, while incomplete
        assertFalse(data.removePdps(Arrays.asList(PDP1)));
        assertEquals("[0, 1, 1]", getCounts().toString());

        // remove PDP2, which checks removal from "failure" set, while incomplete
        assertFalse(data.removePdps(Arrays.asList(PDP2)));
        assertEquals("[0, 0, 1]", getCounts().toString());

        // re-add 1 & 2
        data.addPdps(Arrays.asList(PDP1, PDP2));
        data.success(PDP1);
        data.fail(PDP2);
        assertEquals("[1, 1, 1]", getCounts().toString());

        // remove PDP3, which checks removal from "incomplete" set
        assertTrue(data.removePdps(Arrays.asList(PDP3)));
        assertEquals("[1, 1, 0]", getCounts().toString());

        // remove PDP1, which checks removal from "success" set, while complete
        assertTrue(data.removePdps(Arrays.asList(PDP1)));
        assertEquals("[0, 1, 0]", getCounts().toString());

        // remove PDP2, which checks removal from "failure" set, while complete
        assertTrue(data.removePdps(Arrays.asList(PDP2)));
        assertEquals("[0, 0, 0]", getCounts().toString());

        // re-add 1 and then remove it again
        data.addPdps(Arrays.asList(PDP1));
        assertTrue(data.removePdps(Arrays.asList(PDP1)));
        assertEquals("[0, 0, 0]", getCounts().toString());
    }

    @Test
    public void testRemovePdp() {
        data.addPdps(Arrays.asList(PDP1, PDP2, PDP3, PDP4, PDP5, PDP6));
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
        data.addPdps(Arrays.asList(PDP1, PDP2));
        assertFalse(data.removePdp(PDP1));

        assertTrue(data.removePdp(PDP2));
    }

    /**
     * Tests removePdp() with more variations.
     */
    @Test
    public void testRemovePdpVariations() {
        data.addPdps(Arrays.asList(PDP1, PDP2, PDP3));
        data.success(PDP1);
        data.fail(PDP2);
        assertEquals("[1, 1, 1]", getCounts().toString());

        // remove PDP1, which checks removal from "success" set, while incomplete
        assertFalse(data.removePdp(PDP1));
        assertEquals("[0, 1, 1]", getCounts().toString());

        // remove PDP2, which checks removal from "failure" set, while incomplete
        assertFalse(data.removePdp(PDP2));
        assertEquals("[0, 0, 1]", getCounts().toString());

        // re-add 1 & 2
        data.addPdps(Arrays.asList(PDP1, PDP2));
        data.success(PDP1);
        data.fail(PDP2);
        assertEquals("[1, 1, 1]", getCounts().toString());

        // remove PDP3, which checks removal from "incomplete" set
        assertTrue(data.removePdp(PDP3));
        assertEquals("[1, 1, 0]", getCounts().toString());

        // remove PDP1, which checks removal from "success" set, while complete
        assertTrue(data.removePdp(PDP1));
        assertEquals("[0, 1, 0]", getCounts().toString());

        // remove PDP2, which checks removal from "failure" set, while complete
        assertTrue(data.removePdp(PDP2));
        assertEquals("[0, 0, 0]", getCounts().toString());

        // re-add 1 and then remove it again
        data.addPdps(Arrays.asList(PDP1));
        assertTrue(data.removePdp(PDP1));
        assertEquals("[0, 0, 0]", getCounts().toString());
    }

    @Test
    public void testComplete() {
        // attempt to remove a PDP that isn't in the data
        assertFalse(data.success(PDP1));

        // remove one that was incomplete
        data.addPdps(Arrays.asList(PDP1));
        assertTrue(data.success(PDP1));

        // move from one set to the other
        assertTrue(data.fail(PDP1));

        // already in the correct set
        assertFalse(data.fail(PDP1));
    }

    private List<Integer> getCounts() {
        PolicyStatus status = new PolicyStatus();
        data.putValuesInto(status);

        return Arrays.asList(status.getSuccessCount(), status.getFailureCount(), status.getIncompleteCount());
    }
}
