/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 Nordix Foundation.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import javax.ws.rs.core.Response.Status;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

public class TestPdpGroupCreateOrUpdateProvider extends ProviderSuper {
    private static final String EXPECTED_EXCEPTION = "expected exception";

    private static final String PDP2 = "pdpB";
    private static final String PDP4 = "pdpD";

    private PdpGroupCreateOrUpdateProvider prov;

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
    @Override
    public void setUp() throws Exception {

        super.setUp();

        when(dao.getPolicyTypeList("typeA", "100.2.3")).thenReturn(Arrays.asList(loadPolicyType("daoPolicyType.json")));

        prov = new PdpGroupCreateOrUpdateProvider();
    }

    @Test
    public void testCreateOrUpdateGroups() throws Exception {
        prov.createOrUpdateGroups(loadPdpGroups("emptyGroups.json"));

        // no groups, so no action should have been taken
        assertNoGroupAction();
    }

    @Test
    public void testCreateOrUpdateGroups_InvalidRequest() throws Exception {
        assertThatThrownBy(() -> prov.createOrUpdateGroups(new PdpGroups())).isInstanceOf(PfModelException.class)
            .hasMessageContaining("is null");

        assertNoGroupAction();
    }

    @Test
    public void testCreateOrUpdate_Invalid() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.TERMINATED);

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("pdpGroupState");

        assertNoGroupAction();
    }

    @Test
    public void testAddGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup group = groups.getGroups().get(0);
        group.setPdpGroupState(PdpState.PASSIVE);

        prov.createOrUpdateGroups(groups);

        // should not have updated the state
        assertEquals(PdpState.PASSIVE, group.getPdpGroupState());

        assertSame(group, getGroupCreates().get(0));
    }

    @Test
    public void testAddGroup_Invalid() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.TERMINATED);

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("pdpGroupState");

        assertNoGroupAction();
    }

    @Test
    public void testAddGroup_InvalidSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");

        groups.getGroups().get(0).getPdpSubgroups().get(0).getSupportedPolicyTypes().get(0).setVersion("99.99.99");

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("unknown policy type");

        assertNoGroupAction();
    }

    @Test
    public void testValidateGroupOnly_NullState() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(null);
        Assertions.assertThatCode(() -> prov.createOrUpdateGroups(groups)).doesNotThrowAnyException();
    }

    @Test
    public void testValidateGroupOnly_Active() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.ACTIVE);
        Assertions.assertThatCode(() -> prov.createOrUpdateGroups(groups)).doesNotThrowAnyException();
    }

    @Test
    public void testValidateGroupOnly_Passive() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.PASSIVE);
        Assertions.assertThatCode(() -> prov.createOrUpdateGroups(groups)).doesNotThrowAnyException();
    }

    @Test
    public void testValidateGroupOnly_Invalid() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.TERMINATED);

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("pdpGroupState");
    }

    @Test
    public void testUpdateGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");

        // DB group = new group
        PdpGroup group = new PdpGroup(groups.getGroups().get(0));
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.createOrUpdateGroups(groups);

        assertNoGroupAction();
    }

    @Test
    public void testUpdateGroup_PropertiesChanged() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");

        PdpGroup group = new PdpGroup(groups.getGroups().get(0));
        group.setProperties(new TreeMap<>());

        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("properties");

        assertNoGroupAction();
    }

    @Test
    public void testUpdateGroup_NewDescription() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        group.setDescription("old description");
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.createOrUpdateGroups(groups);

        assertGroupUpdateOnly(group);

        assertEquals("my description", group.getDescription());
        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    public void testUpdateGroup_NewState() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        group.setPdpGroupState(PdpState.TEST);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.createOrUpdateGroups(groups);

        assertGroupUpdateOnly(group);

        assertEquals(PdpState.ACTIVE, group.getPdpGroupState());
        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    public void testUpdateGroup_NewSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.createOrUpdateGroups(groups);

        PdpGroup newgrp = groups.getGroups().get(0);
        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    public void testUpdateGroup_UpdatedSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        // something different in this subgroup
        group.getPdpSubgroups().get(0).setDesiredInstanceCount(10);

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    public void testUpdateGroup_notifyPdpsDelSubGroups() throws Exception {
        PdpGroup dbgroup = new PdpGroup(loadPdpGroups("createGroupsDelSub.json").getGroups().get(0));
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

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
        verify(notifier, never()).addDeploymentData(any());

        // this requires a PDP UPDATE message
        List<PdpUpdate> pdpUpdates = getUpdateRequests(2);
        assertEquals(2, pdpUpdates.size());

        PdpUpdate pdpUpdate = pdpUpdates.get(0);
        assertEquals(PDP2, pdpUpdate.getName());
        assertNull(pdpUpdate.getPdpGroup());

        pdpUpdate = pdpUpdates.get(1);
        assertEquals(PDP4, pdpUpdate.getName());
        assertNull(pdpUpdate.getPdpGroup());

        // it also requires a PDP STATE-CHANGE message
        List<PdpStateChange> changes = getStateChangeRequests(2);
        assertEquals(2, changes.size());

        PdpStateChange change = changes.get(0);
        assertEquals(PDP2, change.getName());
        assertEquals(PdpState.PASSIVE, change.getState());

        change = changes.get(1);
        assertEquals(PDP4, change.getName());
        assertEquals(PdpState.PASSIVE, change.getState());
    }

    @Test
    public void testUpdateField_Unchanged() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.createOrUpdateGroups(groups);

        assertNoGroupAction();
    }

    @Test
    public void testUpdateField_WasNull() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        group.setDescription(null);

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    public void testUpdateField_NowNull() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        newgrp.setDescription(null);

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    public void testUpdateField_Changed() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        newgrp.setDescription(group.getDescription() + "-changed");

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    public void testAddSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

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
    public void testAddSubGroupWildCardPolicyType() throws Exception {
        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyListWildCard.json"));
        when(dao.getPolicyTypeList("some.*", "2.3.4")).thenReturn(Collections.emptyList());

        PdpGroups groups = loadPdpGroups("createGroupsWildCard.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.createOrUpdateGroups(groups);

        PdpGroup newgrp = groups.getGroups().get(0);

        PdpSubGroup newsub = newgrp.getPdpSubgroups().get(1);
        newsub.setCurrentInstanceCount(0);
        newsub.setPdpInstances(new ArrayList<>(0));

        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    public void testAddSubGroup_ValidationPolicyTypeNotFound() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        when(dao.getPolicyTypeList(any(), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).hasMessageContaining("unknown policy type");
    }

    @Test
    public void testAddSubGroup_ValidationPolicyTypeDaoEx() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        PfModelException exc = new PfModelException(Status.CONFLICT, EXPECTED_EXCEPTION);
        when(dao.getPolicyTypeList(any(), any())).thenThrow(exc);

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isSameAs(exc);
    }

    @Test
    public void testAddSubGroup_ValidateVersionPrefixMatch() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
            .thenReturn(loadPolicies("daoPolicyList.json")).thenReturn(loadPolicies("createGroupNewPolicy.json"));

        PdpGroups reqgroups = loadPdpGroups("createGroupsVersPrefix.json");

        prov.createOrUpdateGroups(reqgroups);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(dbgroup.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), dbgroup.toString());
    }

    @Test
    public void testUpdateSubGroup_Invalid() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        // change properties
        newgrp.getPdpSubgroups().get(0).setProperties(new TreeMap<>());

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("properties");

        assertNoGroupAction();
    }

    @Test
    public void testUpdateSubGroup_SupportedPolicies() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        newgrp.getPdpSubgroups().get(0).getSupportedPolicyTypes()
            .add(new ToscaPolicyTypeIdentifier("typeX.*", "9.8.7"));

        // the group is updated with a new supported policy type in subgroup
        assertEquals(2, newgrp.getPdpSubgroups().get(0).getSupportedPolicyTypes().size());
        prov.createOrUpdateGroups(groups);
        // PdpGroup update doesn't allow supported policy type modifications
        // during pdp group update, the ones in db is maintained
        assertEquals(1, newgrp.getPdpSubgroups().get(0).getSupportedPolicyTypes().size());
        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    public void testUpdateSubGroup_DesiredCount() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        newgrp.getPdpSubgroups().get(0).setDesiredInstanceCount(20);

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    public void testUpdateSubGroup_Unchanged() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.createOrUpdateGroups(groups);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(group.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), group.toString());

        // no notifications
        verify(notifier, never()).addDeploymentData(any());
        verify(notifier, never()).addUndeploymentData(any());

        // no group updates
        assertNoGroupAction();
    }

    @Test
    public void testValidateSubGroup_PropertiesMismatch() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        newgrp.setProperties(new TreeMap<>());

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
            .hasMessageContaining("properties");

        assertNoGroupAction();
    }

    protected void assertUpdate(List<PdpUpdate> updates, String groupName, String pdpType, String pdpName) {

        PdpUpdate update = updates.remove(0);

        assertEquals(groupName, update.getPdpGroup());
        assertEquals(pdpType, update.getPdpSubgroup());
        assertEquals(pdpName, update.getName());
        assertTrue(update.getPolicies().contains(policy1));
    }

    private void assertNoGroupAction() throws Exception {
        verify(dao, never()).createPdpGroups(any());
        verify(dao, never()).updatePdpGroups(any());
        verify(reqmap, never()).addRequest(any(), any());
    }

    private void assertGroupUpdateOnly(PdpGroup group) throws Exception {
        verify(dao, never()).createPdpGroups(any());
        verify(reqmap, never()).addRequest(any(), any());

        List<PdpGroup> updates = getGroupUpdates();
        assertEquals(Arrays.asList(group), updates);
    }
}
