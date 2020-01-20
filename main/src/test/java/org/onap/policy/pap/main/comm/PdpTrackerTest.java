/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;

public class PdpTrackerTest {
    private static final String GROUP1 = "group-X";
    private static final String GROUP2 = "group-Y";
    private static final String SUBGRP11 = "apex";
    private static final String SUBGRP12 = "drools";
    private static final String SUBGRP21 = "apex";

    private static final String PDP111 = "pdp-A";
    private static final String PDP112 = "pdp-B";
    private static final String PDP121 = "pdp-C";
    private static final String PDP211 = "pdp-D";

    private PdpTracker tracker;
    private PdpTracker.PdpTrackerBuilder builder;

    private Object modifyLock;

    @Captor
    private ArgumentCaptor<Consumer<String>> handlerCaptor;

    @Mock
    private PdpModifyRequestMap requestMap;

    @Mock
    private PolicyModelsProviderFactoryWrapper daoFactory;

    @Mock
    private PolicyModelsProvider dao;

    /**
     * Sets up.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        modifyLock = new Object();

        builder = PdpTracker.builder().daoFactory(daoFactory).modifyLock(modifyLock).requestMap(requestMap);

        when(daoFactory.create()).thenReturn(dao);

        when(dao.getPdpGroups(null)).thenReturn(Collections.emptyList());

        tracker = builder.build();
    }

    @Test
    public void testBuilderToString() throws Exception {
        assertNotNull(builder.toString());
    }

    @Test
    public void testPdpTracker_MissingRequestMap() throws Exception {
        assertThatThrownBy(() -> builder.requestMap(null).build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testPdpTracker_MissingModifyLock() throws Exception {
        assertThatThrownBy(() -> builder.modifyLock(null).build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testPdpTracker_MissingDaoFactory() throws Exception {
        assertThatThrownBy(() -> builder.daoFactory(null).build()).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testLoadPdps_DaoException() throws Exception {
        // arrange for DAO to throw an exception
        PfModelException ex = mock(PfModelException.class);
        when(daoFactory.create()).thenThrow(ex);

        assertThatThrownBy(() -> tracker.run()).isInstanceOf(PolicyPapRuntimeException.class).hasCause(ex);
    }

    @Test
    public void testRun() throws Exception {
        // arrange for DAO to return a couple of groups
        String groupsJson = ResourceUtils.getResourceAsString("comm/PdpTracker.json");
        List<PdpGroup> groups = new StandardCoder().decode(groupsJson, PdpGroups.class).getGroups();
        when(dao.getPdpGroups(null)).thenReturn(groups);

        // run with everything ok
        tracker.add(PDP111);
        tracker.add(PDP112);
        tracker.add(PDP121);
        tracker.add(PDP211);
        tracker.run();
        verify(requestMap, never()).addRequest(any(PdpStateChange.class));

        // now only two are ok
        tracker.add(PDP111);
        tracker.add(PDP121);
        tracker.run();
        Iterator<PdpStateChange> changeIt = getChanges(0, 2).iterator();

        verifyChange(changeIt.next(), GROUP1, SUBGRP11, PDP112, PdpState.ACTIVE);
        verifyChange(changeIt.next(), GROUP2, SUBGRP21, PDP211, PdpState.PASSIVE);

        // first and last are ok
        tracker.add(PDP111);
        tracker.add(PDP211);
        tracker.run();
        changeIt = getChanges(2, 2).iterator();

        verifyChange(changeIt.next(), GROUP1, SUBGRP11, PDP112, PdpState.ACTIVE);
        verifyChange(changeIt.next(), GROUP1, SUBGRP12, PDP121, PdpState.ACTIVE);

        // none are ok
        tracker.run();
        changeIt = getChanges(4, 4).iterator();

        verifyChange(changeIt.next(), GROUP1, SUBGRP11, PDP111, PdpState.ACTIVE);
        verifyChange(changeIt.next(), GROUP1, SUBGRP11, PDP112, PdpState.ACTIVE);
        verifyChange(changeIt.next(), GROUP1, SUBGRP12, PDP121, PdpState.ACTIVE);
        verifyChange(changeIt.next(), GROUP2, SUBGRP21, PDP211, PdpState.PASSIVE);
    }

    private void verifyChange(PdpStateChange change, String groupName, String pdpType, String pdpName, PdpState state) {
        assertEquals(pdpName, change.getName());
        assertEquals(state, change.getState());
        assertEquals(groupName, change.getPdpGroup());
        assertEquals(pdpType, change.getPdpSubgroup());
    }

    /**
     * Gets change requests that were issued.
     * @param nskip number of requests to be skipped
     * @param ndesired number of requests desired
     * @return the desired requests, sorted by PDP name
     */
    private List<PdpStateChange> getChanges(int nskip, int ndesired) {
        ArgumentCaptor<PdpStateChange> captor = ArgumentCaptor.forClass(PdpStateChange.class);
        verify(requestMap, times(nskip + ndesired)).addRequest(captor.capture());

        List<PdpStateChange> lst = new ArrayList<>(captor.getAllValues()).subList(nskip, nskip + ndesired);
        Collections.sort(lst, (left, right) -> left.getName().compareTo(right.getName()));

        return lst;
    }
}
