/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2022-2023 Nordix Foundation.
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.onap.policy.pap.main.rest.CommonPapRestServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class PolicyStatusServiceTest extends CommonPapRestServer {

    private static final String GROUP_A = "groupA";
    private static final String GROUP_B = "groupB";
    private static final ToscaConceptIdentifier MY_POLICY = new ToscaConceptIdentifier("MyPolicy", "1.2.3");
    private static final ToscaConceptIdentifier MY_POLICY2 = new ToscaConceptIdentifier("MyPolicyB", "2.3.4");

    @Autowired
    private PolicyStatusService policyStatusService;

    private PdpPolicyStatus.PdpPolicyStatusBuilder statusBuilder;

    /**
     * Setup before tests.
     *
     * @throws Exception the exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        ToscaConceptIdentifier policyType = new ToscaConceptIdentifier("MyPolicyType", "1.2.4");

        statusBuilder = PdpPolicyStatus.builder().deploy(true).pdpType("MyPdpType").policy(MY_POLICY)
            .policyType(policyType).state(PdpPolicyStatus.State.SUCCESS);
    }

    @Test
    public void testGetAllPolicyStatus() {
        assertThat(policyStatusService.getAllPolicyStatus()).isEmpty();

        var statusList = createStatusList();
        policyStatusService.cudPolicyStatus(statusList, null, null);
        assertThat(policyStatusService.getAllPolicyStatus()).hasSize(5);
        policyStatusService.cudPolicyStatus(null, null, statusList);
    }

    @Test
    public void testGetAllPolicyStatusPfDaoToscaConceptIdentifierOptVersion() {

        assertThatThrownBy(() -> {
            policyStatusService.getAllPolicyStatus(null);
        }).hasMessageContaining("policy").hasMessageContaining("null");

        assertThat(policyStatusService.getAllPolicyStatus(new ToscaConceptIdentifierOptVersion("somePdp", null)))
            .isEmpty();

        var statusList = createStatusList();
        policyStatusService.cudPolicyStatus(statusList, null, null);

        assertThat(policyStatusService.getAllPolicyStatus(new ToscaConceptIdentifierOptVersion(MY_POLICY))).hasSize(2);
        assertThat(
            policyStatusService.getAllPolicyStatus(new ToscaConceptIdentifierOptVersion(MY_POLICY.getName(), null)))
                .hasSize(3);
        policyStatusService.cudPolicyStatus(null, null, statusList);
    }

    @Test
    public void testGetGroupPolicyStatus() {

        assertThatThrownBy(() -> {
            policyStatusService.getGroupPolicyStatus(null);
        }).hasMessage("pdpGroup is marked non-null but is null");

        assertThat(policyStatusService.getGroupPolicyStatus("PdpGroup0")).isEmpty();

        var statusList = createStatusList();
        policyStatusService.cudPolicyStatus(statusList, null, null);
        assertThat(policyStatusService.getGroupPolicyStatus(GROUP_A)).hasSize(3);
        policyStatusService.cudPolicyStatus(null, null, statusList);
    }

    @Test
    public void testCudPolicyStatus() {
        assertThatCode(() -> policyStatusService.cudPolicyStatus(null, null, null)).doesNotThrowAnyException();

        assertThatThrownBy(() -> {
            policyStatusService.cudPolicyStatus(List.of(new PdpPolicyStatus()), null, null);
        }).isInstanceOf(PfModelRuntimeException.class);
        PdpPolicyStatus invalidStatus = statusBuilder.state(PdpPolicyStatus.State.WAITING).deploy(false).pdpGroup(null)
            .pdpId("pdp1").policy(MY_POLICY).build();
        assertThatThrownBy(() -> {
            policyStatusService.cudPolicyStatus(List.of(invalidStatus), null, null);
        }).isInstanceOf(PfModelRuntimeException.class)
            .hasMessageContaining("item \"pdpGroup\" value \"null\" INVALID, is null");

        // Test create
        PdpPolicyStatus status = statusBuilder.state(PdpPolicyStatus.State.WAITING).deploy(false).pdpGroup(GROUP_A)
            .pdpId("pdp1").policy(MY_POLICY).build();
        policyStatusService.cudPolicyStatus(List.of(status), null, null);
        List<PdpPolicyStatus> createdStatusList = policyStatusService.getAllPolicyStatus();
        assertThat(createdStatusList).hasSize(1);
        assertThat(createdStatusList.get(0).getState()).isEqualTo(PdpPolicyStatus.State.WAITING);

        // Test update
        status.setDeploy(true);
        status.setState(PdpPolicyStatus.State.SUCCESS);
        policyStatusService.cudPolicyStatus(null, List.of(status), null);
        createdStatusList = policyStatusService.getAllPolicyStatus();
        assertThat(createdStatusList).hasSize(1);
        assertThat(createdStatusList.get(0).getState()).isEqualTo(PdpPolicyStatus.State.SUCCESS);

        // Test delete
        policyStatusService.cudPolicyStatus(null, null, List.of(status));
        assertThat(policyStatusService.getAllPolicyStatus()).isEmpty();
    }

    private List<PdpPolicyStatus> createStatusList() {
        // same name, different version
        final ToscaConceptIdentifier policy3 = new ToscaConceptIdentifier(MY_POLICY.getName(), "10.20.30");

        PdpPolicyStatus id1 = statusBuilder.pdpGroup(GROUP_A).pdpId("pdp1").policy(MY_POLICY).build();
        PdpPolicyStatus id2 = statusBuilder.pdpGroup(GROUP_A).pdpId("pdp2").policy(MY_POLICY2).build();
        PdpPolicyStatus id3 = statusBuilder.pdpGroup(GROUP_A).pdpId("pdp3").policy(policy3).build();
        PdpPolicyStatus id4 = statusBuilder.pdpGroup(GROUP_B).pdpId("pdp4").policy(MY_POLICY).build();
        PdpPolicyStatus id5 = statusBuilder.pdpGroup(GROUP_B).pdpId("pdp5").policy(MY_POLICY2).build();
        return List.of(id1, id2, id3, id4, id5);
    }
}
