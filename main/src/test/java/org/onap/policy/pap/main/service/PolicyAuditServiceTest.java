/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.pap.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.concepts.PolicyAudit.AuditAction;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.repository.PolicyAuditRepository;
import org.onap.policy.pap.main.rest.CommonPapRestServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class PolicyAuditServiceTest extends CommonPapRestServer {

    private static final String FIELD_IS_NULL = "%s is marked .*ull but is null";
    private static final String GROUP_A = "groupA";
    private static final String GROUP_B = "groupB";
    private static final ToscaConceptIdentifier MY_POLICY = new ToscaConceptIdentifier("MyPolicy", "1.2.3");
    private static final ToscaConceptIdentifier MY_POLICY2 = new ToscaConceptIdentifier("MyPolicyB", "2.3.4");
    private static final Integer NUMBER_RECORDS = 10;

    @Autowired
    private PolicyAuditService policyAuditService;

    @Autowired
    private PolicyAuditRepository policyAuditRepository;

    /**
     * Teardown after tests.
     */
    @Override
    @After
    public void tearDown() {
        policyAuditRepository.deleteAll();
    }

    @Test
    public void testCreateAuditRecordsSuccess() {
        policyAuditService.createAuditRecords(generatePolicyAudits(Instant.now(), GROUP_A, MY_POLICY));

        assertThat(policyAuditService.getAuditRecords(NUMBER_RECORDS, null, null)).hasSize(2);
    }

    @Test
    public void testCreatePolicyAuditFailure() {
        List<PolicyAudit> audits = List.of(
            PolicyAudit.builder().pdpType("pdpType").action(AuditAction.DEPLOYMENT).timestamp(Instant.now()).build());

        assertThrows(PfModelRuntimeException.class, () -> policyAuditService.createAuditRecords(audits));
        assertThatThrownBy(() -> {
            policyAuditService.createAuditRecords(audits);
        }).isInstanceOf(PfModelRuntimeException.class)
            .hasMessageContaining("\"createAuditRecords\" INVALID, item has status INVALID");

        assertThatThrownBy(() -> {
            policyAuditService.createAuditRecords(null);
        }).hasMessageMatching(String.format(FIELD_IS_NULL, "audits"));
    }

    @Test
    public void testGetAuditRecords() {
        Instant startDate1 = Instant.now();

        policyAuditService.createAuditRecords(generatePolicyAudits(startDate1, GROUP_A, MY_POLICY));

        assertThat(policyAuditService.getAuditRecords(NUMBER_RECORDS, null, null)).hasSize(2);

        assertThat(policyAuditService.getAuditRecords(1, null, null)).hasSize(1);

        // as the start date is 10 min ahead of first record, shouldn't return any records
        assertThat(policyAuditService.getAuditRecords(NUMBER_RECORDS, Instant.now().plusSeconds(600), null)).isEmpty();

        policyAuditService.createAuditRecords(generatePolicyAudits(Instant.now(), GROUP_B, MY_POLICY2));

        assertThat(policyAuditService.getAuditRecords(NUMBER_RECORDS, null, null)).hasSize(4);
        assertThat(policyAuditService.getAuditRecords(NUMBER_RECORDS, null, null)).hasSize(4);

        assertThat(policyAuditService.getAuditRecords(GROUP_A, NUMBER_RECORDS, null, null)).hasSize(2);
        assertThat(policyAuditService.getAuditRecords(GROUP_B, NUMBER_RECORDS, null, null)).hasSize(2);

        assertThat(policyAuditService.getAuditRecords(GROUP_A, MY_POLICY.getName(), MY_POLICY.getVersion(),
            NUMBER_RECORDS, null, null)).hasSize(2);
        assertThat(
            policyAuditService.getAuditRecords(GROUP_A, MY_POLICY.getName(), "9.9.9", NUMBER_RECORDS, null, null))
                .hasSize(0);
        assertThat(policyAuditService.getAuditRecords(GROUP_B, MY_POLICY.getName(), MY_POLICY.getVersion(),
            NUMBER_RECORDS, null, null)).hasSize(0);
        assertThat(policyAuditService.getAuditRecords(GROUP_B, MY_POLICY2.getName(), MY_POLICY2.getVersion(),
            NUMBER_RECORDS, null, null)).hasSize(2);
        assertThat(policyAuditService.getAuditRecords(MY_POLICY2.getName(), MY_POLICY2.getVersion(), NUMBER_RECORDS,
            null, null)).hasSize(2);
        assertThat(
            policyAuditService.getAuditRecords(MY_POLICY.getName(), MY_POLICY.getVersion(), NUMBER_RECORDS, null, null))
                .hasSize(2);

    }

    private List<PolicyAudit> generatePolicyAudits(Instant date, String group, ToscaConceptIdentifier policy) {
        PolicyAudit deploy = PolicyAudit.builder().pdpGroup(group).pdpType("pdpType").policy(policy)
            .action(AuditAction.DEPLOYMENT).timestamp(date.truncatedTo(ChronoUnit.SECONDS)).build();

        PolicyAudit undeploy = PolicyAudit.builder().pdpGroup(group).pdpType("pdpType").policy(policy)
            .action(AuditAction.UNDEPLOYMENT).timestamp(date.plusSeconds(1).truncatedTo(ChronoUnit.SECONDS)).build();

        return List.of(deploy, undeploy);
    }
}
