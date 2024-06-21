/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023-2024 Nordix Foundation.
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

package org.onap.policy.pap.main.comm.msgdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.comm.CommonRequestBase;

class UpdateReqTest extends CommonRequestBase {

    private UpdateReq data;
    private PdpUpdate update;
    private PdpStatus response;

    /**
     * Sets up.
     *
     * @throws Exception if an error occurs
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        response = new PdpStatus();

        update = makeUpdate();

        response.setName(MY_NAME);
        response.setPdpGroup(update.getPdpGroup());
        response.setPdpSubgroup(update.getPdpSubgroup());
        response.setPolicies(
            update.getPoliciesToBeDeployed().stream().map(ToscaPolicy::getIdentifier)
                .toList());

        data = new UpdateReq(reqParams, MY_REQ_NAME, update);
        data.setNotifier(notifier);
    }

    @Test
    void testGetMessage() {
        assertEquals(MY_REQ_NAME, data.getName());
        assertSame(update, data.getMessage());
    }

    @Test
    void testCheckResponse() {
        assertNull(data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyResponse();

        // both policy lists null
        update.setPoliciesToBeDeployed(null);
        response.setPolicies(null);
        assertNull(data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
    }

    @Test
    void testCheckResponse_NullName() {
        response.setName(null);

        assertEquals("null PDP name", data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyNoResponse();
    }

    @Test
    void testCheckResponse_NullMsgName() {
        update.setName(null);

        assertNull(data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyResponse();
    }

    @Test
    void testUpdateReqCheckResponse_MismatchedGroup() {
        response.setPdpGroup(DIFFERENT);

        assertEquals("group does not match", data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyNoResponse();
    }

    @Test
    void testUpdateReqCheckResponse_MismatchedSubGroup() {
        response.setPdpSubgroup(DIFFERENT);

        assertEquals("subgroup does not match", data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyNoResponse();
    }

    @Test
    void testCheckResponse_NullSubGroup() {
        update.setPdpSubgroup(null);
        response.setPdpSubgroup(null);

        // different policy list - should have no impact
        response.setPolicies(Collections.emptyList());

        assertNull(data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyNoResponse();
    }

    @Test
    void testUpdateReqCheckResponse_MismatchedPolicies() {
        ArrayList<ToscaPolicy> policies = new ArrayList<>(update.getPoliciesToBeDeployed());
        policies.set(0, makePolicy(DIFFERENT, "10.0.0"));

        response.setPolicies(policies.stream().map(ToscaPolicy::getIdentifier).toList());

        assertEquals("policies do not match", data.checkResponse(response));
        verifyResponse();

        // the first policy from the original update is all that should be undeployed
        assertEquals(Collections.singleton(update.getPoliciesToBeDeployed().get(0).getIdentifier()).toString(),
            data.getUndeployPolicies().toString());
    }

    @Test
    void testUpdateReqCheckResponse_MismatchedPolicies_Null_NotNull() {
        update.setPoliciesToBeDeployed(null);

        assertNull(data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyResponse();
    }

    @Test
    void testUpdateReqCheckResponse_MismatchedPolicies_NotNull_Null() {
        response.setPolicies(null);

        assertEquals("policies do not match", data.checkResponse(response));
        verifyResponse();

        // all policies in the update should be undeployed
        assertEquals(update.getPoliciesToBeDeployed().stream().map(ToscaPolicy::getIdentifier)
            .toList().toString(), new TreeSet<>(data.getUndeployPolicies()).toString());
    }

    @Test
    void testReconfigure() {
        // different message type should fail and leave message unchanged
        assertFalse(data.reconfigure(new PdpStateChange()));
        assertSame(update, data.getMessage());

        // same content - should succeed, but leave message unchanged
        PdpUpdate msg2 = new PdpUpdate(update);
        assertTrue(data.reconfigure(msg2));
        assertSame(update, data.getMessage());

        // different content - should succeed and install NEW message
        msg2.setPdpGroup(DIFFERENT);
        assertTrue(data.reconfigure(msg2));
        assertSame(msg2, data.getMessage());

        // both lists in the update are null - should not throw an exception
        PdpUpdate msg3 = new PdpUpdate(update);
        msg3.setPdpGroup(DIFFERENT);
        msg3.setPoliciesToBeDeployed(null);
        msg3.setPoliciesToBeUndeployed(null);
        assertThatCode(() -> data.reconfigure(msg3)).doesNotThrowAnyException();

        // both lists in the current msg (i.e., msg3) are null - should not throw an exception
        assertThatCode(() -> data.reconfigure(update)).doesNotThrowAnyException();
    }

    @Test
    void testReconfigureIsFullSameAsDeployList() {
        PdpUpdate msg2 = new PdpUpdate(update);
        ArrayList<ToscaPolicy> policies = new ArrayList<>(update.getPoliciesToBeDeployed());

        msg2.setPoliciesToBeDeployed(policies);
        assertTrue(data.reconfigure(msg2));
        assertThat(data.getMessage().getPoliciesToBeDeployed()).containsAll(msg2.getPoliciesToBeDeployed());
    }

    @Test
    void testListsNewVsResult() {
        PdpUpdate msg2 = new PdpUpdate(update);
        ArrayList<ToscaPolicy> policies = new ArrayList<>(update.getPoliciesToBeDeployed());

        // some items in both deploy and newMessage.deploy
        msg2.setPoliciesToBeDeployed(policies);
        policies.remove(0);
        data.getMessage().setPoliciesToBeDeployed(policies);
        assertTrue(data.reconfigure(msg2));
        assertThat(data.getMessage().getPoliciesToBeDeployed()).containsAll(msg2.getPoliciesToBeDeployed());

        // some items in both deploy and newMessage.undeploy
        policies = new ArrayList<>();
        policies.add(makePolicy("policy-z-1", "1.0.0"));
        policies.add(makePolicy("policy-y-1", "1.0.0"));
        data.getMessage().setPoliciesToBeDeployed(policies);

        policies.clear();
        policies = new ArrayList<>(update.getPoliciesToBeDeployed());
        List<ToscaConceptIdentifier> polsToUndep = policies.parallelStream()
            .map(ToscaPolicy::getIdentifier)
            .toList();
        msg2.setPoliciesToBeUndeployed(polsToUndep);

        assertTrue(data.reconfigure(msg2));

        // some items only in deploy
        policies = new ArrayList<>(update.getPoliciesToBeDeployed());
        msg2.setPoliciesToBeDeployed(policies);
        data.getMessage().setPoliciesToBeDeployed(new LinkedList<>());
        assertTrue(data.reconfigure(msg2));
        assertThat(data.getMessage().getPoliciesToBeDeployed()).containsAll(msg2.getPoliciesToBeDeployed());

        // some items in both undeploy and newMessage.undeploy
        List<ToscaConceptIdentifier> pols = policies.stream().map(ToscaPolicy::getIdentifier)
            .collect(Collectors.toList());
        msg2.setPoliciesToBeUndeployed(pols);
        pols.add(makePolicy("policy-zz-1", "1.1.0").getIdentifier());
        data.getMessage().setPoliciesToBeUndeployed(pols);
        assertTrue(data.reconfigure(msg2));
        assertThat(data.getMessage().getPoliciesToBeUndeployed()).containsAll(msg2.getPoliciesToBeUndeployed());

        // some items in both undeploy and newMessage.deploy
        policies = new ArrayList<>(update.getPoliciesToBeDeployed());
        List<ToscaConceptIdentifier> polsToUndep2 = policies.parallelStream()
            .map(ToscaPolicy::getIdentifier).toList();
        data.getMessage().setPoliciesToBeUndeployed(polsToUndep2);

        List<ToscaPolicy> polsToDep2 = new LinkedList<>();
        polsToDep2.add(makePolicy("policy-m-1", "1.0.0"));
        polsToDep2.add(makePolicy("policy-n-1", "1.0.0"));
        msg2.setPoliciesToBeDeployed(polsToDep2);

        assertTrue(data.reconfigure(msg2));

        List<ToscaConceptIdentifier> dataPols2 = data.getMessage().getPoliciesToBeDeployed().stream()
            .map(ToscaPolicy::getIdentifier)
            .toList();
        assertThat(data.getMessage().getPoliciesToBeUndeployed())
            .doesNotContainAnyElementsOf(dataPols2);

        // some items only in undeploy
        pols = policies.stream().map(ToscaPolicy::getIdentifier).toList();
        msg2.setPoliciesToBeUndeployed(pols);
        data.getMessage().setPoliciesToBeUndeployed(new LinkedList<>());
        assertTrue(data.reconfigure(msg2));
        assertThat(data.getMessage().getPoliciesToBeUndeployed()).containsAll(msg2.getPoliciesToBeUndeployed());
    }

    @Test
    void testIsSameContent() {
        data = new UpdateReq(reqParams, MY_REQ_NAME, update);
        data.setNotifier(notifier);

        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setName("world");
        update.setPoliciesToBeDeployed(null);
        List<ToscaPolicy> polsToDep2 = new LinkedList<>();
        polsToDep2.add(makePolicy("policy-m-1", "1.0.0"));
        polsToDep2.add(makePolicy("policy-n-1", "1.0.0"));
        msg2.setPoliciesToBeDeployed(polsToDep2);
        assertThat(data.isSameContent(msg2)).isFalse();

        // both policy lists null
        msg2.setPoliciesToBeDeployed(null);
        assertEquals(data.getMessage().getPoliciesToBeDeployed(), msg2.getPoliciesToBeDeployed());
    }

    @Test
    void testIsSameContent_BothGroupNamesNull() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpGroup(null);
        update.setPdpGroup(null);
        assertEquals(data.getMessage().getPdpGroup(), msg2.getPdpGroup());
    }

    @Test
    void testIsSameContent_BothSubGroupNamesNull() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpSubgroup(null);
        update.setPdpSubgroup(null);
        assertEquals(data.getMessage().getPdpSubgroup(), msg2.getPdpSubgroup());
    }

    @Test
    void testIsSameContent_DiffGroup() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpGroup(null);
        assertFalse(data.isSameContent(msg2));

        msg2.setPdpGroup(DIFFERENT);
        assertFalse(data.isSameContent(msg2));

        update.setPdpGroup(null);
        assertFalse(data.isSameContent(msg2));
    }

    @Test
    void testIsSameContent_DiffSubGroup() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpSubgroup(null);
        assertFalse(data.isSameContent(msg2));

        msg2.setPdpSubgroup(DIFFERENT);
        assertFalse(data.isSameContent(msg2));

        update.setPdpSubgroup(null);
        assertFalse(data.isSameContent(msg2));
    }

    @Test
    void testIsSameContent_DiffPolicies() {
        PdpUpdate msg2 = new PdpUpdate(update);

        ArrayList<ToscaPolicy> policies = new ArrayList<>(update.getPoliciesToBeDeployed());
        policies.set(0, makePolicy(DIFFERENT, "10.0.0"));
        msg2.setPoliciesToBeDeployed(policies);

        assertFalse(data.isSameContent(msg2));
    }

    @Test
    void testIsSameContent_DiffPolicies_NotNull_Null() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPoliciesToBeDeployed(null);

        assertFalse(data.isSameContent(msg2));
    }

    @Test
    void testIsSameContent_DiffPolicies_Null_NotNull() {
        final PdpUpdate msg2 = new PdpUpdate(update);

        update.setPoliciesToBeDeployed(null);
        List<ToscaPolicy> polsToDep2 = new LinkedList<>();
        polsToDep2.add(makePolicy("policy-m-1", "1.0.0"));
        polsToDep2.add(makePolicy("policy-n-1", "1.0.0"));
        msg2.setPoliciesToBeDeployed(polsToDep2);

        assertThat(data.isSameContent(msg2)).isFalse();
    }

    @SuppressWarnings("unchecked")
    private void verifyResponse() {
        verify(notifier).processResponse(any(), any(), any(Set.class), any(Set.class));
    }

    @SuppressWarnings("unchecked")
    private void verifyNoResponse() {
        verify(notifier, never()).processResponse(any(), any(), any(Set.class), any(Set.class));
    }

    /**
     * Makes an update message.
     *
     * @return a new update message
     */
    private PdpUpdate makeUpdate() {
        PdpUpdate upd = new PdpUpdate();

        upd.setDescription("update-description");
        upd.setName(MY_NAME);
        upd.setPdpGroup(MY_GROUP);
        upd.setPdpSubgroup(MY_SUBGROUP);

        ToscaPolicy policy1 = makePolicy("policy-1-a", "1.0.0");
        ToscaPolicy policy2 = makePolicy("policy-2-a", "1.1.0");

        upd.setPoliciesToBeDeployed(Arrays.asList(policy1, policy2));
        upd.setPoliciesToBeUndeployed(Collections.emptyList());

        return upd;
    }
}
