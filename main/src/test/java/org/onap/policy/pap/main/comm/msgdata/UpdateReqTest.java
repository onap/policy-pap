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

package org.onap.policy.pap.main.comm.msgdata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
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

        // both policy lists null
        update.setPolicies(null);
        response.setPolicies(null);
        assertNull(data.checkResponse(response));
    }

    @Test
    public void testCheckResponse_NullName() {
        response.setName(null);

        assertEquals("null PDP name", data.checkResponse(response));
    }

    @Test
    public void testCheckResponse_NullMsgName() {
        update.setName(null);

        assertEquals(null, data.checkResponse(response));
    }

    @Test
    public void testUpdateReqCheckResponse_MismatchedGroup() {
        response.setPdpGroup(DIFFERENT);

        assertEquals("group does not match", data.checkResponse(response));
    }

    @Test
    public void testUpdateReqCheckResponse_MismatchedSubGroup() {
        response.setPdpSubgroup(DIFFERENT);

        assertEquals("subgroup does not match", data.checkResponse(response));
    }

    @Test
    public void testUpdateReqCheckResponse_MismatchedPolicies() {
        ArrayList<ToscaPolicy> policies = new ArrayList<>(update.getPolicies());
        policies.set(0, makePolicy(DIFFERENT, "10.0.0"));

        response.setPolicies(policies.stream().map(ToscaPolicy::getIdentifier).collect(Collectors.toList()));

        assertEquals("policies do not match", data.checkResponse(response));
    }

    @Test
    public void testUpdateReqCheckResponse_MismatchedPolicies_Null_NotNull() {
        update.setPolicies(null);

        assertEquals("policies do not match", data.checkResponse(response));
    }

    @Test
    public void testUpdateReqCheckResponse_MismatchedPolicies_NotNull_Null() {
        response.setPolicies(null);

        assertEquals("policies do not match", data.checkResponse(response));
    }

    @Test
    public void isSameContent() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setName("world");
        assertTrue(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));

        // different request type
        assertFalse(data.isSameContent(new StateChangeReq(reqParams, MY_REQ_NAME, new PdpStateChange())));

        // both policy lists null
        update.setPolicies(null);
        msg2.setPolicies(null);
        assertTrue(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));
    }

    @Test
    public void isSameContent_BothGroupNamesNull() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpGroup(null);
        update.setPdpGroup(null);
        assertTrue(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));
    }

    @Test
    public void isSameContent_BothSubGroupNamesNull() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpSubgroup(null);
        update.setPdpSubgroup(null);
        assertTrue(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));
    }

    @Test
    public void isSameContent_DiffGroup() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpGroup(null);
        assertFalse(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));

        msg2.setPdpGroup(DIFFERENT);
        assertFalse(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));

        update.setPdpGroup(null);
        assertFalse(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));
    }

    @Test
    public void isSameContent_DiffSubGroup() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPdpSubgroup(null);
        assertFalse(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));

        msg2.setPdpSubgroup(DIFFERENT);
        assertFalse(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));

        update.setPdpSubgroup(null);
        assertFalse(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));
    }

    @Test
    public void isSameContent_DiffPolicies() {
        PdpUpdate msg2 = new PdpUpdate(update);

        ArrayList<ToscaPolicy> policies = new ArrayList<>(update.getPolicies());
        policies.set(0, makePolicy(DIFFERENT, "10.0.0"));
        msg2.setPolicies(policies);

        assertFalse(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));
    }

    @Test
    public void isSameContent_DiffPolicies_NotNull_Null() {
        PdpUpdate msg2 = new PdpUpdate(update);
        msg2.setPolicies(null);

        assertFalse(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));
    }

    @Test
    public void isSameContent_DiffPolicies_Null_NotNull() {
        PdpUpdate msg2 = new PdpUpdate(update);

        update.setPolicies(null);

        assertFalse(data.isSameContent(new UpdateReq(reqParams, MY_REQ_NAME, msg2)));
    }

    @Test
    public void testGetPriority() {
        assertTrue(data.getPriority() > new StateChangeReq(reqParams, MY_REQ_NAME, new PdpStateChange()).getPriority());
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
