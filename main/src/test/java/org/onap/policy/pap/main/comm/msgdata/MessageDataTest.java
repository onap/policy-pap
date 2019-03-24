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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.comm.TimerManager;

public class MessageDataTest {
    private static final int RETRIES = 1;

    private MyData data;
    private TimerManager timers;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        timers = mock(TimerManager.class);

        data = new MyData();
    }

    @Test
    public void testGetMessage() {
        assertNotNull(data.getMessage());
    }

    @Test
    public void testGetType() {
        assertEquals(PdpStateChange.class.getSimpleName(), data.getType());
    }

    @Test
    public void testGetMaxRetryCount() {
        assertEquals(RETRIES, data.getMaxRetryCount());
    }

    @Test
    public void testGetTimers() {
        assertSame(timers, data.getTimers());
    }

    private class MyData extends MessageData {

        public MyData() {
            super(new PdpStateChange(), RETRIES, timers);
        }

        @Override
        public void mismatch(String reason) {
            // do nothing
        }

        @Override
        public void completed() {
            // do nothing
        }

        @Override
        public String checkResponse(PdpStatus response) {
            // always succeed
            return null;
        }
    }
}
