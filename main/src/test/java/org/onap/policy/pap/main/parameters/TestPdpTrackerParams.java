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
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.comm.TimerManager;

public class TestPdpTrackerParams {
    private PdpTrackerParams params;
    private PdpModifyRequestMap requestMap;
    private Object lock;
    private TimerManager timers;
    private PolicyModelsProviderFactoryWrapper daoFactory;

    /**
     * Sets up the objects and creates an empty {@link #params}.
     */
    @Before
    public void setUp() {
        requestMap = mock(PdpModifyRequestMap.class);
        lock = new Object();
        daoFactory = mock(PolicyModelsProviderFactoryWrapper.class);
        timers = mock(TimerManager.class);

        params = new PdpTrackerParams().setRequestMap(requestMap).setModifyLock(lock).setTimers(timers)
                        .setDaoFactory(daoFactory);
    }

    @Test
    public void testGettersSetters() {
        assertSame(requestMap, params.getRequestMap());
        assertSame(lock, params.getModifyLock());
        assertSame(timers, params.getTimers());
        assertSame(daoFactory, params.getDaoFactory());
    }

    @Test
    public void testValidate() {
        // no exception
        params.validate();
    }

    @Test
    public void testValidate_MissingRequestMap() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setRequestMap(null).validate())
                        .withMessageContaining("Map");
    }

    @Test
    public void testValidate_MissingLock() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setModifyLock(null).validate())
                        .withMessageContaining("Lock");
    }

    @Test
    public void testValidate_InvalidTimers() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setTimers(null).validate())
                        .withMessageContaining("timers");
    }

    @Test
    public void testValidate_MissingDaoFactory() {
        assertThatIllegalArgumentException().isThrownBy(() -> params.setDaoFactory(null).validate())
                        .withMessageContaining("dao");
    }
}
