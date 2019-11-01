/*-
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

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.notification.PolicyNotifier;

public class TestPolicyStatusProvider {

    @Mock
    private PolicyStatus status1;

    @Mock
    private PolicyNotifier notifier;

    private PolicyStatusProvider provider;

    /**
     * Creates various objects, including {@link #provider}.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Registry.newRegistry();
        Registry.register(PapConstants.REG_POLICY_NOTIFIER, notifier);

        provider = new PolicyStatusProvider();
    }

    @Test
    public void testGetStatus() {
        List<PolicyStatus> statusList = Arrays.asList(status1);
        when(notifier.getStatus()).thenReturn(statusList);

        assertSame(statusList, provider.getStatus());
    }

    @Test
    public void testGetStatusString() {
        List<PolicyStatus> statusList = Arrays.asList(status1);
        when(notifier.getStatus("a policy")).thenReturn(statusList);

        assertSame(statusList, provider.getStatus("a policy"));
    }

    @Test
    public void testGetStatusToscaPolicyIdentifier() {
        ToscaPolicyIdentifier policy1 = new ToscaPolicyIdentifier("my-id-a", "1.2.0");
        Optional<PolicyStatus> status = Optional.of(status1);
        when(notifier.getStatus(policy1)).thenReturn(status);

        assertSame(status, provider.getStatus(policy1));
    }

}
