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
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class PolicyTypeIdentTest {
    private static final String NAME = "my-name";
    private static final String VERSION = "10.11.12";

    private PolicyTypeIdent resp;
    private PolicyTypeIdent resp2;
    private org.onap.policy.models.pap.concepts.PolicyTypeIdent extresp;

    @Before
    public void setUp() {
        resp = new PolicyTypeIdent();
        extresp = new org.onap.policy.models.pap.concepts.PolicyTypeIdent();
    }

    @Test
    public void testPolicyTypeIdentPolicyTypeIdent() {
        // first check with null values
        resp2 = new PolicyTypeIdent(resp);
        assertEquals(resp.toString(), resp2.toString());
        assertEquals(resp.getName(), resp2.getName());
        assertEquals(resp.getVersion(), resp2.getVersion());

        // populate it and check again
        populate(resp);
        resp2 = new PolicyTypeIdent(resp);
        assertEquals(resp.toString(), resp2.toString());
        assertEquals(resp.getName(), resp2.getName());
        assertEquals(resp.getVersion(), resp2.getVersion());
    }

    @Test
    public void testPolicyTypeIdent() {
        assertTrue(resp.isNullKey());
    }

    @Test
    public void testPolicyTypeIdentExternalPolicyTypeIdent() {
        // first check with null values
        resp2 = new PolicyTypeIdent(extresp);
        assertEquals(IdentUtil.NULL_NAME_INTERNAL, resp2.getName());
        assertEquals(IdentUtil.NULL_VERSION_INTERNAL, resp2.getVersion());

        // populate it and check again
        populate(extresp);
        resp2 = new PolicyTypeIdent(extresp);
        assertEquals(extresp.getName(), resp2.getName());
        assertEquals(extresp.getVersion(), resp2.getVersion());
    }

    @Test
    public void testToExternal() {
        // first check with null values
        extresp = resp.toExternal();
        assertEquals(IdentUtil.NULL_NAME_EXTERNAL, extresp.getName());
        assertEquals(IdentUtil.NULL_VERSION_EXTERNAL, extresp.getVersion());

        // populate it and check again
        populate(resp);
        extresp = resp.toExternal();
        assertEquals(resp.getName(), extresp.getName());
        assertEquals(resp.getVersion(), extresp.getVersion());
    }

    @Test
    public void testCopyToPolicyTypeIdent() {
        // first check with null values
        extresp = new org.onap.policy.models.pap.concepts.PolicyTypeIdent();
        resp.copyTo(extresp);
        assertEquals(IdentUtil.NULL_NAME_EXTERNAL, extresp.getName());
        assertEquals(IdentUtil.NULL_VERSION_EXTERNAL, extresp.getVersion());

        // populate it and check again
        populate(resp);
        extresp = new org.onap.policy.models.pap.concepts.PolicyTypeIdent();
        resp.copyTo(extresp);
        assertEquals(resp.getName(), extresp.getName());
        assertEquals(resp.getVersion(), extresp.getVersion());
    }

    /**
     * Populates all fields in the target.
     *
     * @param target target whose fields are to be set
     */
    private void populate(PolicyTypeIdent target) {
        target.setName(NAME);
        target.setVersion(VERSION);
    }

    /**
     * Populates all fields in the target.
     *
     * @param target target whose fields are to be set
     */
    private void populate(org.onap.policy.models.pap.concepts.PolicyTypeIdent target) {
        target.setName(NAME);
        target.setVersion(VERSION);
    }
}
