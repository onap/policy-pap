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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.pap.main.comm.TimerManager;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.onap.policy.pap.main.parameters.PdpStateChangeParameters;

public class StateChangeDataTest {
    private static final String MY_NAME = "my-name";
    private static final String DIFFERENT = "different";
    private static final PdpState MY_STATE = PdpState.SAFE;
    private static final PdpState DIFF_STATE = PdpState.TERMINATED;
    private static final int RETRIES = 1;

    private MyData data;
    private PdpModifyRequestMapParams params;
    private TimerManager timers;
    private PdpStatus response;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        timers = mock(TimerManager.class);
        response = new PdpStatus();
        PdpParameters pdpParams = mock(PdpParameters.class);
        PdpStateChangeParameters stateParams = mock(PdpStateChangeParameters.class);

        when(stateParams.getMaxRetryCount()).thenReturn(RETRIES);
        when(pdpParams.getStateChangeParameters()).thenReturn(stateParams);

        params = new PdpModifyRequestMapParams().setParams(pdpParams).setStateChangeTimers(timers);

        response.setName(MY_NAME);
        response.setState(MY_STATE);

        data = new MyData();
    }

    @Test
    public void testGetMaxRetryCount() {
        assertEquals(RETRIES, data.getMaxRetryCount());
    }

    @Test
    public void testGetTimers() {
        assertSame(timers, data.getTimers());
    }

    @Test
    public void testStateChangeCheckResponse() {
        assertNull(data.checkResponse(response));
    }

    @Test
    public void testStateChangeCheckResponse_MismatchedName() {
        response.setName(DIFFERENT);

        assertEquals("name does not match", data.checkResponse(response));
    }

    @Test
    public void testStateChangeCheckResponse_MismatchedState() {
        response.setState(DIFF_STATE);

        assertEquals("state is TERMINATED, but expected SAFE", data.checkResponse(response));
    }

    private class MyData extends StateChangeData {

        public MyData() {
            super(new PdpStateChange(), params);

            PdpStateChange msg = (PdpStateChange) getMessage();

            msg.setName(MY_NAME);
            msg.setState(MY_STATE);
        }

        @Override
        public void mismatch(String reason) {
            // do nothing
        }

        @Override
        public void completed() {
            // do nothing
        }
    }
}
