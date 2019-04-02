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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.concepts.PolicyIdentOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PolicyModelsProvider;
import org.onap.policy.pap.main.rest.PdpGroupDeployProvider.SessionData;

public class TestPdpGroupDeployProviderSessionData {
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

    private SessionData session;
    private PolicyModelsProvider dao;

    /**
     * Initializes mocks and a session.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        dao = mock(PolicyModelsProvider.class);
        session = new SessionData(dao);
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

        when(dao.getPolicies(POLICY_NAME, POLICY_VERSION)).thenReturn(policies);

        PolicyIdentOptVersion ident = new PolicyIdentOptVersion();
        ident.setName(POLICY_NAME);
        ident.setVersion(POLICY_VERSION);

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

        when(dao.getPolicyMaxVersion(POLICY_NAME)).thenReturn(policy1);

        assertSame(policy1, session.getPolicyMaxVersion(POLICY_NAME));
    }

    @Test
    public void testIsNewlyCreated_testCreatePdpGroup() throws Exception {
        assertFalse(session.isNewlyCreated(GROUP_NAME));

        PdpGroup group1 = makeGroup(GROUP_NAME, GROUP_VERSION);
        session.createPdpGroup(group1);
        verify(dao).createPdpGroup(group1);
        assertTrue(session.isNewlyCreated(GROUP_NAME));
        assertFalse(session.isNewlyCreated(GROUP_NAME2));

        PdpGroup group2 = makeGroup(GROUP_NAME2, GROUP_VERSION2);
        session.createPdpGroup(group2);
        verify(dao).createPdpGroup(group2);
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
    public void testGetPdpGroups() throws Exception {
        PdpGroup group = makeGroup(GROUP_NAME, GROUP_VERSION);
        when(dao.getPdpGroupMaxVersion(GROUP_NAME)).thenReturn(group);

        assertEquals(group, session.getPdpGroupMaxVersion(GROUP_NAME));
    }

    @Test
    public void testGetActivePdpGroupsByPolicy() throws Exception {
        List<PdpGroup> lst =
                        Arrays.asList(makeGroup(GROUP_NAME, GROUP_VERSION), makeGroup(GROUP_NAME2, GROUP_VERSION2));
        when(dao.getActivePdpGroupsByPolicy(POLICY_NAME, POLICY_VERSION)).thenReturn(lst);

        assertEquals(lst, session.getActivePdpGroupsByPolicy(POLICY_NAME, POLICY_VERSION));
    }

    @Test
    public void testUpdatePdpGroup() throws Exception {
        PdpGroup group = makeGroup(GROUP_NAME, GROUP_VERSION);

        session.updatePdpGroup(group);
        verify(dao).updatePdpGroup(group);
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
