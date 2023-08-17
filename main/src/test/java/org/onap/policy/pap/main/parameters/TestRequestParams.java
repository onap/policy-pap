/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.parameters;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.TimerManager;

public class TestRequestParams {
    private static final int RETRIES = 1;

    private RequestParams params;
    private Publisher<PdpMessage> pub;
    private RequestIdDispatcher<PdpStatus> disp;
    private Object lock;
    private TimerManager timers;

    /**
     * Sets up the objects and creates an empty {@link #params}.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        pub = mock(Publisher.class);
        disp = mock(RequestIdDispatcher.class);
        lock = new Object();
        timers = mock(TimerManager.class);

        params = new RequestParams().setModifyLock(lock).setPdpPublisher(pub).setResponseDispatcher(disp)
                        .setTimers(timers).setMaxRetryCount(RETRIES);
    }

    @Test
    public void testGettersSetters() {
        assertSame(params, params.setModifyLock(lock).setPdpPublisher(pub).setResponseDispatcher(disp));

        assertSame(pub, params.getPdpPublisher());
        assertSame(disp, params.getResponseDispatcher());
        assertSame(lock, params.getModifyLock());
        assertSame(timers, params.getTimers());
        assertEquals(RETRIES, params.getMaxRetryCount());
    }

    @Test
    public void testValidate() {
        // no exception
        params.validate();
    }

    @Test
    public void testValidate_MissingLock() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setModifyLock(null).validate())
                        .withMessageContaining("Lock");
    }

    @Test
    public void testValidate_MissingDispatcher() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setResponseDispatcher(null).validate())
                        .withMessageContaining("Dispatcher");
    }

    @Test
    public void testValidate_MissingPublisher() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setPdpPublisher(null).validate())
                        .withMessageContaining("publisher");
    }

    @Test
    public void testValidate_MissingTimers() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setTimers(null).validate())
                        .withMessageContaining("timer");
    }
}
