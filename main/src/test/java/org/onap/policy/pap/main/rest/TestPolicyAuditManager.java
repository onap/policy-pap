/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021, 2023 Nordix Foundation.
 *  Modifications Copyright (C) 2022 Bell Canada. All rights reserved.
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PolicyAudit.AuditAction;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class TestPolicyAuditManager extends ProviderSuper {

    private static final ToscaConceptIdentifier MY_POLICY = new ToscaConceptIdentifier("myPolicy", "1.0.0");
    private static final String GROUP_A = "pdpGroup-A";
    private static final String GROUP_B = "pdpGroup-B";
    private static final String PDP_TYPE = "typeABC";
    private static final String USER = "healthcheck";

    PolicyAuditManager auditManager;

    /**
     * Setup the test variables.
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        auditManager = new PolicyAuditManager(policyAuditService);
    }

    @AfterAll
    public static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    @Test
    void testDeployments() {
        auditManager.addDeploymentAudit(MY_POLICY, GROUP_A, PDP_TYPE, USER);
        auditManager.addUndeploymentAudit(MY_POLICY, GROUP_B, PDP_TYPE, USER);

        assertThat(auditManager.getAuditRecords()).hasSize(2);
        assertEquals(AuditAction.DEPLOYMENT, auditManager.getAuditRecords().get(0).getAction());
        assertEquals(AuditAction.UNDEPLOYMENT, auditManager.getAuditRecords().get(1).getAction());

        auditManager.saveRecordsToDb();

        assertThat(auditManager.getAuditRecords()).isEmpty();
    }

    @Test
    void testSaveRecordsToDb_EmptyList() {
        assertThat(auditManager.getAuditRecords()).isEmpty();
        ;
        auditManager.saveRecordsToDb();

        assertThatCode(() -> auditManager.saveRecordsToDb()).doesNotThrowAnyException();
    }

    @Test
    void testSaveRecordsToDb_Exception() {
        auditManager.addDeploymentAudit(MY_POLICY, GROUP_A, PDP_TYPE, USER);

        assertThat(auditManager.getAuditRecords()).hasSize(1);

        doThrow(PfModelRuntimeException.class).when(policyAuditService).createAuditRecords(any());
        auditManager.saveRecordsToDb();

        assertThat(auditManager.getAuditRecords()).isNotEmpty();
    }
}
