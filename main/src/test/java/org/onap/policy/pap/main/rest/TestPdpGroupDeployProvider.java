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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pap.concepts.PdpGroups;
import org.onap.policy.models.pap.concepts.PolicyIdentOptVersion;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.concepts.PolicyTypeIdent;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProvider;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.powermock.reflect.Whitebox;
import static org.onap.policy.pap.main.rest.PdpGroupDeployProvider.*;

public class TestPdpGroupDeployProvider {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final Object REQUEST_FAILED_MSG = "request failed";

    private static final String POLICY1_NAME = "policyA";
    private static final String POLICY1_VERSION = "1.2.3";
    private static final String POLICY1_TYPE = "typeA";
    private static final String POLICY1_TYPE_VERSION = "100.2.3";
    private static final String GROUP1_NAME = "groupA";
    private static final String GROUP1_VERSION = "200.2.3";
    private static final String GROUP1_NEW_VERSION = "201.0.0";
    private static final String PDP1_TYPE = "pdpTypeA";
    private static final String PDP1 = "pdpA";

    private PdpGroupDeployProvider prov;
    private Object lockit;
    private PdpModifyRequestMap reqmap;
    private PolicyModelsProviderFactoryWrapper daofact;
    private PolicyModelsProvider dao;
    private ToscaPolicy policy1;

    @AfterClass
    public static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    @Before
    public void setUp() throws Exception {
        Registry.newRegistry();

        lockit = new Object();
        reqmap = mock(PdpModifyRequestMap.class);
        daofact = mock(PolicyModelsProviderFactoryWrapper.class);
        dao = mock(PolicyModelsProvider.class);
        policy1 = new ToscaPolicy();

        policy1.setName(POLICY1_NAME);
        policy1.setVersion(POLICY1_VERSION);
        policy1.setType(POLICY1_TYPE);
        policy1.setTypeVersion(POLICY1_TYPE_VERSION);

        when(daofact.create()).thenReturn(dao);
        when(dao.getPolicies(POLICY1_NAME, POLICY1_VERSION)).thenReturn(Arrays.asList(policy1));

        PdpGroup group1 = makeGroup(GROUP1_NAME, GROUP1_VERSION, PDP1_TYPE);
        when(dao.getActivePdpGroupsByPolicy(POLICY1_TYPE, POLICY1_TYPE_VERSION)).thenReturn(Arrays.asList(group1));
        when(dao.getPdpGroupMaxVersion(GROUP1_NAME)).thenReturn(group1);

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
        PdpGroups groups = new PdpGroups();

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployGroup(groups);
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals("not implemented yet", pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeployPolicies() {
        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(makeEmptyDeployment());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploy() throws Exception {
        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(makeSingleDeployment());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        assertGroup(getGroupUpdates(1), GROUP1_NAME, GROUP1_VERSION);
        assertGroup(getGroupCreates(1), GROUP1_NAME, GROUP1_NEW_VERSION);

        assertUpdate(getUpdateRequests(1), GROUP1_NAME, PDP1_TYPE, PDP1);
    }

    @Test
    public void testDeploy_CreateEx() throws Exception {
        when(daofact.create()).thenThrow(new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(makeEmptyDeployment());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals(DB_ERROR_MSG, pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploy_PapEx() throws Exception {
        when(daofact.create()).thenThrow(new PolicyPapRuntimeException(EXPECTED_EXCEPTION));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(makeEmptyDeployment());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals(EXPECTED_EXCEPTION, pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploy_RuntimeEx() throws Exception {
        when(daofact.create()).thenThrow(new RuntimeException(EXPECTED_EXCEPTION));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(makeEmptyDeployment());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals(REQUEST_FAILED_MSG, pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeployGroups() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeploySimplePolicies() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeploySimplePolicies_DaoEx() throws Exception {
        when(dao.getPolicies(any(), any())).thenThrow(new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(makeSingleDeployment());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals(DB_ERROR_MSG, pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploySimplePolicies_RuntimeEx() throws Exception {
        when(dao.getPolicies(any(), any())).thenThrow(new RuntimeException(EXPECTED_EXCEPTION));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(makeSingleDeployment());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals(REQUEST_FAILED_MSG, pair.getRight().getErrorDetails());
    }

    @Test
    public void testProcessPolicy() {
        fail("Not yet implemented");
    }

    @Test
    public void testProcessPolicy_NullDesired() {
        PdpDeployPolicies deployment = makeSingleDeployment();
        deployment.setPolicies(Arrays.asList(new PolicyIdentOptVersion()));

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(deployment);
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals("null policy name", pair.getRight().getErrorDetails());
    }

    @Test
    public void testDeploySimplePolicies_NoGroups() throws Exception {
        when(dao.getActivePdpGroupsByPolicy(any(), any())).thenReturn(new LinkedList<>());

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(makeSingleDeployment());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals("not supported by any PDP group", pair.getRight().getErrorDetails());
    }

    @Test
    public void testGetPolicy() throws Exception {
        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(makeSingleDeployment());
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        verify(dao).getPolicies(any(), any());
        verify(dao, never()).getPolicyMaxVersion(POLICY1_NAME);
    }

    @Test
    public void testGetPolicy_NullVersion() throws Exception {
        when(dao.getPolicyMaxVersion(POLICY1_NAME)).thenReturn(policy1);

        // null out the version
        PdpDeployPolicies deployment = makeSingleDeployment();
        deployment.getPolicies().get(0).setVersion(null);

        Pair<Status, PdpGroupDeployResponse> pair = prov.deployPolicies(deployment);
        assertEquals(Status.OK, pair.getLeft());
        assertNull(pair.getRight().getErrorDetails());

        verify(dao).getPolicyMaxVersion(POLICY1_NAME);
        verify(dao, never()).getPolicies(any(), any());
    }

    @Test
    public void testGetGroup() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpgradeGroup() {
        fail("Not yet implemented");
    }

    @Test
    public void testMakeUpdate() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpgradeGroupVersion() {
        fail("Not yet implemented");
    }

    @Test
    public void testMakeNewVersion() {
        fail("Not yet implemented");
    }


    private void assertGroup(List<PdpGroup> groups, String name, String version) {
        PdpGroup group = groups.remove(0);

        assertEquals(name, group.getName());
        assertEquals(version, group.getVersion());
    }

    private void assertUpdate(List<PdpUpdate> updates, String groupName,
                    String pdpType, String pdpName) {

        PdpUpdate update = updates.remove(0);

        assertEquals(groupName, update.getPdpGroup());
        assertEquals(pdpType, update.getPdpSubgroup());
        assertEquals(pdpName, update.getName());
        assertTrue(update.getPolicies().contains(policy1));
    }

    private List<PdpGroup> getGroupCreates(int count) throws Exception {
        ArgumentCaptor<PdpGroup> captor = ArgumentCaptor.forClass(PdpGroup.class);

        verify(dao, times(count)).createPdpGroup(captor.capture());

        return captor.getAllValues();
    }

    private List<PdpGroup> getGroupUpdates(int count) throws Exception {
        ArgumentCaptor<PdpGroup> captor = ArgumentCaptor.forClass(PdpGroup.class);

        verify(dao, times(count)).updatePdpGroup(captor.capture());

        return captor.getAllValues();
    }

    private List<PdpUpdate> getUpdateRequests(int count) throws Exception {
        ArgumentCaptor<PdpUpdate> captor = ArgumentCaptor.forClass(PdpUpdate.class);

        verify(reqmap, times(count)).addRequest(captor.capture());

        return captor.getAllValues();
    }

    private PdpDeployPolicies makeEmptyDeployment() {
        PdpDeployPolicies deployments = new PdpDeployPolicies();

        deployments.setPolicies(new LinkedList<>());
        return deployments;
    }

    private PdpDeployPolicies makeSingleDeployment() {
        PdpDeployPolicies deployments = new PdpDeployPolicies();

        deployments.setPolicies(Arrays.asList(makeIdent(POLICY1_NAME, POLICY1_VERSION)));

        return deployments;
    }

    private PdpGroup makeGroup(String name, String version, String... pdpTypes) {
        PdpGroup group = new PdpGroup();

        group.setName(name);
        group.setVersion(version);

        List<PdpSubGroup> lst = new ArrayList<>(pdpTypes.length);
        group.setPdpSubgroups(lst);

        for (String pdpType : pdpTypes) {
            lst.add(makeSubGroup(pdpType));
        }

        return group;
    }

    private PdpSubGroup makeSubGroup(String pdpType) {
        PdpSubGroup subgrp = new PdpSubGroup();

        subgrp.setPdpType(pdpType);
        subgrp.setSupportedPolicyTypes(Arrays.asList(makeType(POLICY1_TYPE, POLICY1_TYPE_VERSION)));
        subgrp.setPdpInstances(Arrays.asList(makePdp(PDP1)));

        return subgrp;
    }

    private Pdp makePdp(String name) {
        Pdp pdp = new Pdp();

        pdp.setInstanceId(name);

        return pdp;
    }

    private PolicyTypeIdent makeType(String name, String version) {
        PolicyTypeIdent ident = new PolicyTypeIdent();

        ident.setName(name);
        ident.setVersion(version);

        return ident;
    }

    private PolicyIdentOptVersion makeIdent(String name, String version) {
        PolicyIdentOptVersion ident = new PolicyIdentOptVersion();

        ident.setName(name);
        ident.setVersion(version);

        return ident;
    }
}
