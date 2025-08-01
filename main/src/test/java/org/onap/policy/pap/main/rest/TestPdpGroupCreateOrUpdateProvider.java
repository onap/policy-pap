/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021, 2023-2025 OpenInfra Foundation Europe. All rights reserved.
 * Modifications Copyright (C) 2021 AT&T Intellectual Property.
 * Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.PapConstants;

class TestPdpGroupCreateOrUpdateProvider extends ProviderSuper {
    private static final String EXPECTED_EXCEPTION = "expected exception";

    private static final String PDP2 = "pdpB";
    private static final String PDP4 = "pdpD";

    private PdpGroupCreateOrUpdateProvider prov;


    @AfterAll
    static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    /**
     * Configures mocks and objects.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        prov = new PdpGroupCreateOrUpdateProvider();
        super.initialize(prov);
        try {
            when(toscaService.getPolicyTypeList("typeA", "100.2.3"))
                .thenReturn(Collections.singletonList(loadPolicyType()));
        } catch (PfModelException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCreateOrUpdateGroups() throws Exception {
        prov.createOrUpdateGroups(loadPdpGroups("emptyGroups.json"));

        // no groups, so no action should have been taken
        assertNoGroupAction();
    }

    @Test
    void testCreateOrUpdateGroups_InvalidRequest() {
        assertThatThrownBy(() -> prov.createOrUpdateGroups(new PdpGroups())).isInstanceOf(PfModelException.class)
            .hasMessageContaining("is null");

        assertNoGroupAction();
    }

    @Test
    void testCreateOrUpdate_Invalid() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.TERMINATED);

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("pdpGroupState");

        assertNoGroupAction();
    }

    @Test
    void testAddGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup group = groups.getGroups().get(0);
        group.setPdpGroupState(PdpState.PASSIVE);

        prov.createOrUpdateGroups(groups);

        // should not have updated the state
        assertEquals(PdpState.PASSIVE, group.getPdpGroupState());

        assertSame(group, getGroupCreates().get(0));
    }

    @Test
    void testAddGroup_InvalidSubGroup() {
        PdpGroups groups = loadPdpGroups("createGroups.json");

        groups.getGroups().get(0).getPdpSubgroups().get(0).getSupportedPolicyTypes().get(0).setVersion("99.99.99");

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("unknown policy type");

        assertNoGroupAction();
    }

    @Test
    void testValidateGroupOnly_NullState() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(null);
        Assertions.assertThatCode(() -> prov.createOrUpdateGroups(groups)).doesNotThrowAnyException();
    }

    @Test
    void testValidateGroupOnly_Active() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.ACTIVE);
        Assertions.assertThatCode(() -> prov.createOrUpdateGroups(groups)).doesNotThrowAnyException();
    }

    @Test
    void testValidateGroupOnly_Passive() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.PASSIVE);
        Assertions.assertThatCode(() -> prov.createOrUpdateGroups(groups)).doesNotThrowAnyException();
    }

    @Test
    void testValidateGroupOnly_Invalid() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.TERMINATED);

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("pdpGroupState");
    }

    @Test
    void testUpdateGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");

        // DB group = new group
        PdpGroup group = new PdpGroup(groups.getGroups().get(0));
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        prov.createOrUpdateGroups(groups);

        assertNoGroupAction();
    }

    @Test
    void testUpdateGroup_PropertiesChanged() {
        PdpGroups groups = loadPdpGroups("createGroups.json");

        PdpGroup group = new PdpGroup(groups.getGroups().get(0));
        group.setProperties(new TreeMap<>());

        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("properties");

        assertNoGroupAction();
    }

    @Test
    void testUpdateGroup_NewDescription() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        group.setDescription("old description");
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        prov.createOrUpdateGroups(groups);

        assertGroupUpdateOnly(group);

        assertEquals("my description", group.getDescription());
        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    void testUpdateGroup_NewState() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        group.setPdpGroupState(PdpState.TEST);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        prov.createOrUpdateGroups(groups);

        assertGroupUpdateOnly(group);

        assertEquals(PdpState.ACTIVE, group.getPdpGroupState());
        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    void testUpdateGroup_NewSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        prov.createOrUpdateGroups(groups);

        PdpGroup newgrp = groups.getGroups().get(0);
        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    void testUpdateGroup_UpdatedSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        // something different in this subgroup
        group.getPdpSubgroups().get(0).setDesiredInstanceCount(10);

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    void testUpdateGroup_notifyPdpsDelSubGroups() throws Exception {
        PdpGroup dbgroup = new PdpGroup(loadPdpGroups("createGroupsDelSub.json").getGroups().get(0));
        when(pdpGroupService.getPdpGroups(dbgroup.getName())).thenReturn(List.of(dbgroup));

        PdpGroups groups = loadPdpGroups("createGroups.json");

        prov.createOrUpdateGroups(groups);

        // verify that DB group was updated
        List<PdpGroup> updates = getGroupUpdates();
        assertEquals(1, updates.size());
        dbgroup = updates.get(0);

        PdpGroup newgrp = groups.getGroups().get(0);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(dbgroup.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), dbgroup.toString());

        // no deployment notifications
        checkEmptyNotification();

        // this requires a PDP UPDATE message
        List<PdpUpdate> pdpUpdates = getUpdateRequests(2);
        assertEquals(2, pdpUpdates.size());

        PdpUpdate pdpUpdate = pdpUpdates.get(0);
        assertEquals(PapConstants.PAP_NAME, pdpUpdate.getSource());
        assertEquals(PDP2, pdpUpdate.getName());
        assertNull(pdpUpdate.getPdpGroup());

        pdpUpdate = pdpUpdates.get(1);
        assertEquals(PapConstants.PAP_NAME, pdpUpdate.getSource());
        assertEquals(PDP4, pdpUpdate.getName());
        assertNull(pdpUpdate.getPdpGroup());

        // it also requires a PDP STATE-CHANGE message
        List<PdpStateChange> changes = getStateChangeRequests(2);
        assertEquals(2, changes.size());

        PdpStateChange change = changes.get(0);
        assertEquals(PapConstants.PAP_NAME, change.getSource());
        assertEquals(PDP2, change.getName());
        assertEquals(PdpState.PASSIVE, change.getState());

        change = changes.get(1);
        assertEquals(PapConstants.PAP_NAME, change.getSource());
        assertEquals(PDP4, change.getName());
        assertEquals(PdpState.PASSIVE, change.getState());
    }

    @Test
    void testUpdateField_Unchanged() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        prov.createOrUpdateGroups(groups);

        assertNoGroupAction();
    }

    @Test
    void testUpdateField_WasNull() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        group.setDescription(null);

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    void testUpdateField_NowNull() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        newgrp.setDescription(null);

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    void testUpdateField_Changed() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        newgrp.setDescription(group.getDescription() + "-changed");

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    void testAddSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        prov.createOrUpdateGroups(groups);

        PdpGroup newgrp = groups.getGroups().get(0);

        PdpSubGroup newsub = newgrp.getPdpSubgroups().get(1);
        newsub.setCurrentInstanceCount(0);
        newsub.setPdpInstances(new ArrayList<>(0));

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    /**
     * Tests addSubgroup() when the new subgroup has a wild-card policy type.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testAddSubGroupWildCardPolicyType() throws Exception {
        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyListWildCard.json"));
        when(toscaService.getPolicyTypeList("some.*", "2.3.4")).thenReturn(Collections.emptyList());

        PdpGroups groups = loadPdpGroups("createGroupsWildCard.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        prov.createOrUpdateGroups(groups);

        PdpGroup newgrp = groups.getGroups().get(0);

        PdpSubGroup newsub = newgrp.getPdpSubgroups().get(1);
        newsub.setCurrentInstanceCount(0);
        newsub.setPdpInstances(new ArrayList<>(0));

        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    void testAddSubGroup_ValidationPolicyTypeNotFound() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        when(toscaService.getPolicyTypeList(any(), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).hasMessageContaining("unknown policy type");
    }

    @Test
    void testAddSubGroup_ValidationPolicyTypeDaoEx() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        PfModelException exc = new PfModelException(Status.CONFLICT, EXPECTED_EXCEPTION);
        when(toscaService.getPolicyTypeList(any(), any())).thenThrow(exc);

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isSameAs(exc);
    }

    @Test
    void testAddSubGroup_ValidateVersionPrefixMatch() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(dbgroup.getName())).thenReturn(List.of(dbgroup));

        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
            .thenReturn(loadPolicies("daoPolicyList.json")).thenReturn(loadPolicies("createGroupNewPolicy.json"));

        PdpGroups reqgroups = loadPdpGroups("createGroupsVersPrefix.json");

        prov.createOrUpdateGroups(reqgroups);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(dbgroup.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), dbgroup.toString());
    }

    @Test
    void testUpdateSubGroup_Invalid() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        // change properties
        newgrp.getPdpSubgroups().get(0).setProperties(new TreeMap<>());

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("properties");

        assertNoGroupAction();
    }

    @Test
    void testUpdateSubGroup_SupportedPolicies() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        newgrp.getPdpSubgroups().get(0).getSupportedPolicyTypes()
            .add(new ToscaConceptIdentifier("typeX.*", "9.8.7"));

        // the group is updated with a new supported policy type in subgroup
        assertEquals(2, newgrp.getPdpSubgroups().get(0).getSupportedPolicyTypes().size());
        prov.createOrUpdateGroups(groups);
        // PdpGroup update doesn't allow supported policy type modifications
        // during pdp group update, the ones in db is maintained
        assertEquals(1, newgrp.getPdpSubgroups().get(0).getSupportedPolicyTypes().size());
        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    void testUpdateSubGroup_DesiredCount() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        newgrp.getPdpSubgroups().get(0).setDesiredInstanceCount(20);

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    void testUpdateSubGroup_Unchanged() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        prov.createOrUpdateGroups(groups);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(group.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), group.toString());

        // no notifications
        checkEmptyNotification();

        // no group updates
        assertNoGroupAction();
    }

    @Test
    void testValidateSubGroup_PropertiesMismatch() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        newgrp.setProperties(new TreeMap<>());

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("properties");

        assertNoGroupAction();
    }

    private void assertNoGroupAction() {
        verify(pdpGroupService, never()).createPdpGroups(any());
        verify(pdpGroupService, never()).updatePdpGroups(any());
        verify(reqmap, never()).addRequest(any(), any());
    }

    private void assertGroupUpdateOnly(PdpGroup group) {
        verify(pdpGroupService, never()).createPdpGroups(any());
        verify(reqmap, never()).addRequest(any(), any());

        List<PdpGroup> updates = getGroupUpdates();
        assertEquals(Collections.singletonList(group), updates);
    }
}
