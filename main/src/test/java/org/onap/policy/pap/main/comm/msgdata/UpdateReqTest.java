/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.comm.CommonRequestBase;

public class UpdateReqTest extends CommonRequestBase {

    private UpdateReq data;
    private PdpUpdate update;
    private PdpStatus response;

    /**
     * Sets up.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        response = new PdpStatus();

        update = makeUpdate();

        response.setName(MY_NAME);
        response.setPdpGroup(update.getPdpGroup());
        response.setPdpSubgroup(update.getPdpSubgroup());
        response.setPolicies(
                        update.getPolicies().stream().map(ToscaPolicy::getIdentifier).collect(Collectors.toList()));

        data = new UpdateReq(reqParams, MY_REQ_NAME, update);
        data.setNotifier(notifier);
    }

    @Test
    public void testGetMessage() {
        assertEquals(MY_REQ_NAME, data.getName());
        assertSame(update, data.getMessage());
    }

    @Test
    public void testCheckResponse() {
        assertNull(data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyResponse();

        // both policy lists null
        update.setPolicies(null);
        response.setPolicies(null);
        assertNull(data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
    }

    @Test
    public void testCheckResponse_NullName() {
        response.setName(null);

        assertEquals("null PDP name", data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyNoResponse();
    }

    @Test
    public void testCheckResponse_NullMsgName() {
        update.setName(null);

        assertEquals(null, data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyResponse();
    }

    @Test
    public void testUpdateReqCheckResponse_MismatchedGroup() {
        response.setPdpGroup(DIFFERENT);

        assertEquals("group does not match", data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyResponse();
    }

    @Test
    public void testUpdateReqCheckResponse_MismatchedSubGroup() {
        response.setPdpSubgroup(DIFFERENT);

        assertEquals("subgroup does not match", data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyResponse();
    }

    @Test
    public void testCheckResponse_NullSubGroup() {
        update.setPdpSubgroup(null);
        response.setPdpSubgroup(null);

        // different policy list - should have no impact
        response.setPolicies(Collections.emptyList());

        assertEquals(null, data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyResponse();
    }

    @Test
    public void testUpdateReqCheckResponse_MismatchedPolicies() {
        ArrayList<ToscaPolicy> policies = new ArrayList<>(update.getPolicies());
        policies.set(0, makePolicy(DIFFERENT, "10.0.0"));

        response.setPolicies(policies.stream().map(ToscaPolicy::getIdentifier).collect(Collectors.toList()));

        assertEquals("policies do not match", data.checkResponse(response));
        verifyResponse();

        // the first policy from the original update is all that should be undeployed
        assertEquals(Collections.singleton(update.getPolicies().get(0).getIdentifier()).toString(),
                        data.getUndeployPolicies().toString());
    }

    @Test
    public void testUpdateReqCheckResponse_MismatchedPolicies_Null_NotNull() {
        update.setPolicies(null);

        assertEquals("policies do not match", data.checkResponse(response));
        assertTrue(data.getUndeployPolicies().isEmpty());
        verifyResponse();
    }

    @Test
    public void testUpdateReqCheckResponse_MismatchedPolicies_NotNull_Null() {
        response.setPolicies(null);

        assertEquals("policies do not match", data.checkResponse(response));
        verifyResponse();

        // all policies in the update should be undeployed
        assertEquals(update.getPolicies().stream().map(ToscaPolicy::getIdentifier).collect(Collectors.toList())
                        .toString(), new TreeSet<>(data.getUndeployPolicies()).toString());
    }

    @Test
    public void testReconfigure() {
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
    }

    @Test
    public void testIsSameContent() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setName("world");
        assertTrue(data.isSameContent(msg2));

        // both policy lists null
        update.setPolicies(null);
        msg2.setPolicies(null);
        assertTrue(data.isSameContent(msg2));
    }

    @Test
    public void testIsSameContent_BothGroupNamesNull() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpGroup(null);
        update.setPdpGroup(null);
        assertTrue(data.isSameContent(msg2));
    }

    @Test
    public void testIsSameContent_BothSubGroupNamesNull() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpSubgroup(null);
        update.setPdpSubgroup(null);
        assertTrue(data.isSameContent(msg2));
    }

    @Test
    public void testIsSameContent_DiffGroup() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpGroup(null);
        assertFalse(data.isSameContent(msg2));

        msg2.setPdpGroup(DIFFERENT);
        assertFalse(data.isSameContent(msg2));

        update.setPdpGroup(null);
        assertFalse(data.isSameContent(msg2));
    }

    @Test
    public void testIsSameContent_DiffSubGroup() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpSubgroup(null);
        assertFalse(data.isSameContent(msg2));

        msg2.setPdpSubgroup(DIFFERENT);
        assertFalse(data.isSameContent(msg2));

        update.setPdpSubgroup(null);
        assertFalse(data.isSameContent(msg2));
    }

    @Test
    public void testIsSameContent_DiffPolicies() {
        PdpUpdate msg2 = new PdpUpdate(update);

        ArrayList<ToscaPolicy> policies = new ArrayList<>(update.getPolicies());
        policies.set(0, makePolicy(DIFFERENT, "10.0.0"));
        msg2.setPolicies(policies);

        assertFalse(data.isSameContent(msg2));
    }

    @Test
    public void testIsSameContent_DiffPolicies_NotNull_Null() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPolicies(null);

        assertFalse(data.isSameContent(msg2));
    }

    @Test
    public void testIsSameContent_DiffPolicies_Null_NotNull() {
        PdpUpdate msg2 = new PdpUpdate(update);

        update.setPolicies(null);

        assertFalse(data.isSameContent(msg2));
    }

    @SuppressWarnings("unchecked")
    private void verifyResponse() {
        verify(notifier).processResponse(any(), any(Set.class));
    }

    @SuppressWarnings("unchecked")
    private void verifyNoResponse() {
        verify(notifier, never()).processResponse(any(), any(Set.class));
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

        upd.setPolicies(Arrays.asList(policy1, policy2));

        return upd;
    }
}
