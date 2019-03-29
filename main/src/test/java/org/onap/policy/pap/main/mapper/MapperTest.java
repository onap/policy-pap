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
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PolicyIdent;
import org.onap.policy.models.pdp.concepts.PolicyIdentOptVersion;
import org.onap.policy.models.pdp.concepts.PolicyTypeIdent;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.internal.PdpDeployPolicies;
import org.powermock.reflect.Whitebox;

public class MapperTest {
    private static final String MY_NAME = "my-name";
    private static final String MY_VERSION = "10.11.12";
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
    public void testPdpDeployPolicies() {

        // TO-INTERNAL

        // test populated object
        org.onap.policy.models.pap.concepts.PdpDeployPolicies external =
                        new org.onap.policy.models.pap.concepts.PdpDeployPolicies();

        external.setPolicies(Arrays.asList(makePolicyIdentOptVersion(1), makePolicyIdentOptVersion(2)));
        compareWithExternal(external, Mapper.toInternal(external));
    }

    private org.onap.policy.models.pap.concepts.PolicyIdentOptVersion makePolicyIdentOptVersion(int count) {

        org.onap.policy.models.pap.concepts.PolicyIdentOptVersion type =
                        new org.onap.policy.models.pap.concepts.PolicyIdentOptVersion();


        type.setName(MY_NAME + "-id-" + count);
        type.setVersion(count + ".1.2");

        return type;
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

        group.setDescription(MY_DESCRIPTION + "-" + count);
        group.setName(MY_NAME);
        group.setVersion(MY_VERSION);
        group.setPdpGroupState(MY_STATE);

        int inc = count + 1;
        group.setPdpSubgroups(Arrays.asList(makeSubGroup(count * 10), makeSubGroup(inc * 10)));
        group.setProperties(makeProperties());

        return group;
    }

    private PdpSubGroup makeSubGroup(int count) {
        PdpSubGroup subgroup = new PdpSubGroup();

        subgroup.setCurrentInstanceCount(CURRENT_COUNT);
        subgroup.setDesiredInstanceCount(DESIRED_COUNT);

        int inc = count + 1;
        subgroup.setPdpInstances(Arrays.asList(makePdp(count), makePdp(inc)));
        subgroup.setPdpType(PDP_TYPE1);
        subgroup.setPolicies(Arrays.asList(makePolicy(count, count), makePolicy(inc, inc)));
        subgroup.setProperties(makeProperties());
        subgroup.setSupportedPolicyTypes(Arrays.asList(makePolicyType(count), makePolicyType(inc)));

        return subgroup;
    }

    private Pdp makePdp(int count) {
        Pdp object = new Pdp();

        object.setHealthy(MY_HEALTH);
        object.setInstanceId(MY_INSTANCE + count);
        object.setMessage(MY_MESSAGE);
        object.setPdpState(MY_STATE);

        return object;
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

    @Test
    public void testPdpGroup() {

        // TO-EXTERNAL

        // test populated object
        PdpGroup internal = makeGroup(1);
        compareWithInternal(internal, Mapper.toExternal(internal));
    }

    @Test
    public void testPdpSubGroup() {

        // TO-EXTERNAL

        // test populated object
        PdpSubGroup internal = makeSubGroup(1);
        compareWithInternal(internal, Mapper.toExternal(internal));
    }

    @Test
    public void testPolicy() {

        // TO-EXTERNAL

        // test populated object
        ToscaPolicy internal = makePolicy(1, 1);
        compareWithInternal(internal, Mapper.toExternal(internal));
    }

    private ToscaPolicy makePolicy(int count, int typeCount) {
        ToscaPolicy policy = new ToscaPolicy();

        policy.setName(MY_NAME + "-policy-" + count);
        policy.setVersion("1.2." + count);
        policy.setType(MY_NAME + "-type-" + typeCount);
        policy.setTypeVersion("1.2." + typeCount);

        return policy;
    }

    @Test
    public void testPdpInstanceDetails() {

        // TO-EXTERNAL

        // test populated object
        Pdp internal = makePdp(1);
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
        compareWithInternal(internal, external);

        // now test populated object
        internal.setName(MY_NAME);
        internal.setVersion(MY_VERSION);
        external = Mapper.toExternal(internal);
        compareWithInternal(internal, external);


        // TO-INTERNAL

        // first test default object
        external = new org.onap.policy.models.pap.concepts.PolicyIdent();
        compareWithExternal(external, Mapper.toInternal(external));

        // test populated object
        external.setName(MY_NAME);
        external.setVersion(MY_VERSION);
        compareWithExternal(external, Mapper.toInternal(external));
    }

    @Test
    public void testPolicyTypeIdent() {

        // TO-EXTERNAL

        // first test default object
        PolicyTypeIdent internal = new PolicyTypeIdent();
        org.onap.policy.models.pap.concepts.PolicyTypeIdent external = Mapper.toExternal(internal);
        compareWithInternal(internal, external);

        // now test populated object
        internal.setName(MY_NAME);
        internal.setVersion(MY_VERSION);
        external = Mapper.toExternal(internal);
        compareWithInternal(internal, external);


        // TO-INTERNAL

        // first test default object
        external = new org.onap.policy.models.pap.concepts.PolicyTypeIdent();
        compareWithExternal(external, Mapper.toInternal(external));

        // test populated object
        external.setName(MY_NAME);
        external.setVersion(MY_VERSION);
        compareWithExternal(external, Mapper.toInternal(external));
    }

    @Test
    public void testPolicyIdentOptVersion() {

        // TO-EXTERNAL

        // first test default object
        PolicyIdentOptVersion internal = new PolicyIdentOptVersion();
        org.onap.policy.models.pap.concepts.PolicyIdentOptVersion external = Mapper.toExternal(internal);
        compareWithInternal(internal, external);

        // now test populated object
        internal.setName(MY_NAME);
        internal.setVersion(MY_VERSION);
        external = Mapper.toExternal(internal);
        compareWithInternal(internal, external);


        // TO-INTERNAL

        // first test default object
        external = new org.onap.policy.models.pap.concepts.PolicyIdentOptVersion();
        compareWithExternal(external, Mapper.toInternal(external));

        // test populated object
        external.setName(MY_NAME);
        external.setVersion(MY_VERSION);
        compareWithExternal(external, Mapper.toInternal(external));
    }


    /**
     * Compares the items in two lists.
     *
     * @param expected the expected items
     * @param actual the actual items
     * @param compareItem function to compare an expected item with an actual item
     */
    private static <L, R> void compareList(List<L> expected, List<R> actual, BiConsumer<L, R> compareItem) {

        assertEquals(expected.size(), actual.size());

        Iterator<L> expIterator = expected.iterator();
        for (R actualItem : actual) {
            L expectedItem = expIterator.next();
            compareItem.accept(expectedItem, actualItem);
        }
    }


    // compares actual internal objects with expected external objects

    private static void compareWithExternal(org.onap.policy.models.pap.concepts.PolicyIdent expected,
                    PolicyIdent actual) {

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());
    }

    private static void compareWithExternal(org.onap.policy.models.pap.concepts.PolicyTypeIdent expected,
                    PolicyTypeIdent actual) {

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());
    }

    private static void compareWithExternal(org.onap.policy.models.pap.concepts.PolicyIdentOptVersion expected,
                    PolicyIdentOptVersion actual) {

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());
    }

    private static void compareWithExternal(org.onap.policy.models.pap.concepts.PdpDeployPolicies expected,
                    PdpDeployPolicies actual) {

        compareList(expected.getPolicies(), actual.getPolicies(), MapperTest::compareWithExternal);
    }

    private static void compareWithExternal(org.onap.policy.models.pap.concepts.PdpInstanceDetails expected,
                    Pdp actual) {

        assertEquals(expected.getHealthy(), actual.getHealthy());
        assertEquals(expected.getInstanceId(), actual.getInstanceId());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getPdpState(), actual.getPdpState());
    }

    // compares actual external objects with expected internal objects

    private static void compareWithInternal(PolicyIdent expected,
                    org.onap.policy.models.pap.concepts.PolicyIdent actual) {

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());
    }

    private static void compareWithInternal(PolicyTypeIdent expected,
                    org.onap.policy.models.pap.concepts.PolicyTypeIdent actual) {

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());
    }

    private static void compareWithInternal(PolicyIdentOptVersion expected,
                    org.onap.policy.models.pap.concepts.PolicyIdentOptVersion actual) {

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());
    }

    private static void compareWithInternal(PdpGroups expected, org.onap.policy.models.pap.concepts.PdpGroups actual) {
        compareList(expected.getGroups(), actual.getGroups(), MapperTest::compareWithInternal);
    }

    private static void compareWithInternal(PdpGroup expected, org.onap.policy.models.pap.concepts.PdpGroup actual) {
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(actual.getVersion(), actual.getVersion());
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
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getPolicyVersion());
        assertEquals(expected.getType(), actual.getPolicyType());
        assertEquals(expected.getTypeVersion(), actual.getPolicyTypeVersion());
    }

    private static void compareWithInternal(Pdp expected,
                    org.onap.policy.models.pap.concepts.PdpInstanceDetails actual) {

        assertEquals(expected.getHealthy(), actual.getHealthy());
        assertEquals(expected.getInstanceId(), actual.getInstanceId());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getPdpState(), actual.getPdpState());
    }
}
