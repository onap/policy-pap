/*
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

package org.onap.policy.pap.main.mapper;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.junit.Test;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpInstanceDetails;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PolicyIdent;
import org.onap.policy.models.pdp.concepts.PolicyIdentOptVersion;
import org.onap.policy.models.pdp.concepts.PolicyTypeIdent;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.simple.concepts.ToscaPolicy;
import org.powermock.reflect.Whitebox;

public class MapperTest {
    private static final String MY_NAME = "my-name";
    private static final String MY_VERSION = "10.11.12";
    private static final String MY_SUB_NAME = "sub-name";
    private static final String MY_SUB_VERSION = "3.2.1";
    private static final PdpHealthStatus MY_HEALTH = PdpHealthStatus.TEST_IN_PROGRESS;
    private static final String MY_INSTANCE = "my_1";
    private static final String MY_MESSAGE = "my message";
    private static final PdpState MY_STATE = PdpState.SAFE;
    private static final int CURRENT_COUNT = 1;
    private static final int DESIRED_COUNT = 2;
    private static final String PDP_TYPE1 = "drools";
    private static final String PROPKEY1 = "prop-a";
    private static final String PROPVAL1 = "value-a";
    private static final String PROPKEY2 = "prop-b";
    private static final String PROPVAL2 = "value-b";
    private static final String MY_DESCRIPTION = "my description";

    @Test
    public void testConstructor() throws Exception {
        Whitebox.invokeConstructor(Mapper.class);
    }

    @Test
    public void testPdpGroups() {

        // TO-EXTERNAL

        // test populated object
        PdpGroups internal = new PdpGroups();
        internal.setGroups(Arrays.asList(makeGroup(1), makeGroup(2)));

        compareWithInternal(internal, Mapper.toExternal(internal));
    }

    private PdpGroup makeGroup(int count) {
        PdpGroup group = new PdpGroup();

        group.setDescription(MY_DESCRIPTION);
        populateIdent(group.getKey());
        group.setPdpGroupState(MY_STATE);
        group.setPdpSubgroups(Arrays.asList(makeSubGroup(count * 10)));
        group.setProperties(makeProperties());

        return group;
    }

    @Test
    public void testPdpGroup() {

        // TO-EXTERNAL

        // test populated object
        PdpGroup internal = new PdpGroup();
        internal.setDescription(MY_DESCRIPTION);
        populateIdent(internal.getKey());
        internal.setPdpGroupState(MY_STATE);
        internal.setPdpSubgroups(Arrays.asList(makeSubGroup(1), makeSubGroup(2)));
        internal.setProperties(makeProperties());

        compareWithInternal(internal, Mapper.toExternal(internal));
    }

    private PdpSubGroup makeSubGroup(int count) {
        PdpSubGroup subgroup = new PdpSubGroup();

        subgroup.setCurrentInstanceCount(CURRENT_COUNT);
        subgroup.setDesiredInstanceCount(DESIRED_COUNT);
        subgroup.setPdpInstances(Arrays.asList(makePdpInstance(count)));
        subgroup.setPdpType(PDP_TYPE1);
        subgroup.setPolicies(Arrays.asList(makePolicy(count, count)));
        subgroup.setProperties(makeProperties());
        subgroup.setSupportedPolicyTypes(Arrays.asList(makePolicyType(count)));

        return subgroup;
    }

    @Test
    public void testPdpSubGroup() {

        // TO-EXTERNAL

        // test populated object
        PdpSubGroup internal = new PdpSubGroup();
        internal.setCurrentInstanceCount(CURRENT_COUNT);
        internal.setDesiredInstanceCount(DESIRED_COUNT);
        internal.setPdpInstances(Arrays.asList(makePdpInstance(1), makePdpInstance(2)));
        internal.setPdpType(PDP_TYPE1);
        internal.setPolicies(Arrays.asList(makePolicy(1, 1), makePolicy(2, 2)));
        internal.setProperties(makeProperties());
        internal.setSupportedPolicyTypes(Arrays.asList(makePolicyType(1), makePolicyType(2)));

        compareWithInternal(internal, Mapper.toExternal(internal));
    }

    private ToscaPolicy makePolicy(int count, int typeCount) {
        ToscaPolicy policy = new ToscaPolicy();

        policy.getKey().setName(MY_NAME + "-policy-" + count);
        policy.getKey().setVersion("1.2." + count);
        policy.getType().setName(MY_NAME + "-type-" + typeCount);
        policy.getType().setVersion("1.2." + typeCount);

        return policy;
    }

    private PolicyTypeIdent makePolicyType(int count) {
        PolicyTypeIdent type = new PolicyTypeIdent();

        type.setName(MY_NAME + "-type-" + count);
        type.setVersion(count + ".1.2");

        return type;
    }

    private Map<String, String> makeProperties() {
        Map<String, String> map = new TreeMap<>();

        map.put(PROPKEY1, PROPVAL1);
        map.put(PROPKEY2, PROPVAL2);

        return map;
    }

    private PdpInstanceDetails makePdpInstance(int count) {
        PdpInstanceDetails object = new PdpInstanceDetails();

        object.setHealthy(MY_HEALTH);
        object.setInstanceId(MY_INSTANCE + count);
        object.setMessage(MY_MESSAGE);
        object.setPdpState(MY_STATE);

        return object;
    }

    @Test
    public void testPolicy() {

        // TO-EXTERNAL

        // test populated object
        ToscaPolicy internal = new ToscaPolicy();
        internal.getKey().setName(MY_NAME);
        internal.getKey().setVersion(MY_VERSION);
        internal.getType().setName(MY_SUB_NAME);
        internal.getType().setVersion(MY_SUB_VERSION);

        compareWithInternal(internal, Mapper.toExternal(internal));
    }

    @Test
    public void testPdpInstanceDetails() {

        // TO-EXTERNAL

        // test populated object
        PdpInstanceDetails internal = new PdpInstanceDetails();
        internal.setHealthy(MY_HEALTH);
        internal.setInstanceId(MY_INSTANCE);
        internal.setMessage(MY_MESSAGE);
        internal.setPdpState(MY_STATE);

        compareWithInternal(internal, Mapper.toExternal(internal));


        // TO-INTERNAL

        // test populated object
        org.onap.policy.models.pap.concepts.PdpInstanceDetails external =
                        new org.onap.policy.models.pap.concepts.PdpInstanceDetails();
        external.setHealthy(MY_HEALTH);
        external.setInstanceId(MY_INSTANCE);
        external.setMessage(MY_MESSAGE);
        external.setPdpState(MY_STATE);
        compareWithExternal(external, Mapper.toInternal(external));
    }

    @Test
    public void testPolicyIdent() {

        // TO-EXTERNAL

        // first test default object
        PolicyIdent internal = new PolicyIdent();
        org.onap.policy.models.pap.concepts.PolicyIdent external = Mapper.toExternal(internal);
        compareWithInternal(internal, external.getName(), external.getVersion());

        // now test populated object
        populateIdent(internal);
        external = Mapper.toExternal(internal);
        compareWithInternal(internal, external.getName(), external.getVersion());


        // TO-INTERNAL

        // first test default object
        external = new org.onap.policy.models.pap.concepts.PolicyIdent();
        compareWithExternal(external.getName(), external.getVersion(), Mapper.toInternal(external));

        // test populated object
        external.setName(MY_NAME);
        external.setVersion(MY_VERSION);
        compareWithExternal(external.getName(), external.getVersion(), Mapper.toInternal(external));
    }

    private void populateIdent(PfConceptKey target) {
        target.setName(MY_NAME);
        target.setVersion(MY_VERSION);
    }

    @Test
    public void testPolicyTypeIdent() {

        // TO-EXTERNAL

        // first test default object
        PolicyTypeIdent internal = new PolicyTypeIdent();
        org.onap.policy.models.pap.concepts.PolicyTypeIdent external = Mapper.toExternal(internal);
        compareWithInternal(internal, external.getName(), external.getVersion());

        // now test populated object
        populateIdent(internal);
        external = Mapper.toExternal(internal);
        compareWithInternal(internal, external.getName(), external.getVersion());


        // TO-INTERNAL

        // first test default object
        external = new org.onap.policy.models.pap.concepts.PolicyTypeIdent();
        compareWithExternal(external.getName(), external.getVersion(), Mapper.toInternal(external));

        // test populated object
        external.setName(MY_NAME);
        external.setVersion(MY_VERSION);
        compareWithExternal(external.getName(), external.getVersion(), Mapper.toInternal(external));
    }

    @Test
    public void testPolicyIdentOptVersion() {

        // TO-EXTERNAL

        // first test default object
        PolicyIdentOptVersion internal = new PolicyIdentOptVersion();
        org.onap.policy.models.pap.concepts.PolicyIdentOptVersion external = Mapper.toExternal(internal);
        compareWithInternal(internal, external.getName(), external.getVersion());

        // now test populated object
        populateIdent(internal);
        external = Mapper.toExternal(internal);
        compareWithInternal(internal, external.getName(), external.getVersion());


        // TO-INTERNAL

        // first test default object
        external = new org.onap.policy.models.pap.concepts.PolicyIdentOptVersion();
        compareWithExternal(external.getName(), external.getVersion(), Mapper.toInternal(external));

        // test populated object
        external.setName(MY_NAME);
        external.setVersion(MY_VERSION);
        compareWithExternal(external.getName(), external.getVersion(), Mapper.toInternal(external));
    }

    private static <L, R> void compareList(List<L> expected, List<R> actual, BiConsumer<L, R> compareItem) {

        assertEquals(expected.size(), actual.size());

        Iterator<L> expIterator = expected.iterator();
        for (R actualItem : actual) {
            L expectedItem = expIterator.next();
            compareItem.accept(expectedItem, actualItem);
        }
    }

    private static void compareWithExternal(org.onap.policy.models.pap.concepts.PdpInstanceDetails expected,
                    PdpInstanceDetails actual) {

        assertEquals(expected.getHealthy(), actual.getHealthy());
        assertEquals(expected.getInstanceId(), actual.getInstanceId());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getPdpState(), actual.getPdpState());
    }

    private void compareWithExternal(String expectedName, String expectedVersion, PfConceptKey actual) {
        PfConceptKey expected = new PfConceptKey();
        IdentUtil.toInternal(expectedName, expectedVersion, expected);

        assertEquals(expected.getName(), expected.getName());
        assertEquals(expected.getVersion(), expected.getVersion());
    }


    private static void compareWithInternal(PolicyTypeIdent expected,
                    org.onap.policy.models.pap.concepts.PolicyTypeIdent actual) {

        PfConceptKey target = new PfConceptKey();
        IdentUtil.toInternal(actual.getName(), actual.getVersion(), target);

        assertEquals(expected.getName(), target.getName());
        assertEquals(expected.getVersion(), target.getVersion());
    }

    private static void compareWithInternal(PdpGroups expected, org.onap.policy.models.pap.concepts.PdpGroups actual) {
        compareList(expected.getGroups(), actual.getGroups(), MapperTest::compareWithInternal);
    }

    private static void compareWithInternal(PdpGroup expected, org.onap.policy.models.pap.concepts.PdpGroup actual) {
        assertEquals(expected.getDescription(), actual.getDescription());
        compareWithInternal(expected.getKey(), actual.getName(), actual.getVersion());
        assertEquals(expected.getPdpGroupState(), actual.getPdpGroupState());
        compareList(expected.getPdpSubgroups(), actual.getPdpSubgroups(), MapperTest::compareWithInternal);
        assertEquals(expected.getProperties(), actual.getProperties());
    }

    private static void compareWithInternal(PdpSubGroup expected,
                    org.onap.policy.models.pap.concepts.PdpSubGroup actual) {

        assertEquals(expected.getCurrentInstanceCount(), actual.getCurrentInstanceCount());
        assertEquals(expected.getDesiredInstanceCount(), actual.getDesiredInstanceCount());
        compareList(expected.getPdpInstances(), actual.getPdpInstances(), MapperTest::compareWithInternal);
        assertEquals(expected.getPdpType(), actual.getPdpType());
        compareList(expected.getPolicies(), actual.getPolicies(), MapperTest::compareWithInternal);
        assertEquals(expected.getProperties(), actual.getProperties());
        compareList(expected.getSupportedPolicyTypes(), actual.getSupportedPolicyTypes(),
                        MapperTest::compareWithInternal);
    }

    private static void compareWithInternal(ToscaPolicy expected, org.onap.policy.models.pap.concepts.Policy actual) {
        assertEquals(expected.getKey().getName(), actual.getName());
        assertEquals(expected.getKey().getVersion(), actual.getPolicyVersion());
        assertEquals(expected.getType().getName(), actual.getPolicyType());
        assertEquals(expected.getType().getVersion(), actual.getPolicyTypeVersion());
    }

    private static void compareWithInternal(PdpInstanceDetails expected,
                    org.onap.policy.models.pap.concepts.PdpInstanceDetails actual) {

        assertEquals(expected.getHealthy(), actual.getHealthy());
        assertEquals(expected.getInstanceId(), actual.getInstanceId());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getPdpState(), actual.getPdpState());
    }

    private static void compareWithInternal(PfConceptKey expected, String actualName, String actualVersion) {
        assertEquals(IdentUtil.nameToExternal(expected.getName()), actualName);
        assertEquals(IdentUtil.versionToExternal(expected.getVersion()), actualVersion);
    }
}
