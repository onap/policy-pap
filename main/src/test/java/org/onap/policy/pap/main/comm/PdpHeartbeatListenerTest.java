/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2020 Nordix Foundation.
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

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.rest.e2e.End2EndBase;

/**
 * Class to perform unit test of {@link PdpHeartbeatListener}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpHeartbeatListenerTest extends End2EndBase {

    private static final String POLICY_VERSION = "1.0.0";
    private static final String POLICY_NAME = "onap.policies.controlloop.operational.Apex.SampleDomain";
    private static final String APEX_TYPE = "apex";
    private static final String DEFAULT_GROUP = "defaultGroup";
    private static final String PDP_NAME = "pdp_1";
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";

    private Date timeStamp;
    private PdpHeartbeatListener pdpHeartbeatListener;

    @Test
    public void testPdpHeartbeatListener() throws CoderException, PfModelException {
        addGroups("PdpGroups.json");
        pdpHeartbeatListener = new PdpHeartbeatListener();

        // Testing pdp registration success case
        final PdpStatus status1 = new PdpStatus();
        status1.setName(PDP_NAME);
        status1.setState(PdpState.ACTIVE);
        status1.setPdpGroup(DEFAULT_GROUP);
        status1.setPdpType(APEX_TYPE);
        status1.setHealthy(PdpHealthStatus.HEALTHY);
        final List<ToscaPolicyIdentifier> idents1 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status1.setPolicies(idents1);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status1);
        verifyPdpGroup(DEFAULT_GROUP, 1);

        // Testing pdp heartbeat success case
        final PdpStatus status2 = new PdpStatus();
        status2.setName(PDP_NAME);
        status2.setState(PdpState.ACTIVE);
        status2.setPdpGroup(DEFAULT_GROUP);
        status2.setPdpType(APEX_TYPE);
        status2.setHealthy(PdpHealthStatus.HEALTHY);
        status2.setPdpSubgroup(APEX_TYPE);
        final List<ToscaPolicyIdentifier> idents2 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status2.setPolicies(idents2);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status2);
        verifyPdpGroup(DEFAULT_GROUP, 1);

        // Testing pdp heartbeat failure case with pdp missing
        final PdpStatus status3 = new PdpStatus();
        status3.setName("pdp_2");
        status3.setState(PdpState.ACTIVE);
        status3.setPdpGroup(DEFAULT_GROUP);
        status3.setPdpType(APEX_TYPE);
        status3.setHealthy(PdpHealthStatus.HEALTHY);
        status3.setPdpSubgroup(APEX_TYPE);
        final List<ToscaPolicyIdentifier> idents3 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status3.setPolicies(idents3);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status3);
        verifyPdpGroup(DEFAULT_GROUP, 2);

        // Testing pdp registration failure case
        final PdpStatus status4 = new PdpStatus();
        status4.setName("pdp_3");
        status4.setState(PdpState.ACTIVE);
        status4.setPdpGroup("wrongGroup");
        status4.setPdpType(APEX_TYPE);
        status4.setHealthy(PdpHealthStatus.HEALTHY);
        final List<ToscaPolicyIdentifier> idents4 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status4.setPolicies(idents4);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status4);
        verifyPdpGroup(DEFAULT_GROUP, 2);

        // Testing pdp heartbeat failure case with pdp state mismatch
        final PdpStatus status5 = new PdpStatus();
        status5.setName(PDP_NAME);
        status5.setState(PdpState.PASSIVE);
        status5.setPdpGroup(DEFAULT_GROUP);
        status5.setPdpType(APEX_TYPE);
        status5.setHealthy(PdpHealthStatus.HEALTHY);
        status5.setPdpSubgroup(APEX_TYPE);
        final List<ToscaPolicyIdentifier> idents5 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status5.setPolicies(idents5);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status5);
        verifyPdpGroup(DEFAULT_GROUP, 2);

        // Testing pdp heartbeat failure case with pdp policies mismatch
        final PdpStatus status6 = new PdpStatus();
        status6.setName(PDP_NAME);
        status6.setState(PdpState.ACTIVE);
        status6.setPdpGroup(DEFAULT_GROUP);
        status6.setPdpType(APEX_TYPE);
        status6.setHealthy(PdpHealthStatus.HEALTHY);
        status6.setPdpSubgroup(APEX_TYPE);
        final List<ToscaPolicyIdentifier> idents6 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION),
                        new ToscaPolicyIdentifier("onap.restart.tca", POLICY_VERSION));
        status6.setPolicies(idents6);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status6);
        verifyPdpGroup(DEFAULT_GROUP, 2);

        // Testing pdp heartbeat failure case with pdp no policies
        final PdpStatus status7 = new PdpStatus();
        status7.setName(PDP_NAME);
        status7.setState(PdpState.ACTIVE);
        status7.setPdpGroup(DEFAULT_GROUP);
        status7.setPdpType(APEX_TYPE);
        status7.setHealthy(PdpHealthStatus.HEALTHY);
        status7.setPdpSubgroup(APEX_TYPE);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status7);
        verifyPdpGroup(DEFAULT_GROUP, 2);

        // Testing pdp termination case for pdp_1
        final PdpStatus status8 = new PdpStatus();
        status8.setName(PDP_NAME);
        status8.setState(PdpState.TERMINATED);
        status8.setPdpGroup(DEFAULT_GROUP);
        status8.setPdpType(APEX_TYPE);
        status8.setPdpSubgroup(APEX_TYPE);
        status8.setHealthy(PdpHealthStatus.HEALTHY);
        final List<ToscaPolicyIdentifier> idents8 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status8.setPolicies(idents8);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status8);
        verifyPdpGroup(DEFAULT_GROUP, 1);

        // Testing pdp termination case for pdp_2
        final PdpStatus status9 = new PdpStatus();
        status9.setName("pdp_2");
        status9.setState(PdpState.TERMINATED);
        status9.setPdpGroup(DEFAULT_GROUP);
        status9.setPdpType(APEX_TYPE);
        status9.setPdpSubgroup(APEX_TYPE);
        status9.setHealthy(PdpHealthStatus.HEALTHY);
        final List<ToscaPolicyIdentifier> idents9 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status9.setPolicies(idents9);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status9);
        verifyPdpGroup(DEFAULT_GROUP, 0);
    }

    @Test
    public void testPdpStatistics() throws CoderException, PfModelException, ParseException {
        addGroups("PdpGroups.json");
        pdpHeartbeatListener = new PdpHeartbeatListener();
        timeStamp = new SimpleDateFormat("yyyy-MM-dd").parse("2020-01-01");

        // init default pdp group
        final PdpStatus status1 = new PdpStatus();
        status1.setName(PDP_NAME);
        status1.setState(PdpState.ACTIVE);
        status1.setPdpGroup(DEFAULT_GROUP);
        status1.setPdpType(APEX_TYPE);
        status1.setHealthy(PdpHealthStatus.HEALTHY);
        final List<ToscaPolicyIdentifier> idents1 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status1.setPolicies(idents1);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status1);
        verifyPdpGroup(DEFAULT_GROUP, 1);

        // init pdp instance
        final PdpStatus status2 = new PdpStatus();
        status2.setName(PDP_NAME);
        status2.setState(PdpState.ACTIVE);
        status2.setPdpGroup(DEFAULT_GROUP);
        status2.setPdpType(APEX_TYPE);
        status2.setHealthy(PdpHealthStatus.HEALTHY);
        status2.setPdpSubgroup(APEX_TYPE);
        final List<ToscaPolicyIdentifier> idents2 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status2.setPolicies(idents2);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status2);

        // Testing passing pdp statistics success case
        final PdpStatus status3 = new PdpStatus();
        status3.setName(PDP_NAME);
        status3.setState(PdpState.ACTIVE);
        status3.setPdpGroup(DEFAULT_GROUP);
        status3.setPdpType(APEX_TYPE);
        status3.setHealthy(PdpHealthStatus.HEALTHY);
        status3.setPdpSubgroup(APEX_TYPE);
        final List<ToscaPolicyIdentifier> idents3 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status3.setPolicies(idents3);

        PdpStatistics pdpStatistics03 = new PdpStatistics();
        pdpStatistics03.setPdpInstanceId(PDP_NAME);
        pdpStatistics03.setPdpGroupName(DEFAULT_GROUP);
        pdpStatistics03.setPdpSubGroupName(APEX_TYPE);
        pdpStatistics03.setTimeStamp(timeStamp);
        status3.setStatistics(pdpStatistics03);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status3);
        verifyPdpStatistics(PDP_NAME, DEFAULT_GROUP, null, 1);

        // Testing pdp statistics failure having the pdpStatistics null in the heartbeat for already registered pdp
        final PdpStatus status4 = new PdpStatus();
        status4.setName(PDP_NAME);
        status4.setState(PdpState.ACTIVE);
        status4.setPdpGroup(DEFAULT_GROUP);
        status4.setPdpType(APEX_TYPE);
        status4.setHealthy(PdpHealthStatus.HEALTHY);
        status4.setPdpSubgroup(APEX_TYPE);
        final List<ToscaPolicyIdentifier> idents4 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status4.setPolicies(idents4);
        status4.setStatistics(null);
        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status4);
        verifyPdpStatistics(PDP_NAME, DEFAULT_GROUP, null, 1);

        // Testing pdp statistics failure passing different pdpGroup, PdpSubGroup & pdpInstanceId
        final PdpStatus status5 = new PdpStatus();
        status5.setName(PDP_NAME);
        status5.setState(PdpState.ACTIVE);
        status5.setPdpGroup(DEFAULT_GROUP);
        status5.setPdpType(APEX_TYPE);
        status5.setHealthy(PdpHealthStatus.HEALTHY);
        status5.setPdpSubgroup(APEX_TYPE);
        final List<ToscaPolicyIdentifier> idents5 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status5.setPolicies(idents5);

        PdpStatistics pdpStatistics05 = new PdpStatistics();
        pdpStatistics05.setPdpInstanceId("pdp_2");
        pdpStatistics05.setPdpGroupName("defaultGroup_1");
        pdpStatistics05.setPdpSubGroupName("apex_1");
        pdpStatistics03.setTimeStamp(timeStamp);
        status5.setStatistics(pdpStatistics05);

        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status5);
        verifyPdpStatistics(null, DEFAULT_GROUP, null, 1);

        // Test pdp statistics failure passing negative values
        final PdpStatus status6 = new PdpStatus();
        status6.setName(PDP_NAME);
        status6.setState(PdpState.ACTIVE);
        status6.setPdpGroup(DEFAULT_GROUP);
        status6.setPdpType(APEX_TYPE);
        status6.setHealthy(PdpHealthStatus.HEALTHY);
        status6.setPdpSubgroup(APEX_TYPE);
        final List<ToscaPolicyIdentifier> idents6 =
                Arrays.asList(new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION));
        status5.setPolicies(idents6);

        PdpStatistics pdpStatistics06 = new PdpStatistics();
        pdpStatistics06.setPdpInstanceId(PDP_NAME);
        pdpStatistics06.setPdpGroupName(DEFAULT_GROUP);
        pdpStatistics06.setPdpSubGroupName(APEX_TYPE);
        pdpStatistics03.setTimeStamp(timeStamp);

        pdpStatistics06.setPolicyDeployCount(-1);
        pdpStatistics06.setPolicyDeployFailCount(-1);
        status5.setStatistics(pdpStatistics06);

        pdpHeartbeatListener.onTopicEvent(INFRA, TOPIC, status5);
        verifyPdpStatistics(null, DEFAULT_GROUP, null, 1);
    }

    private void verifyPdpGroup(final String name, final int count) throws PfModelException {
        final List<PdpGroup> fetchedGroups = fetchGroups(name);
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

    private void verifyPdpStatistics(final String pdpInstanceId, final String pdpGroupName,
            final String pdpSubGroupName, final int count) throws  PfModelException {
        final List<PdpStatistics> fetchedPdpStatistics =
                fetchPdpStatistics(pdpInstanceId, pdpGroupName, pdpSubGroupName);
        assertEquals(count, fetchedPdpStatistics.size());
    }
}
