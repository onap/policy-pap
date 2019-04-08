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

package org.onap.policy.pap.main.comm.msgdata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.pap.main.comm.CommonRequestBase;

public class StateChangeReqTest extends CommonRequestBase {

    private StateChangeReq data;
    private PdpStatus response;
    private PdpStateChange msg;

    /**
     * Sets up.
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        response = new PdpStatus();

        response.setName(MY_NAME);
        response.setState(MY_STATE);

        msg = new PdpStateChange();
        msg.setName(MY_NAME);
        msg.setState(MY_STATE);

        data = new StateChangeReq(reqParams, MY_REQ_NAME, msg);
    }

    @Test
    public void testGetMessage() {
        assertEquals(MY_REQ_NAME, data.getName());
        assertSame(msg, data.getMessage());
    }

    @Test
    public void testCheckResponse() {
        assertNull(data.checkResponse(response));
    }

    @Test
    public void testCheckResponse_NullName() {
        response.setName(null);

        assertEquals("null PDP name", data.checkResponse(response));
    }

    @Test
    public void testCheckResponse_NullMsgName() {
        msg.setName(null);

        assertEquals(null, data.checkResponse(response));
    }

    @Test
    public void testCheckResponse_MismatchedState() {
        response.setState(DIFF_STATE);

        assertEquals("state is TERMINATED, but expected SAFE", data.checkResponse(response));
    }

    @Test
    public void isSameContent() {
        PdpStateChange msg2 = new PdpStateChange();
        msg2.setName("world");
        msg2.setState(MY_STATE);
        assertTrue(data.isSameContent(new StateChangeReq(reqParams, MY_REQ_NAME, msg2)));

        // different state
        msg2.setState(DIFF_STATE);
        assertFalse(data.isSameContent(new StateChangeReq(reqParams, MY_REQ_NAME, msg2)));

        // different request type
        assertFalse(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, new PdpUpdate())));
    }

    @Test
    public void testGetPriority() {
        assertTrue(data.getPriority() < new UpdateReq(reqParams, MY_REQ_NAME, new PdpUpdate()).getPriority());
    }
}
