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

package org.onap.policy.pap.main.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;

public class TestPolicyUndeployerImpl extends ProviderSuper {
    private static final String MY_GROUP = "my-group";
    private static final String MY_SUBGROUP = "my-subgroup";
    private static final String MY_SUBGROUP0 = "my-subgroup-0";
    private static final String PDP1 = "my-pdp-a";

    @Mock
    private SessionData session;

    @Captor
    private ArgumentCaptor<Set<String>> pdpCaptor;

    private ToscaPolicyIdentifier ident1;
    private ToscaPolicyIdentifier ident2;
    private ToscaPolicyIdentifier ident3;
    private ToscaPolicyIdentifier ident4;
    private PdpGroup group;
    private PdpSubGroup subgroup;
    private MyProvider prov;


    @AfterClass
    public static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    /**
     * Configures mocks and objects.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {

        super.setUp();

        ident1 = new ToscaPolicyIdentifier("ident-a", "2.3.1");
        ident2 = new ToscaPolicyIdentifier("ident-b", "2.3.2");
        ident3 = new ToscaPolicyIdentifier("ident-c", "2.3.3");
        ident4 = new ToscaPolicyIdentifier("ident-d", "2.3.4");

        group = new PdpGroup();
        group.setName(MY_GROUP);

        subgroup = new PdpSubGroup();
        subgroup.setPdpType(MY_SUBGROUP);

        Pdp pdp1 = new Pdp();
        pdp1.setInstanceId(PDP1);

        subgroup.setPdpInstances(Arrays.asList(pdp1));

        // this subgroup should never be touched
        PdpSubGroup subgroup0 = new PdpSubGroup();
        subgroup0.setPdpType(MY_SUBGROUP0);
        subgroup0.setPolicies(Collections.unmodifiableList(Arrays.asList(ident1, ident2, ident3, ident4)));
        subgroup.setPdpInstances(Arrays.asList(pdp1));

        group.setPdpSubgroups(Arrays.asList(subgroup0, subgroup));

        when(session.getGroup(MY_GROUP)).thenReturn(group);
        when(session.getPolicy(any())).thenReturn(policy1);

        prov = new MyProvider();
    }

    @Test
    public void testUndeployPolicies() throws PfModelException {
        subgroup.setPolicies(new LinkedList<>(Arrays.asList(ident1, ident2, ident3, ident4)));

        prov.undeploy(MY_GROUP, MY_SUBGROUP, Arrays.asList(ident1, ident2));

        // group should have been updated
        verify(session).update(group);

        // subgroup should only have remaining policies
        assertEquals(Arrays.asList(ident3, ident4).toString(), subgroup.getPolicies().toString());

        // should have generated PDP-UPDATE for the PDP
        verify(session).addUpdate(any());
    }

    /**
     * Tests undeployPolicies() when the policies do not exist in the subgroup.
     */
    @Test
    public void testUndeployPoliciesUnchanged() throws PfModelException {
        List<ToscaPolicyIdentifier> origlist = Arrays.asList(ident3, ident4);
        subgroup.setPolicies(new LinkedList<>(origlist));

        prov.undeploy(MY_GROUP, MY_SUBGROUP, Arrays.asList(ident1, ident2));

        // group NOT should have been updated
        verify(session, never()).update(group);

        // subgroup's policies should be unchanged
        assertEquals(origlist.toString(), subgroup.getPolicies().toString());

        // should NOT have generated PDP-UPDATE for the PDP
        verify(session, never()).addUpdate(any());
    }

    /**
     * Tests undeployPolicies() when the group is not found.
     */
    @Test
    public void testUndeployPoliciesGroupNotFound() throws PfModelException {
        // force exception to be thrown if the list is changed
        subgroup.setPolicies(Collections.unmodifiableList(Arrays.asList(ident1, ident2, ident3, ident4)));

        when(session.getGroup(any())).thenReturn(null);

        prov.undeploy(MY_GROUP, MY_SUBGROUP, Arrays.asList(ident1, ident2));

        // group should have been updated
        verify(session, never()).update(group);

        // should have generated PDP-UPDATE for the PDP
        verify(session, never()).addUpdate(any());
    }

    /**
     * Tests undeployPolicies() when the subgroup is not found.
     */
    @Test
    public void testUndeployPoliciesSubGroupNotFound() throws PfModelException {
        // force exception to be thrown if the list is changed
        subgroup.setPolicies(Collections.unmodifiableList(Arrays.asList(ident1, ident2, ident3, ident4)));

        subgroup.setPdpType(MY_SUBGROUP + "X");

        prov.undeploy(MY_GROUP, MY_SUBGROUP, Arrays.asList(ident1, ident2));

        // group should have been updated
        verify(session, never()).update(group);

        // should have generated PDP-UPDATE for the PDP
        verify(session, never()).addUpdate(any());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMakeUpdater() {
        prov.makeUpdater(null, null, null);
    }


    private class MyProvider extends PolicyUndeployerImpl {

        @Override
        protected <T> void process(T request, BiConsumerWithEx<SessionData, T> processor) throws PfModelException {
            processor.accept(session, request);
        }
    }
}
