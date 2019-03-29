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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.concepts.PolicyIdentOptVersion;
import org.onap.policy.models.tosca.simple.concepts.ToscaPolicies;
import org.onap.policy.models.tosca.simple.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.simple.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.ToscaTopologyTemplate;
import org.onap.policy.pap.main.PolicyModelsProvider;
import org.onap.policy.pap.main.rest.PdpGroupDeployProvider.SessionData;

public class PdpGroupDeployProviderSessionDataTest {
    private static final String GROUP_NAME = "group";
    private static final String GROUP_VERSION_PREFIX = "9.8.";
    private static final String PDP1 = "pdp_1";
    private static final String PDP2 = "pdp_2";
    private static final String PDP3 = "pdp_3";
    private static final String POLICY_NAME = "myPolicy";
    private static final String POLICY_VERSION_PREFIX = "1.2.";
    private static final String POLICY_VERSION = POLICY_VERSION_PREFIX + "3";
    private static final PfConceptKey POLICY_KEY1 = new PfConceptKey(POLICY_NAME + "1", POLICY_VERSION_PREFIX + ".1");
    private static final PfConceptKey POLICY_KEY2 = new PfConceptKey(POLICY_NAME + "2", POLICY_VERSION_PREFIX + ".2");
    private static final PfConceptKey GROUP_KEY1 = new PfConceptKey(GROUP_NAME + "6", GROUP_VERSION_PREFIX + ".6");
    private static final PfConceptKey GROUP_KEY2 = new PfConceptKey(GROUP_NAME + "5", GROUP_VERSION_PREFIX + ".5");

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
        PolicyIdentOptVersion ident = new PolicyIdentOptVersion();
        ident.setName(POLICY_NAME);
        ident.setVersion(POLICY_VERSION);

        ToscaServiceTemplate template = mock(ToscaServiceTemplate.class);
        when(dao.getPolicies(any())).thenReturn(template);

        ToscaTopologyTemplate toptmpl = mock(ToscaTopologyTemplate.class);
        when(template.getTopologyTemplate()).thenReturn(toptmpl);

        ToscaPolicies tmplpol = mock(ToscaPolicies.class);
        when(toptmpl.getPolicies()).thenReturn(tmplpol);

        @SuppressWarnings("unchecked")
        Map<PfConceptKey, ToscaPolicy> concepts = mock(Map.class);
        when(tmplpol.getConceptMap()).thenReturn(concepts);

        ToscaPolicy policy1 = mock(ToscaPolicy.class);
        ToscaPolicy policy2 = mock(ToscaPolicy.class);
        when(concepts.values()).thenReturn(Arrays.asList(policy1, policy2));

        when(policy1.getKey()).thenReturn(POLICY_KEY1);
        when(policy2.getKey()).thenReturn(POLICY_KEY2);

        List<ToscaPolicy> lst = sort(session.getPolicies(ident), this::compare);
        assertEquals(Arrays.asList(policy1, policy2).toString(), lst.toString());
    }

    @Test
    public void testIsNewlyCreated_testCreatePdpGroup() throws Exception {
        assertFalse(session.isNewlyCreated(GROUP_KEY1));

        PdpGroup group1 = makeGroup(GROUP_KEY1);
        session.createPdpGroup(group1);
        verify(dao).createPdpGroup(group1);
        assertTrue(session.isNewlyCreated(GROUP_KEY1));
        assertFalse(session.isNewlyCreated(GROUP_KEY2));

        PdpGroup group2 = makeGroup(GROUP_KEY2);
        session.createPdpGroup(group2);
        verify(dao).createPdpGroup(group2);
        assertTrue(session.isNewlyCreated(GROUP_KEY1));
        assertTrue(session.isNewlyCreated(GROUP_KEY2));
    }

    private PdpGroup makeGroup(PfConceptKey groupKey) {
        PdpGroup group = new PdpGroup();

        group.setKey(groupKey);

        return group;
    }

    @Test
    public void testGetPdpGroups() throws Exception {
        List<PdpGroup> lst = Arrays.asList(makeGroup(GROUP_KEY1), makeGroup(GROUP_KEY2));
        when(dao.getPdpGroups(GROUP_KEY1)).thenReturn(lst);

        assertEquals(lst, session.getPdpGroups(GROUP_KEY1));
    }

    @Test
    public void testGetActivePdpGroupsByPolicy() throws Exception {
        List<PdpGroup> lst = Arrays.asList(makeGroup(GROUP_KEY1), makeGroup(GROUP_KEY2));
        when(dao.getActivePdpGroupsByPolicy(POLICY_KEY1)).thenReturn(lst);

        assertEquals(lst, session.getActivePdpGroupsByPolicy(POLICY_KEY1));
    }

    @Test
    public void testUpdatePdpGroup() throws Exception {
        PdpGroup group = makeGroup(GROUP_KEY1);

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
        int result = left.getKey().getName().compareTo(right.getKey().getName());
        return (result != 0 ? result : left.getKey().compareTo(right.getKey()));
    }
}
