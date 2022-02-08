/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2021 Nordix Foundation.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import lombok.NonNull;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.PdpPolicyStatusBuilder;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.State;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;

public class TestPolicyStatusProvider extends ProviderSuper {
    private static final String MY_GROUP = "MyGroup";
    private static final String MY_PDP_TYPE = "MySubGroup";
    private static final @NonNull String VERSION = "1.2.3";
    private static final String PDP_A = "pdpA";
    private static final String PDP_B = "pdpB";
    private static final String PDP_C = "pdpC";
    private static final ToscaConceptIdentifier POLICY_TYPE = new ToscaConceptIdentifier("MyPolicyType", VERSION);
    private static final ToscaConceptIdentifier POLICY_A = new ToscaConceptIdentifier("MyPolicyA", VERSION);
    private static final ToscaConceptIdentifier POLICY_B = new ToscaConceptIdentifier("MyPolicyB", VERSION);
    private static final ToscaConceptIdentifier POLICY_C = new ToscaConceptIdentifier("MyPolicyC", VERSION);

    private PolicyStatusProvider prov;

    @AfterClass
    public static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    /**
     * Configures mocks and objects.
     *
     * @throws Exception if an error occurs
     */
    @Override
    @Before
    public void setUp() throws Exception {

        super.setUp();

        prov = new PolicyStatusProvider(policyStatusService);
    }

    @Test
    public void testGetStatus_testAccumulate() throws PfModelException {

        buildPolicyStatusToReturn1();

        List<PolicyStatus> result = new ArrayList<>(prov.getStatus());
        Collections.sort(result, (rec1, rec2) -> rec1.getPolicy().compareTo(rec2.getPolicy()));

        assertThat(result).hasSize(3);

        Iterator<PolicyStatus> iter = result.iterator();

        PolicyStatus status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_A);
        assertThat(status.getPolicyType()).isEqualTo(POLICY_TYPE);
        assertThat(status.getIncompleteCount()).isEqualTo(2);
        assertThat(status.getFailureCount()).isZero();
        assertThat(status.getSuccessCount()).isZero();

        status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_B);
        assertThat(status.getPolicyType()).isEqualTo(POLICY_TYPE);
        assertThat(status.getIncompleteCount()).isZero();
        assertThat(status.getFailureCount()).isZero();
        assertThat(status.getSuccessCount()).isEqualTo(1);

        status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_C);
        assertThat(status.getPolicyType()).isEqualTo(POLICY_TYPE);
        assertThat(status.getIncompleteCount()).isEqualTo(1);
        assertThat(status.getFailureCount()).isZero();
        assertThat(status.getSuccessCount()).isZero();
    }

    @Test
    public void testGetStatusToscaConceptIdentifierOptVersion() throws PfModelException {

        ToscaConceptIdentifierOptVersion optIdent = buildPolicyStatusToReturn2();

        List<PolicyStatus> result = new ArrayList<>(prov.getStatus(optIdent));
        assertThat(result).hasSize(1);

        Iterator<PolicyStatus> iter = result.iterator();

        PolicyStatus status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_A);
        assertThat(status.getPolicyType()).isEqualTo(POLICY_TYPE);
        assertThat(status.getIncompleteCount()).isEqualTo(2);
        assertThat(status.getFailureCount()).isZero();
        assertThat(status.getSuccessCount()).isZero();
    }

    @Test
    public void testGetPolicyStatus() throws PfModelException {

        buildPolicyStatusToReturn1();

        List<PdpPolicyStatus> result = new ArrayList<>(prov.getPolicyStatus());
        Collections.sort(result, (rec1, rec2) -> rec1.getPolicy().compareTo(rec2.getPolicy()));

        assertThat(result).hasSize(5);
        Iterator<PdpPolicyStatus> iter = result.iterator();

        PdpPolicyStatus status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_A);
        assertThat(status.getPolicyType()).isEqualTo(POLICY_TYPE);
        assertThat(status.getPdpId()).isEqualTo(PDP_A);
        assertThat(status.getPdpType()).isEqualTo(MY_PDP_TYPE);
        assertThat(status.getPdpGroup()).isEqualTo(MY_GROUP);
        assertTrue(status.isDeploy());
        assertThat(status.getState()).isEqualTo(State.WAITING);

        status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_A);
        assertThat(status.getPdpId()).isEqualTo(PDP_B);
        assertTrue(status.isDeploy());
        assertThat(status.getState()).isEqualTo(State.WAITING);

        status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_B);
        assertThat(status.getPdpId()).isEqualTo(PDP_A);
        assertFalse(status.isDeploy());
        assertThat(status.getState()).isEqualTo(State.WAITING);

        status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_B);
        assertThat(status.getPdpId()).isEqualTo(PDP_B);
        assertTrue(status.isDeploy());
        assertThat(status.getState()).isEqualTo(State.SUCCESS);
    }

    @Test
    public void testGetPolicyStatusByGroupAndPolicyIdVersion() throws PfModelException {

        ToscaConceptIdentifierOptVersion optIdent = buildPolicyStatusToReturn2();

        List<PdpPolicyStatus> result = new ArrayList<>(prov.getPolicyStatus(MY_GROUP, optIdent));
        assertThat(result).hasSize(3);

        Iterator<PdpPolicyStatus> iter = result.iterator();

        PdpPolicyStatus status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_A);
        assertThat(status.getPolicyType()).isEqualTo(POLICY_TYPE);
        assertThat(status.getPdpId()).isEqualTo(PDP_A);
        assertThat(status.getPdpType()).isEqualTo(MY_PDP_TYPE);
        assertThat(status.getPdpGroup()).isEqualTo(MY_GROUP);
        assertTrue(status.isDeploy());
        assertThat(status.getState()).isEqualTo(State.WAITING);

        status = iter.next();
        assertThat(status.getPolicy()).isEqualTo(POLICY_A);
        assertThat(status.getPolicyType()).isEqualTo(POLICY_TYPE);
        assertThat(status.getPdpId()).isEqualTo(PDP_B);
        assertThat(status.getPdpType()).isEqualTo(MY_PDP_TYPE);
        assertThat(status.getPdpGroup()).isEqualTo(MY_GROUP);
        assertFalse(status.isDeploy());
        assertThat(status.getState()).isEqualTo(State.FAILURE);
    }

    @Test
    public void testGetPolicyStatusByRegexNoMatch() throws PfModelException {
        buildPolicyStatusToReturn1();
        final String pattern = "Hello";

        final Collection<PolicyStatus> actual = prov.getByRegex(pattern);
        assertThat(actual).isEmpty();
    }

    @Test
    public void testGetPolicyStatusOneMatch() throws PfModelException {
        buildPolicyStatusToReturn1();
        final String pattern = "My(We|Po)[li]{0,3}c.A";

        final Collection<PolicyStatus> actual = prov.getByRegex(pattern);
        assertThat(actual).hasSize(1);

        final String actualName = actual.iterator().next().getPolicy().getName();
        assertThat(actualName).isEqualTo("MyPolicyA");
    }

    @Test
    public void testGetPolicyStatusAllMatch() throws PfModelException {
        buildPolicyStatusToReturn1();
        final String pattern = "My(We|Po)[li]{0,3}c.{2}0*";

        final Collection<PolicyStatus> actual = prov.getByRegex(pattern);

        assertThat(actual).hasSize(3);
    }

    private void buildPolicyStatusToReturn1() throws PfModelException {

        PdpPolicyStatusBuilder builder = PdpPolicyStatus.builder().pdpGroup(MY_GROUP).pdpType(MY_PDP_TYPE)
            .policyType(POLICY_TYPE).state(State.WAITING);

        PdpPolicyStatus notDeployed = builder.deploy(false).policy(POLICY_B).pdpId(PDP_A).build();

        // remaining policies are deployed
        builder.deploy(true);

        // @formatter:off
        when(policyStatusService.getAllPolicyStatus()).thenReturn(List.of(
                        builder.policy(POLICY_A).pdpId(PDP_A).build(),
                        builder.policy(POLICY_A).pdpId(PDP_B).build(),
                        notDeployed,
                        builder.policy(POLICY_C).pdpId(PDP_A).build(),
                        builder.policy(POLICY_B).pdpId(PDP_B).state(State.SUCCESS).build()
                    ));
        // @formatter:on
    }

    private ToscaConceptIdentifierOptVersion buildPolicyStatusToReturn2() throws PfModelException {
        PdpPolicyStatusBuilder builder =
            PdpPolicyStatus.builder().pdpGroup(MY_GROUP).pdpType(MY_PDP_TYPE).policy(POLICY_A).policyType(POLICY_TYPE);

        PdpPolicyStatus notDeployed = builder.deploy(false).pdpId(PDP_B).state(State.FAILURE).build();

        // remaining policies are deployed
        builder.deploy(true).state(State.WAITING);

        ToscaConceptIdentifierOptVersion optIdent = new ToscaConceptIdentifierOptVersion(POLICY_A);

        // @formatter:off
        when(policyStatusService.getAllPolicyStatus(optIdent)).thenReturn(List.of(
                        builder.policy(POLICY_A).pdpId(PDP_A).build(),
                        notDeployed,
                        builder.policy(POLICY_A).pdpId(PDP_C).build()
                        ));
        // @formatter:on
        return optIdent;
    }
}
