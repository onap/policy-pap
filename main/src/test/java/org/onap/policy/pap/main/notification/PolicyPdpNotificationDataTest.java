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
import java.util.Collections;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

/**
 * Note: this wraps the PDPs in a TreeSet so that the content can be verified without
 * worrying about order.
 */
public class PolicyPdpNotificationDataTest {
    private static final String PDP1 = "pdp-1";
    private static final String PDP2 = "pdp-2";
    private static final String PDP3 = "pdp-3";
    private static final String PDP4 = "pdp-4";

    private ToscaPolicyIdentifier policyId;
    private ToscaPolicyTypeIdentifier policyType;
    private PolicyPdpNotificationData data;

    /**
     * Creates various objects, including {@link #data}.
     */
    @Before
    public void setUp() {
        policyId = new ToscaPolicyIdentifier("my-id", "1.2.3");
        policyType = new ToscaPolicyTypeIdentifier("my-type", "3.2.1");

        data = new PolicyPdpNotificationData(policyId, policyType);
    }

    @Test
    public void testPolicyPdpNotificationData() {
        assertSame(policyId, data.getPolicyId());
        assertSame(policyType, data.getPolicyType());
        assertTrue(data.getPdps().isEmpty());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(data.isEmpty());

        data.add(PDP1);
        assertFalse(data.isEmpty());

        data.add(PDP2);
        data.add(PDP3);
        assertFalse(data.isEmpty());

        data.removeAll(Arrays.asList(PDP1, PDP3));
        assertFalse(data.isEmpty());

        data.removeAll(Arrays.asList(PDP2));
        assertTrue(data.isEmpty());
    }

    @Test
    public void testAdd() {
        data.add(PDP1);
        assertEquals("[pdp-1]", new TreeSet<>(data.getPdps()).toString());

        data.add(PDP3);
        assertEquals("[pdp-1, pdp-3]", new TreeSet<>(data.getPdps()).toString());
    }

    @Test
    public void testAddAll() {
        // verify we can add an empty list
        data.addAll(Collections.emptyList());
        assertTrue(data.getPdps().isEmpty());

        // try a non-empty list
        data.addAll(Arrays.asList(PDP1, PDP3));
        assertEquals("[pdp-1, pdp-3]", new TreeSet<>(data.getPdps()).toString());
    }

    @Test
    public void testRemoveAll() {
        // verify we can remove an empty list
        data.removeAll(Collections.emptyList());
        assertTrue(data.getPdps().isEmpty());

        // now test with non-empty lists
        data.addAll(Arrays.asList(PDP1, PDP2, PDP3, PDP4));

        data.removeAll(Arrays.asList(PDP1, PDP3));
        assertEquals("[pdp-2, pdp-4]", new TreeSet<>(data.getPdps()).toString());

        data.removeAll(Arrays.asList(PDP2, PDP4));
        assertEquals("[]", new TreeSet<>(data.getPdps()).toString());
    }
}
