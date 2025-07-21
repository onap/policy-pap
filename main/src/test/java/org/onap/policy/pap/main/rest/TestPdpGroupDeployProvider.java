/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023-2025 OpenInfra Foundation Europe. All rights reserved.
 * Modifications Copyright (C) 2021-2023 Bell Canada. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

class TestPdpGroupDeployProvider extends ProviderSuper {

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

    @AfterAll
    static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    /**
     * Configures mocks and objects.
     *
     */
    @Override
    @BeforeEach
    public void setUp() {

        super.setUp();
        prov = new PdpGroupDeployProvider();
        super.initialize(prov);

        try {
            when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList2.json"));
            when(toscaService.getPolicyTypeList("typeA", "100.2.3"))
                .thenReturn(List.of(loadPolicyType()));
        } catch (PfModelException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tests updateGroupPolicies when policies are being added.
     */
    @Test
    void testUpdateGroupPoliciesAdd() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(dbgroup.getName())).thenReturn(List.of(dbgroup));

        // add new policies
        List<ToscaConceptIdentifier> policies = newgrp.getPdpSubgroups().get(0).getPolicies();
        policies.add(new ToscaConceptIdentifier(POLICY2_NAME, POLICY2_VERSION));
        policies.add(new ToscaConceptIdentifier(POLICY3_NAME, POLICY3_VERSION));

        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                .thenReturn(loadPolicies("createGroupNewPolicy2.json")).thenReturn(loadPolicies("daoPolicyList.json"));

        // add = POST
        DeploymentGroups depgroups = toDeploymentGroups(groups);
        depgroups.getGroups().get(0).getDeploymentSubgroups().get(0).setAction(Action.POST);

        prov.updateGroupPolicies(depgroups, DEFAULT_USER);

        assertEquals(newgrp.toString(), dbgroup.toString());
        assertGroupUpdate(dbgroup, dbgroup.getPdpSubgroups().get(0));
    }

    /**
     * Tests updateGroupPolicies when policies are being deleted.
     */
    @Test
    void testUpdateGroupPoliciesDelete() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);

        // additional policies in the DB that will be removed
        List<ToscaConceptIdentifier> policies = newgrp.getPdpSubgroups().get(0).getPolicies();
        policies.add(new ToscaConceptIdentifier(POLICY2_NAME, POLICY2_VERSION));
        policies.add(new ToscaConceptIdentifier(POLICY3_NAME, POLICY3_VERSION));

        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(dbgroup.getName())).thenReturn(List.of(dbgroup));

        // policy that should be left
        final ToscaConceptIdentifier policyId1 = policies.remove(0);

        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                .thenReturn(loadPolicies("createGroupNewPolicy2.json")).thenReturn(loadPolicies("daoPolicyList.json"));

        DeploymentGroups depgroups = toDeploymentGroups(groups);
        depgroups.getGroups().get(0).getDeploymentSubgroups().get(0).setAction(Action.DELETE);

        prov.updateGroupPolicies(depgroups, DEFAULT_USER);

        // only the first policy should remain
        policies.clear();
        policies.add(policyId1);

        assertEquals(newgrp.toString(), dbgroup.toString());
        assertGroupUpdate(dbgroup, dbgroup.getPdpSubgroups().get(0));
    }

    /**
     * Tests updateGroupPolicies when policies are being added and deleted in the same
     * subgroup.
     */
    @Test
    void testUpdateGroupPoliciesAddAndDelete() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpSubGroup subgrp = newgrp.getPdpSubgroups().get(0);

        // put policy3 into db subgroup
        subgrp.getPolicies().add(new ToscaConceptIdentifier(POLICY3_NAME, POLICY3_VERSION));
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(dbgroup.getName())).thenReturn(List.of(dbgroup));

        // now make the subgrp reflect our final expectation
        subgrp.getPolicies().remove(1);
        subgrp.getPolicies().add(new ToscaConceptIdentifier(POLICY2_NAME, POLICY2_VERSION));

        // indicate policy2 being added and policy3 being deleted
        DeploymentSubGroup depsub1 = new DeploymentSubGroup();
        depsub1.setAction(Action.POST);
        depsub1.setPdpType(subgrp.getPdpType());
        depsub1.setPolicies(List.of(new ToscaConceptIdentifier(POLICY2_NAME, POLICY2_VERSION)));

        DeploymentSubGroup depsub2 = new DeploymentSubGroup();
        depsub2.setAction(Action.DELETE);
        depsub2.setPdpType(subgrp.getPdpType());
        depsub2.setPolicies(List.of(new ToscaConceptIdentifier(POLICY3_NAME, POLICY3_VERSION)));

        DeploymentGroup depgroup = new DeploymentGroup();
        depgroup.setName(newgrp.getName());
        depgroup.setDeploymentSubgroups(Arrays.asList(depsub1, depsub2));

        DeploymentGroups depgroups = new DeploymentGroups();
        depgroups.setGroups(List.of(depgroup));

        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                .thenReturn(loadPolicies("daoPolicyList.json")).thenReturn(loadPolicies("createGroupNewPolicy2.json"));

        prov.updateGroupPolicies(depgroups, DEFAULT_USER);

        assertEquals(newgrp.toString(), dbgroup.toString());
        assertGroupUpdate(dbgroup, dbgroup.getPdpSubgroups().get(0));
    }

    @Test
    void testUpdateGroupPolicies() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        // something different in this subgroup
        group.getPdpSubgroups().get(0).getPolicies().add(new ToscaConceptIdentifier(POLICY2_NAME, POLICY2_VERSION));

        prov.updateGroupPolicies(toDeploymentGroups(groups), DEFAULT_USER);

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdate(group, group.getPdpSubgroups().get(0));
    }

    @Test
    void testUpdateGroupPolicies_EmptyRequest() throws Exception {
        prov.updateGroupPolicies(toDeploymentGroups(loadPdpGroups("emptyGroups.json")), DEFAULT_USER);

        // no groups, so no action should have been taken
        assertNoGroupAction();
    }

    @Test
    void testUpdateGroupPolicies_InvalidRequest() {
        assertThatThrownBy(() -> prov.updateGroupPolicies(new DeploymentGroups(), DEFAULT_USER))
                .isInstanceOf(PfModelException.class).hasMessageContaining("is null");

        assertNoGroupAction();
    }

    @Test
    void testUpdateGroup_UnknownGroup() {
        PdpGroups groups = loadPdpGroups("deployGroups.json");

        String groupName = groups.getGroups().get(0).getName();

        // group not found
        when(pdpGroupService.getPdpGroups(groupName)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> prov.updateGroupPolicies(toDeploymentGroups(groups), DEFAULT_USER))
                .isInstanceOf(PfModelException.class).hasMessageContaining(groupName)
                .hasMessageContaining("unknown group");

        assertNoGroupAction();
    }

    @Test
    void testUpdateGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");

        // DB group = new group
        PdpGroup group = new PdpGroup(groups.getGroups().get(0));
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        prov.updateGroupPolicies(toDeploymentGroups(groups), DEFAULT_USER);

        assertNoGroupAction();
    }

    @Test
    void testUpdateGroup_NewSubGroup() {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("deployGroups.json").getGroups().get(0);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        assertThatThrownBy(() -> prov.updateGroupPolicies(toDeploymentGroups(groups), DEFAULT_USER))
                .isInstanceOf(PfModelException.class).hasMessageContaining("pdpTypeB")
                .hasMessageContaining("unknown subgroup");

        assertNoGroupAction();
    }

    @Test
    void testUpdateSubGroup_Invalid() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);

        // group has no policies yet
        group.getPdpSubgroups().get(0).getPolicies().clear();
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        // unknown policy
        when(toscaService.getFilteredPolicyList(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> prov.updateGroupPolicies(toDeploymentGroups(groups), DEFAULT_USER))
                .isInstanceOf(PfModelException.class)
                .hasMessageContaining(newgrp.getPdpSubgroups().get(0).getPolicies().get(0).getName())
                .hasMessageContaining("unknown policy");

        assertNoGroupAction();
    }

    @Test
    void testUpdateSubGroup_Policies() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);

        // add a second subgroup, which will be left unchanged
        PdpSubGroup subgrp = newgrp.getPdpSubgroups().get(0);
        PdpSubGroup subgrp2 = new PdpSubGroup(subgrp);
        subgrp2.setPdpType(PDP2_TYPE);
        newgrp.getPdpSubgroups().add(subgrp2);

        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        // add two new policies
        ToscaConceptIdentifier policyId2 = new ToscaConceptIdentifier(POLICY2_NAME, POLICY2_VERSION);
        subgrp.getPolicies().add(policyId2);

        ToscaConceptIdentifier policyId3 = new ToscaConceptIdentifier(POLICY3_NAME, POLICY3_VERSION);
        subgrp.getPolicies().add(policyId3);

        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("createGroupNewPolicy.json"))
                .thenReturn(loadPolicies("createGroupNewPolicy2.json")).thenReturn(loadPolicies("daoPolicyList.json"));

        prov.updateGroupPolicies(toDeploymentGroups(groups), DEFAULT_USER);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(group.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), group.toString());

        // nothing is complete - notification should be empty
        checkEmptyNotification();

        // this requires a PDP UPDATE message
        assertGroupUpdate(newgrp, subgrp);
    }

    @Test
    void testUpdateSubGroup_PolicyVersionPrefix() throws Exception {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);

        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        // use version prefix
        PdpSubGroup subgrp = newgrp.getPdpSubgroups().get(0);
        ToscaConceptIdentifier ident = subgrp.getPolicies().get(0);
        String version = ident.getVersion();
        ident.setVersion("1");

        prov.updateGroupPolicies(toDeploymentGroups(groups), DEFAULT_USER);

        // restore full type before comparing
        ident.setVersion(version);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(group.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), group.toString());

        assertNoGroupAction();
    }

    @Test
    void testUpdateSubGroup_PolicyVersionPrefixMismatch() {
        PdpGroups groups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);

        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        // use incorrect version prefix
        newgrp.getPdpSubgroups().get(0).getPolicies().get(0).setVersion("9");

        assertThatThrownBy(() -> prov.updateGroupPolicies(toDeploymentGroups(groups), DEFAULT_USER))
                .isInstanceOf(PfModelException.class).hasMessageContaining("different version already deployed");

        assertNoGroupAction();
    }

    @Test
    void testUpdateSubGroup_Unchanged() throws Exception {
        PdpGroups dbgroups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = dbgroups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(group.getName())).thenReturn(List.of(group));

        prov.updateGroupPolicies(toDeploymentGroups(dbgroups), DEFAULT_USER);

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(group.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), group.toString());

        // no notifications
        checkEmptyNotification();

        // no group updates
        assertNoGroupAction();
    }

    @Test
    void testUpdateSubGroup_PolicyVersionMismatch() throws Exception {
        PdpGroups dbgroups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = dbgroups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(dbgroup.getName())).thenReturn(List.of(dbgroup));

        // arrange for DB policy version to be different
        PdpSubGroup dbsubgrp = dbgroup.getPdpSubgroups().get(0);
        dbsubgrp.getPolicies().get(0).setVersion("9.9.9");

        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"));

        assertThatThrownBy(() -> prov.updateGroupPolicies(toDeploymentGroups(dbgroups), DEFAULT_USER))
                .isInstanceOf(PfModelException.class).hasMessageContaining("different version already deployed");

        assertNoGroupAction();
    }

    @Test
    void testUpdateSubGroup_UnsupportedType() throws Exception {
        PdpGroups dbgroups = loadPdpGroups("deployGroups.json");
        PdpGroup newgrp = dbgroups.getGroups().get(0);
        PdpGroup dbgroup = new PdpGroup(newgrp);
        when(pdpGroupService.getPdpGroups(dbgroup.getName())).thenReturn(List.of(dbgroup));

        final DeploymentGroups groups = toDeploymentGroups(dbgroups);

        PdpSubGroup dbsubgrp = dbgroup.getPdpSubgroups().get(0);

        // DB has no policies
        dbsubgrp.getPolicies().clear();

        // DB has a different supported type
        dbsubgrp.getSupportedPolicyTypes().get(0).setName("some-other-type");

        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"));

        assertThatThrownBy(() -> prov.updateGroupPolicies(groups, DEFAULT_USER)).isInstanceOf(PfModelException.class)
                .hasMessageContaining(newgrp.getPdpSubgroups().get(0).getPolicies().get(0).getName())
                .hasMessageContaining("not a supported policy for the subgroup");

        assertNoGroupAction();
    }

    @Test
    void testDeployPolicies() {
        assertThatCode(() -> prov.deployPolicies(loadEmptyRequest(), DEFAULT_USER)).doesNotThrowAnyException();
    }

    /**
     * Tests deployPolicies() when the policies are invalid.
     */
    @Test
    void testDeployPoliciesInvalidPolicies() {
        // valid list
        PdpDeployPolicies policies0 = loadFile("PapPoliciesList.json", PdpDeployPolicies.class);
        assertThatCode(() -> prov.deployPolicies(policies0, DEFAULT_USER)).doesNotThrowAnyException();

        // null list
        PdpDeployPolicies policies = new PdpDeployPolicies();
        assertThatThrownBy(() -> prov.deployPolicies(policies, DEFAULT_USER)).isInstanceOf(PfModelException.class)
                .hasMessageContaining("policies");

        // list containing null item
        PdpDeployPolicies policies2 = loadFile("PapPoliciesNullItem.json", PdpDeployPolicies.class);
        assertThatThrownBy(() -> prov.deployPolicies(policies2, DEFAULT_USER)).isInstanceOf(PfModelException.class)
                .hasMessageContaining("policies").hasMessageContaining("null");

        // list containing a policy with a null name
        PdpDeployPolicies policies3 = loadFile("PapPoliciesNullPolicyName.json", PdpDeployPolicies.class);
        assertThatThrownBy(() -> prov.deployPolicies(policies3, DEFAULT_USER)).isInstanceOf(PfModelException.class)
                .hasMessageContaining("policies").hasMessageContaining("policy-id").hasMessageContaining("null")
                .hasMessageNotContaining("\"value\"");

        // list containing a policy with an invalid name
        PdpDeployPolicies policies4 = loadFile("PapPoliciesInvalidPolicyName.json", PdpDeployPolicies.class);
        assertThatThrownBy(() -> prov.deployPolicies(policies4, DEFAULT_USER)).isInstanceOf(PfModelException.class)
                .hasMessageContaining("policies").hasMessageContaining("policy-id").hasMessageContaining("$ abc")
                .hasMessageNotContaining("version");

        // list containing a policy with an invalid version
        PdpDeployPolicies policies5 = loadFile("PapPoliciesInvalidPolicyVersion.json", PdpDeployPolicies.class);
        assertThatThrownBy(() -> prov.deployPolicies(policies5, DEFAULT_USER)).isInstanceOf(PfModelException.class)
                .hasMessageContaining("policies").hasMessageContaining("version").hasMessageContaining("abc123")
                .hasMessageNotContaining("policy-id");
    }

    /**
     * Tests deployPolicies() when the supported policy type uses a wild-card.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testDeployPoliciesWildCard() throws Exception {
        when(pdpGroupService.getFilteredPdpGroups(any())).thenReturn(loadGroups("deployPoliciesWildCard.json"));
        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyListWildCard.json"));
        when(toscaService.getPolicyTypeList(any(), any())).thenReturn(Collections.emptyList());

        policy1.setName("policy.some");
        policy1.setVersion(POLICY1_VERSION);
        policy1.setType("some.type");
        policy1.setTypeVersion("100.2.3");

        PdpDeployPolicies depreq = loadRequest();
        depreq.getPolicies().get(0).setName("policy.some");

        prov.deployPolicies(depreq, DEFAULT_USER);

        assertGroup(getGroupUpdates(), GROUP1_NAME);

        List<PdpUpdate> requests = getUpdateRequests(1);
        assertUpdate(requests, PDP2_TYPE, PDP2);

        // nothing is complete - notification should be empty
        checkEmptyNotification();
    }

    @Test
    void testDeploySimplePolicies() {
        assertThatCode(() -> prov.deployPolicies(loadEmptyRequest(), DEFAULT_USER)).doesNotThrowAnyException();
    }

    @Test
    void testDeploySimplePolicies_PfRtEx() {
        PfModelRuntimeException exc = new PfModelRuntimeException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);
        when(pdpGroupService.getFilteredPdpGroups(any())).thenThrow(exc);

        assertThatThrownBy(() -> prov.deployPolicies(loadRequest(), DEFAULT_USER)).isSameAs(exc);
    }

    @Test
    void testDeploySimplePolicies_RuntimeEx() throws Exception {
        RuntimeException exc = new RuntimeException(EXPECTED_EXCEPTION);
        when(toscaService.getFilteredPolicyList(any())).thenThrow(exc);

        assertThatThrownBy(() -> prov.deployPolicies(loadRequest(), DEFAULT_USER)).isInstanceOf(PfModelException.class)
                .hasCause(exc);
    }

    @Test
    void testDeploySimplePolicies_NoGroups() {
        when(pdpGroupService.getFilteredPdpGroups(any())).thenReturn(loadGroups("emptyGroups.json"));

        assertThatThrownBy(() -> prov.deployPolicies(loadRequest(), DEFAULT_USER)).isInstanceOf(PfModelException.class)
                .hasMessage("policy not supported by any PDP group: policyA 1.2.3");
    }

    @Test
    void testMakeUpdater() throws Exception {
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

        when(pdpGroupService.getFilteredPdpGroups(any())).thenReturn(loadGroups("upgradeGroupDao.json"));

        prov.deployPolicies(loadRequest(), DEFAULT_USER);

        assertGroup(getGroupUpdates(), GROUP1_NAME);

        List<PdpUpdate> requests = getUpdateRequests(2);
        assertUpdate(requests, PDP2_TYPE, PDP2);
        assertUpdate(requests, PDP4_TYPE, PDP4);

        // nothing is complete - notification should be empty
        checkEmptyNotification();
    }

    @Test
    void testMakeUpdater_PolicyVersionMismatch() {

        // subgroup has a different version of the Policy
        when(pdpGroupService.getFilteredPdpGroups(any())).thenReturn(loadGroups("upgradeGroupDao_DiffVers.json"));

        PdpDeployPolicies req = loadRequest();
        assertThatThrownBy(() -> prov.deployPolicies(req, DEFAULT_USER)).isInstanceOf(PfModelRuntimeException.class)
                .hasMessageContaining("pdpTypeC").hasMessageContaining("different version already deployed");

        verify(pdpGroupService, never()).createPdpGroups(any());
        verify(pdpGroupService, never()).updatePdpGroups(any());
        verify(reqmap, never()).addRequest(any(PdpUpdate.class));
    }

    @Test
    void testMakeUpdater_NoPdps() {

        // subgroup has no PDPs
        when(pdpGroupService.getFilteredPdpGroups(any())).thenReturn(loadGroups("upgradeGroup_NoPdpsDao.json"));

        PdpDeployPolicies req = loadRequest();
        assertThatThrownBy(() -> prov.deployPolicies(req, DEFAULT_USER)).isInstanceOf(PfModelRuntimeException.class)
                .hasMessage("group " + GROUP1_NAME + " subgroup " + PDP1_TYPE + " has no active PDPs");

        verify(pdpGroupService, never()).createPdpGroups(any());
        verify(pdpGroupService, never()).updatePdpGroups(any());
        verify(reqmap, never()).addRequest(any(PdpUpdate.class));
    }

    protected void assertUpdate(List<PdpUpdate> updates, String pdpType, String pdpName) {

        PdpUpdate update = updates.remove(0);

        assertEquals(TestPdpGroupDeployProvider.GROUP1_NAME, update.getPdpGroup());
        assertEquals(pdpType, update.getPdpSubgroup());
        assertEquals(pdpName, update.getName());
        assertThat(update.getPoliciesToBeDeployed()).contains(policy1);
    }

    private void assertNoGroupAction() {
        verify(pdpGroupService, never()).createPdpGroups(any());
        verify(pdpGroupService, never()).updatePdpGroups(any());
        verify(reqmap, never()).addRequest(any(), any());
    }

    private void assertGroupUpdate(PdpGroup group, PdpSubGroup subgrp) {
        verify(pdpGroupService, never()).createPdpGroups(any());

        assertEquals(0, getStateChangeRequests(1).size());

        List<PdpUpdate> pdpUpdates = getUpdateRequests(1);
        assertEquals(1, pdpUpdates.size());

        PdpUpdate pdpUpdate = pdpUpdates.get(0);
        assertEquals("pdpA", pdpUpdate.getName());
        assertEquals(group.getName(), pdpUpdate.getPdpGroup());

        assertEquals(subgrp.getPdpType(), pdpUpdate.getPdpSubgroup());

        List<ToscaConceptIdentifier> pdpPolicies = pdpUpdate.getPoliciesToBeDeployed().stream()
            .map(ToscaPolicy::getIdentifier).sorted().collect(Collectors.toList());

        assertThat(subgrp.getPolicies()).containsAll(pdpPolicies);

        List<PdpGroup> updates = getGroupUpdates();
        assertEquals(List.of(group), updates);
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
        group.setDeploymentSubgroups(
                dbgroup.getPdpSubgroups().stream().map(this::toDeploymentSubGroup).collect(Collectors.toList()));

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
