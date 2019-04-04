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

package org.onap.policy.pap.main.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;

public class TestPdpUpdateSessionData extends PdpGroupDeployProviderBase {
    private static final String GROUP_VERSION_PREFIX = "9.8.";
    private static final String GROUP_NAME = "group";
    private static final String GROUP_VERSION = GROUP_VERSION_PREFIX + "7";
    private static final String GROUP_NAME2 = "group2";
    private static final String GROUP_VERSION2 = GROUP_VERSION_PREFIX + "6";
    private static final String PDP1 = "pdp_1";
    private static final String PDP2 = "pdp_2";
    private static final String PDP3 = "pdp_3";
    private static final String POLICY_VERSION_PREFIX = "1.2.";
    private static final String POLICY_NAME = "myPolicy";
    private static final String POLICY_VERSION = POLICY_VERSION_PREFIX + "3";
    private static final String POLICY_NAME2 = "myPolicy2";
    private static final String POLICY_VERSION2 = POLICY_VERSION_PREFIX + "4";

    private PdpUpdateSessionData session;
    private ToscaPolicyIdentifier ident;

    /**
     * Initializes mocks and a session.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        ident = new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION);

        session = new PdpUpdateSessionData(dao);
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

        assertThatThrownBy(() -> session.getPolicy(ident)).hasMessage("cannot find policy: " + ident);
    }

    @Test
    public void testGetPolicy_TooMany() throws Exception {
        ToscaPolicy policy = new ToscaPolicy();
        when(dao.getPolicyList(any(), any())).thenReturn(Arrays.asList(policy, policy));

        assertThatThrownBy(() -> session.getPolicy(ident)).hasMessage("too many policies match: " + ident);
    }

    @Test
    public void testGetPolicy_DaoEx() throws Exception {
        PfModelException ex = new PfModelException(Status.INTERNAL_SERVER_ERROR, "expected exception");
        when(dao.getPolicyList(any(), any())).thenThrow(ex);

        assertThatThrownBy(() -> session.getPolicy(ident)).hasMessage("cannot get policy: " + ident).hasCause(ex);
    }

    @Test
    public void testAddUpdate() {
        // several different updates, but one duplicate
        PdpUpdate update1 = makeUpdate(PDP1);
        session.addUpdate(update1);

        PdpUpdate update2 = makeUpdate(PDP2);
        session.addUpdate(update2);

        PdpUpdate update3 = makeUpdate(PDP3);
        session.addUpdate(update3);

        List<PdpUpdate> lst = sort(session.getUpdates(), this::compare);
        assertEquals(Arrays.asList(update1, update2, update3).toString(), lst.toString());

        // overwrite one
        update2 = makeUpdate(PDP2);
        session.addUpdate(update2);

        lst = sort(session.getUpdates(), this::compare);
        assertEquals(Arrays.asList(update1, update2, update3).toString(), lst.toString());
    }

    @Test
    public void testGetPolicies() throws Exception {
        List<ToscaPolicy> policies = Arrays.asList(makePolicy(POLICY_NAME, POLICY_VERSION),
                        makePolicy(POLICY_NAME2, POLICY_VERSION2));

        when(dao.getPolicyList(POLICY_NAME, POLICY_VERSION)).thenReturn(policies);

        ToscaPolicyIdentifierOptVersion ident = new ToscaPolicyIdentifierOptVersion(POLICY_NAME, POLICY_VERSION);

        List<ToscaPolicy> lst = session.getPolicies(ident);
        assertEquals(policies.toString(), sort(lst, this::compare).toString());
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

        // try empty list
        when(dao.getFilteredPolicyList(any())).thenReturn(Collections.emptyList());
        assertNull(session.getPolicyMaxVersion(POLICY_NAME));
    }

    @Test
    public void testIsNewlyCreated_testCreatePdpGroup() throws Exception {
        assertFalse(session.isNewlyCreated(GROUP_NAME));

        PdpGroup group1 = makeGroup(GROUP_NAME, GROUP_VERSION);
        when(dao.createPdpGroups(any())).thenReturn(Arrays.asList(group1));

        session.createPdpGroup(group1);

        assertTrue(session.isNewlyCreated(GROUP_NAME));
        assertFalse(session.isNewlyCreated(GROUP_NAME2));

        PdpGroup group2 = makeGroup(GROUP_NAME2, GROUP_VERSION2);
        when(dao.createPdpGroups(any())).thenReturn(Arrays.asList(group2));
        session.createPdpGroup(group2);

        List<List<PdpGroup>> creates = getGroupCreates(2);
        assertEquals(group1, creates.get(0).get(0));
        assertEquals(group2, creates.get(1).get(0));

        assertTrue(session.isNewlyCreated(GROUP_NAME));
        assertTrue(session.isNewlyCreated(GROUP_NAME2));
    }

    private PdpGroup makeGroup(String name, String version) {
        PdpGroup group = new PdpGroup();

        group.setName(name);
        group.setVersion(version);

        return group;
    }

    @Test
    public void testGetPdpGroupMaxVersion() throws Exception {
        PdpGroup group = makeGroup(GROUP_NAME, GROUP_VERSION);
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group));

        assertEquals(group, session.getPdpGroupMaxVersion(GROUP_NAME));

        // try empty list
        when(dao.getFilteredPdpGroups(any())).thenReturn(Collections.emptyList());
        assertNull(session.getPdpGroupMaxVersion(GROUP_NAME));
    }

    @Test
    public void testGetActivePdpGroupsByPolicy() throws Exception {
        List<PdpGroup> groups =
                        Arrays.asList(makeGroup(GROUP_NAME, GROUP_VERSION), makeGroup(GROUP_NAME2, GROUP_VERSION2));
        when(dao.getFilteredPdpGroups(any())).thenReturn(groups);

        assertEquals(groups, session.getActivePdpGroupsByPolicy(POLICY_NAME, POLICY_VERSION));
    }

    @Test
    public void testUpdatePdpGroup() throws Exception {
        PdpGroup group = makeGroup(GROUP_NAME, GROUP_VERSION);
        when(dao.updatePdpGroups(any())).thenReturn(Arrays.asList(group));

        session.updatePdpGroup(group);

        List<PdpGroup> updates = getGroupUpdates(1).get(0);
        assertEquals(group, updates.get(0));
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

    private int compare(ToscaPolicy left, ToscaPolicy right) {
        int result = left.getName().compareTo(right.getName());
        if (result != 0) {
            return result;
        }

        return left.getVersion().compareTo(right.getVersion());
    }
}
