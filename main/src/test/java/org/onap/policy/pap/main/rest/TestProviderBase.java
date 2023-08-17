/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023 Nordix Foundation.
 * Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PapPolicyIdentifier;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.springframework.test.util.ReflectionTestUtils;

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

    @AfterAll
    public static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    /**
     * Configures mocks and objects.
     *
     * @throws Exception if an error occurs
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        prov = new MyProvider();
        super.initialize(prov);
        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"));
    }

    @Test
    void testProviderBase() {
        assertSame(lockit, ReflectionTestUtils.getField(prov, "updateLock"));
        assertSame(reqmap, ReflectionTestUtils.getField(prov, "requestMap"));
    }

    @Test
    void testProcess() throws Exception {
        prov.process(loadRequest(), this::handle);

        assertGroup(getGroupUpdates(), GROUP1_NAME);

        assertUpdate(getUpdateRequests(1), GROUP1_NAME, PDP1_TYPE, PDP1);

        checkEmptyNotification();
    }

    @Test
    void testProcess_PfRtEx() throws Exception {
        PfModelRuntimeException ex = new PfModelRuntimeException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);
        when(pdpGroupService.updatePdpGroups(any())).thenThrow(ex);

        assertThatThrownBy(() -> prov.process(loadEmptyRequest(), this::handle)).isSameAs(ex);
    }

    @Test
    void testProcess_RuntimeEx() throws Exception {
        RuntimeException ex = new RuntimeException(EXPECTED_EXCEPTION);
        when(pdpGroupService.updatePdpGroups(any())).thenThrow(ex);

        assertThatThrownBy(() -> prov.process(loadEmptyRequest(), this::handle)).isInstanceOf(PfModelException.class)
                        .hasMessage("request failed").hasCause(ex);
    }

    @Test
    void testProcessPolicy_NoGroups() throws Exception {
        when(pdpGroupService.getFilteredPdpGroups(any())).thenReturn(Collections.emptyList());

        SessionData session =
            new SessionData(DEFAULT_USER, toscaService, pdpGroupService, policyStatusService, policyAuditService);
        ToscaConceptIdentifierOptVersion ident = new ToscaConceptIdentifierOptVersion(POLICY1_NAME, POLICY1_VERSION);
        assertThatThrownBy(() -> prov.processPolicy(session, ident)).isInstanceOf(PfModelException.class)
            .hasMessage("policy not supported by any PDP group: policyA 1.2.3");

    }

    @Test
    void testGetPolicy() throws Exception {
        PfModelException exc = new PfModelException(Status.CONFLICT, EXPECTED_EXCEPTION);
        when(toscaService.getFilteredPolicyList(any())).thenThrow(exc);

        ToscaConceptIdentifierOptVersion req = loadRequest();
        assertThatThrownBy(() -> prov.process(req, this::handle)).isInstanceOf(PfModelRuntimeException.class)
                        .hasCause(exc);
    }

    @Test
    void testGetPolicy_NotFound() throws Exception {
        when(toscaService.getFilteredPolicyList(any())).thenReturn(Collections.emptyList());

        ToscaConceptIdentifierOptVersion req = loadRequest();
        assertThatThrownBy(() -> prov.process(req, this::handle)).isInstanceOf(PfModelRuntimeException.class)
                        .hasMessage("cannot find policy: policyA 1.2.3")
                        .extracting(ex -> ((PfModelRuntimeException) ex).getErrorResponse().getResponseCode())
                        .isEqualTo(Status.NOT_FOUND);
    }

    @Test
    void testGetGroup() throws Exception {
        when(pdpGroupService.getFilteredPdpGroups(any())).thenReturn(loadGroups("getGroupDao.json"))
                        .thenReturn(loadGroups("groups.json"));

        prov.process(loadRequest(), this::handle);

        assertGroup(getGroupUpdates(), GROUP1_NAME);
    }

    @Test
    void testUpgradeGroup() throws Exception {
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

        prov.clear();
        prov.add(false, true, false, true);

        prov.process(loadRequest(), this::handle);

        assertGroup(getGroupUpdates(), GROUP1_NAME);

        List<PdpUpdate> requests = getUpdateRequests(2);
        assertUpdate(requests, GROUP1_NAME, PDP2_TYPE, PDP2);
        assertUpdate(requests, GROUP1_NAME, PDP4_TYPE, PDP4);
    }

    @Test
    void testUpgradeGroup_Multiple() throws Exception {
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

        when(toscaService.getFilteredPolicyList(any())).thenReturn(loadPolicies("daoPolicyList.json"))
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
        when(pdpGroupService.getFilteredPdpGroups(any()))
            .thenReturn(groups1).thenReturn(groups1)
            .thenReturn(groups2).thenReturn(groups2)
            .thenReturn(groups1).thenReturn(groups1)
            .thenReturn(groups1).thenReturn(groups1);
        // @formatter:on

        // multiple policies in the request
        PdpDeployPolicies request = loadFile("updateGroupReqMultiple.json", PdpDeployPolicies.class);

        prov.process(request, (data, deploy) -> {
            for (ToscaConceptIdentifierOptVersion policy : deploy.getPolicies()) {
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
    void testUpgradeGroup_NothingUpdated() throws Exception {
        prov.clear();
        prov.add(false);

        prov.process(loadRequest(), this::handle);

        verify(pdpGroupService, never()).createPdpGroups(any());
        verify(pdpGroupService, never()).updatePdpGroups(any());
        verify(reqmap, never()).addRequest(any(PdpUpdate.class));
    }


    protected void assertUpdate(List<PdpUpdate> updates, String groupName, String pdpType, String pdpName) {

        PdpUpdate update = updates.remove(0);

        assertEquals(PapConstants.PAP_NAME, update.getSource());
        assertEquals(groupName, update.getPdpGroup());
        assertEquals(pdpType, update.getPdpSubgroup());
        assertEquals(pdpName, update.getName());
        assertTrue(update.getPoliciesToBeDeployed().contains(policy1));
    }

    /**
     * Loads a standard request.
     *
     * @return a standard request
     */
    protected ToscaConceptIdentifierOptVersion loadRequest() {
        return loadRequest("requestBase.json");
    }

    /**
     * Loads a request from a JSON file.
     *
     * @param fileName name of the file from which to load
     * @return the request that was loaded
     */
    protected ToscaConceptIdentifierOptVersion loadRequest(String fileName) {
        return loadFile(fileName, PapPolicyIdentifier.class).getGenericIdentifier();
    }

    /**
     * Loads an empty request.
     *
     * @return an empty request
     */
    protected ToscaConceptIdentifierOptVersion loadEmptyRequest() {
        return loadRequest("emptyRequestBase.json");
    }

    /**
     * Handles a request by invoking the provider's processPolicy method.
     *
     * @param data session data
     * @param request request to be handled
     * @throws PfModelException if an error occurred
     */
    private void handle(SessionData data, ToscaConceptIdentifierOptVersion request) throws PfModelException {
        prov.processPolicy(data, request);
    }


    private static class MyProvider extends ProviderBase {
        /**
         * Used to determine whether to make an update when
         * makeUpdater() is called. The updater function removes an
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
            shouldUpdate.addAll(List.of(update));
        }

        @Override
        protected Updater makeUpdater(SessionData data, ToscaPolicy policy,
                        ToscaConceptIdentifierOptVersion desiredPolicy) {

            return (group, subgroup) -> {
                if (shouldUpdate.remove()) {
                    ToscaConceptIdentifier ident1 = policy.getIdentifier();

                    // queue indicated that the update should succeed
                    subgroup.getPolicies().add(ident1);

                    ToscaPolicy testPolicy1 = data.getPolicy(new ToscaConceptIdentifierOptVersion(ident1));
                    data.trackDeploy(testPolicy1, Collections.singleton(PDP1), GROUP1_NAME, PDP1_TYPE);
                    data.trackUndeploy(ident1, Collections.singleton(PDP2), GROUP1_NAME, PDP2_TYPE);

                    ToscaConceptIdentifier ident2 = new ToscaConceptIdentifier(POLICY1_NAME, "9.9.9");
                    ToscaPolicy testPolicy2 = data.getPolicy(new ToscaConceptIdentifierOptVersion(ident2));
                    data.trackDeploy(testPolicy2, Collections.singleton(PDP3), GROUP1_NAME, PDP3_TYPE);
                    data.trackUndeploy(ident2, Collections.singleton(PDP4), GROUP1_NAME, PDP4_TYPE);
                    return true;

                } else {
                    // queue indicated that no update should be made this time
                    return false;
                }
            };
        }
    }
}
