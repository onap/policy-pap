/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Bell Canada. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.concepts.PolicyAudit.AuditAction;
import org.onap.policy.models.pap.persistence.provider.PolicyAuditProvider.AuditFilter;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class TestPolicyAuditProvider extends ProviderSuper {
    private static final String TEST_GROUP = "testGroup";
    private static final String TEST_PDP_TYPE = "testPdpType";
    private static final ToscaConceptIdentifier POLICY_A = new ToscaConceptIdentifier("PolicyA", "1.0.0");
    private static final ToscaConceptIdentifier POLICY_B = new ToscaConceptIdentifier("PolicyB", "2.0.0");

    private PolicyAuditProvider provider;

    @AfterClass
    public static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    @Override
    @Before
    public void setUp() throws Exception {

        super.setUp();
        provider = new PolicyAuditProvider();
    }

    @Test
    public void testGetAuditRecords() throws PfModelException {

        AuditFilter auditFilter = AuditFilter.builder().recordNum(5).fromDate(null).toDate(null).build();

        buildAuditRecords(auditFilter);

        List<PolicyAudit> result = new ArrayList<>(provider.getAuditRecords(auditFilter));
        validateAuditRecords(result, 2);
    }

    private void buildAuditRecords(AuditFilter auditFilter) {
        PolicyAudit audit1 = PolicyAudit.builder().auditId(123L).pdpGroup(TEST_GROUP).pdpType(TEST_PDP_TYPE)
                        .policy(POLICY_A).action(AuditAction.DEPLOYMENT).timestamp(Instant.now()).user(DEFAULT_USER)
                        .build();

        PolicyAudit audit2 = PolicyAudit.builder().auditId(456L).pdpGroup(TEST_GROUP).pdpType(TEST_PDP_TYPE)
                        .policy(POLICY_B).action(AuditAction.UNDEPLOYMENT).timestamp(Instant.now()).user(DEFAULT_USER)
                        .build();

        if (auditFilter.getName() == null) {
            when(dao.getAuditRecords(auditFilter)).thenReturn(List.of(audit1, audit2));
        } else {
            when(dao.getAuditRecords(auditFilter)).thenReturn(List.of(audit1));
        }

    }

    private void validateAuditRecords(List<PolicyAudit> result, int count) {
        assertThat(result).hasSize(count);
        for (PolicyAudit audit : result) {
            if (audit.getAuditId() == 123L) {
                assertThat(audit.getPdpGroup()).isEqualTo(TEST_GROUP);
                assertThat(audit.getPdpType()).isEqualTo(TEST_PDP_TYPE);
                assertThat(audit.getPolicy()).isEqualTo(POLICY_A);
                assertThat(audit.getAction()).isEqualTo(AuditAction.DEPLOYMENT);
                assertThat(audit.getUser()).isEqualTo(DEFAULT_USER);
            } else if (audit.getAuditId() == 456L) {
                assertThat(audit.getPdpGroup()).isEqualTo(TEST_GROUP);
                assertThat(audit.getPdpType()).isEqualTo(TEST_PDP_TYPE);
                assertThat(audit.getPolicy()).isEqualTo(POLICY_B);
                assertThat(audit.getAction()).isEqualTo(AuditAction.UNDEPLOYMENT);
                assertThat(audit.getUser()).isEqualTo(DEFAULT_USER);
            }
        }
    }
}
