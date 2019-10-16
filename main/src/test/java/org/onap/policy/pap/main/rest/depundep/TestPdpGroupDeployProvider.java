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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.notification.PolicyPdpNotificationData;

public class TestPdpGroupDeployProvider extends ProviderSuper {
    private static final String EXPECTED_EXCEPTION = "expected exception";

    private static final String POLICY2_NAME = "policyB";
    private static final String POLICY1_VERSION = "1.2.3";
    private static final String GROUP1_NAME = "groupA";
    private static final String PDP1_TYPE = "pdpTypeA";
    private static final String PDP2_TYPE = "pdpTypeB";
    private static final String PDP4_TYPE = "pdpTypeD";
    private static final String PDP2 = "pdpB";
    private static final String PDP4 = "pdpD";

    private PdpGroupDeployProvider prov;


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

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"));
        when(dao.getPolicyTypeList("typeA", "100.2.3")).thenReturn(Arrays.asList(loadPolicyType("daoPolicyType.json")));

        prov = new PdpGroupDeployProvider();
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

        // policy won't match supported type
        groups.getGroups().get(0).getPdpSubgroups().get(0).getSupportedPolicyTypes().get(0).setVersion("99.99.99");

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
                        .hasMessageContaining("supported policy");

        assertNoGroupAction();
    }

    @Test
    public void testValidateGroupOnly_NullState() throws PfModelException {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(null);
        prov.createOrUpdateGroups(groups);
    }

    @Test
    public void testValidateGroupOnly_Active() throws PfModelException {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.ACTIVE);
        prov.createOrUpdateGroups(groups);
    }

    @Test
    public void testValidateGroupOnly_Passive() throws PfModelException {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.PASSIVE);
        prov.createOrUpdateGroups(groups);
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

        assertEquals(group.getDescription(), "my description");
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

        // should have notified of deleted subgroup's policies/PDPs
        ArgumentCaptor<PolicyPdpNotificationData> captor = ArgumentCaptor.forClass(PolicyPdpNotificationData.class);
        verify(notifier).addUndeploymentData(captor.capture());
        PolicyPdpNotificationData data = captor.getValue();
        assertEquals(policy1.getIdentifier(), data.getPolicyId());
        assertEquals(policy1.getTypeIdentifier(), data.getPolicyType());
        assertEquals("[pdpB, pdpD]", new TreeSet<>(data.getPdps()).toString());

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
    public void testUpdateGroup_MultipleChanges() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        PdpSubGroup subgrp = newgrp.getPdpSubgroups().get(0);
        subgrp.setDesiredInstanceCount(30);
        subgrp.getPolicies().add(new ToscaPolicyIdentifier(POLICY2_NAME, POLICY1_VERSION));
        subgrp.getSupportedPolicyTypes().add(new ToscaPolicyTypeIdentifier("typeX", "9.8.7"));

        when(dao.getFilteredPolicyList(any()))
                        .thenReturn(loadPolicies("createGroupNewPolicy.json"))
                        .thenReturn(loadPolicies("daoPolicyList.json"))
                        .thenReturn(loadPolicies("createGroupNewPolicy.json"));

        prov.createOrUpdateGroups(groups);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(group.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), group.toString());

        // this requires a PDP UPDATE message
        assertGroupUpdate(group, subgrp);
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
    public void testAddSubGroup_ValidationPolicyNotFound() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSubNotFound.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        when(dao.getFilteredPolicyList(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).hasMessageContaining("unknown policy");
    }

    @Test
    public void testAddSubGroup_ValidationPolicyDaoEx() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSubNotFound.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        PfModelException exc = new PfModelException(Status.CONFLICT, EXPECTED_EXCEPTION);
        when(dao.getFilteredPolicyList(any())).thenThrow(exc);

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isSameAs(exc);
    }

    @Test
    public void testAddSubGroup_ValidateVersionPrefixMatch() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                        .thenReturn(loadPolicies("daoPolicyList.json"))
                        .thenReturn(loadPolicies("createGroupNewPolicy.json"));

        PdpGroups reqgroups = loadPdpGroups("createGroupsVersPrefix.json");

        prov.createOrUpdateGroups(reqgroups);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(dbgroup.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), dbgroup.toString());
    }

    @Test
    public void testAddSubGroup_ValidateVersionPrefixMismatch() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"));


        PdpGroups reqgroups = loadPdpGroups("createGroupsVersPrefixMismatch.json");

        assertThatThrownBy(() -> prov.createOrUpdateGroups(reqgroups)).isInstanceOf(PfModelException.class)
                        .hasMessageContaining("different version already deployed");

        assertNoGroupAction();
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

        newgrp.getPdpSubgroups().get(0).getSupportedPolicyTypes().add(new ToscaPolicyTypeIdentifier("typeX", "9.8.7"));

        prov.createOrUpdateGroups(groups);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
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
    public void testUpdateSubGroup_Policies() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsDelPolicy.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        PdpSubGroup subgrp = newgrp.getPdpSubgroups().get(0);

        // delete second policy
        subgrp.setPolicies(subgrp.getPolicies().subList(0, 1));

        // add new policy
        ToscaPolicyIdentifier policyId2 = new ToscaPolicyIdentifier(POLICY2_NAME, POLICY1_VERSION);
        subgrp.getPolicies().add(policyId2);

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                        .thenReturn(loadPolicies("daoPolicyList.json"))
                        .thenReturn(loadPolicies("daoPolicyListDelPolicy.json"))
                        .thenReturn(loadPolicies("createGroupNewPolicy.json"));

        prov.createOrUpdateGroups(groups);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(group.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), group.toString());

        // should have notified of added policy/PDPs
        ArgumentCaptor<PolicyPdpNotificationData> captor = ArgumentCaptor.forClass(PolicyPdpNotificationData.class);
        verify(notifier).addDeploymentData(captor.capture());
        PolicyPdpNotificationData data = captor.getValue();
        assertEquals(policyId2, data.getPolicyId());
        assertEquals(policy1.getTypeIdentifier(), data.getPolicyType());
        assertEquals("[pdpA]", new TreeSet<>(data.getPdps()).toString());

        // should have notified of deleted policy/PDPs
        captor = ArgumentCaptor.forClass(PolicyPdpNotificationData.class);
        verify(notifier).addUndeploymentData(captor.capture());
        data = captor.getValue();
        assertEquals(new ToscaPolicyIdentifier("ToBeDeleted", POLICY1_VERSION), data.getPolicyId());
        assertEquals(policy1.getTypeIdentifier(), data.getPolicyType());
        assertEquals("[pdpA]", new TreeSet<>(data.getPdps()).toString());

        // this requires a PDP UPDATE message
        assertGroupUpdate(group, subgrp);
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
    public void testUpdateSubGroup_PolicyVersionMismatch() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

        // arrange for DB policy version to be different
        PdpSubGroup dbsubgrp = dbgroup.getPdpSubgroups().get(0);
        dbsubgrp.getPolicies().get(0).setVersion("9.9.9");

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"));

        assertThatThrownBy(() -> prov.createOrUpdateGroups(groups)).isInstanceOf(PfModelException.class)
                        .hasMessageContaining("different version already deployed");

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

    @Test
    public void testDeployPolicies() throws PfModelException {
        prov.deployPolicies(loadEmptyRequest());
    }

    @Test
    public void testDeploySimplePolicies() throws Exception {
        prov.deployPolicies(loadEmptyRequest());
    }

    @Test
    public void testDeploySimplePolicies_DaoEx() throws Exception {
        PfModelException exc = new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);
        when(dao.getFilteredPdpGroups(any())).thenThrow(exc);

        assertThatThrownBy(() -> prov.deployPolicies(loadRequest())).isSameAs(exc);
    }

    @Test
    public void testDeploySimplePolicies_DaoPfRtEx() throws Exception {
        PfModelRuntimeException exc = new PfModelRuntimeException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);
        when(dao.getFilteredPdpGroups(any())).thenThrow(exc);

        assertThatThrownBy(() -> prov.deployPolicies(loadRequest())).isSameAs(exc);
    }

    @Test
    public void testDeploySimplePolicies_RuntimeEx() throws Exception {
        RuntimeException exc = new RuntimeException(EXPECTED_EXCEPTION);
        when(dao.getFilteredPolicyList(any())).thenThrow(exc);

        assertThatThrownBy(() -> prov.deployPolicies(loadRequest())).isInstanceOf(PfModelException.class).hasCause(exc);
    }

    @Test
    public void testDeploySimplePolicies_NoGroups() throws Exception {
        when(dao.getFilteredPdpGroups(any())).thenReturn(loadGroups("emptyGroups.json"));

        assertThatThrownBy(() -> prov.deployPolicies(loadRequest())).isInstanceOf(PfModelException.class)
                        .hasMessage("policy not supported by any PDP group: policyA 1.2.3");
    }

    @Test
    public void testMakeUpdater() throws Exception {
        /*
         * Each subgroup has a different PDP type and name.
         *
         * Type is not supported by the first subgroup.
         *
         * Second subgroup matches.
         *
         * Third subgroup already contains the policy.
         *
         * Last subgroup matches.
         */

        when(dao.getFilteredPdpGroups(any())).thenReturn(loadGroups("upgradeGroupDao.json"));

        prov.deployPolicies(loadRequest());

        assertGroup(getGroupUpdates(), GROUP1_NAME);

        List<PdpUpdate> requests = getUpdateRequests(2);
        assertUpdate(requests, GROUP1_NAME, PDP2_TYPE, PDP2);
        assertUpdate(requests, GROUP1_NAME, PDP4_TYPE, PDP4);

        // should have notified of added policy/PDPs
        ArgumentCaptor<PolicyPdpNotificationData> captor = ArgumentCaptor.forClass(PolicyPdpNotificationData.class);
        verify(notifier).addDeploymentData(captor.capture());
        PolicyPdpNotificationData data = captor.getValue();
        assertEquals(policy1.getIdentifier(), data.getPolicyId());
        assertEquals(policy1.getTypeIdentifier(), data.getPolicyType());
        assertEquals("[pdpB, pdpD]", new TreeSet<>(data.getPdps()).toString());

        // no undeployment notifications
        verify(notifier, never()).addUndeploymentData(any());
    }

    @Test
    public void testMakeUpdater_PolicyVersionMismatch() throws Exception {

        // subgroup has a different version of the Policy
        when(dao.getFilteredPdpGroups(any())).thenReturn(loadGroups("upgradeGroupDao_DiffVers.json"));

        assertThatThrownBy(() -> prov.deployPolicies(loadRequest())).isInstanceOf(PfModelRuntimeException.class)
                        .hasMessageContaining("pdpTypeC").hasMessageContaining("different version already deployed");

        verify(dao, never()).createPdpGroups(any());
        verify(dao, never()).updatePdpGroups(any());
        verify(reqmap, never()).addRequest(any(PdpUpdate.class));
    }

    @Test
    public void testMakeUpdater_NoPdps() throws Exception {

        // subgroup has no PDPs
        when(dao.getFilteredPdpGroups(any())).thenReturn(loadGroups("upgradeGroup_NoPdpsDao.json"));

        assertThatThrownBy(() -> prov.deployPolicies(loadRequest())).isInstanceOf(PfModelRuntimeException.class)
                        .hasMessage("group " + GROUP1_NAME + " subgroup " + PDP1_TYPE + " has no active PDPs");

        verify(dao, never()).createPdpGroups(any());
        verify(dao, never()).updatePdpGroups(any());
        verify(reqmap, never()).addRequest(any(PdpUpdate.class));
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

    private void assertGroupUpdate(PdpGroup group, PdpSubGroup subgrp) throws Exception {
        verify(dao, never()).createPdpGroups(any());

        assertEquals(0, getStateChangeRequests(1).size());

        List<PdpUpdate> pdpUpdates = getUpdateRequests(1);
        assertEquals(1, pdpUpdates.size());

        PdpUpdate pdpUpdate = pdpUpdates.get(0);
        assertEquals("pdpA", pdpUpdate.getName());
        assertEquals(group.getName(), pdpUpdate.getPdpGroup());

        assertEquals(subgrp.getPdpType(), pdpUpdate.getPdpSubgroup());

        List<ToscaPolicyIdentifier> pdpPolicies =
                        pdpUpdate.getPolicies().stream().map(ToscaPolicy::getIdentifier).collect(Collectors.toList());
        Collections.sort(pdpPolicies);

        assertEquals(subgrp.getPolicies().toString(), pdpPolicies.toString());

        List<PdpGroup> updates = getGroupUpdates();
        assertEquals(Arrays.asList(group), updates);
    }

    private void assertGroupUpdateOnly(PdpGroup group) throws Exception {
        verify(dao, never()).createPdpGroups(any());
        verify(reqmap, never()).addRequest(any(), any());

        List<PdpGroup> updates = getGroupUpdates();
        assertEquals(Arrays.asList(group), updates);
    }

    /**
     * Loads a standard request.
     *
     * @return a standard request
     */
    protected PdpDeployPolicies loadRequest() {
        return loadRequest("request.json");
    }

    /**
     * Loads a request from a JSON file.
     *
     * @param fileName name of the file from which to load
     * @return the request that was loaded
     */
    protected PdpDeployPolicies loadRequest(String fileName) {
        return loadFile(fileName, PdpDeployPolicies.class);
    }

    /**
     * Loads an empty request.
     *
     * @return an empty request
     */
    protected PdpDeployPolicies loadEmptyRequest() {
        return loadRequest("emptyRequest.json");
    }
}
