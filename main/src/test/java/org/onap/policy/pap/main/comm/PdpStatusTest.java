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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.pap.main.comm.PdpStatus;

public class PdpStatusTest {

    private static final String MY_NAME = "my-name";
    private static final String REQ_ID = "request-id";

    // the last character of these should be one higher, alphabetically
    private static final String DIFF_NAME = "my-namf";
    private static final String DIFF_ID = "request-ie";

    private PdpStatus msg;

    /**
     * Creates an empty message.
     */
    @Before
    public void setUp() {
        msg = new PdpStatus();
    }

    @Test
    public void testPdpStatus() {
        assertNull(msg.getName());
        assertNull(msg.getRequestId());
    }

    @Test
    public void testPdpStatusString() {
        msg = new PdpStatus(MY_NAME);
        assertEquals(MY_NAME, msg.getName());
        assertNull(msg.getRequestId());
    }

    @Test
    public void testPdpStatusStringString() {
        msg = new PdpStatus(MY_NAME, REQ_ID);
        assertEquals(MY_NAME, msg.getName());
        assertEquals(REQ_ID, msg.getRequestId());
    }

    @Test
    public void testGetName_testSetName() {
        assertNull(msg.getName());
        msg.setName(MY_NAME);
        assertEquals(MY_NAME, msg.getName());
    }

    @Test
    public void testGetRequestId_testSetRequestId() {
        assertNull(msg.getRequestId());
        msg.setRequestId(REQ_ID);
        assertEquals(REQ_ID, msg.getRequestId());
    }

    @Test
    public void testEquals() {
        msg = new PdpStatus(MY_NAME, REQ_ID);
        assertFalse(msg.equals(null));
        assertFalse(msg.equals(this));

        assertTrue(msg.equals(msg));
        assertTrue(msg.equals(new PdpStatus(MY_NAME, REQ_ID)));

        assertFalse(new PdpStatus(MY_NAME).equals(msg));
        assertFalse(msg.equals(new PdpStatus(MY_NAME)));
        assertFalse(msg.equals(new PdpStatus(MY_NAME, DIFF_ID)));
        assertFalse(msg.equals(new PdpStatus(DIFF_NAME, REQ_ID)));
    }

    @Test
    public void testHashCode() {
        // should work, even with null values
        msg.hashCode();
        new PdpStatus(MY_NAME).hashCode();

        msg = new PdpStatus(MY_NAME, REQ_ID);
        assertTrue(msg.hashCode() != new PdpStatus(MY_NAME, DIFF_ID).hashCode());
        assertTrue(msg.hashCode() != new PdpStatus(DIFF_NAME, REQ_ID).hashCode());
    }

    @Test
    public void testToString() {
        String text = new PdpStatus(MY_NAME, REQ_ID).toString();
        assertNotNull(text);
        assertTrue(text.contains(MY_NAME));
        assertTrue(text.contains(REQ_ID));
    }

}
