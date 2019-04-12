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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.pap.main.rest.depundep.ProviderBase.DB_ERROR_MSG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

public class TestPdpGroupDeployProvider extends ProviderSuper {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final Object REQUEST_FAILED_MSG = "request failed";

    private static final String POLICY1_NAME = "policyA";
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

        when(dao.getPolicyList(POLICY1_NAME, POLICY1_VERSION)).thenReturn(loadPolicies("daoPolicyList.json"));

        prov = new PdpGroupDeployProvider();
    }

    @Test
    public void testCreateOrUpdateGroups() throws Exception {
        Pair<Status, PdpGroupDeployResponse> pair = prov.createOrUpdateGroups(loadPdpGroups("emptyGroups.json"));
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        // no groups, so no action should have been taken
        assertNoGroupAction();
    }

    @Test
    public void testCreateOrUpdateGroups_InvalidRequest() throws Exception {
        Pair<Status, PdpGroupDeployResponse> pair = prov.createOrUpdateGroups(new PdpGroups());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertTrue(pair.getRight().getErrorDetails().contains("is null"));

        assertNoGroupAction();
    }

    @Test
    public void testCreateOrUpdate_Invalid() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.TERMINATED);

        Pair<Status, PdpGroupDeployResponse> pair = prov.createOrUpdateGroups(groups);
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertTrue(pair.getRight().getErrorDetails().contains("pdpGroupState"));

        assertNoGroupAction();
    }

    @Test
    public void testAddGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup group = groups.getGroups().get(0);
        group.setPdpGroupState(PdpState.PASSIVE);

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

        // should not have updated the state
        assertEquals(PdpState.PASSIVE, group.getPdpGroupState());

        assertSame(group, getGroupCreates().get(0));
    }

    @Test
    public void testAddGroup_Invalid() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.TERMINATED);

        Pair<Status, PdpGroupDeployResponse> pair = prov.createOrUpdateGroups(groups);
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertTrue(pair.getRight().getErrorDetails().contains("pdpGroupState"));

        assertNoGroupAction();
    }

    @Test
    public void testAddGroup_InvalidSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");

        // policy won't match supported type
        groups.getGroups().get(0).getPdpSubgroups().get(0).getSupportedPolicyTypes().get(0).setVersion("99.99.99");

        Pair<Status, PdpGroupDeployResponse> pair = prov.createOrUpdateGroups(groups);
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertTrue(pair.getRight().getErrorDetails().contains("supported policy"));

        assertNoGroupAction();
    }

    @Test
    public void testValidateGroupOnly_NullState() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(null);
        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());
    }

    @Test
    public void testValidateGroupOnly_Active() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.ACTIVE);
        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());
    }

    @Test
    public void testValidateGroupOnly_Passive() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.PASSIVE);
        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());
    }

    @Test
    public void testValidateGroupOnly_Invalid() {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        groups.getGroups().get(0).setPdpGroupState(PdpState.TERMINATED);

        Pair<Status, PdpGroupDeployResponse> pair = prov.createOrUpdateGroups(groups);
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertTrue(pair.getRight().getErrorDetails().contains("pdpGroupState"));
    }

    @Test
    public void testUpdateGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");

        // DB group = new group
        PdpGroup group = new PdpGroup(groups.getGroups().get(0));
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

        assertNoGroupAction();
    }

    @Test
    public void testUpdateGroup_PropertiesChanged() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");

        PdpGroup group = new PdpGroup(groups.getGroups().get(0));
        group.setProperties(new TreeMap<>());

        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        Pair<Status, PdpGroupDeployResponse> pair = prov.createOrUpdateGroups(groups);
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertTrue(pair.getRight().getErrorDetails().contains("properties"));

        assertNoGroupAction();
    }

    @Test
    public void testUpdateGroup_NewDescription() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        group.setDescription("old description");
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

        assertGroupUpdateOnly(group);

        assertEquals(group.getDescription(), "my description");
        assertEquals(newgrp.toString(), group.toString());
    }

    @Test
    public void testUpdateGroup_NewSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

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

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
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

        when(dao.getPolicyList(POLICY2_NAME, POLICY1_VERSION)).thenReturn(loadPolicies("createGroupNewPolicy.json"));

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

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

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

        assertNoGroupAction();
    }

    @Test
    public void testUpdateField_WasNull() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        group.setDescription(null);

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

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

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

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

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    public void testAddSubGroup() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroupsNewSub.json");
        PdpGroup group = loadPdpGroups("createGroups.json").getGroups().get(0);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

        PdpGroup newgrp = groups.getGroups().get(0);

        PdpSubGroup newsub = newgrp.getPdpSubgroups().get(1);
        newsub.setCurrentInstanceCount(0);
        newsub.setPdpInstances(new ArrayList<>(0));

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    public void testUpdateSubGroup_Invalid() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        // change properties
        newgrp.getPdpSubgroups().get(0).setProperties(new TreeMap<>());

        Pair<Status, PdpGroupDeployResponse> pair = prov.createOrUpdateGroups(groups);
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertTrue(pair.getRight().getErrorDetails().contains("properties"));

        assertNoGroupAction();
    }

    @Test
    public void testUpdateSubGroup_SupportedPolicies() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        newgrp.getPdpSubgroups().get(0).getSupportedPolicyTypes().add(new ToscaPolicyTypeIdentifier("typeX", "9.8.7"));

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

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

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

        assertEquals(newgrp.toString(), group.toString());
        assertGroupUpdateOnly(group);
    }

    @Test
    public void testUpdateSubGroup_Policies() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        PdpSubGroup subgrp = newgrp.getPdpSubgroups().get(0);
        subgrp.getPolicies().add(new ToscaPolicyIdentifier(POLICY2_NAME, POLICY1_VERSION));

        when(dao.getPolicyList(POLICY2_NAME, POLICY1_VERSION)).thenReturn(loadPolicies("createGroupNewPolicy.json"));

        assertEquals(Status.OK, prov.createOrUpdateGroups(groups).getLeft());

        Collections.sort(newgrp.getPdpSubgroups().get(0).getPolicies());
        Collections.sort(group.getPdpSubgroups().get(0).getPolicies());

        assertEquals(newgrp.toString(), group.toString());

        // this requires a PDP UPDATE message
        assertGroupUpdate(group, subgrp);
    }

    @Test
    public void testValidateSubGroup_PropertiesMismatch() throws Exception {
        PdpGroups groups = loadPdpGroups("createGroups.json");
        PdpGroup newgrp = groups.getGroups().get(0);
        PdpGroup group = new PdpGroup(newgrp);
        when(dao.getPdpGroups(group.getName())).thenReturn(Arrays.asList(group));

        newgrp.setProperties(new TreeMap<>());

        Pair<Status, PdpGroupDeployResponse> pair = prov.createOrUpdateGroups(groups);
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertTrue(pair.getRight().getErrorDetails().contains("properties"));

        assertNoGroupAction();
    }

    @Test
    public void testDeployPolicies() {
        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadEmptyRequest());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploySimplePolicies() throws Exception {
        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploySimplePolicies_DaoEx() throws Exception {
        when(dao.getFilteredPdpGroups(any())).thenThrow(new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals(DB_ERROR_MSG, pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploySimplePolicies_RuntimeEx() throws Exception {
        when(dao.getPolicyList(any(), any())).thenThrow(new RuntimeException(EXPECTED_EXCEPTION));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals(REQUEST_FAILED_MSG, pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploySimplePolicies_NoGroups() throws Exception {
        when(dao.getFilteredPdpGroups(any())).thenReturn(loadGroups("emptyGroups.json"));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals("policy not supported by any PDP group: policyA 1.2.3", pair.getRight().getErrorDetails());
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

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        assertGroup(getGroupUpdates(), GROUP1_NAME);

        List<PdpUpdate> requests = getUpdateRequests(2);
        assertUpdate(requests, GROUP1_NAME, PDP2_TYPE, PDP2);
        assertUpdate(requests, GROUP1_NAME, PDP4_TYPE, PDP4);
    }

    @Test
    public void testMakeUpdater_NoPdps() throws Exception {

        // subgroup has no PDPs
        when(dao.getFilteredPdpGroups(any())).thenReturn(loadGroups("upgradeGroup_NoPdpsDao.json"));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals("group " + GROUP1_NAME + " subgroup " + PDP1_TYPE + " has no active PDPs",
                        pair.getRight().getErrorDetails());

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
