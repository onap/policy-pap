/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020-2023 Nordix Foundation.
 * Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.onap.policy.pap.main.rest.ProviderBase.Updater;

public class TestPdpGroupDeleteProvider extends ProviderSuper {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String GROUP1_NAME = "groupA";

    @Mock
    private SessionData session;

    @Captor
    private ArgumentCaptor<Set<String>> pdpCaptor;

    private MyProvider prov;
    private ToscaConceptIdentifierOptVersion optIdent;
    private ToscaConceptIdentifierOptVersion fullIdent;
    private ToscaConceptIdentifier ident;
    private Updater updater;

    @AfterAll
    public static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    /**
     * Configures mocks and objects.
     *
     * @throws Exception if an error occurs
     */
    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        prov = new MyProvider();
        super.initialize(prov);

        ident = policy1.getIdentifier();
        optIdent = new ToscaConceptIdentifierOptVersion(ident.getName(), null);
        fullIdent = new ToscaConceptIdentifierOptVersion(ident.getName(), ident.getVersion());

        updater = prov.makeUpdater(session, policy1, fullIdent);
    }

    @Test
    public void testDeleteGroup_Active() throws Exception {
        PdpGroup group = loadGroup("deleteGroup.json");

        group.setPdpGroupState(PdpState.ACTIVE);

        when(session.getGroup(GROUP1_NAME)).thenReturn(group);

        assertThatThrownBy(() -> prov.deleteGroup(GROUP1_NAME)).isInstanceOf(PfModelException.class)
            .hasMessage("group is still ACTIVE");
    }

    @Test
    public void testDeleteGroup_NotFound() {
        assertThatThrownBy(() -> prov.deleteGroup(GROUP1_NAME)).isInstanceOf(PfModelException.class)
            .hasMessage("group not found")
            .extracting(ex -> ((PfModelException) ex).getErrorResponse().getResponseCode())
            .isEqualTo(Status.NOT_FOUND);
    }

    @Test
    public void testDeleteGroup_Inactive() throws Exception {
        PdpGroup group = loadGroup("deleteGroup.json");

        when(session.getGroup(GROUP1_NAME)).thenReturn(group);

        prov.deleteGroup(GROUP1_NAME);

        verify(session).deleteGroupFromDb(group);

        // should have done no requests for the PDPs
        verify(session, never()).addRequests(any(), any());
    }

    @Test
    public void testDeleteGroup_Ex() throws Exception {
        PdpGroup group = loadGroup("deleteGroup.json");

        when(session.getGroup(GROUP1_NAME)).thenReturn(group);

        PfModelRuntimeException ex = new PfModelRuntimeException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);
        doThrow(ex).when(session).deleteGroupFromDb(group);

        assertThatThrownBy(() -> prov.deleteGroup(GROUP1_NAME)).isSameAs(ex);
    }

    /**
     * Tests using a real provider, just to verify end-to-end functionality.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testUndeploy_Full() throws Exception {
        when(toscaService.getFilteredPolicyList(any())).thenReturn(List.of(policy1));

        PdpGroup group = loadGroup("undeploy.json");

        when(pdpGroupService.getFilteredPdpGroups(any())).thenReturn(List.of(group));
        when(toscaService.getFilteredPolicyList(any())).thenReturn(List.of(policy1));

        PdpGroupDeleteProvider deleteProvider = new PdpGroupDeleteProvider();
        super.initialize(deleteProvider);
        deleteProvider.undeploy(fullIdent, DEFAULT_USER);

        // should have updated the old group
        List<PdpGroup> updates = getGroupUpdates();
        assertEquals(1, updates.size());
        assertSame(group, updates.get(0));
        assertEquals(PdpState.ACTIVE, group.getPdpGroupState());

        // should be one less item in the new subgroup
        assertEquals(2, group.getPdpSubgroups().get(0).getPolicies().size());

        // should have updated the PDPs
        List<PdpUpdate> requests = getUpdateRequests(1);
        assertEquals(1, requests.size());
        PdpUpdate req = requests.get(0);
        assertEquals("pdpA", req.getName());
        assertEquals(GROUP1_NAME, req.getPdpGroup());
        assertEquals("pdpTypeA", req.getPdpSubgroup());
        assertEquals(List.of(policy1.getIdentifier()), req.getPoliciesToBeUndeployed());
    }

    @Test
    public void testUndeployPolicy_NotFound() {
        when(session.isUnchanged()).thenReturn(true);

        assertThatThrownBy(() -> prov.undeploy(optIdent, DEFAULT_USER)).isInstanceOf(PfModelException.class)
            .hasMessage("policy does not appear in any PDP group: policyA null");
    }

    @Test
    public void testUndeployPolicy_DaoEx() throws Exception {
        PfModelException exc = new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);

        prov = spy(prov);
        doThrow(exc).when(prov).processPolicy(any(), any());

        assertThatThrownBy(() -> prov.undeploy(optIdent, null)).isSameAs(exc);
    }

    @Test
    public void testUndeployPolicy_RtEx() throws Exception {
        RuntimeException exc = new RuntimeException(EXPECTED_EXCEPTION);

        prov = spy(prov);
        doThrow(exc).when(prov).processPolicy(any(), any());

        // process method catches RuntimeException and re-throws as PfModelException
        assertThatThrownBy(() -> prov.undeploy(fullIdent, null)).isInstanceOf(PfModelException.class)
            .hasRootCauseMessage(EXPECTED_EXCEPTION);
    }

    @Test
    public void testMakeUpdater_WithVersion() throws PfModelException {
        /*
         * this group has two matching policies and one policy with a different name.
         */
        PdpGroup group = loadGroup("undeploy.json");

        PdpSubGroup subgroup = group.getPdpSubgroups().get(0);
        int origSize = subgroup.getPolicies().size();

        // invoke updater - matching both name and version
        assertTrue(updater.apply(group, subgroup));

        // identified policy should have been removed
        assertEquals(origSize - 1, subgroup.getPolicies().size());
        assertFalse(subgroup.getPolicies().contains(ident));

        verify(session).trackUndeploy(eq(ident), pdpCaptor.capture(), eq(group.getName()), eq(subgroup.getPdpType()));
        assertEquals("[pdpA]", pdpCaptor.getValue().toString());
    }

    @Test
    public void testMakeUpdater_NullVersion() throws PfModelException {
        /*
         * this group has two matching policies and one policy with a different name.
         */
        PdpGroup group = loadGroup("undeploy.json");

        PdpSubGroup subgroup = group.getPdpSubgroups().get(0);
        int origSize = subgroup.getPolicies().size();

        // invoke updater - matching the name, but with a null (i.e., wild-card) version
        updater = prov.makeUpdater(session, policy1, optIdent);
        assertTrue(updater.apply(group, subgroup));

        // identified policy should have been removed
        assertEquals(origSize - 2, subgroup.getPolicies().size());
        assertFalse(subgroup.getPolicies().contains(ident));
    }

    @Test
    public void testMakeUpdater_NotFound() throws PfModelException {
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

        private MyProvider() {
            super.initialize();
        }

        @Override
        protected <T> void process(T request, BiConsumerWithEx<SessionData, T> processor) throws PfModelException {
            processor.accept(session, request);
        }

        @Override
        protected void processPolicy(SessionData data, ToscaConceptIdentifierOptVersion desiredPolicy)
            throws PfModelException {
            // do nothing
        }
    }
}
