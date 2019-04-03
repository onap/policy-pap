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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.comm.TimerManager;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.onap.policy.pap.main.parameters.PdpUpdateParameters;

public class UpdateDataTest {
    private static final String MY_NAME = "my-name";
    private static final String DIFFERENT = "different";
    private static final int RETRIES = 1;

    private MyData data;
    private PdpModifyRequestMapParams params;
    private TimerManager timers;
    private PdpUpdate update;
    private PdpStatus response;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        timers = mock(TimerManager.class);
        response = new PdpStatus();
        PdpParameters pdpParams = mock(PdpParameters.class);
        PdpUpdateParameters stateParams = mock(PdpUpdateParameters.class);

        when(stateParams.getMaxRetryCount()).thenReturn(RETRIES);
        when(pdpParams.getUpdateParameters()).thenReturn(stateParams);

        params = new PdpModifyRequestMapParams().setParams(pdpParams).setUpdateTimers(timers);

        update = makeUpdate();

        response.setName(MY_NAME);
        response.setPdpGroup(update.getPdpGroup());
        response.setPdpSubgroup(update.getPdpSubgroup());
        response.setPolicies(convertToscaPolicyToToscaPolicyIndentifier(update.getPolicies()));

        data = new MyData(update);
    }

    @Test
    public void testGetMaxRetryCount() {
        assertEquals(RETRIES, data.getMaxRetryCount());
    }

    @Test
    public void testGetTimers() {
        assertSame(timers, data.getTimers());
    }

    @Test
    public void testUpdateCheckResponse() {
        assertNull(data.checkResponse(response));
    }

    @Test
    public void testUpdateDataCheckResponse_MismatchedName() {
        response.setName(DIFFERENT);

        assertEquals("name does not match", data.checkResponse(response));
    }

    @Test
    public void testUpdateDataCheckResponse_MismatchedGroup() {
        response.setPdpGroup(DIFFERENT);

        assertEquals("group does not match", data.checkResponse(response));
    }

    @Test
    public void testUpdateDataCheckResponse_MismatchedSubGroup() {
        response.setPdpSubgroup(DIFFERENT);

        assertEquals("subgroup does not match", data.checkResponse(response));
    }

    @Test
    public void testUpdateDataCheckResponse_MismatchedPoliciesLength() {
        response.setPolicies(convertToscaPolicyToToscaPolicyIndentifier(Arrays.asList(update.getPolicies().get(0))));

        assertEquals("policies do not match", data.checkResponse(response));
    }

    @Test
    public void testUpdateDataCheckResponse_MismatchedPolicies() {
        ArrayList<ToscaPolicyIdentifier> policies =
                new ArrayList<>(convertToscaPolicyToToscaPolicyIndentifier(update.getPolicies()));
        policies.set(0, new ToscaPolicyIdentifier(DIFFERENT, "10.0.0"));

        response.setPolicies(policies);

        assertEquals("policies do not match", data.checkResponse(response));
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
        upd.setPdpGroup("group1-a");
        upd.setPdpSubgroup("sub1-a");

        ToscaPolicy policy1 = makePolicy("policy-1-a", "1.0.0");
        ToscaPolicy policy2 = makePolicy("policy-2-a", "1.1.0");

        upd.setPolicies(Arrays.asList(policy1, policy2));

        return upd;
    }

    /**
     * Creates a new policy.
     *
     * @param name policy name
     * @param version policy version
     * @return a new policy
     */
    private ToscaPolicy makePolicy(String name, String version) {
        ToscaPolicy policy = new ToscaPolicy();

        policy.setName(name);
        policy.setVersion(version);

        return policy;
    }

    private class MyData extends UpdateData {

        public MyData(PdpUpdate message) {
            super(message, params);
        }

        @Override
        public void mismatch(String reason) {
            // do nothing
        }

        @Override
        public void completed() {
            // do nothing
        }
    }
    
    /**
     * Converts a ToscaPolicy list to ToscaPolicyIdentifier list.
     *
     * @param toscaPolicies the list of ToscaPolicy
     * @return the ToscaPolicyIdentifier list
     */
    private List<ToscaPolicyIdentifier> convertToscaPolicyToToscaPolicyIndentifier(List<ToscaPolicy> toscaPolicies) {
        final List<ToscaPolicyIdentifier> toscaPolicyIdentifiers = new ArrayList<>();
        for (final ToscaPolicy toscaPolicy : toscaPolicies) {
            toscaPolicyIdentifiers.add(new ToscaPolicyIdentifier(toscaPolicy.getName(), toscaPolicy.getVersion()));
        }
        return toscaPolicyIdentifiers;
    }
}
