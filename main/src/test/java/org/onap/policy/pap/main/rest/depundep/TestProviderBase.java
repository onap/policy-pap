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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import javax.ws.rs.core.Response.Status;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.powermock.reflect.Whitebox;

public class TestProviderBase extends ProviderSuper {
    private static final String EXPECTED_EXCEPTION = "expected exception";

    private static final String POLICY1_NAME = "policyA";
    private static final String POLICY1_VERSION = "1.2.3";
    private static final String GROUP1_NAME = "groupA";
    private static final String GROUP2_NAME = "groupB";
    private static final String PDP1_TYPE = "pdpTypeA";
    private static final String PDP2_TYPE = "pdpTypeB";
    private static final String PDP3_TYPE = "pdpTypeC";
    private static final String PDP4_TYPE = "pdpTypeD";
    private static final String PDP1 = "pdpA";
    private static final String PDP2 = "pdpB";
    private static final String PDP3 = "pdpC";
    private static final String PDP4 = "pdpD";

    private MyProvider prov;


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

        prov = new MyProvider();
    }

    @Test
    public void testProviderBase() {
        assertSame(lockit, Whitebox.getInternalState(prov, "updateLock"));
        assertSame(reqmap, Whitebox.getInternalState(prov, "requestMap"));
        assertSame(daofact, Whitebox.getInternalState(prov, "daoFactory"));
    }

    @Test
    public void testProcess() throws Exception {
        prov.process(loadRequest(), this::handle);

        assertGroup(getGroupUpdates(), GROUP1_NAME);

        assertUpdate(getUpdateRequests(1), GROUP1_NAME, PDP1_TYPE, PDP1);
    }

    @Test
    public void testProcess_CreateEx() throws Exception {
        PfModelException ex = new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);
        when(daofact.create()).thenThrow(ex);

        assertThatThrownBy(() -> prov.process(loadEmptyRequest(), this::handle)).isSameAs(ex);
    }

    @Test
    public void testProcess_PfRtEx() throws Exception {
        PfModelRuntimeException ex = new PfModelRuntimeException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);
        when(daofact.create()).thenThrow(ex);

        assertThatThrownBy(() -> prov.process(loadEmptyRequest(), this::handle)).isSameAs(ex);
    }

    @Test
    public void testProcess_RuntimeEx() throws Exception {
        RuntimeException ex = new RuntimeException(EXPECTED_EXCEPTION);
        when(daofact.create()).thenThrow(ex);

        assertThatThrownBy(() -> prov.process(loadEmptyRequest(), this::handle)).isInstanceOf(PfModelException.class)
                        .hasMessage("request failed").hasCause(ex);
    }

    @Test
    public void testProcessPolicy_NoGroups() throws Exception {
        when(dao.getFilteredPdpGroups(any())).thenReturn(Collections.emptyList());

        SessionData session = new SessionData(dao);
        ToscaPolicyIdentifierOptVersion ident = new ToscaPolicyIdentifierOptVersion(POLICY1_NAME, POLICY1_VERSION);
        assertThatThrownBy(() -> prov.processPolicy(session, ident)).isInstanceOf(PfModelException.class)
                        .hasMessage("policy not supported by any PDP group: policyA 1.2.3");

    }

    @Test
    public void testGetPolicy() throws Exception {
        PfModelException exc = new PfModelException(Status.CONFLICT, EXPECTED_EXCEPTION);
        when(dao.getFilteredPolicyList(any())).thenThrow(exc);

        assertThatThrownBy(() -> prov.process(loadRequest(), this::handle)).isInstanceOf(PfModelRuntimeException.class)
                        .hasCause(exc);
    }

    @Test
    public void testGetPolicy_NotFound() throws Exception {
        when(dao.getFilteredPolicyList(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> prov.process(loadRequest(), this::handle)).isInstanceOf(PfModelRuntimeException.class)
                        .hasMessage("cannot find policy: policyA 1.2.3").matches(thr -> {
                            PfModelRuntimeException exc = (PfModelRuntimeException) thr;
                            return (exc.getErrorResponse().getResponseCode() == Status.NOT_FOUND);
                        });
    }

    @Test
    public void testGetGroup() throws Exception {
        when(dao.getFilteredPdpGroups(any())).thenReturn(loadGroups("getGroupDao.json"))
                        .thenReturn(loadGroups("groups.json"));

        prov.process(loadRequest(), this::handle);

        assertGroup(getGroupUpdates(), GROUP1_NAME);
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

        prov.clear();
        prov.add(false, true, false, true);

        prov.process(loadRequest(), this::handle);

        assertGroup(getGroupUpdates(), GROUP1_NAME);

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

        when(dao.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"))
                        .thenReturn(loadPolicies("upgradeGroupPolicy2.json"))
                        .thenReturn(loadPolicies("upgradeGroupPolicy3.json"))
                        .thenReturn(loadPolicies("upgradeGroupPolicy4.json"));

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

        // multiple policies in the request
        PdpDeployPolicies request = loadFile("updateGroupReqMultiple.json", PdpDeployPolicies.class);

        prov.process(request, (data, deploy) -> {
            for (ToscaPolicyIdentifierOptVersion policy : deploy.getPolicies()) {
                handle(data, policy);
            }
        });

        // verify updates
        List<PdpGroup> changes = getGroupUpdates();
        assertGroup(changes, GROUP1_NAME);
        assertGroup(changes, GROUP2_NAME);

        List<PdpUpdate> requests = getUpdateRequests(3);
        assertUpdateIgnorePolicy(requests, GROUP1_NAME, PDP1_TYPE, PDP1);
        assertUpdateIgnorePolicy(requests, GROUP2_NAME, PDP2_TYPE, PDP2);
        assertUpdateIgnorePolicy(requests, GROUP1_NAME, PDP3_TYPE, PDP3);
    }

    @Test
    public void testUpgradeGroup_NothingUpdated() throws Exception {
        prov.clear();
        prov.add(false);

        prov.process(loadRequest(), this::handle);

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
    protected ToscaPolicyIdentifierOptVersion loadRequest() {
        return loadRequest("requestBase.json");
    }

    /**
     * Loads a request from a JSON file.
     *
     * @param fileName name of the file from which to load
     * @return the request that was loaded
     */
    protected ToscaPolicyIdentifierOptVersion loadRequest(String fileName) {
        return loadFile(fileName, ToscaPolicyIdentifierOptVersion.class);
    }

    /**
     * Loads an empty request.
     *
     * @return an empty request
     */
    protected ToscaPolicyIdentifierOptVersion loadEmptyRequest() {
        return loadRequest("emptyRequestBase.json");
    }

    /**
     * Handles a request by invoking the provider's processPolicy method.
     *
     * @param data session data
     * @param request request to be handled
     * @throws PfModelException if an error occurred
     */
    private void handle(SessionData data, ToscaPolicyIdentifierOptVersion request) throws PfModelException {
        prov.processPolicy(data, request);
    }


    private static class MyProvider extends ProviderBase {
        /**
         * Used to determine whether or not to make an update when
         * {@link #makeUpdater(ToscaPolicy)} is called. The updater function removes an
         * item from this queue each time it is invoked.
         */
        private final Queue<Boolean> shouldUpdate = new LinkedList<>();

        /**
         * Constructs the object and queues up several successful updates.
         */
        public MyProvider() {
            for (int x = 0; x < 10; ++x) {
                shouldUpdate.add(true);
            }
        }

        public void clear() {
            shouldUpdate.clear();
        }

        public void add(Boolean... update) {
            shouldUpdate.addAll(Arrays.asList(update));
        }

        @Override
        protected BiFunction<PdpGroup, PdpSubGroup, Boolean> makeUpdater(ToscaPolicy policy) {
            return (group, subgroup) -> {
                if (shouldUpdate.remove()) {
                    // queue indicated that the update should succeed
                    subgroup.getPolicies().add(policy.getIdentifier());
                    return true;

                } else {
                    // queue indicated that no update should be made this time
                    return false;
                }
            };
        }
    }
}
