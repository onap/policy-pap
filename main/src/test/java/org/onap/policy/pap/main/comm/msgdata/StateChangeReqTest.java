/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pap.main.comm.CommonRequestBase;

class StateChangeReqTest extends CommonRequestBase {

    private StateChangeReq data;
    private PdpStatus response;
    private PdpStateChange msg;

    /**
     * Sets up.
     *
     * @throws Exception if an error occurs
     */
    @Override
    @BeforeEach
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
    void testGetMessage() {
        assertEquals(MY_REQ_NAME, data.getName());
        assertSame(msg, data.getMessage());
    }

    @Test
    void testCheckResponse() {
        assertNull(data.checkResponse(response));
    }

    @Test
    void testCheckResponse_NullName() {
        response.setName(null);

        assertEquals("null PDP name", data.checkResponse(response));
    }

    @Test
    void testCheckResponse_NullMsgName() {
        msg.setName(null);

        assertNull(data.checkResponse(response));
    }

    @Test
    void testCheckResponse_MismatchedState() {
        response.setState(DIFF_STATE);

        assertEquals("state is TERMINATED, but expected SAFE", data.checkResponse(response));
    }

    @Test
    void testReconfigure() {
        // different message type should fail and leave message unchanged
        assertFalse(data.reconfigure(new PdpUpdate()));
        assertSame(msg, data.getMessage());

        // same state - should succeed, but leave message unchanged
        PdpStateChange msg2 = new PdpStateChange();
        msg2.setState(msg.getState());
        assertTrue(data.reconfigure(msg2));
        assertSame(msg, data.getMessage());

        // change old state to active - should fail and leave message unchanged
        msg2.setState(PdpState.ACTIVE);
        assertFalse(data.reconfigure(msg2));
        assertSame(msg, data.getMessage());

        // different, inactive state - should succeed and install NEW message
        msg2.setState(PdpState.PASSIVE);
        assertTrue(data.reconfigure(msg2));
        assertSame(msg2, data.getMessage());
    }
}
