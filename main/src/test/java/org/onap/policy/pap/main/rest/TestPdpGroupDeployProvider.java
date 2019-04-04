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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.pap.main.rest.PdpGroupDeployProvider.DB_ERROR_MSG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.powermock.reflect.Whitebox;

public class TestPdpGroupDeployProvider extends PdpGroupDeployProviderBase {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final Object REQUEST_FAILED_MSG = "request failed";

    private static final String POLICY1_NAME = "policyA";
    private static final String POLICY1_VERSION = "1.2.3";
    private static final String GROUP1_NAME = "groupA";
    private static final String GROUP1_VERSION = "200.2.3";
    private static final String GROUP1_NEW_VERSION = "201.0.0";
    private static final String GROUP2_NAME = "groupB";
    private static final String PDP1_TYPE = "pdpTypeA";
    private static final String PDP2_TYPE = "pdpTypeB";
    private static final String PDP3_TYPE = "pdpTypeC";
    private static final String PDP4_TYPE = "pdpTypeD";
    private static final String PDP1 = "pdpA";
    private static final String PDP2 = "pdpB";
    private static final String PDP3 = "pdpC";
    private static final String PDP4 = "pdpD";

    private PdpGroupDeployProvider prov;
    private Object lockit;
    private PdpModifyRequestMap reqmap;
    private PolicyModelsProviderFactoryWrapper daofact;
    private ToscaPolicy policy1;


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

        Registry.newRegistry();

        lockit = new Object();
        reqmap = mock(PdpModifyRequestMap.class);
        daofact = mock(PolicyModelsProviderFactoryWrapper.class);
        policy1 = loadFile("policy.json", ToscaPolicy.class);

        when(daofact.create()).thenReturn(dao);

        when(dao.getPolicyList(POLICY1_NAME, POLICY1_VERSION)).thenReturn(loadPolicies("daoPolicyList.json"));

        List<PdpGroup> groups = loadGroups("groups.json");

        when(dao.getFilteredPdpGroups(any())).thenReturn(groups);

        when(dao.createPdpGroups(any())).thenAnswer(answer -> answer.getArgumentAt(0, List.class));
        when(dao.updatePdpGroups(any())).thenAnswer(answer -> answer.getArgumentAt(0, List.class));

        Registry.register(PapConstants.REG_PDP_MODIFY_LOCK, lockit);
        Registry.register(PapConstants.REG_PDP_MODIFY_MAP, reqmap);
        Registry.register(PapConstants.REG_PAP_DAO_FACTORY, daofact);

        prov = new PdpGroupDeployProvider();
    }

    @Test
    public void testPdpGroupDeployProvider() {
        assertSame(lockit, Whitebox.getInternalState(prov, "updateLock"));
        assertSame(reqmap, Whitebox.getInternalState(prov, "requestMap"));
        assertSame(daofact, Whitebox.getInternalState(prov, "daoFactory"));
    }

    @Test
    public void testDeployGroup() {
        Pair<Status, PdpGroupDeployResponse> pair = prov.deployGroup(new PdpGroups());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals("not implemented yet", pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeployPolicies() {
        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadEmptyRequest());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploy() throws Exception {
        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        assertGroup(getGroupUpdates(1).get(0), GROUP1_NAME, GROUP1_VERSION);
        assertGroup(getGroupCreates(1).get(0), GROUP1_NAME, GROUP1_NEW_VERSION);

        assertUpdate(getUpdateRequests(1), GROUP1_NAME, PDP1_TYPE, PDP1);
    }

    @Test
    public void testDeploy_CreateEx() throws Exception {
        when(daofact.create()).thenThrow(new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadEmptyRequest());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals(DB_ERROR_MSG, pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploy_PapEx() throws Exception {
        when(daofact.create()).thenThrow(new PolicyPapRuntimeException(EXPECTED_EXCEPTION));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadEmptyRequest());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals(EXPECTED_EXCEPTION, pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploy_RuntimeEx() throws Exception {
        when(daofact.create()).thenThrow(new RuntimeException(EXPECTED_EXCEPTION));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadEmptyRequest());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals(REQUEST_FAILED_MSG, pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploySimplePolicies_DaoEx() throws Exception {
        when(dao.getPolicyList(any(), any())).thenThrow(new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION));

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
        assertEquals("not supported by any PDP group", pair.getRight().getErrorDetails());
    }

    @Test
    public void testGetPolicy() throws Exception {
        /*
         * The first queries generates a list of policies that are designed to exercise
         * the version-checking, while the second query is used when "load" is invoked.
         */
        // @formatter:off
        when(dao.getPolicyList(POLICY1_NAME, POLICY1_VERSION))
            .thenReturn(loadPolicies("getPolicyDao.json"))
            .thenReturn(loadPolicies("daoPolicyList.json"));
        // @formatter:on

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        verify(dao, times(2)).getPolicyList(any(), any());
        verify(dao, never()).getFilteredPolicyList(any());
    }

    @Test
    public void testGetPolicy_NullVersion() throws Exception {
        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest("getPolicyReqNullVersion.json"));
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        verify(dao).getFilteredPolicyList(any());
        verify(dao, never()).getPolicies(any(), any());
    }

    @Test
    public void testGetPolicy_NotFound() throws Exception {
        when(dao.getPolicyList(any(), any())).thenReturn(Collections.emptyList());

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals("no policy for policy-id=" + POLICY1_NAME + " and version=" + POLICY1_VERSION,
                        pair.getRight().getErrorDetails());
    }

    @Test
    public void testGetGroup() throws Exception {
        when(dao.getFilteredPdpGroups(any())).thenReturn(loadGroups("getGroupDao.json"))
                        .thenReturn(loadGroups("groups.json"));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        assertGroup(getGroupUpdates(1).get(0), GROUP1_NAME, GROUP1_VERSION);
        assertGroup(getGroupCreates(1).get(0), GROUP1_NAME, GROUP1_NEW_VERSION);
    }

    @Test
    public void testUpgradeGroup() throws Exception {
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

        assertGroup(getGroupUpdates(1).get(0), GROUP1_NAME, GROUP1_VERSION);
        assertGroup(getGroupCreates(1).get(0), GROUP1_NAME, GROUP1_NEW_VERSION);

        List<PdpUpdate> requests = getUpdateRequests(2);
        assertUpdate(requests, GROUP1_NAME, PDP2_TYPE, PDP2);
        assertUpdate(requests, GROUP1_NAME, PDP4_TYPE, PDP4);
    }

    @Test
    public void testUpgradeGroup_Multiple() throws Exception {
        /*
         * Policy data in the DB: policy1=type1, policy2=type2, policy3=type3,
         * policy4=type1
         *
         * Group data in the DB: group1=(type1=pdp1, type3=pdp3) group2=(type2=pdp2)
         *
         * Request specifies: policy1, policy2, policy3, policy4
         *
         * Should create new versions of group1 and group2.
         *
         * Should update old versions of group1 and group2. Should also update new version
         * of group1 twice.
         *
         * Should generate updates to pdp1, pdp2, and pdp3.
         */

        when(dao.getPolicyList(POLICY1_NAME, POLICY1_VERSION)).thenReturn(loadPolicies("daoPolicyList.json"));
        when(dao.getPolicyList("policyB", POLICY1_VERSION)).thenReturn(loadPolicies("upgradeGroupPolicy2.json"));
        when(dao.getPolicyList("policyC", POLICY1_VERSION)).thenReturn(loadPolicies("upgradeGroupPolicy3.json"));
        when(dao.getPolicyList("policyD", POLICY1_VERSION)).thenReturn(loadPolicies("upgradeGroupPolicy4.json"));

        List<PdpGroup> groups1 = loadGroups("upgradeGroupGroup1.json");
        List<PdpGroup> groups2 = loadGroups("upgradeGroupGroup2.json");

        /*
         * these are in pairs of (get-group, get-max-group) matching each policy in the
         * request
         */
        // @formatter:off
        when(dao.getFilteredPdpGroups(any()))
            .thenReturn(groups1).thenReturn(groups1)
            .thenReturn(groups2).thenReturn(groups2)
            .thenReturn(groups1).thenReturn(groups1)
            .thenReturn(groups1).thenReturn(groups1);
        // @formatter:on

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest("updateGroupReqMultiple.json"));
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        List<List<PdpGroup>> creates = getGroupCreates(2);
        assertGroup(creates.get(0), GROUP1_NAME, GROUP1_NEW_VERSION);
        assertGroup(creates.get(1), GROUP2_NAME, "301.0.0");

        List<List<PdpGroup>> updates = getGroupUpdates(4);
        assertGroup(updates.get(0), GROUP1_NAME, GROUP1_VERSION);
        assertGroup(updates.get(1), GROUP2_NAME, "300.2.3");
        assertGroup(updates.get(2), GROUP1_NAME, GROUP1_NEW_VERSION);
        assertGroup(updates.get(3), GROUP1_NAME, GROUP1_NEW_VERSION);

        List<PdpUpdate> requests = getUpdateRequests(3);
        assertUpdateIgnorePolicy(requests, GROUP1_NAME, PDP1_TYPE, PDP1);
        assertUpdateIgnorePolicy(requests, GROUP2_NAME, PDP2_TYPE, PDP2);
        assertUpdateIgnorePolicy(requests, GROUP1_NAME, PDP3_TYPE, PDP3);
    }

    @Test
    public void testUpgradeGroup_NoPdps() throws Exception {

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

    @Test
    public void testUpgradeGroup_NothingUpdated() throws Exception {

        // subgroup doesn't support the policy type
        when(dao.getFilteredPdpGroups(any())).thenReturn(loadGroups("upgradeGroup_NothingUpdatedDao.json"));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(loadRequest());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        verify(dao, never()).createPdpGroups(any());
        verify(dao, never()).updatePdpGroups(any());
        verify(reqmap, never()).addRequest(any(PdpUpdate.class));
    }


    private void assertGroup(List<PdpGroup> groups, String name, String version) {
        PdpGroup group = groups.remove(0);

        assertEquals(name, group.getName());
        assertEquals(version, group.getVersion());
    }

    private void assertUpdate(List<PdpUpdate> updates, String groupName, String pdpType, String pdpName) {

        PdpUpdate update = updates.remove(0);

        assertEquals(groupName, update.getPdpGroup());
        assertEquals(pdpType, update.getPdpSubgroup());
        assertEquals(pdpName, update.getName());
        assertTrue(update.getPolicies().contains(policy1));
    }

    private void assertUpdateIgnorePolicy(List<PdpUpdate> updates, String groupName, String pdpType, String pdpName) {

        PdpUpdate update = updates.remove(0);

        assertEquals(groupName, update.getPdpGroup());
        assertEquals(pdpType, update.getPdpSubgroup());
        assertEquals(pdpName, update.getName());
    }

    private List<PdpUpdate> getUpdateRequests(int count) throws Exception {
        ArgumentCaptor<PdpUpdate> captor = ArgumentCaptor.forClass(PdpUpdate.class);

        verify(reqmap, times(count)).addRequest(captor.capture());

        return new ArrayList<>(captor.getAllValues());
    }
}
