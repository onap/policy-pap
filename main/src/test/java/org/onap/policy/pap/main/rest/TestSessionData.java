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

package org.onap.policy.pap.main.rest;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyFilter;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;
import org.onap.policy.pap.main.notification.PolicyPdpNotificationData;
import org.onap.policy.pap.main.rest.SessionData;

public class TestSessionData extends ProviderSuper {
    private static final String GROUP_NAME = "groupA";
    private static final String PDP1 = "pdp_1";
    private static final String PDP2 = "pdp_2";
    private static final String PDP3 = "pdp_3";
    private static final String POLICY_VERSION_PREFIX = "1.2.";
    private static final String POLICY_NAME = "myPolicy";
    private static final String POLICY_VERSION = POLICY_VERSION_PREFIX + "3";
    private static final String POLICY_TYPE = "myType";
    private static final String POLICY_TYPE_VERSION = "10.20.30";
    private static final String EXPECTED_EXCEPTION = "expected exception";

    private SessionData session;
    private ToscaPolicyIdentifierOptVersion ident;
    private ToscaPolicyTypeIdentifier type;
    private ToscaPolicyTypeIdentifier type2;
    private PdpGroup group1;
    private PdpGroup group2;

    /**
     * Initializes mocks and a session.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        ident = new ToscaPolicyIdentifierOptVersion(POLICY_NAME, POLICY_VERSION);
        type = new ToscaPolicyTypeIdentifier(POLICY_TYPE, POLICY_TYPE_VERSION);
        type2 = new ToscaPolicyTypeIdentifier(POLICY_TYPE, POLICY_TYPE_VERSION + "0");
        group1 = loadGroup("group1.json");
        group2 = loadGroup("group2.json");

        session = new SessionData(dao);
    }

    @Test
    public void testGetPolicyType() throws Exception {
        ToscaPolicyType policy1 = makePolicyType(POLICY_TYPE, POLICY_TYPE_VERSION);
        when(dao.getPolicyTypeList(POLICY_TYPE, POLICY_TYPE_VERSION)).thenReturn(Arrays.asList(policy1));

        assertSame(policy1, session.getPolicyType(type));

        // retrieve a second time - should use cache
        assertSame(policy1, session.getPolicyType(type));
    }

    @Test
    public void testGetPolicyType_NotFound() throws Exception {
        when(dao.getPolicyTypeList(any(), any())).thenReturn(Collections.emptyList());

        assertNull(session.getPolicyType(type));
    }

    @Test
    public void testGetPolicyType_DaoEx() throws Exception {
        PfModelException ex = new PfModelException(Status.INTERNAL_SERVER_ERROR, EXPECTED_EXCEPTION);
        when(dao.getPolicyTypeList(POLICY_TYPE, POLICY_TYPE_VERSION)).thenThrow(ex);

        assertThatThrownBy(() -> session.getPolicyType(type)).isSameAs(ex);
    }

    @Test
    public void testGetPolicy_NullVersion() throws Exception {
        ToscaPolicy policy1 = makePolicy(POLICY_NAME, POLICY_VERSION);
        when(dao.getFilteredPolicyList(any())).thenReturn(Arrays.asList(policy1));

        ident.setVersion(null);
        assertSame(policy1, session.getPolicy(ident));

        ToscaPolicyFilter filter = getPolicyFilter();
        assertEquals(POLICY_NAME, filter.getName());
        assertEquals(ToscaPolicyFilter.LATEST_VERSION, filter.getVersion());
        assertEquals(null, filter.getVersionPrefix());

        // retrieve a second time using full version - should use cache
        assertSame(policy1, session.getPolicy(new ToscaPolicyIdentifierOptVersion(policy1.getIdentifier())));
        verify(dao).getFilteredPolicyList(any());
    }

    @Test
    public void testGetPolicy_MajorVersion() throws Exception {
        ToscaPolicy policy1 = makePolicy(POLICY_NAME, POLICY_VERSION);
        when(dao.getFilteredPolicyList(any())).thenReturn(Arrays.asList(policy1));

        ident.setVersion("1");
        assertSame(policy1, session.getPolicy(ident));

        ToscaPolicyFilter filter = getPolicyFilter();
        assertEquals(POLICY_NAME, filter.getName());
        assertEquals(ToscaPolicyFilter.LATEST_VERSION, filter.getVersion());
        assertEquals("1.", filter.getVersionPrefix());

        // retrieve a second time using full version - should use cache
        assertSame(policy1, session.getPolicy(new ToscaPolicyIdentifierOptVersion(policy1.getIdentifier())));
        verify(dao).getFilteredPolicyList(any());
    }

    @Test
    public void testGetPolicy_MajorMinorVersion() throws Exception {
        ToscaPolicy policy1 = makePolicy(POLICY_NAME, POLICY_VERSION);
        when(dao.getFilteredPolicyList(any())).thenReturn(Arrays.asList(policy1));

        ident.setVersion(POLICY_VERSION);
        assertSame(policy1, session.getPolicy(ident));

        ToscaPolicyFilter filter = getPolicyFilter();
        assertEquals(POLICY_NAME, filter.getName());
        assertEquals(POLICY_VERSION, filter.getVersion());
        assertEquals(null, filter.getVersionPrefix());

        // retrieve a second time using full version - should use cache
        assertSame(policy1, session.getPolicy(new ToscaPolicyIdentifierOptVersion(policy1.getIdentifier())));
        verify(dao).getFilteredPolicyList(any());
    }

    @Test
    public void testGetPolicy_NotFound() throws Exception {
        when(dao.getFilteredPolicyList(any())).thenReturn(Collections.emptyList());

        assertNull(session.getPolicy(ident));
    }

    @Test
    public void testGetPolicy_DaoEx() throws Exception {
        PfModelException ex = new PfModelException(Status.INTERNAL_SERVER_ERROR, EXPECTED_EXCEPTION);
        when(dao.getFilteredPolicyList(any())).thenThrow(ex);

        assertThatThrownBy(() -> session.getPolicy(ident)).isSameAs(ex);
    }

    @Test
    public void testIsVersionPrefix() {
        assertTrue(SessionData.isVersionPrefix("1"));
        assertTrue(SessionData.isVersionPrefix("12"));
        assertTrue(SessionData.isVersionPrefix("1.2"));
        assertTrue(SessionData.isVersionPrefix("1.23"));

        assertFalse(SessionData.isVersionPrefix("1."));
        assertFalse(SessionData.isVersionPrefix("1.2."));
        assertFalse(SessionData.isVersionPrefix("1.2.3"));
        assertFalse(SessionData.isVersionPrefix("1.2.3."));
        assertFalse(SessionData.isVersionPrefix("1.2.3.4"));
    }

    @Test
    public void testAddRequests_testGetPdpStateChanges_testGetPdpUpdates() {
        // pre-load with a update and state-change for other PDPs
        PdpUpdate update2 = makeUpdate(PDP2);
        session.addUpdate(update2);

        PdpStateChange change3 = makeStateChange(PDP3);
        session.addStateChange(change3);

        // add requests
        PdpUpdate update = makeUpdate(PDP1);
        PdpStateChange change = makeStateChange(PDP1);
        session.addRequests(update, change);
        verifyRequests(update, update2, change, change3);

        /*
         * repeat with a new pair
         */
        update = makeUpdate(PDP1);
        change = makeStateChange(PDP1);
        session.addRequests(update, change);
        verifyRequests(update, update2, change, change3);

        // just make an update this time
        update = makeUpdate(PDP1);
        session.addUpdate(update);
        verifyRequests(update, update2, change, change3);
    }

    private void verifyRequests(PdpUpdate update, PdpUpdate update2, PdpStateChange change, PdpStateChange change3) {
        List<Pair<PdpUpdate, PdpStateChange>> requests = sort(session.getPdpRequests(), this::compare);
        assertEquals(3, requests.size());

        System.out.println(requests);
        System.out.println(update);

        Iterator<Pair<PdpUpdate, PdpStateChange>> reqiter = requests.iterator();
        Pair<PdpUpdate, PdpStateChange> pair = reqiter.next();
        assertSame(update, pair.getLeft());
        assertSame(change, pair.getRight());

        pair = reqiter.next();
        assertSame(update2, pair.getLeft());
        assertSame(null, pair.getRight());

        pair = reqiter.next();
        assertSame(null, pair.getLeft());
        assertSame(change3, pair.getRight());

        // verify individual lists
        List<PdpUpdate> updates = Arrays.asList(update, update2);
        assertEquals(sort(updates, this::compare), sort(session.getPdpUpdates(), this::compare));

        List<PdpStateChange> changes = Arrays.asList(change, change3);
        assertEquals(sort(changes, this::compare), sort(session.getPdpStateChanges(), this::compare));
    }

    @Test
    public void testAddRequests_MismatchedNames() {
        PdpUpdate update = makeUpdate(PDP1);
        PdpStateChange change = makeStateChange(PDP2);
        assertThatIllegalArgumentException().isThrownBy(() -> session.addRequests(update, change))
                        .withMessage("PDP name mismatch pdp_1, pdp_2");
    }

    @Test
    public void testAddUpdate_testGetPdpUpdates() {
        // several different updates, but one duplicate
        PdpUpdate update1 = makeUpdate(PDP1);
        session.addUpdate(update1);

        PdpUpdate update2 = makeUpdate(PDP2);
        session.addUpdate(update2);

        PdpUpdate update3 = makeUpdate(PDP3);
        session.addUpdate(update3);

        List<PdpUpdate> lst = sort(getUpdateRequests(), this::compare);
        assertEquals(Arrays.asList(update1, update2, update3).toString(), lst.toString());

        // overwrite one
        update2 = makeUpdate(PDP2);
        session.addUpdate(update2);

        lst = sort(getUpdateRequests(), this::compare);
        assertEquals(Arrays.asList(update1, update2, update3).toString(), lst.toString());
    }

    @Test
    public void testAddStateChange_testGetPdpStateChanges() {
        // several different changes, but one duplicate
        PdpStateChange change1 = makeStateChange(PDP1);
        session.addStateChange(change1);

        PdpStateChange change2 = makeStateChange(PDP2);
        session.addStateChange(change2);

        PdpStateChange change3 = makeStateChange(PDP3);
        session.addStateChange(change3);

        List<PdpStateChange> lst = sort(getStateChangeRequests(), this::compare);
        assertEquals(Arrays.asList(change1, change2, change3).toString(), lst.toString());

        // overwrite one
        change2 = makeStateChange(PDP2);
        session.addStateChange(change2);

        lst = sort(getStateChangeRequests(), this::compare);
        assertEquals(Arrays.asList(change1, change2, change3).toString(), lst.toString());
    }

    private ToscaPolicyType makePolicyType(String name, String version) {
        ToscaPolicyType type = new ToscaPolicyType();

        type.setName(name);
        type.setVersion(version);

        return type;
    }

    private ToscaPolicy makePolicy(String name, String version) {
        ToscaPolicy policy = new ToscaPolicy();

        policy.setName(name);
        policy.setVersion(version);

        return policy;
    }

    @Test
    public void testCreate() throws Exception {
        assertTrue(session.isUnchanged());

        session.create(group1);
        assertSame(group1, session.getGroup(group1.getName()));
        assertFalse(session.isUnchanged());

        // can add another
        session.create(group2);
        assertSame(group1, session.getGroup(group1.getName()));
        assertSame(group2, session.getGroup(group2.getName()));
        assertFalse(session.isUnchanged());

        // cannot overwrite
        assertThatIllegalStateException().isThrownBy(() -> session.create(group1))
                        .withMessage("group already cached: groupA");
    }

    @Test
    public void testUpdate() throws Exception {
        assertTrue(session.isUnchanged());

        // force the groups into the cache
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1, group2));
        session.getActivePdpGroupsByPolicyType(type);

        /*
         * try group 1
         */
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1));
        PdpGroup newgrp = new PdpGroup(group1);
        session.update(newgrp);
        assertFalse(session.isUnchanged());

        // repeat
        newgrp = new PdpGroup(group1);
        session.update(newgrp);
        assertFalse(session.isUnchanged());

        /*
         * try group 2
         */
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group2));
        newgrp = new PdpGroup(group2);
        session.update(newgrp);
        assertFalse(session.isUnchanged());

        // repeat
        newgrp = new PdpGroup(group2);
        session.update(newgrp);
        assertFalse(session.isUnchanged());
    }

    @Test
    public void testUpdate_NotInCache() throws Exception {
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1));

        assertThatIllegalStateException().isThrownBy(() -> session.update(new PdpGroup(group1)))
                        .withMessage("group not cached: groupA");
    }

    @Test
    public void testGetGroup() throws Exception {
        when(dao.getPdpGroups(GROUP_NAME)).thenReturn(Arrays.asList(group1));

        assertSame(group1, session.getGroup(GROUP_NAME));
        verify(dao).getPdpGroups(any());

        // repeat
        assertSame(group1, session.getGroup(GROUP_NAME));

        // should not access dao again
        verify(dao, times(1)).getPdpGroups(any());
    }

    @Test
    public void testGetGroup_NotFound() throws Exception {
        when(dao.getPdpGroups(GROUP_NAME)).thenReturn(Collections.emptyList());

        assertNull(session.getGroup(GROUP_NAME));
        verify(dao).getPdpGroups(any());

        // repeat
        assertNull(session.getGroup(GROUP_NAME));

        // SHOULD access dao again
        verify(dao, times(2)).getPdpGroups(GROUP_NAME);

        // find it this time
        when(dao.getPdpGroups(GROUP_NAME)).thenReturn(Arrays.asList(group1));
        assertSame(group1, session.getGroup(GROUP_NAME));
        verify(dao, times(3)).getPdpGroups(GROUP_NAME);
    }

    @Test
    public void testGetGroup_DaoEx() throws Exception {
        PfModelException ex = new PfModelException(Status.BAD_REQUEST, EXPECTED_EXCEPTION);
        when(dao.getPdpGroups(GROUP_NAME)).thenThrow(ex);

        assertThatThrownBy(() -> session.getGroup(GROUP_NAME)).isSameAs(ex);
    }

    @Test
    public void testGetActivePdpGroupsByPolicyType() throws Exception {
        List<PdpGroup> groups = Arrays.asList(group1, group2);
        when(dao.getFilteredPdpGroups(any())).thenReturn(groups);

        // repeat
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type));
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type));
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type));

        // only invoked once - should have used the cache for the rest
        verify(dao, times(1)).getFilteredPdpGroups(any());
    }

    @Test
    public void testAddGroup() throws Exception {
        List<PdpGroup> groups = Arrays.asList(group1, group2);
        when(dao.getFilteredPdpGroups(any())).thenReturn(groups);

        // query by each type
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type));
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type2));

        // invoked once for each type
        verify(dao, times(2)).getFilteredPdpGroups(any());

        // repeat - should be no more invocations
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type));
        assertEquals(groups, session.getActivePdpGroupsByPolicyType(type2));
        verify(dao, times(2)).getFilteredPdpGroups(any());
    }

    @Test
    public void testUpdateDb() throws Exception {
        // force the groups into the cache
        PdpGroup group3 = loadGroup("group3.json");
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1, group2, group3));
        session.getActivePdpGroupsByPolicyType(type);

        // create groups 4 & 5
        PdpGroup group4 = loadGroup("group4.json");
        session.create(group4);

        PdpGroup group5 = loadGroup("group5.json");
        session.create(group5);

        // update group 1
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1));
        PdpGroup newgrp1 = new PdpGroup(group1);
        session.update(newgrp1);

        // another update
        newgrp1 = new PdpGroup(newgrp1);
        session.update(newgrp1);

        // update group 3
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group3));
        PdpGroup newgrp3 = new PdpGroup(group3);
        session.update(newgrp3);

        // update group 5
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group5));
        PdpGroup newgrp5 = new PdpGroup(group5);
        session.update(newgrp5);

        // push the changes to the DB
        session.updateDb();

        // expect one create for groups 4 & 5 (group5 replaced by newgrp5)
        List<PdpGroup> creates = getGroupCreates();
        assertEquals(2, creates.size());
        assertSame(group4, creates.get(0));
        assertSame(newgrp5, creates.get(1));

        // expect one update for groups 1 & 3
        List<PdpGroup> updates = getGroupUpdates();
        assertEquals(2, updates.size());
        assertSame(newgrp1, updates.get(0));
        assertSame(newgrp3, updates.get(1));
    }

    @Test
    public void testUpdateDb_Empty() throws Exception {
        // force data into the cache
        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group1, group2));
        session.getActivePdpGroupsByPolicyType(type);

        session.updateDb();
        verify(dao, never()).createPdpGroups(any());
        verify(dao, never()).updatePdpGroups(any());
    }

    @Test
    public void testDeleteGroupFromDb() throws Exception {
        session.deleteGroupFromDb(group1);

        verify(dao).deletePdpGroup(group1.getName());
    }

    @Test
    public void testTrackDeploy() throws PfModelException {
        testTrack(session::getDeployData, session::getUndeployData, session::trackDeploy);
    }

    /**
     * Tests trackDeploy() when there is something in the undeployed list.
     *
     * @throws PfModelException if an error occurs
     */
    @Test
    public void testTrackDeployRemoveUndeploy() throws PfModelException {
        testTrack(session::getDeployData, session::getUndeployData, session::trackUndeploy, session::trackDeploy);
    }

    @Test
    public void testTrackUndeploy() throws PfModelException {
        testTrack(session::getUndeployData, session::getDeployData, session::trackUndeploy);
    }

    /**
     * Tests trackUndeploy() when there is something in the deployed list.
     *
     * @throws PfModelException if an error occurs
     */
    @Test
    public void testTrackUndeployRemoveUndeploy() throws PfModelException {
        testTrack(session::getUndeployData, session::getDeployData, session::trackDeploy, session::trackUndeploy);
    }

    protected void testTrack(Supplier<Collection<PolicyPdpNotificationData>> expected,
                    Supplier<Collection<PolicyPdpNotificationData>> unexpected, TrackEx... trackFuncs)
                    throws PfModelException {

        ToscaPolicy policy = makePolicy(POLICY_NAME, POLICY_VERSION);
        policy.setType(POLICY_TYPE);
        policy.setTypeVersion(POLICY_TYPE_VERSION);

        when(dao.getFilteredPolicyList(any())).thenReturn(Arrays.asList(policy));

        ToscaPolicyIdentifier policyId = new ToscaPolicyIdentifier(POLICY_NAME, POLICY_VERSION);
        List<String> pdps = Arrays.asList(PDP1, PDP2);

        for (TrackEx trackFunc : trackFuncs) {
            trackFunc.accept(policyId, pdps);
        }

        // "unexpected" list should be empty of any PDPs
        Collection<PolicyPdpNotificationData> dataList = unexpected.get();
        assertTrue(dataList.size() <= 1);
        if (!dataList.isEmpty()) {
            PolicyPdpNotificationData data = dataList.iterator().next();
            assertTrue(data.getPdps().isEmpty());
        }

        dataList = expected.get();
        assertEquals(1, dataList.size());

        PolicyPdpNotificationData data = dataList.iterator().next();
        assertEquals(policyId, data.getPolicyId());
        assertEquals(type, data.getPolicyType());
        assertEquals("[pdp_1, pdp_2]", new TreeSet<>(data.getPdps()).toString());
    }

    private PdpUpdate makeUpdate(String pdpName) {
        PdpUpdate update = new PdpUpdate();

        update.setName(pdpName);

        return update;
    }

    private PdpStateChange makeStateChange(String pdpName) {
        PdpStateChange change = new PdpStateChange();

        change.setName(pdpName);

        return change;
    }

    private ToscaPolicyFilter getPolicyFilter() throws Exception {
        ArgumentCaptor<ToscaPolicyFilter> captor = ArgumentCaptor.forClass(ToscaPolicyFilter.class);
        verify(dao).getFilteredPolicyList(captor.capture());

        return captor.getValue();
    }

    private List<PdpUpdate> getUpdateRequests() {
        return session.getPdpUpdates();
    }

    private List<PdpStateChange> getStateChangeRequests() {
        return session.getPdpStateChanges();
    }

    private <T> List<T> sort(Collection<T> collection, Comparator<T> comparator) {
        List<T> lst = new ArrayList<>(collection);
        Collections.sort(lst, comparator);

        return lst;
    }

    private int compare(Pair<PdpUpdate, PdpStateChange> left, Pair<PdpUpdate, PdpStateChange> right) {
        return getName(left).compareTo(getName(right));
    }

    private int compare(PdpUpdate left, PdpUpdate right) {
        return left.getName().compareTo(right.getName());
    }

    private int compare(PdpStateChange left, PdpStateChange right) {
        return left.getName().compareTo(right.getName());
    }

    private String getName(Pair<PdpUpdate, PdpStateChange> pair) {
        return (pair.getKey() != null ? pair.getKey().getName() : pair.getValue().getName());
    }

    @FunctionalInterface
    private static interface TrackEx {
        public void accept(ToscaPolicyIdentifier policyId, Collection<String> pdps) throws PfModelException;
    }
}
