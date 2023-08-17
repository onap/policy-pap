/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021, 2023 Nordix Foundation.
 *  Modifications Copyright (C) 2020-2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2023 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.comm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.parameters.CommonTestData;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.onap.policy.pap.main.rest.e2e.End2EndBase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class to perform unit test of {@link PdpHeartbeatListener}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpHeartbeatListenerTest extends End2EndBase {

    private static final String POLICY_VERSION = "1.0.0";
    private static final String POLICY_NAME = "onap.policies.controlloop.operational.common.apex.SampleDomain";
    private static final String APEX_TYPE = "apex";
    private static final String DEFAULT_GROUP = "defaultGroup";
    private static final String PDP_NAME = "pdp_1";
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";

    @Autowired
    private PdpHeartbeatListener pdpHeartbeatListener;

    @Test
    public void testPdpHeartbeatListener() {
        addGroups("PdpGroups.json");
        PapParameterGroup parameterGroup = new PapParameterGroup();
        parameterGroup.setPdpParameters(new PdpParameters());

        // Testing pdp registration success case
        final PdpStatus status1 = new PdpStatus();
        status1.setName(PDP_NAME);
        status1.setState(PdpState.ACTIVE);
        status1.setPdpGroup(DEFAULT_GROUP);
        status1.setPdpType(APEX_TYPE);
        status1.setHealthy(PdpHealthStatus.HEALTHY);
        final List<ToscaConceptIdentifier> idents1 = List.of(new ToscaConceptIdentifier(POLICY_NAME, POLICY_VERSION));
        status1.setPolicies(idents1);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status1);
        verifyPdpGroup(1);

        // Testing pdp heartbeat success case
        final PdpStatus status2 = new PdpStatus();
        status2.setName(PDP_NAME);
        status2.setState(PdpState.ACTIVE);
        status2.setPdpGroup(DEFAULT_GROUP);
        status2.setPdpType(APEX_TYPE);
        status2.setHealthy(PdpHealthStatus.HEALTHY);
        status2.setPdpSubgroup(APEX_TYPE);
        final List<ToscaConceptIdentifier> idents2 = List.of(new ToscaConceptIdentifier(POLICY_NAME, POLICY_VERSION));
        status2.setPolicies(idents2);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status2);
        verifyPdpGroup(1);

        // Testing pdp heartbeat failure case with pdp missing
        final PdpStatus status3 = new PdpStatus();
        status3.setName("pdp_2");
        status3.setState(PdpState.ACTIVE);
        status3.setPdpGroup(DEFAULT_GROUP);
        status3.setPdpType(APEX_TYPE);
        status3.setHealthy(PdpHealthStatus.HEALTHY);
        status3.setPdpSubgroup(APEX_TYPE);
        final List<ToscaConceptIdentifier> idents3 = List.of(new ToscaConceptIdentifier(POLICY_NAME, POLICY_VERSION));
        status3.setPolicies(idents3);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status3);
        verifyPdpGroup(2);

        // Testing pdp registration failure case
        final PdpStatus status4 = new PdpStatus();
        status4.setName("pdp_3");
        status4.setState(PdpState.ACTIVE);
        status4.setPdpGroup("wrongGroup");
        status4.setPdpType(APEX_TYPE);
        status4.setHealthy(PdpHealthStatus.HEALTHY);
        final List<ToscaConceptIdentifier> idents4 = List.of(new ToscaConceptIdentifier(POLICY_NAME, POLICY_VERSION));
        status4.setPolicies(idents4);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status4);
        verifyPdpGroup(2);

        // Testing pdp heartbeat failure case with pdp state mismatch
        final PdpStatus status5 = new PdpStatus();
        status5.setName(PDP_NAME);
        status5.setState(PdpState.PASSIVE);
        status5.setPdpGroup(DEFAULT_GROUP);
        status5.setPdpType(APEX_TYPE);
        status5.setHealthy(PdpHealthStatus.HEALTHY);
        status5.setPdpSubgroup(APEX_TYPE);
        final List<ToscaConceptIdentifier> idents5 = List.of(new ToscaConceptIdentifier(POLICY_NAME, POLICY_VERSION));
        status5.setPolicies(idents5);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status5);
        verifyPdpGroup(2);

        // Testing pdp heartbeat failure case with pdp policies mismatch
        final PdpStatus status6 = new PdpStatus();
        status6.setName(PDP_NAME);
        status6.setState(PdpState.ACTIVE);
        status6.setPdpGroup(DEFAULT_GROUP);
        status6.setPdpType(APEX_TYPE);
        status6.setHealthy(PdpHealthStatus.HEALTHY);
        status6.setPdpSubgroup(APEX_TYPE);
        final List<ToscaConceptIdentifier> idents6 =
                Arrays.asList(new ToscaConceptIdentifier(POLICY_NAME, POLICY_VERSION),
                        new ToscaConceptIdentifier("onap.restart.tca", POLICY_VERSION));
        status6.setPolicies(idents6);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status6);
        verifyPdpGroup(2);

        // Testing pdp heartbeat failure case with pdp no policies
        final PdpStatus status7 = new PdpStatus();
        status7.setName(PDP_NAME);
        status7.setState(PdpState.ACTIVE);
        status7.setPdpGroup(DEFAULT_GROUP);
        status7.setPdpType(APEX_TYPE);
        status7.setHealthy(PdpHealthStatus.HEALTHY);
        status7.setPdpSubgroup(APEX_TYPE);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status7);
        verifyPdpGroup(2);

        // Testing old message for pdp_1 - should have no effect
        final PdpStatus status7b = new PdpStatus();
        status7b.setTimestampMs(System.currentTimeMillis() - PdpParameters.DEFAULT_MAX_AGE_MS - 1);
        status7b.setName(PDP_NAME);
        status7b.setState(PdpState.TERMINATED);
        status7b.setPdpGroup(DEFAULT_GROUP);
        status7b.setPdpType(APEX_TYPE);
        status7b.setPdpSubgroup(APEX_TYPE);
        status7b.setHealthy(PdpHealthStatus.HEALTHY);
        final List<ToscaConceptIdentifier> idents7b = List.of(new ToscaConceptIdentifier(POLICY_NAME, POLICY_VERSION));
        status7b.setPolicies(idents7b);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status7b);
        verifyPdpGroup(2);

        // Testing pdp termination case for pdp_1
        final PdpStatus status8 = new PdpStatus();
        status8.setName(PDP_NAME);
        status8.setState(PdpState.TERMINATED);
        status8.setPdpGroup(DEFAULT_GROUP);
        status8.setPdpType(APEX_TYPE);
        status8.setPdpSubgroup(APEX_TYPE);
        status8.setHealthy(PdpHealthStatus.HEALTHY);
        final List<ToscaConceptIdentifier> idents8 = List.of(new ToscaConceptIdentifier(POLICY_NAME, POLICY_VERSION));
        status8.setPolicies(idents8);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status8);
        verifyPdpGroup(1);

        // Testing pdp termination case for pdp_2
        final PdpStatus status9 = new PdpStatus();
        status9.setName("pdp_2");
        status9.setState(PdpState.TERMINATED);
        status9.setPdpGroup(DEFAULT_GROUP);
        status9.setPdpType(APEX_TYPE);
        status9.setPdpSubgroup(APEX_TYPE);
        status9.setHealthy(PdpHealthStatus.HEALTHY);
        final List<ToscaConceptIdentifier> idents9 = List.of(new ToscaConceptIdentifier(POLICY_NAME, POLICY_VERSION));
        status9.setPolicies(idents9);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status9);
        verifyPdpGroup(0);

        // Test policy lists updated in createUpdate
        ToscaPolicy polA = new ToscaPolicy();
        polA.setName("pol-a-1.1.1");
        polA.setVersion("1.1.1");
        ToscaPolicy polB = new ToscaPolicy();
        polB.setName("pol-b-1.1.1");
        polB.setVersion("1.1.1");
        List<ToscaPolicy> policies = new ArrayList<>();
        policies.add(polA);
        policies.add(polB);
        final PapParameterGroup testGroup = new CommonTestData().getPapParameterGroup(1);
        List<ToscaConceptIdentifier> polsUndep =
            policies.stream().map(ToscaPolicy::getIdentifier).collect(Collectors.toList());
        PdpStatusMessageHandler handler = new PdpStatusMessageHandler(testGroup, pdpGroupService);
        PdpUpdate update10 =
            handler.createPdpUpdateMessage(status3.getPdpGroup(), new PdpSubGroup(), "pdp_2", policies, polsUndep);
        assertSame(update10.getPoliciesToBeDeployed(), policies);
        assertSame(update10.getPoliciesToBeUndeployed(), polsUndep);
        assertThat(update10.getPoliciesToBeDeployed()).isInstanceOf(List.class);
    }
  
    private void verifyPdpGroup(final int count) {
        final List<PdpGroup> fetchedGroups = fetchGroups(PdpHeartbeatListenerTest.DEFAULT_GROUP);
        for (final PdpSubGroup subGroup : fetchedGroups.get(0).getPdpSubgroups()) {
            if (subGroup.getPdpType().equals(APEX_TYPE)) {
                assertEquals(count, subGroup.getPdpInstances().size());
                assertEquals(count, subGroup.getCurrentInstanceCount());
                if (count > 0) {
                    assertEquals(PdpHealthStatus.HEALTHY, subGroup.getPdpInstances().get(0).getHealthy());
                }
            }
        }
    }
}
