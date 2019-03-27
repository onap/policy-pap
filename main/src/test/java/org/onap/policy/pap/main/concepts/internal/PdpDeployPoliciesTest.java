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

package org.onap.policy.pap.main.concepts.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class PdpDeployPoliciesTest {
    private static final String POLICY1 = "policy-a";
    private static final String POLICY2 = "policy-b";

    private PdpDeployPolicies request;
    private PdpDeployPolicies request2;
    private org.onap.policy.models.pap.concepts.PdpDeployPolicies extreq;
    private List<PolicyIdentOptVersion> internalPolicies;
    private List<org.onap.policy.models.pap.concepts.PolicyIdentOptVersion> externalPolicies;

    /**
     * Sets up policy lists.
     */
    @Before
    public void setUp() {
        internalPolicies = Arrays.asList(makeInternalPolicy(POLICY1), makeInternalPolicy(POLICY2));
        externalPolicies = Arrays.asList(makeExternalPolicy(POLICY1), makeExternalPolicy(POLICY2));

    }

    @Test
    public void testPdpDeployPoliciesPdpDeployPolicies() {
        request = new PdpDeployPolicies();
        request.setPolicies(internalPolicies);

        request2 = new PdpDeployPolicies(request);
        assertEquals(request.toString(), request2.toString());

        assertEquals(internalPolicies, request2.getPolicies());

        // ensure we received copies and not the original
        assertTrue(request2.getPolicies() != internalPolicies);
        assertTrue(request2.getPolicies().get(0) != internalPolicies.get(0));
    }

    @Test
    public void testPdpDeployPoliciesExternalPdpDeployPolicies() {
        extreq = new org.onap.policy.models.pap.concepts.PdpDeployPolicies();
        extreq.setPolicies(externalPolicies);

        request2 = new PdpDeployPolicies(extreq);
        assertEquals(internalPolicies, request2.getPolicies());
    }

    @Test
    public void testGetPolicies_testSetPolicies() {
        request = new PdpDeployPolicies();
        assertNull(request.getPolicies());

        request.setPolicies(internalPolicies);
        assertSame(internalPolicies, request.getPolicies());
    }

    @Test
    public void testToString() {
        request = new PdpDeployPolicies();
        assertNotNull(request.toString());

        request.setPolicies(internalPolicies);
        assertNotNull(request.toString());
        assertTrue(request.toString().contains(POLICY1));
        assertTrue(request.toString().contains(POLICY2));
    }


    private PolicyIdentOptVersion makeInternalPolicy(String name) {
        PolicyIdentOptVersion policy = new PolicyIdentOptVersion();

        policy.setName(name);

        return policy;
    }

    private org.onap.policy.models.pap.concepts.PolicyIdentOptVersion makeExternalPolicy(String name) {
        return makeInternalPolicy(name).toExternal();
    }
}
