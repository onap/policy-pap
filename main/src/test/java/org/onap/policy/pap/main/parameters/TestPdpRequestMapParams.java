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
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.TimerManager;

public class TestPdpRequestMapParams {
    private PdpRequestMapParams params;
    private Publisher pub;
    private RequestIdDispatcher<PdpStatus> disp;
    private Object lock;
    private PdpParameters pdpParams;
    private TimerManager updTimers;
    private TimerManager stateTimers;
    private TimerManager healthTimers;
    private PolicyModelsProviderFactoryWrapper daoFactory;

    /**
     * Sets up the objects and creates an empty {@link #params}.
     */
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        pub = mock(Publisher.class);
        disp = mock(RequestIdDispatcher.class);
        lock = new Object();
        pdpParams = mock(PdpParameters.class);
        updTimers = mock(TimerManager.class);
        stateTimers = mock(TimerManager.class);
        healthTimers = mock(TimerManager.class);
        daoFactory = mock(PolicyModelsProviderFactoryWrapper.class);

        params = new PdpRequestMapParams().setModifyLock(lock).setPublisher(pub).setResponseDispatcher(disp)
                        .setParams(pdpParams).setStateChangeTimers(stateTimers).setUpdateTimers(updTimers)
                        .setHealthCheckTimers(healthTimers).setDaoFactory(daoFactory);
    }

    @Test
    public void testGettersSetters() {
        assertSame(pub, params.getPublisher());
        assertSame(disp, params.getResponseDispatcher());
        assertSame(lock, params.getModifyLock());
        assertSame(pdpParams, params.getParams());
        assertSame(updTimers, params.getUpdateTimers());
        assertSame(stateTimers, params.getStateChangeTimers());
        assertSame(daoFactory, params.getDaoFactory());
    }

    @Test
    public void testValidate() {
        // no exception
        params.validate();
    }

    @Test
    public void testValidate_MissingPublisher() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setPublisher(null).validate())
                        .withMessageContaining("publisher");
    }

    @Test
    public void testValidate_MissingDispatcher() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setResponseDispatcher(null).validate())
                        .withMessageContaining("Dispatch");
    }

    @Test
    public void testValidate_MissingLock() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setModifyLock(null).validate())
                        .withMessageContaining("Lock");
    }

    @Test
    public void testValidate_MissingPdpParams() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setParams(null).validate())
                        .withMessageContaining("PDP param");
    }

    @Test
    public void testValidate_MissingStateChangeTimers() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setStateChangeTimers(null).validate())
                        .withMessageContaining("state");
    }

    @Test
    public void testValidate_MissingHealthCheckTimers() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setHealthCheckTimers(null).validate())
                        .withMessageContaining("health");
    }

    @Test
    public void testValidate_MissingUpdateTimers() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setUpdateTimers(null).validate())
                        .withMessageContaining("update");
    }

    @Test
    public void testValidate_MissingDaoFactory() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setDaoFactory(null).validate())
                        .withMessageContaining("dao");
    }
}
