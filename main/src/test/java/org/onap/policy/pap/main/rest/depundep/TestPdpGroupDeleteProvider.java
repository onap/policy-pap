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

package org.onap.policy.pap.main.rest.depundep;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpGroupDeleteResponse;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.pap.main.PolicyPapRuntimeException;

public class TestPdpGroupDeleteProvider extends ProviderSuper {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String POLICY1_NAME = "policyA";
    private static final String POLICY1_VERSION = "1.2.3";
    private static final String GROUP1_NAME = "groupA";
    private static final String PDP1_1 = "pdpA_1";
    private static final String PDP1_2 = "pdpA_2";
    private static final String PDP3_1 = "pdpC_1";

    private MyProvider prov;
    private SessionData session;
    private ToscaPolicyIdentifierOptVersion optIdent;
    private ToscaPolicyIdentifier ident;
    private BiFunction<PdpGroup, PdpSubGroup, Boolean> updater;


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

        session = mock(SessionData.class);
        ident = policy1.getIdentifier();
        optIdent = new ToscaPolicyIdentifierOptVersion(ident.getName(), null);

        prov = new MyProvider();

        updater = prov.makeUpdater(policy1);
    }

    @Test
    public void testDeleteGroup_Active() throws Exception {
        PdpGroup group = loadGroup("deleteGroup.json");

        when(session.getGroup(GROUP1_NAME)).thenReturn(group);

        prov.deleteGroup(GROUP1_NAME);

        verify(session).deleteGroupFromDb(group);

        // should have an update for each PDP
        ArgumentCaptor<PdpUpdate> updateRequests = ArgumentCaptor.forClass(PdpUpdate.class);
        verify(session, times(3)).addRequests(updateRequests.capture(), any());
        List<PdpUpdate> updates = updateRequests.getAllValues();
        assertEquals(PDP1_1, updates.remove(0).getName());
        assertEquals(PDP1_2, updates.remove(0).getName());
        assertEquals(PDP3_1, updates.remove(0).getName());

        ArgumentCaptor<PdpStateChange> changeRequests = ArgumentCaptor.forClass(PdpStateChange.class);
        verify(session, times(3)).addRequests(any(), changeRequests.capture());
        List<PdpStateChange> changes = changeRequests.getAllValues();
        assertEquals(PDP1_1, changes.remove(0).getName());
        assertEquals(PDP1_2, changes.remove(0).getName());
        assertEquals(PDP3_1, changes.remove(0).getName());
    }

    @Test
    public void testDeleteGroup_NotFound() throws Exception {
        assertThatThrownBy(() -> prov.deleteGroup(GROUP1_NAME)).isInstanceOf(PolicyPapRuntimeException.class)
                        .hasMessage("group not found: groupA");
    }

    @Test
    public void testDeleteGroup_Inactive() throws Exception {
        PdpGroup group = loadGroup("deleteGroup.json");

        group.setPdpGroupState(PdpState.PASSIVE);

        when(session.getGroup(GROUP1_NAME)).thenReturn(group);

        prov.deleteGroup(GROUP1_NAME);

        verify(session).deleteGroupFromDb(group);

        // should done no requests for the PDPs
        verify(session, never()).addRequests(any(), any());
    }

    @Test
    public void testDeleteGroup_DaoEx() throws Exception {
        PdpGroup group = loadGroup("deleteGroup.json");

        when(session.getGroup(GROUP1_NAME)).thenReturn(group);

        PfModelException ex = new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);
        doThrow(ex).when(session).deleteGroupFromDb(group);

        assertThatThrownBy(() -> prov.deleteGroup(GROUP1_NAME)).isInstanceOf(PolicyPapRuntimeException.class)
                        .hasMessage(ProviderBase.DB_ERROR_MSG);
    }

    @Test
    public void testUndeploy_testDeletePolicy() {
        Pair<Status, PdpGroupDeleteResponse> pair = prov.undeploy(optIdent);
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());
    }

    @Test
    public void removePdps() throws Exception {
        PdpGroup group = loadGroup("deleteGroup.json");

        group.setPdpGroupState(PdpState.ACTIVE);

        when(session.getGroup(GROUP1_NAME)).thenReturn(group);

        prov.deleteGroup(GROUP1_NAME);

        // should have an update for each PDP
        ArgumentCaptor<PdpUpdate> updateRequests = ArgumentCaptor.forClass(PdpUpdate.class);
        verify(session, times(3)).addRequests(updateRequests.capture(), any());

        PdpUpdate update = updateRequests.getValue();
        assertEquals(PDP3_1, update.getName());
        assertNull(update.getPdpGroup());
        assertNull(update.getPdpSubgroup());

        ArgumentCaptor<PdpStateChange> changeRequests = ArgumentCaptor.forClass(PdpStateChange.class);
        verify(session, times(3)).addRequests(any(), changeRequests.capture());

        PdpStateChange change = changeRequests.getValue();
        assertEquals(PDP3_1, change.getName());
        assertEquals(PdpState.PASSIVE, change.getState());
    }

    /**
     * Tests using a real provider, just to verify end-to-end functionality.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testUndeploy_Full() throws Exception {
        when(dao.getFilteredPolicyList(any())).thenReturn(Arrays.asList(policy1));

        PdpGroup group = loadGroup("undeploy.json");

        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group));

        when(dao.getPolicyList(POLICY1_NAME, "1.2.300")).thenReturn(Arrays.asList(policy1));
        when(dao.getPolicyList("policyB", POLICY1_VERSION)).thenReturn(Arrays.asList(policy1));

        Pair<Status, PdpGroupDeleteResponse> pair = new PdpGroupDeleteProvider().undeploy(optIdent);
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        // should have updated the old group
        List<PdpGroup> updates = getGroupUpdates();
        assertEquals(1, updates.size());
        assertSame(group, updates.get(0));
        assertEquals(PdpState.ACTIVE, group.getPdpGroupState());

        // should be one less item in the new group
        assertEquals(2, group.getPdpSubgroups().get(0).getPolicies().size());

        // should have updated the PDPs
        List<PdpUpdate> requests = getUpdateRequests(1);
        assertEquals(1, requests.size());
        PdpUpdate req = requests.get(0);
        assertEquals("pdpA", req.getName());
        assertEquals(GROUP1_NAME, req.getPdpGroup());
        assertEquals("pdpTypeA", req.getPdpSubgroup());
        assertEquals(Arrays.asList(policy1, policy1), req.getPolicies());
    }

    @Test
    public void testDeletePolicy_DaoEx() throws Exception {
        PfModelException exc = new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);

        prov = spy(prov);
        doThrow(exc).when(prov).processPolicy(any(), any());

        assertThatThrownBy(() -> prov.undeploy(optIdent)).isInstanceOf(PolicyPapRuntimeException.class)
                        .hasMessage(PdpGroupDeleteProvider.DB_ERROR_MSG);
    }

    @Test
    public void testDeletePolicy_RtEx() throws Exception {
        RuntimeException exc = new RuntimeException(EXPECTED_EXCEPTION);

        prov = spy(prov);
        doThrow(exc).when(prov).processPolicy(any(), any());

        assertThatThrownBy(() -> prov.undeploy(optIdent)).isSameAs(exc);
    }

    @Test
    public void testMakeResponse() {
        PdpGroupDeleteResponse resp = prov.makeResponse(null);
        assertNull(resp.getErrorDetails());

        resp = prov.makeResponse(EXPECTED_EXCEPTION);
        assertEquals(EXPECTED_EXCEPTION, resp.getErrorDetails());
    }

    @Test
    public void testMakeUpdater() {
        /*
         * this group has one policy with a different name, one matching policy, and one
         * with a different version.
         */
        PdpGroup group = loadGroup("undeploy.json");

        PdpSubGroup subgroup = group.getPdpSubgroups().get(0);
        int origSize = subgroup.getPolicies().size();

        // invoke updater
        assertTrue(updater.apply(group, subgroup));

        // identified policy should have been removed
        assertEquals(origSize - 1, subgroup.getPolicies().size());
        assertFalse(subgroup.getPolicies().contains(ident));
    }

    @Test
    public void testMakeUpdater_NotFound() {
        /*
         * this group has one policy with a different name and one with a different
         * version, but not the policy of interest.
         */
        PdpGroup group = loadGroup("undeployMakeUpdaterGroupNotFound.json");

        PdpSubGroup subgroup = group.getPdpSubgroups().get(0);
        int origSize = subgroup.getPolicies().size();

        // invoke updater
        assertFalse(updater.apply(group, subgroup));

        // should be unchanged
        assertEquals(origSize, subgroup.getPolicies().size());
    }


    private class MyProvider extends PdpGroupDeleteProvider {

        @Override
        protected <T> Pair<Status, PdpGroupDeleteResponse> process(T request, BiConsumer<SessionData, T> processor) {
            processor.accept(session, request);

            return Pair.of(Status.OK, new PdpGroupDeleteResponse());
        }

        @Override
        protected void processPolicy(SessionData data, ToscaPolicyIdentifierOptVersion desiredPolicy)
                        throws PfModelException {
            // do nothing
        }
    }
}
