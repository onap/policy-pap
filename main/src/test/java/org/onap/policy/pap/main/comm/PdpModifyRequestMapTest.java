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

package org.onap.policy.pap.main.comm;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.pap.main.comm.msgdata.Request;
import org.onap.policy.pap.main.comm.msgdata.RequestListener;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;
import org.powermock.reflect.Whitebox;

public class PdpModifyRequestMapTest extends CommonRequestBase {
    private static final String MY_REASON = "my reason";

    /**
     * Used to capture input to dao.createPdpGroups().
     */
    @Captor
    private ArgumentCaptor<List<PdpGroup>> createCaptor;


    /**
     * Used to capture input to dao.updatePdpGroups().
     */
    @Captor
    private ArgumentCaptor<List<PdpGroup>> updateCaptor;

    /**
     * Used to capture input to undeployer.undeploy().
     */
    @Captor
    private ArgumentCaptor<Collection<ToscaPolicyIdentifier>> undeployCaptor;

    @Mock
    private PdpRequests requests;

    @Mock
    private PolicyUndeployer undeployer;

    private MyMap map;
    private PdpUpdate update;
    private PdpStateChange change;
    private PdpStatus response;

    /**
     * Sets up.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        MockitoAnnotations.initMocks(this);

        response = new PdpStatus();

        update = makeUpdate(PDP1, MY_GROUP, MY_SUBGROUP);
        change = makeStateChange(PDP1, MY_STATE);

        when(requests.getPdpName()).thenReturn(PDP1);
        when(requests.isFirstInQueue(any())).thenReturn(true);

        response.setName(MY_NAME);
        response.setState(MY_STATE);
        response.setPdpGroup(update.getPdpGroup());
        response.setPdpSubgroup(update.getPdpSubgroup());
        response.setPolicies(Collections.emptyList());

        map = new MyMap(mapParams);
        map.setPolicyUndeployer(undeployer);
    }

    @Test
    public void testPdpModifyRequestMap() {
        assertSame(mapParams, Whitebox.getInternalState(map, "params"));
        assertSame(lock, Whitebox.getInternalState(map, "modifyLock"));
        assertSame(daoFactory, Whitebox.getInternalState(map, "daoFactory"));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(map.isEmpty());

        map.addRequest(change);
        assertFalse(map.isEmpty());

        // indicate success
        getListener(getSingletons(1).get(0)).success(PDP1);

        assertTrue(map.isEmpty());
    }

    @Test
    public void testStopPublishing() {
        // try with non-existent PDP
        map.stopPublishing(PDP1);

        // now start a PDP and try it
        map.addRequest(change);
        map.stopPublishing(PDP1);
        verify(requests).stopPublishing();

        // try again - it shouldn't stop publishing again
        map.stopPublishing(PDP1);
        verify(requests, times(1)).stopPublishing();
    }

    @Test
    public void testAddRequestPdpUpdatePdpStateChange_BothNull() {
        // nulls should be ok
        map.addRequest(null, null);
    }

    @Test
    public void testAddRequestPdpUpdatePdpStateChange_NullUpdate() {
        map.addRequest(null, change);

        Request req = getSingletons(1).get(0);
        assertSame(change, req.getMessage());
        assertEquals("pdp_1 PdpStateChange", req.getName());
    }

    @Test
    public void testAddRequestPdpUpdatePdpStateChange_NullStateChange() {
        map.addRequest(update, null);

        Request req = getSingletons(1).get(0);
        assertSame(update, req.getMessage());
        assertEquals("pdp_1 PdpUpdate", req.getName());
    }

    /**
     * Tests addRequest() when two requests are provided and the second is an "activate"
     * message.
     */
    @Test
    public void testAddRequestPdpUpdatePdpStateChange_BothProvided_Active() {
        change.setState(PdpState.ACTIVE);
        map.addRequest(update, change);

        // should have only allocated one request structure
        assertEquals(1, map.nalloc);

        // both requests should have been added
        List<Request> values = getSingletons(2);

        // update should appear first
        Request req = values.remove(0);
        assertSame(update, req.getMessage());
        assertEquals("pdp_1 PdpUpdate", req.getName());

        req = values.remove(0);
        assertSame(change, req.getMessage());
        assertEquals("pdp_1 PdpStateChange", req.getName());
    }

    /**
     * Tests addRequest() when two requests are provided and the second is "deactivate"
     * message.
     */
    @Test
    public void testAddRequestPdpUpdatePdpStateChange_BothProvided_Passive() {
        change.setState(PdpState.PASSIVE);
        map.addRequest(update, change);

        // should have only allocated one request structure
        assertEquals(1, map.nalloc);

        // both requests should have been added
        List<Request> values = getSingletons(2);

        // state-change should appear first
        Request req = values.remove(0);
        assertSame(change, req.getMessage());
        assertEquals("pdp_1 PdpStateChange", req.getName());

        req = values.remove(0);
        assertSame(update, req.getMessage());
        assertEquals("pdp_1 PdpUpdate", req.getName());
    }

    @Test
    public void testAddRequestPdpUpdatePdpStateChange() {
        // null should be ok
        map.addRequest(null, null);

        map.addRequest(change);

        Request req = getSingletons(1).get(0);
        assertSame(change, req.getMessage());
        assertEquals("pdp_1 PdpStateChange", req.getName());

        // broadcast should throw an exception
        change.setName(null);
        assertThatIllegalArgumentException().isThrownBy(() -> map.addRequest(change))
                        .withMessageStartingWith("unexpected broadcast message: PdpStateChange");
    }

    @Test
    public void testAddRequestPdpUpdate() {
        // null should be ok
        map.addRequest((PdpUpdate) null);

        map.addRequest(update);

        Request req = getSingletons(1).get(0);
        assertSame(update, req.getMessage());
        assertEquals("pdp_1 PdpUpdate", req.getName());

        // broadcast should throw an exception
        update.setName(null);
        assertThatIllegalArgumentException().isThrownBy(() -> map.addRequest(update))
                        .withMessageStartingWith("unexpected broadcast message: PdpUpdate");
    }

    @Test
    public void testAddRequestPdpStateChange() {
        // null should be ok
        map.addRequest((PdpStateChange) null);

        map.addRequest(change);

        Request req = getSingletons(1).get(0);
        assertSame(change, req.getMessage());
        assertEquals("pdp_1 PdpStateChange", req.getName());

        // broadcast should throw an exception
        change.setName(null);
        assertThatIllegalArgumentException().isThrownBy(() -> map.addRequest(change))
                        .withMessageStartingWith("unexpected broadcast message: PdpStateChange");
    }

    @Test
    public void testAddSingleton() {
        map.addRequest(change);
        assertEquals(1, map.nalloc);

        // should have one singleton
        getSingletons(1);

        // add another request with the same PDP
        map.addRequest(makeStateChange(PDP1, MY_STATE));
        assertEquals(1, map.nalloc);

        // should now have another singleton
        getSingletons(2);


        // add another request with a different PDP
        map.addRequest(makeStateChange(DIFFERENT, MY_STATE));

        // should now have another allocation
        assertEquals(2, map.nalloc);

        // should now have another singleton
        getSingletons(3);
    }

    @Test
    public void testStartNextRequest_NoMore() {
        map.addRequest(change);

        // indicate success
        getListener(getSingletons(1).get(0)).success(PDP1);

        /*
         * the above should have removed the requests so next time should allocate a new
         * one
         */
        map.addRequest(change);
        assertEquals(2, map.nalloc);
    }

    @Test
    public void testStartNextRequest_HaveMore() {
        map.addRequest(update);
        map.addRequest(change);

        Request updateReq = getSingletons(2).get(0);

        // indicate success with the update
        when(requests.startNextRequest(updateReq)).thenReturn(true);
        getListener(updateReq).success(PDP1);

        // should have started the next request
        verify(requests).startNextRequest(updateReq);

        /*
         * requests should still be there, so adding another request should not allocate a
         * new one
         */
        map.addRequest(update);
        assertEquals(1, map.nalloc);
    }

    @Test
    public void testRemovePdp() throws Exception {
        map.addRequest(update);

        // put the PDP in a group
        PdpGroup group = makeGroup(MY_GROUP);
        group.setPdpSubgroups(Arrays.asList(makeSubGroup(MY_SUBGROUP, PDP1)));

        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group));

        // indicate retries exhausted
        invokeLastRetryHandler(1);

        // should have stopped publishing
        verify(requests).stopPublishing();

        // should have generated a notification
        verify(notifier).removePdp(PDP1);

        // should have removed from the group
        List<PdpGroup> groups = getGroupUpdates();
        assertEquals(1, groups.size());
        assertSame(group, groups.get(0));
        assertEquals(0, group.getPdpSubgroups().get(0).getCurrentInstanceCount());
    }

    @Test
    public void testRemovePdp_NotInGroup() throws PfModelException {
        map.addRequest(update);

        // indicate retries exhausted
        invokeLastRetryHandler(1);

        // should have stopped publishing
        verify(requests).stopPublishing();

        // should not have done any updates
        verify(dao, never()).updatePdpGroups(any());
    }

    @Test
    public void testRemovePdp_AlreadyRemovedFromMap() throws PfModelException {
        map.addRequest(change);
        map.stopPublishing(PDP1);

        // put the PDP in a group
        PdpGroup group = makeGroup(MY_GROUP);
        group.setPdpSubgroups(Arrays.asList(makeSubGroup(MY_SUBGROUP, PDP1)));

        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group));

        invokeLastRetryHandler(1);

        // should have stopped publishing a second time
        verify(requests, times(2)).stopPublishing();

        // should NOT have done any updates
        verify(dao, never()).updatePdpGroups(any());
    }

    @Test
    public void testRemovePdp_NoGroup() throws PfModelException {
        map.addRequest(change);

        invokeLastRetryHandler(1);

        // should not have stopped publishing
        verify(requests).stopPublishing();

        // should not have done any updates
        verify(dao, never()).updatePdpGroups(any());
    }

    @Test
    public void testRemoveFromGroup() throws Exception {
        map.addRequest(change);

        PdpGroup group = makeGroup(MY_GROUP);
        group.setPdpSubgroups(Arrays.asList(makeSubGroup(MY_SUBGROUP + "a", PDP1 + "a"),
                        makeSubGroup(MY_SUBGROUP, PDP1), makeSubGroup(MY_SUBGROUP + "c", PDP1 + "c")));

        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group));

        invokeLastRetryHandler(1);

        // verify that the PDP was removed from the subgroup
        List<PdpGroup> groups = getGroupUpdates();
        assertEquals(1, groups.size());
        assertSame(group, groups.get(0));

        List<PdpSubGroup> subgroups = group.getPdpSubgroups();
        assertEquals(3, subgroups.size());
        assertEquals("[pdp_1a]", getPdpNames(subgroups.get(0)));
        assertEquals("[]", getPdpNames(subgroups.get(1)));
        assertEquals("[pdp_1c]", getPdpNames(subgroups.get(2)));
    }

    @Test
    public void testRemoveFromGroup_DaoEx() throws Exception {
        map.addRequest(change);

        when(dao.getFilteredPdpGroups(any())).thenThrow(makeException());

        invokeLastRetryHandler(1);

        // should still stop publishing
        verify(requests).stopPublishing();

        // requests should have been removed from the map so this should allocate another
        map.addRequest(update);
        assertEquals(2, map.nalloc);
    }

    protected PfModelException makeException() {
        return new PfModelException(Status.BAD_REQUEST, "expected exception");
    }

    @Test
    public void testRemoveFromGroup_NoGroups() throws Exception {
        map.addRequest(change);

        invokeLastRetryHandler(1);

        verify(dao, never()).updatePdpGroups(any());
    }

    @Test
    public void testRemoveFromGroup_NoMatchingSubgroup() throws Exception {
        map.addRequest(change);

        PdpGroup group = makeGroup(MY_GROUP);
        group.setPdpSubgroups(Arrays.asList(makeSubGroup(MY_SUBGROUP, DIFFERENT)));

        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group));

        invokeLastRetryHandler(1);

        verify(dao, never()).updatePdpGroups(any());
    }

    @Test
    public void testRemoveFromSubgroup() throws Exception {
        map.addRequest(change);

        PdpGroup group = makeGroup(MY_GROUP);
        group.setPdpSubgroups(Arrays.asList(makeSubGroup(MY_SUBGROUP, PDP1, PDP1 + "x", PDP1 + "y")));

        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group));

        invokeLastRetryHandler(1);

        // verify that the PDP was removed from the subgroup
        List<PdpGroup> groups = getGroupUpdates();
        assertEquals(1, groups.size());
        assertSame(group, groups.get(0));

        PdpSubGroup subgroup = group.getPdpSubgroups().get(0);
        assertEquals(2, subgroup.getCurrentInstanceCount());
        assertEquals("[pdp_1x, pdp_1y]", getPdpNames(subgroup));
    }

    @Test
    public void testMakePdpRequests() {
        // this should invoke the real method without throwing an exception
        new PdpModifyRequestMap(mapParams).addRequest(change);

        QueueToken<PdpMessage> token = queue.poll();
        assertNotNull(token);
        assertSame(change, token.get());

        verify(dispatcher).register(eq(change.getRequestId()), any());
        verify(timers).register(eq(change.getRequestId()), any());
    }

    @Test
    public void testSingletonListenerFailure() throws Exception {
        map.addRequest(change);

        // invoke the method
        invokeFailureHandler(1);

        verify(undeployer, never()).undeploy(any(), any(), any());
        verify(requests, never()).stopPublishing();

        // requests should have been removed from the map so this should allocate another
        map.addRequest(update);
        assertEquals(2, map.nalloc);
    }

    /**
     * Tests Listener.failure() when something has to be undeployed.
     */
    @Test
    public void testSingletonListenerFailureUndeploy() throws Exception {

        ToscaPolicyIdentifier ident = new ToscaPolicyIdentifier("undeployed", "2.3.4");
        ToscaPolicy policy = mock(ToscaPolicy.class);
        when(policy.getIdentifier()).thenReturn(ident);

        // add some policies to the update
        update.setPolicies(Arrays.asList(policy));

        map.addRequest(update);

        /*
         * Reconfigure the request when undeploy() is called. Also arrange for undeploy()
         * to throw an exception.
         */
        Request req = getSingletons(1).get(0);

        doAnswer(ans -> {
            PdpUpdate update2 = new PdpUpdate(update);
            update2.setPolicies(Collections.emptyList());
            assertTrue(req.reconfigure(update2));
            throw makeException();
        }).when(undeployer).undeploy(any(), any(), any());

        // indicate that all policies failed (because response has no policies)
        response.setName(PDP1);
        req.setNotifier(notifier);
        req.checkResponse(response);

        // invoke the method
        invokeFailureHandler(1);

        verify(undeployer).undeploy(eq(MY_GROUP), eq(MY_SUBGROUP), undeployCaptor.capture());
        assertEquals(Arrays.asList(ident).toString(), undeployCaptor.getValue().toString());

        // no effect on the map
        map.addRequest(update);
        assertEquals(1, map.nalloc);
    }

    /**
     * Tests Listener.failure() when something has to be undeployed, but the message
     * remains unchanged.
     */
    @Test
    public void testSingletonListenerFailureUndeployMessageUnchanged() throws Exception {

        ToscaPolicyIdentifier ident = new ToscaPolicyIdentifier("msg-unchanged", "8.7.6");
        ToscaPolicy policy = mock(ToscaPolicy.class);
        when(policy.getIdentifier()).thenReturn(ident);

        // add some policies to the update
        update.setPolicies(Arrays.asList(policy));

        map.addRequest(update);

        // indicate that all policies failed (because response has no policies)
        response.setName(PDP1);
        Request req = getSingletons(1).get(0);
        req.setNotifier(notifier);
        req.checkResponse(response);

        // invoke the method
        invokeFailureHandler(1);

        verify(undeployer).undeploy(eq(MY_GROUP), eq(MY_SUBGROUP), undeployCaptor.capture());
        assertEquals(Arrays.asList(ident).toString(), undeployCaptor.getValue().toString());

        // requests should have been removed from the map so this should allocate another
        map.addRequest(update);
        assertEquals(2, map.nalloc);
    }

    @Test
    public void testSingletonListenerSuccess() throws Exception {
        map.addRequest(change);

        // invoke the method
        invokeSuccessHandler(1);

        verify(requests, never()).stopPublishing();

        // requests should have been removed from the map so this should allocate another
        map.addRequest(update);
        assertEquals(2, map.nalloc);
    }

    @Test
    public void testRequestCompleted_LastRequest() throws Exception {
        map.addRequest(change);

        // invoke the method
        invokeSuccessHandler(1);

        verify(requests, never()).stopPublishing();

        // requests should have been removed from the map so this should allocate another
        map.addRequest(update);
        assertEquals(2, map.nalloc);
    }

    @Test
    public void testRequestCompleted_NameMismatch() throws Exception {
        // use a different name
        when(requests.getPdpName()).thenReturn(DIFFERENT);

        map.addRequest(change);

        // put the PDP in a group
        PdpGroup group = makeGroup(MY_GROUP);
        group.setPdpSubgroups(Arrays.asList(makeSubGroup(MY_SUBGROUP, PDP1, DIFFERENT)));

        when(dao.getFilteredPdpGroups(any())).thenReturn(Arrays.asList(group));

        // invoke the method - with a different name (i.e., PDP1 instead of DIFFERENT)
        invokeSuccessHandler(1);

        verify(requests, never()).stopPublishing();

        // no effect on the map
        map.addRequest(update);
        assertEquals(1, map.nalloc);

        // no updates
        verify(dao, never()).updatePdpGroups(any());
    }

    @Test
    public void testRequestCompleted_AlreadyStopped() throws Exception {
        map.addRequest(change);

        map.stopPublishing(PDP1);

        // invoke the method
        invokeSuccessHandler(1);

        // should have called this a second time
        verify(requests, times(2)).stopPublishing();

        // requests should have been removed from the map so this should allocate another
        map.addRequest(update);
        assertEquals(2, map.nalloc);
    }

    @Test
    public void testRequestCompleted_NotFirstInQueue() throws Exception {
        map.addRequest(change);

        when(requests.isFirstInQueue(any())).thenReturn(false);

        // invoke the method
        invokeSuccessHandler(1);

        // should not have called this
        verify(requests, never()).stopPublishing();

        // no effect on the map
        map.addRequest(update);
        assertEquals(1, map.nalloc);
    }

    @Test
    public void testSingletonListenerRetryCountExhausted() throws Exception {
        map.addRequest(change);

        // invoke the method
        invokeLastRetryHandler(1);

        verify(requests).stopPublishing();
    }


    /**
     * Invokes the first request's listener.success() method.
     *
     * @param count expected number of requests
     */
    private void invokeSuccessHandler(int count) {
        getListener(getSingletons(count).get(0)).success(PDP1);
    }

    /**
     * Invokes the first request's listener.failure() method.
     *
     * @param count expected number of requests
     */
    private void invokeFailureHandler(int count) {
        getListener(getSingletons(count).get(0)).failure(PDP1, MY_REASON);
    }

    /**
     * Invokes the first request's listener.retryCountExhausted() method.
     *
     * @param count expected number of requests
     */
    private void invokeLastRetryHandler(int count) {
        getListener(getSingletons(count).get(0)).retryCountExhausted();
    }

    /**
     * Gets the name of the PDPs contained within a subgroup.
     *
     * @param subgroup subgroup of interest
     * @return the name of the PDPs contained within the subgroup
     */
    private String getPdpNames(PdpSubGroup subgroup) {
        return subgroup.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toList()).toString();
    }

    /**
     * Gets the singleton requests added to {@link #requests}.
     *
     * @param count number of singletons expected
     * @return the singleton requests
     */
    private List<Request> getSingletons(int count) {
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);

        verify(requests, times(count)).addSingleton(captor.capture());
        return captor.getAllValues();
    }

    /**
     * Gets the listener from a request.
     *
     * @param request request of interest
     * @return the request's listener
     */
    private RequestListener getListener(Request request) {
        return Whitebox.getInternalState(request, "listener");
    }

    private PdpGroup makeGroup(String name) {
        PdpGroup group = new PdpGroup();

        group.setName(name);

        return group;
    }

    private PdpSubGroup makeSubGroup(String pdpType, String... pdpNames) {
        PdpSubGroup subgroup = new PdpSubGroup();

        subgroup.setPdpType(pdpType);
        subgroup.setPdpInstances(Arrays.asList(pdpNames).stream().map(this::makePdp).collect(Collectors.toList()));

        return subgroup;
    }

    private Pdp makePdp(String pdpName) {
        Pdp pdp = new Pdp();
        pdp.setInstanceId(pdpName);

        return pdp;
    }

    /**
     * Gets the input to the method.
     *
     * @return the input that was passed to the dao.updatePdpGroups() method
     * @throws Exception if an error occurred
     */
    private List<PdpGroup> getGroupUpdates() throws Exception {
        verify(dao).updatePdpGroups(updateCaptor.capture());

        return copyList(updateCaptor.getValue());
    }

    /**
     * Copies a list and sorts it by group name.
     *
     * @param source source list to copy
     * @return a copy of the source list
     */
    private List<PdpGroup> copyList(List<PdpGroup> source) {
        List<PdpGroup> newlst = new ArrayList<>(source);
        Collections.sort(newlst, (left, right) -> left.getName().compareTo(right.getName()));
        return newlst;
    }

    private class MyMap extends PdpModifyRequestMap {
        /**
         * Number of times requests were allocated.
         */
        private int nalloc = 0;

        public MyMap(PdpModifyRequestMapParams params) {
            super(params);
        }

        @Override
        protected PdpRequests makePdpRequests(String pdpName) {
            ++nalloc;
            return requests;
        }
    }
}
