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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.pap.main.rest.depundep.ProviderBase.DB_ERROR_MSG;

import java.util.List;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpUpdate;

public class TestPdpGroupDeployProvider extends ProviderSuper {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final Object REQUEST_FAILED_MSG = "request failed";

    private static final String POLICY1_NAME = "policyA";
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
    public void testDeployGroup() {
        Pair<Status, PdpGroupDeployResponse> pair = prov.deployGroup(new PdpGroups());
        assertEquals(Status.INTERNAL_SERVER_ERROR, pair.getLeft());
        assertEquals("request failed", pair.getRight().getErrorDetails());
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
