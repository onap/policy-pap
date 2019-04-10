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

package org.onap.policy.pap.main.rest.depundep;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.ws.rs.core.Response.Status;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

public class TestSessionData extends ProviderSuper {
    private static final String GROUP_NAME = "groupA";
    private static final String GROUP_NAME2 = "groupB";
    private static final String PDP1 = "pdp_1";
    private static final String PDP2 = "pdp_2";
    private static final String PDP3 = "pdp_3";
    private static final String POLICY_VERSION_PREFIX = "1.2.";
    private static final String POLICY_NAME = "myPolicy";
    private static final String POLICY_VERSION = POLICY_VERSION_PREFIX + "3";
    private static final String POLICY_VERSION2 = POLICY_VERSION_PREFIX + "4";
    private static final String POLICY_TYPE = "myType";
    private static final String POLICY_TYPE_VERSION = "10.20.30";

    private SessionData session;
    private ToscaPolicyIdentifier ident;
    private ToscaPolicyTypeIdentifier type;
    private ToscaPolicyTypeIdentifier type2;
    private PdpGroup group1;
    private PdpGroup group2;

    /**
     * Initializes mocks and a session.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        ident = new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION);
        type = new ToscaPolicyTypeIdentifier(POLICY_TYPE, POLICY_TYPE_VERSION);
        type2 = new ToscaPolicyTypeIdentifier(POLICY_TYPE, POLICY_TYPE_VERSION + "0");
        group1 = makeGroup(GROUP_NAME);
        group2 = makeGroup(GROUP_NAME2);

        session = new SessionData(dao);
    }

    @Test
    public void testGetPolicy() throws Exception {
        ToscaPolicy policy1 = makePolicy(POLICY_NAME, POLICY_VERSION);
        when(dao.getPolicyList(POLICY_NAME, POLICY_VERSION)).thenReturn(Arrays.asList(policy1));

        ToscaPolicy policy2 = makePolicy(POLICY_NAME, POLICY_VERSION2);
        when(dao.getPolicyList(POLICY_NAME, POLICY_VERSION2)).thenReturn(Arrays.asList(policy2));

        ToscaPolicyIdentifier ident2 = new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION2);

        assertSame(policy1, session.getPolicy(ident));
        assertSame(policy2, session.getPolicy(ident2));

        // repeat
        assertSame(policy1, session.getPolicy(ident));
        assertSame(policy2, session.getPolicy(ident2));

        assertSame(policy1, session.getPolicy(ident));
        assertSame(policy2, session.getPolicy(ident2));

        // should have only invoked this once for each policy
        verify(dao, times(2)).getPolicyList(any(), any());
    }

    @Test
    public void testGetPolicy_NotFound() throws Exception {
        when(dao.getPolicyList(any(), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> session.getPolicy(ident)).hasMessage("cannot find policy: myPolicy 1.2.3");
    }

    @Test
    public void testGetPolicy_TooMany() throws Exception {
        ToscaPolicy policy = new ToscaPolicy();
        when(dao.getPolicyList(any(), any())).thenReturn(Arrays.asList(policy, policy));

        assertThatThrownBy(() -> session.getPolicy(ident)).hasMessage("too many policies match: myPolicy 1.2.3");
    }

    @Test
    public void testGetPolicy_DaoEx() throws Exception {
        PfModelException ex = new PfModelException(Status.INTERNAL_SERVER_ERROR, "expected exception");
        when(dao.getPolicyList(any(), any())).thenThrow(ex);

        assertThatThrownBy(() -> session.getPolicy(ident)).hasMessage("cannot get policy: myPolicy 1.2.3").hasCause(ex);
    }

    @Test
    public void testAddUpdate_testGetPdpUpdates() {
        // several different updates, but one duplicate
        PdpUpdate update1 = makeUpdate(PDP1);
        session.addUpdate(update1);

        PdpUpdate update2 = makeUpdate(PDP2);
        session.addUpdate(update2);

        PdpUpdate update3 = makeUpdate(PDP3);
        session.addUpdate(update3);

        List<PdpUpdate> lst = sort(session.getPdpUpdates(), this::compare);
        assertEquals(Arrays.asList(update1, update2, update3).toString(), lst.toString());

        // overwrite one
        update2 = makeUpdate(PDP2);
        session.addUpdate(update2);

        lst = sort(session.getPdpUpdates(), this::compare);
        assertEquals(Arrays.asList(update1, update2, update3).toString(), lst.toString());
    }

    private ToscaPolicy makePolicy(String name, String version) {
        ToscaPolicy policy = new ToscaPolicy();

        policy.setName(name);
        policy.setVersion(version);

        return policy;
    }

    @Test
    public void testGetPolicyMaxVersion() throws Exception {
        ToscaPolicy policy1 = makePolicy(POLICY_NAME, POLICY_VERSION);

        when(dao.getFilteredPolicyList(any())).thenReturn(Arrays.asList(policy1));

        assertSame(policy1, session.getPolicyMaxVersion(POLICY_NAME));
        assertSame(policy1, session.getPolicyMaxVersion(POLICY_NAME));
        assertSame(policy1, session.getPolicyMaxVersion(POLICY_NAME));

        // should have only invoked DAO once; used cache for other requests
        verify(dao, times(1)).getFilteredPolicyList(any());
    }

    @Test
    public void testGetPolicyMaxVersion_NotFound() throws Exception {
        when(dao.getFilteredPolicyList(any())).thenReturn(Collections.emptyList());
        assertThatThrownBy(() -> session.getPolicyMaxVersion(POLICY_NAME)).hasMessage("cannot find policy: myPolicy");
    }

    private PdpGroup makeGroup(String name) {
        PdpGroup group = new PdpGroup();

        group.setName(name);

        return group;
    }

    @Test
    public void testUpdate() throws Exception {
        // force the groups into the cache
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1, group2));
        session.getActivePdpGroupsByPolicyType(type);

        /*
         * try group 1
         */
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1));
        PdpGroup newgrp = new PdpGroup(group1);
        session.update(newgrp);

        // repeat
        newgrp = new PdpGroup(group1);
        session.update(newgrp);

        /*
         * try group 2
         */
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group2));
        newgrp = new PdpGroup(group2);
        session.update(newgrp);

        // repeat
        newgrp = new PdpGroup(group2);
        session.update(newgrp);
    }

    @Test
    public void testUpdate_NotInCache() throws Exception {
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1));

        assertThatIllegalStateException().isThrownBy(() -> session.update(new PdpGroup(group1)))
                        .withMessage("group not cached: groupA");
    }

    @Test
    public void testGetActivePdpGroupsByPolicyType() throws Exception {
        List<PdpGroup> groups = Arrays.asList(group1, group2);
        when(dao.getFilteredPdpGroups(any())).thenReturn(groups);

        // repeat
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type));
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type));
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type));

        // only invoked once - should have used the cache for the rest
        verify(dao, times(1)).getFilteredPdpGroups(any());
    }

    @Test
    public void testAddGroup() throws Exception {
        List<PdpGroup> groups = Arrays.asList(group1, group2);
        when(dao.getFilteredPdpGroups(any())).thenReturn(groups);

        // query by each type
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type));
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type2));

        // invoked once for each type
        verify(dao, times(2)).getFilteredPdpGroups(any());

        // repeat - should be no more invocations
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type));
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type2));
        verify(dao, times(2)).getFilteredPdpGroups(any());
    }

    @Test
    public void testUpdateDb() throws Exception {
        // force the groups into the cache
        PdpGroup group3 = makeGroup("groupC");
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1, group2, group3));
        session.getActivePdpGroupsByPolicyType(type);

        // update group 1
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1));
        PdpGroup newgrp1 = new PdpGroup(group1);
        session.update(newgrp1);

        // another update
        newgrp1 = new PdpGroup(newgrp1);
        session.update(newgrp1);

        // update group 3
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group3));
        PdpGroup newgrp3 = new PdpGroup(group3);
        session.update(newgrp3);

        // push the changes to the DB
        session.updateDb();

        // expect one update for groups 1 & 3
        List<PdpGroup> changes = getGroupUpdates();
        assertSame(newgrp1, changes.get(0));
        assertSame(newgrp3, changes.get(1));
    }

    @Test
    public void testUpdateDb_Empty() throws Exception {
        // force data into the cache
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1, group2));
        session.getActivePdpGroupsByPolicyType(type);

        session.updateDb();
        verify(dao, never()).createPdpGroups(any());
        verify(dao, never()).updatePdpGroups(any());
    }

    private PdpUpdate makeUpdate(String pdpName) {
        PdpUpdate update = new PdpUpdate();

        update.setName(pdpName);

        return update;
    }

    private <T> List<T> sort(Collection<T> collection, Comparator<T> comparator) {
        List<T> lst = new ArrayList<>(collection);
        Collections.sort(lst, comparator);

        return lst;
    }

    private int compare(PdpUpdate left, PdpUpdate right) {
        return left.getName().compareTo(right.getName());
    }
}
