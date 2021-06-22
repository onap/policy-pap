/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.concepts.PolicyAudit.AuditAction;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.State;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;


public class TestPolicyAuditManager extends ProviderSuper {

    private static final ToscaConceptIdentifier MY_POLICY = new ToscaConceptIdentifier("myPolicy", "1.0.0");
    private static final String GROUP_A = "pdpGroup-A";
    private static final String GROUP_B = "pdpGroup-B";
    private static final String PDP_TYPE = "typeABC";

    PolicyAuditManager auditManager;

    /**
     * Setup the test variables.
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        auditManager = new PolicyAuditManager(dao);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    @Test
    public void testDeployments() {
        auditManager.addDeploymentAudit(MY_POLICY, GROUP_A, PDP_TYPE);
        auditManager.addUndeploymentAudit(MY_POLICY, GROUP_B, PDP_TYPE);

        assertThat(auditManager.getDeployments()).hasSize(2);

        auditManager.saveDeploymentsAudits();

        assertThat(auditManager.getDeployments()).isEmpty();
    }

    @Test
    public void testCheckForFailure() {
        PdpPolicyStatus status1 = PdpPolicyStatus.builder().policy(MY_POLICY).state(State.FAILURE).build();
        PdpPolicyStatus status2 = PdpPolicyStatus.builder().policy(MY_POLICY).state(State.SUCCESS).build();

        auditManager.checkForFailure(status1);
        auditManager.checkForFailure(status2);

        assertThat(auditManager.getDeployments()).hasSize(1);
        auditManager.saveDeploymentsAudits();

        assertThat(auditManager.getDeployments()).isEmpty();
    }

    @Test
    public void testGetPolicyAudits() {
        PolicyAudit audit = PolicyAudit.builder().action(AuditAction.DEPLOYMENT).pdpGroup(GROUP_A).pdpType(PDP_TYPE)
                .policy(MY_POLICY).timestamp(Instant.now().truncatedTo(ChronoUnit.SECONDS)).user("user").build();

        // mock for first get, then second get
        when(dao.getAuditRecords(any())).thenReturn(List.of(audit)).thenReturn(List.of());

        List<PolicyAudit> audits = auditManager.getPolicyAudits(MY_POLICY, null, GROUP_A, null, null, 10);
        assertThat(audits).hasSize(1);

        // coverage for policy == null
        audits = auditManager.getPolicyAudits(null, null, GROUP_A, null, null, 10);
        assertThat(audits).isEmpty();
    }
}
