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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.onap.policy.models.pdp.concepts.DeploymentGroup;
import org.onap.policy.models.pdp.concepts.DeploymentGroups;
import org.onap.policy.models.pdp.concepts.DeploymentSubGroup;
import org.onap.policy.models.pdp.concepts.DeploymentSubGroup.Action;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.notification.PolicyPdpNotificationData;

public class TestPdpGroupDeployProvider extends ProviderSuper {
    private static final String EXPECTED_EXCEPTION = "expected exception";

    private static final String POLICY2_NAME = "policyB";
    private static final String POLICY3_NAME = "policyC";
    private static final String POLICY1_VERSION = "1.2.3";
    private static final String POLICY2_VERSION = "1.2.3";
    private static final String POLICY3_VERSION = "1.2.3";
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

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList2.json"));
        when(dao.getPolicyTypeList("typeA", "100.2.3")).thenReturn(Arrays.asList(loadPolicyType("daoPolicyType.json")));

        prov = new PdpGroupDeployProvider();
    }

    /**
     * Tests updateGroupPolicies when policies are being added.
     */
    @Test
    public void testUpdateGroupPoliciesAdd() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

        // add new policies
        List<ToscaPolicyIdentifier> policies = newgrp.getPdpSubgroups().get(0).getPolicies();
        policies.add(new ToscaPolicyIdentifier(POLICY2_NAME, POLICY2_VERSION));
        policies.add(new ToscaPolicyIdentifier(POLICY3_NAME, POLICY3_VERSION));

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                        .thenReturn(loadPolicies("createGroupNewPolicy2.json"))
                        .thenReturn(loadPolicies("daoPolicyList.json"));

        // add = POST
        DeploymentGroups depgroups = toDeploymentGroups(groups);
        depgroups.getGroups().get(0).getDeploymentSubgroups().get(0).setAction(Action.POST);

        prov.updateGroupPolicies(depgroups);

        assertEquals(newgrp.toString(), dbgroup.toString());
        assertGroupUpdate(dbgroup, dbgroup.getPdpSubgroups().get(0));
    }

    /**
     * Tests updateGroupPolicies when policies are being deleted.
     */
    @Test
    public void testUpdateGroupPoliciesDelete() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);

        // additional policies in the DB that will be removed
        List<ToscaPolicyIdentifier> policies = newgrp.getPdpSubgroups().get(0).getPolicies();
        policies.add(new ToscaPolicyIdentifier(POLICY2_NAME, POLICY2_VERSION));
        policies.add(new ToscaPolicyIdentifier(POLICY3_NAME, POLICY3_VERSION));

        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

        // policy that should be left
        final ToscaPolicyIdentifier policyId1 = policies.remove(0);

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                        .thenReturn(loadPolicies("createGroupNewPolicy2.json"))
                        .thenReturn(loadPolicies("daoPolicyList.json"));

        DeploymentGroups depgroups = toDeploymentGroups(groups);
        depgroups.getGroups().get(0).getDeploymentSubgroups().get(0).setAction(Action.DELETE);

        prov.updateGroupPolicies(depgroups);

        // only the first policy should remain
        policies.clear();
        policies.add(policyId1);

        assertEquals(newgrp.toString(), dbgroup.toString());
        assertGroupUpdate(dbgroup, dbgroup.getPdpSubgroups().get(0));
    }

    /**
     * Tests updateGroupPolicies when policies are being added and deleted in the same subgroup.
     */
    @Test
    public void testUpdateGroupPoliciesAddAndDelete() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpSubGroup subgrp = newgrp.getPdpSubgroups().get(0);

        // put policy3 into db subgroup
        subgrp.getPolicies().add(new ToscaPolicyIdentifier(POLICY3_NAME, POLICY3_VERSION));
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

        // now make the subgrp reflect our final expectation
        subgrp.getPolicies().remove(1);
        subgrp.getPolicies().add(new ToscaPolicyIdentifier(POLICY2_NAME, POLICY2_VERSION));

        // indicate policy2 being added and policy3 being deleted
        DeploymentSubGroup depsub1 = new DeploymentSubGroup();
        depsub1.setAction(Action.POST);
        depsub1.setPdpType(subgrp.getPdpType());
        depsub1.setPolicies(Arrays.asList(new ToscaPolicyIdentifier(POLICY2_NAME, POLICY2_VERSION)));

        DeploymentSubGroup depsub2 = new DeploymentSubGroup();
        depsub2.setAction(Action.DELETE);
        depsub2.setPdpType(subgrp.getPdpType());
        depsub2.setPolicies(Arrays.asList(new ToscaPolicyIdentifier(POLICY3_NAME, POLICY3_VERSION)));

        DeploymentGroup depgroup = new DeploymentGroup();
        depgroup.setName(newgrp.getName());
        depgroup.setDeploymentSubgroups(Arrays.asList(depsub1, depsub2));

        DeploymentGroups depgroups = new DeploymentGroups();
        depgroups.setGroups(Arrays.asList(depgroup));

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                        .thenReturn(loadPolicies("daoPolicyList.json"))
                        .thenReturn(loadPolicies("createGroupNewPolicy2.json"));

        prov.updateGroupPolicies(depgroups);

        assertEquals(newgrp.toString(), dbgroup.toString());
        assertGroupUpdate(dbgroup, dbgroup.getPdpSubgroups().get(0));
    }

    @Test
    public void testDeleteGroupPolicies() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);

        // additional policies in the DB that will be removed
        List<ToscaPolicyIdentifier> policies = newgrp.getPdpSubgroups().get(0).getPolicies();
        policies.add(new ToscaPolicyIdentifier(POLICY2_NAME, POLICY2_VERSION));
        policies.add(new ToscaPolicyIdentifier(POLICY3_NAME, POLICY3_VERSION));

        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

        // policy that should be left
        final ToscaPolicyIdentifier policyId1 = policies.remove(0);

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                        .thenReturn(loadPolicies("createGroupNewPolicy2.json"))
                        .thenReturn(loadPolicies("daoPolicyList.json"));

        DeploymentGroups depgroups = toDeploymentGroups(groups);
        depgroups.getGroups().get(0).getDeploymentSubgroups().get(0).setAction(Action.DELETE);

        prov.updateGroupPolicies(depgroups);

        // only the first policy should remain
        policies.clear();
        policies.add(policyId1);

        assertEquals(newgrp.toString(), dbgroup.toString());
        assertGroupUpdate(dbgroup, dbgroup.getPdpSubgroups().get(0));
    }

    @Test
    public void testUpdateGroupPolicies() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        // something different in this subgroup
        group.getPdpSubgroups().get(0).getPolicies().add(new ToscaPolicyIdentifier(POLICY2_NAME, POLICY2_VERSION));

        prov.updateGroupPolicies(toDeploymentGroups(groups));

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdate(group, group.getPdpSubgroups().get(0));
    }

    @Test
    public void testUpdateGroupPolicies_EmptyRequest() throws Exception {
        prov.updateGroupPolicies(toDeploymentGroups(loadPdpGroups("emptyGroups.json")));

        // no groups, so no action should have been taken
        assertNoGroupAction();
    }

    @Test
    public void testUpdateGroupPolicies_InvalidRequest() throws Exception {
        assertThatThrownBy(() -> prov.updateGroupPolicies(new DeploymentGroups())).isInstanceOf(PfModelException.class)
                        .hasMessageContaining("is null");

        assertNoGroupAction();
    }

    @Test
    public void testUpdateGroup_UnknownGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");

        String groupName = groups.getGroups().get(0).getName();

        // group not found
        when(dao.getPdpGroups(groupName)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> prov.updateGroupPolicies(toDeploymentGroups(groups)))
                        .isInstanceOf(PfModelException.class).hasMessageContaining(groupName)
                        .hasMessageContaining("unknown group");

        assertNoGroupAction();
    }

    @Test
    public void testUpdateGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");

        // DB group = new group
        PdpGroup group = new PdpGroup(groups.getGroups().get(0));
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.updateGroupPolicies(toDeploymentGroups(groups));

        assertNoGroupAction();
    }

    @Test
    public void testUpdateGroup_PropertiesChanged() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");

        PdpGroup group = new PdpGroup(groups.getGroups().get(0));
        group.setProperties(new TreeMap<>());

        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        // should not throw an exception
        prov.updateGroupPolicies(toDeploymentGroups(groups));

        assertNoGroupAction();
    }

    @Test
    public void testUpdateGroup_NewDescription() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        group.setDescription("old description");
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.updateGroupPolicies(toDeploymentGroups(groups));

        // description should be unchanged - no updates
        newgrp.setDescription(group.getDescription());

        assertNoGroupAction();

        assertEquals("old description", group.getDescription());
        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    public void testUpdateGroup_NewState() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        group.setPdpGroupState(PdpState.TEST);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.updateGroupPolicies(toDeploymentGroups(groups));

        // state should be unchanged - no updates
        newgrp.setPdpGroupState(PdpState.TEST);

        assertNoGroupAction();

        assertEquals(PdpState.TEST, group.getPdpGroupState());
        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    public void testUpdateGroup_NewSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("deployGroups.json").getGroups().get(0);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        assertThatThrownBy(() -> prov.updateGroupPolicies(toDeploymentGroups(groups)))
                        .isInstanceOf(PfModelException.class).hasMessageContaining("pdpTypeB")
                        .hasMessageContaining("unknown subgroup");


        assertNoGroupAction();
    }

    @Test
    public void testUpdateGroup_UpdatedSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        // something different in this subgroup
        group.getPdpSubgroups().get(0).getPolicies().add(new ToscaPolicyIdentifier(POLICY2_NAME, POLICY2_VERSION));

        prov.updateGroupPolicies(toDeploymentGroups(groups));

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdate(group, group.getPdpSubgroups().get(0));
    }

    @Test
    public void testUpdateSubGroup_Invalid() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);

        // group has no policies yet
        group.getPdpSubgroups().get(0).getPolicies().clear();
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        // unknown policy
        when(dao.getFilteredPolicyList(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> prov.updateGroupPolicies(toDeploymentGroups(groups)))
                        .isInstanceOf(PfModelException.class)
                        .hasMessageContaining(newgrp.getPdpSubgroups().get(0).getPolicies().get(0).getName())
                        .hasMessageContaining("unknown policy");

        assertNoGroupAction();
    }

    @Test
    public void testUpdateSubGroup_Policies() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);

        // add a second subgroup, which will be left unchanged
        PdpSubGroup subgrp = newgrp.getPdpSubgroups().get(0);
        PdpSubGroup subgrp2 = new PdpSubGroup(subgrp);
        subgrp2.setPdpType(PDP2_TYPE);
        newgrp.getPdpSubgroups().add(subgrp2);

        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        // add two new policies
        ToscaPolicyIdentifier policyId2 = new ToscaPolicyIdentifier(POLICY2_NAME, POLICY2_VERSION);
        subgrp.getPolicies().add(policyId2);

        ToscaPolicyIdentifier policyId3 = new ToscaPolicyIdentifier(POLICY3_NAME, POLICY3_VERSION);
        subgrp.getPolicies().add(policyId3);

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                        .thenReturn(loadPolicies("createGroupNewPolicy2.json"))
                        .thenReturn(loadPolicies("daoPolicyList.json"));

        prov.updateGroupPolicies(toDeploymentGroups(groups));

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(group.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), group.toString());

        // should have notified of added policy/PDPs
        ArgumentCaptor<PolicyPdpNotificationData> captor = ArgumentCaptor.forClass(PolicyPdpNotificationData.class);
        verify(notifier, times(2)).addDeploymentData(captor.capture());
        assertDeploymentData(captor.getAllValues().get(0), policyId2, "[pdpA]");
        assertDeploymentData(captor.getAllValues().get(1), policyId3, "[pdpA]");

        // should NOT have notified of any deleted policy/PDPs
        verify(notifier, never()).addUndeploymentData(any());

        // this requires a PDP UPDATE message
        assertGroupUpdate(newgrp, subgrp);
    }

    @Test
    public void testUpdateSubGroup_PolicyVersionPrefix() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);

        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        // use version prefix
        PdpSubGroup subgrp = newgrp.getPdpSubgroups().get(0);
        ToscaPolicyIdentifier ident = subgrp.getPolicies().get(0);
        String version = ident.getVersion();
        ident.setVersion("1");

        prov.updateGroupPolicies(toDeploymentGroups(groups));

        // restore full type before comparing
        ident.setVersion(version);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(group.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), group.toString());

        assertNoGroupAction();
    }

    @Test
    public void testUpdateSubGroup_PolicyVersionPrefixMismatch() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);

        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        // use incorrect version prefix
        newgrp.getPdpSubgroups().get(0).getPolicies().get(0).setVersion("9");

        assertThatThrownBy(() -> prov.updateGroupPolicies(toDeploymentGroups(groups)))
                        .isInstanceOf(PfModelException.class)
                        .hasMessageContaining("different version already deployed");

        assertNoGroupAction();
    }

    @Test
    public void testUpdateSubGroup_Unchanged() throws Exception {
        PdpGroups dbgroups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = dbgroups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        prov.updateGroupPolicies(toDeploymentGroups(dbgroups));

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
        PdpGroups dbgroups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = dbgroups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

        // arrange for DB policy version to be different
        PdpSubGroup dbsubgrp = dbgroup.getPdpSubgroups().get(0);
        dbsubgrp.getPolicies().get(0).setVersion("9.9.9");

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"));

        assertThatThrownBy(() -> prov.updateGroupPolicies(toDeploymentGroups(dbgroups)))
                        .isInstanceOf(PfModelException.class)
                        .hasMessageContaining("different version already deployed");

        assertNoGroupAction();
    }

    @Test
    public void testUpdateSubGroup_UnsupportedType() throws Exception {
        PdpGroups dbgroups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = dbgroups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(dao.getPdpGroups(dbgroup.getName())).thenReturn(Arrays.asList(dbgroup));

        final DeploymentGroups groups = toDeploymentGroups(dbgroups);

        PdpSubGroup dbsubgrp = dbgroup.getPdpSubgroups().get(0);

        // DB has no policies
        dbsubgrp.getPolicies().clear();

        // DB has a different supported type
        dbsubgrp.getSupportedPolicyTypes().get(0).setName("some-other-type");

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"));

        assertThatThrownBy(() -> prov.updateGroupPolicies(groups))
                        .isInstanceOf(PfModelException.class)
                        .hasMessageContaining(newgrp.getPdpSubgroups().get(0).getPolicies().get(0).getName())
                        .hasMessageContaining("not a supported policy for the subgroup");

        assertNoGroupAction();
    }

    @Test
    public void testDeployPolicies() throws PfModelException {
        assertThatCode(() -> prov.deployPolicies(loadEmptyRequest())).doesNotThrowAnyException();
    }

    /**
     * Tests deployPolicies() when the supported policy type uses a wild-card.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testDeployPoliciesWildCard() throws Exception {
        when(dao.getFilteredPdpGroups(any())).thenReturn(loadGroups("deployPoliciesWildCard.json"));
        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyListWildCard.json"));
        when(dao.getPolicyTypeList(any(), any())).thenReturn(Collections.emptyList());

        policy1.setName("policy.some");
        policy1.setVersion(POLICY1_VERSION);
        policy1.setType("some.type");
        policy1.setTypeVersion("100.2.3");

        PdpDeployPolicies depreq = loadRequest();
        depreq.getPolicies().get(0).setName("policy.some");

        prov.deployPolicies(depreq);

        assertGroup(getGroupUpdates(), GROUP1_NAME);

        List<PdpUpdate> requests = getUpdateRequests(1);
        assertUpdate(requests, GROUP1_NAME, PDP2_TYPE, PDP2);

        // should have notified of added policy/PDPs
        ArgumentCaptor<PolicyPdpNotificationData> captor = ArgumentCaptor.forClass(PolicyPdpNotificationData.class);
        verify(notifier).addDeploymentData(captor.capture());
        assertDeploymentData(captor.getValue(), policy1.getIdentifier(), "[pdpB]");

        // no undeployment notifications
        verify(notifier, never()).addUndeploymentData(any());
    }

    @Test
    public void testDeploySimplePolicies() throws Exception {
        assertThatCode(() -> prov.deployPolicies(loadEmptyRequest())).doesNotThrowAnyException();
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
        assertDeploymentData(captor.getValue(), policy1.getIdentifier(), "[pdpB, pdpD]");

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

    private void assertDeploymentData(PolicyPdpNotificationData data, ToscaPolicyIdentifier policyId,
                    String expectedPdps) {
        assertEquals(policyId, data.getPolicyId());
        assertEquals(policy1.getTypeIdentifier(), data.getPolicyType());
        assertEquals(expectedPdps, new TreeSet<>(data.getPdps()).toString());
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

    private DeploymentGroups toDeploymentGroups(PdpGroups dbgroups) {
        DeploymentGroups groups = new DeploymentGroups();

        groups.setGroups(dbgroups.getGroups().stream().map(this::toDeploymentGroup).collect(Collectors.toList()));

        return groups;
    }

    private DeploymentGroup toDeploymentGroup(PdpGroup dbgroup) {
        DeploymentGroup group = new DeploymentGroup();

        group.setName(dbgroup.getName());
        group.setDeploymentSubgroups(dbgroup.getPdpSubgroups().stream().map(this::toDeploymentSubGroup)
                        .collect(Collectors.toList()));

        return group;
    }

    private DeploymentSubGroup toDeploymentSubGroup(PdpSubGroup dbsubgrp) {
        DeploymentSubGroup subgrp = new DeploymentSubGroup();

        subgrp.setAction(Action.PATCH);
        subgrp.setPdpType(dbsubgrp.getPdpType());
        subgrp.setPolicies(new ArrayList<>(dbsubgrp.getPolicies()));

        return subgrp;
    }
}
