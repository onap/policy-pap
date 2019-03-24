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

package org.onap.policy.pap.main.parameters;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.comm.Publisher;

public class TestRequestDataParams {
    private RequestDataParams params;
    private Publisher pub;
    private RequestIdDispatcher<PdpStatus> disp;
    private Object lock;

    /**
     * Sets up the objects and creates an empty {@link #params}.
     */
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        pub = mock(Publisher.class);
        disp = mock(RequestIdDispatcher.class);
        lock = new Object();

        params = new RequestDataParams();
    }

    @Test
    public void testGettersSetters() {
        assertSame(params, params.setModifyLock(lock).setPublisher(pub).setResponseDispatcher(disp));

        assertSame(pub, params.getPublisher());
        assertSame(disp, params.getResponseDispatcher());
        assertSame(lock, params.getModifyLock());
    }

    @Test
    public void testValidate() {
        // no exception
        params.setModifyLock(lock).setPublisher(pub).setResponseDispatcher(disp).validate();
    }

    @Test
    public void testValidate_MissingLock() {
        assertThatIllegalArgumentException().isThrownBy(
            () -> params.setPublisher(pub).setResponseDispatcher(disp).validate())
            .withMessageContaining("Lock");
    }

    @Test
    public void testValidate_MissingDispatcher() {
        assertThatIllegalArgumentException().isThrownBy(
            () -> params.setModifyLock(lock).setPublisher(pub).validate())
            .withMessageContaining("Dispatcher");
    }

    @Test
    public void testValidate_MissingPublisher() {
        assertThatIllegalArgumentException().isThrownBy(
            () -> params.setModifyLock(lock).setResponseDispatcher(disp).validate())
            .withMessageContaining("publisher");
    }
}
