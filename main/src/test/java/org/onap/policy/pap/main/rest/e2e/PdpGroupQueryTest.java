/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023 Nordix Foundation.
 * Modifications Copyright (C) 2022 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.rest.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class PdpGroupQueryTest extends End2EndBase {
    private static final String GROUP_ENDPOINT = "pdps";

    /**
     * Sets up.
     *
     * @throws Exception the exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {

        addToscaPolicyTypes("monitoring.policy-type.yaml");
        addToscaPolicies("monitoring.policy.yaml");

        addGroups("queryGroup.json");
        super.setUp();
    }

    @Test
    public void test() throws Exception {
        Invocation.Builder invocationBuilder = sendRequest(GROUP_ENDPOINT);
        Response rawresp = invocationBuilder.get();
        PdpGroups resp = rawresp.readEntity(PdpGroups.class);
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        assertEquals("[queryGroup1, queryGroup2]", mapList(resp.getGroups(), PdpGroup::getName).toString());

        checkGroup1(resp.getGroups().get(0));
        checkGroup2(resp.getGroups().get(1));
    }

    private void checkGroup1(PdpGroup group) {
        assertEquals("[pdpTypeA, pdpTypeB]", mapList(group.getPdpSubgroups(), PdpSubGroup::getPdpType).toString());

        assertEquals("my description", group.getDescription());
        assertEquals(PdpState.PASSIVE, group.getPdpGroupState());

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("abc", "def");
        assertEquals(props.toString(), group.getProperties().toString());

        checkSubGroup11(group.getPdpSubgroups().get(0));
    }

    private void checkSubGroup11(PdpSubGroup subgrp) {
        assertEquals(3, subgrp.getCurrentInstanceCount());
        assertEquals(2, subgrp.getDesiredInstanceCount());
        assertEquals("[pdpAA_1, pdpAA_2]", mapList(subgrp.getPdpInstances(), Pdp::getInstanceId).toString());
        assertEquals(2, filterList(subgrp.getPdpInstances(), pdp -> pdp.getPdpState() == PdpState.PASSIVE).size());
        assertEquals(2, filterList(subgrp.getPdpInstances(), pdp -> pdp.getHealthy() == PdpHealthStatus.HEALTHY)
                        .size());
        assertEquals("pdpTypeA", subgrp.getPdpType());
        assertEquals("[onap.restart.tca]", mapList(subgrp.getPolicies(), ToscaConceptIdentifier::getName).toString());
        assertEquals("[1.0.0]", mapList(subgrp.getPolicies(), ToscaConceptIdentifier::getVersion).toString());

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("ten", 10);
        assertEquals(props.toString(), subgrp.getProperties().toString());

        assertEquals("[onap.policies.monitoring.cdap.tca.hi.lo.app]",
                        mapList(subgrp.getSupportedPolicyTypes(), ToscaConceptIdentifier::getName).toString());
        assertEquals("[1.0.0]",
                        mapList(subgrp.getSupportedPolicyTypes(), ToscaConceptIdentifier::getVersion).toString());
    }

    private void checkGroup2(PdpGroup group) {
        assertEquals("[pdpTypeA]", mapList(group.getPdpSubgroups(), PdpSubGroup::getPdpType).toString());

        assertEquals(PdpState.ACTIVE, group.getPdpGroupState());
        assertTrue(group.getProperties() == null || group.getProperties().isEmpty());
    }

    private <T, R> List<R> mapList(List<T> list, Function<T, R> mapFunc) {
        return list.stream().map(mapFunc).collect(Collectors.toList());
    }

    private <T> List<T> filterList(List<T> list, Predicate<T> pred) {
        return list.stream().filter(pred).collect(Collectors.toList());
    }
}
