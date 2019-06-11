/*-
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.TimerManager.Timer;
import org.onap.policy.pap.main.parameters.PdpTrackerParams;

public class PdpTrackerTest {
    private static final String PDP1 = "pdp1";
    private static final String PDP2 = "pdp2";

    private PdpTracker tracker;
    private PdpTrackerParams params;

    private Object modifyLock;

    @Captor
    private ArgumentCaptor<Consumer<String>> handlerCaptor;

    @Mock
    private PdpModifyRequestMap requestMap;

    @Mock
    private TimerManager timers;

    @Mock
    private PolicyModelsProviderFactoryWrapper daoFactory;

    @Mock
    private PolicyModelsProvider dao;

    @Mock
    private Timer timer1;

    @Mock
    private Timer timer2;

    /**
     * Sets up.
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        modifyLock = new Object();

        params = new PdpTrackerParams().setDaoFactory(daoFactory).setModifyLock(modifyLock).setRequestMap(requestMap)
                        .setTimers(timers);

        when(daoFactory.create()).thenReturn(dao);

        when(dao.getPdpGroups(null)).thenReturn(Collections.emptyList());

        when(timers.register(eq(PDP1), any())).thenReturn(timer1);
        when(timers.register(eq(PDP2), any())).thenReturn(timer2);

        tracker = new PdpTracker(params);
    }

    @Test
    public void testPdpTracker() throws Exception {
        // verify that PDPs were loaded
        verify(dao).getPdpGroups(null);
    }

    @Test
    public void testLoadPdps_testLoadPdpsFromGroup() throws Exception {
        // arrange for DAO to return a couple of groups
        String groupsJson = ResourceUtils.getResourceAsString("comm/PdpTracker.json");
        List<PdpGroup> groups = new StandardCoder().decode(groupsJson, PdpGroups.class).getGroups();
        when(dao.getPdpGroups(null)).thenReturn(groups);

        tracker = new PdpTracker(params);

        // verify that all PDPs were registered
        verify(timers).register(eq("pdp-A"), any());
        verify(timers).register(eq("pdp-B"), any());
        verify(timers).register(eq("pdp-C"), any());
        verify(timers).register(eq("pdp-D"), any());
    }

    @Test
    public void testLoadPdps_DaoException() throws Exception {
        // arrange for DAO to throw an exception
        PfModelException ex = mock(PfModelException.class);
        when(daoFactory.create()).thenThrow(ex);

        assertThatThrownBy(() -> new PdpTracker(params)).isInstanceOf(PolicyPapRuntimeException.class).hasCause(ex);
    }

    @Test
    public void testAdd() {
        tracker.add(PDP1);
        verify(timers).register(eq(PDP1), any());
        verify(timer1, never()).cancel();

        tracker.add(PDP2);
        verify(timers).register(eq(PDP2), any());
        verify(timer1, never()).cancel();
        verify(timer2, never()).cancel();

        // re-add PDP1 - old timer should be canceled and a new timer added
        Timer timer3 = mock(Timer.class);
        when(timers.register(eq(PDP1), any())).thenReturn(timer3);
        tracker.add(PDP1);
        verify(timer1).cancel();
        verify(timer2, never()).cancel();
        verify(timer3, never()).cancel();
    }

    @Test
    public void testHandleTimeout() throws Exception {
        tracker.add(PDP1);
        tracker.add(PDP2);

        verify(timers).register(eq(PDP1), handlerCaptor.capture());

        handlerCaptor.getValue().accept(PDP1);

        verify(requestMap).removeFromGroups(PDP1);

        // now we'll re-add PDP1 - the original timer should not be canceled
        Timer timer3 = mock(Timer.class);
        when(timers.register(eq(PDP1), any())).thenReturn(timer3);
        tracker.add(PDP1);
        verify(timer1, never()).cancel();
    }

    @Test
    public void testHandleTimeout_MapException() throws Exception {
        tracker.add(PDP1);

        verify(timers).register(eq(PDP1), handlerCaptor.capture());

        // arrange for request map to throw an exception
        PfModelException ex = mock(PfModelException.class);
        when(requestMap.removeFromGroups(PDP1)).thenThrow(ex);

        // exception should be caught, but not re-thrown
        handlerCaptor.getValue().accept(PDP1);
    }
}
