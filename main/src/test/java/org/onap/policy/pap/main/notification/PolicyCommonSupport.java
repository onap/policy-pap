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

package org.onap.policy.pap.main.notification;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

/**
 * Super class for policy notification test classes.
 */
public class PolicyCommonSupport {
    protected static final String MAP_FIELD = "policy2data";
    protected static final String PDP1 = "pdp-1";
    protected static final String PDP2 = "pdp-2";
    protected static final String PDP3 = "pdp-3";
    protected static final String PDP4 = "pdp-4";

    protected ToscaPolicyTypeIdentifier type;
    protected ToscaPolicyIdentifier policy1;
    protected ToscaPolicyIdentifier policy2;
    protected ToscaPolicyIdentifier policy3;
    protected ToscaPolicyIdentifier policy4;

    /**
     * Creates various objects.
     */
    @Before
    public void setUp() {
        type = new ToscaPolicyTypeIdentifier("my-type", "3.2.1");
        policy1 = new ToscaPolicyIdentifier("my-id-a", "1.2.0");
        policy2 = new ToscaPolicyIdentifier("my-id-b", "1.2.1");
        policy3 = new ToscaPolicyIdentifier("my-id-c", "1.2.2");
        policy4 = new ToscaPolicyIdentifier("my-id-d", "1.2.3");
    }

    protected PolicyPdpNotificationData makeData(ToscaPolicyIdentifier policyId, String... pdps) {
        PolicyPdpNotificationData data = new PolicyPdpNotificationData(policyId, type);
        data.addAll(Arrays.asList(pdps));
        return data;
    }

    protected List<Integer> getCounts(PolicyTrackerData data) {
        PolicyStatus status = new PolicyStatus();
        data.putValuesInto(status);
        return getCounts(status);
    }

    protected List<Integer> getCounts(PolicyStatus status) {
        return Arrays.asList(status.getSuccessCount(), status.getFailureCount(), status.getIncompleteCount());
    }
}
