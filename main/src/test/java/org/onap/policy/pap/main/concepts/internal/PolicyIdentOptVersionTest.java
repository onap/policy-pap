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

public class PolicyIdentOptVersionTest {
    private static final String NAME = "my-name";
    private static final String VERSION = "10.11.12";

    private PolicyIdentOptVersion resp;
    private PolicyIdentOptVersion resp2;
    private org.onap.policy.models.pap.concepts.PolicyIdentOptVersion extresp;

    @Before
    public void setUp() {
        resp = new PolicyIdentOptVersion();
        extresp = new org.onap.policy.models.pap.concepts.PolicyIdentOptVersion();
    }

    @Test
    public void testPolicyIdentOptVersionPolicyIdentOptVersion() {
        // first check with null values
        resp2 = new PolicyIdentOptVersion(resp);
        assertEquals(resp.toString(), resp2.toString());
        assertTrue(resp2.isNullKey());
        assertTrue(resp2.isNullVersion());

        // populate it and check again
        populate(resp);
        resp2 = new PolicyIdentOptVersion(resp);
        assertEquals(resp.toString(), resp2.toString());
        assertEquals(resp.getName(), resp2.getName());
        assertEquals(resp.getVersion(), resp2.getVersion());
    }

    @Test
    public void testPolicyIdentOptVersion() {
        assertTrue(resp.isNullKey());
    }

    @Test
    public void testPolicyIdentOptVersionExternalPolicyIdentOptVersion() {
        // first check with null values
        resp2 = new PolicyIdentOptVersion(extresp);
        assertEquals(IdentUtil.NULL_INTERNAL_NAME, resp2.getName());
        assertEquals(IdentUtil.NULL_INTERNAL_VERSION, resp2.getVersion());

        // populate it and check again
        populate(extresp);
        resp2 = new PolicyIdentOptVersion(extresp);
        assertEquals(extresp.getName(), resp2.getName());
        assertEquals(extresp.getVersion(), resp2.getVersion());
    }

    @Test
    public void testToExternal() {
        // first check with null values
        extresp = resp.toExternal();
        assertEquals(IdentUtil.NULL_EXTERNAL_NAME, extresp.getName());
        assertEquals(IdentUtil.NULL_EXTERNAL_VERSION, extresp.getVersion());

        // populate it and check again
        populate(resp);
        extresp = resp.toExternal();
        assertEquals(resp.getName(), extresp.getName());
        assertEquals(resp.getVersion(), extresp.getVersion());
    }

    @Test
    public void testCopyToPolicyIdentOptVersion() {
        // first check with null values
        resp.copyTo(extresp);
        assertEquals(IdentUtil.NULL_EXTERNAL_NAME, extresp.getName());
        assertEquals(IdentUtil.NULL_EXTERNAL_VERSION, extresp.getVersion());

        // populate it and check again
        populate(resp);
        extresp = new org.onap.policy.models.pap.concepts.PolicyIdentOptVersion();
        resp.copyTo(extresp);
        assertEquals(resp.getName(), extresp.getName());
        assertEquals(resp.getVersion(), extresp.getVersion());
    }

    /**
     * Populates all fields in the target.
     *
     * @param target target whose fields are to be set
     */
    private void populate(PolicyIdentOptVersion target) {
        target.setName(NAME);
        target.setVersion(VERSION);
    }

    /**
     * Populates all fields in the target.
     *
     * @param target target whose fields are to be set
     */
    private void populate(org.onap.policy.models.pap.concepts.PolicyIdentOptVersion target) {
        target.setName(NAME);
        target.setVersion(VERSION);
    }
}
