/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020-2021 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.comm.msgdata.Request;
import org.onap.policy.pap.main.comm.msgdata.RequestListener;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.class)
public class PdpModifyRequestMapTest extends CommonRequestBase {
    private static final String MY_REASON = "my reason";
    private static final int EXPIRED_SECONDS = 100;

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
    private ArgumentCaptor<Collection<ToscaConceptIdentifier>> undeployCaptor;

    @Mock
    private PdpRequests requests;

    @Mock
    private PolicyUndeployer undeployer;

    @Mock
    private PdpStatusMessageHandler responseHandler;

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
    @Override
    public void setUp() throws Exception {
        super.setUp();

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
        getListener(getSingletons(1).get(0)).success(PDP1, response);

        assertTrue(map.isEmpty());
        verify(responseHandler, never()).handlePdpStatus(response);
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
        Assertions.assertThatCode(() -> map.addRequest(null, null)).doesNotThrowAnyException();
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
        getListener(getSingletons(1).get(0)).success(PDP1, response);

        verify(responseHandler, never()).handlePdpStatus(response);

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
        getListener(updateReq).success(PDP1, response);

        // should be called for the update
        verify(responseHandler).handlePdpStatus(response);

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
    public void testRemoveExpiredPdps() throws Exception {
        PdpGroup group1 = makeGroup(MY_GROUP);
        group1.setPdpSubgroups(List.of(makeSubGroup(MY_SUBGROUP, PDP1)));

        PdpGroup group2 = makeGroup(MY_GROUP2);
        group2.setPdpSubgroups(List.of(makeSubGroup(MY_SUBGROUP, PDP2, PDP3), makeSubGroup(MY_SUBGROUP2, PDP4)));

        // expire all items in group2's first subgroup
        Instant expired = Instant.now().minusSeconds(EXPIRED_SECONDS);
        group2.getPdpSubgroups().get(0).getPdpInstances().forEach(pdp -> pdp.setLastUpdate(expired));

        when(dao.getFilteredPdpGroups(any())).thenReturn(List.of(group1, group2));

        // run it
        map.removeExpiredPdps();

        // should have removed from the group
        List<PdpGroup> groups = getGroupUpdates();
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0)).isSameAs(group2);
        assertThat(group2.getPdpSubgroups()).hasSize(2);

        final Iterator<PdpSubGroup> iter = group2.getPdpSubgroups().iterator();

        PdpSubGroup subgrp = iter.next();
        assertThat(subgrp.getPdpInstances()).hasSize(0);
        assertThat(subgrp.getCurrentInstanceCount()).isEqualTo(0);

        subgrp = iter.next();
        assertThat(subgrp.getPdpInstances()).hasSize(1);
        assertThat(subgrp.getCurrentInstanceCount()).isEqualTo(1);
        assertThat(subgrp.getPdpInstances().get(0).getInstanceId()).isEqualTo(PDP4);
    }

    @Test
    public void testRemoveExpiredPdps_NothingExpired() throws Exception {
        PdpGroup group1 = makeGroup(MY_GROUP);
        group1.setPdpSubgroups(List.of(makeSubGroup(MY_SUBGROUP, PDP1)));

        when(dao.getFilteredPdpGroups(any())).thenReturn(List.of(group1));

        // run it
        map.removeExpiredPdps();

        verify(dao, never()).updatePdpGroups(any());
        verify(publisher, never()).enqueue(any());
    }

    @Test
    public void testRemoveExpiredPdps_DaoEx() throws Exception {
        when(dao.getFilteredPdpGroups(any())).thenThrow(makeException());

        assertThatCode(map::removeExpiredPdps).doesNotThrowAnyException();
    }

    @Test
    public void testRemoveFromSubgroup() throws Exception {
        PdpGroup group = makeGroup(MY_GROUP);
        group.setPdpSubgroups(List.of(makeSubGroup(MY_SUBGROUP, PDP1, PDP2, PDP3)));

        // expire pdp1 and pdp3
        Instant expired = Instant.now().minusSeconds(EXPIRED_SECONDS);
        List<Pdp> pdps = group.getPdpSubgroups().get(0).getPdpInstances();
        pdps.get(0).setLastUpdate(expired);
        pdps.get(2).setLastUpdate(expired);

        when(dao.getFilteredPdpGroups(any())).thenReturn(List.of(group));

        // run it
        map.removeExpiredPdps();

        // should have removed from the group
        List<PdpGroup> groups = getGroupUpdates();
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0)).isSameAs(group);
        assertThat(group.getPdpSubgroups()).hasSize(1);
        assertThat(group.getPdpSubgroups().get(0).getCurrentInstanceCount()).isEqualTo(1);

        pdps = group.getPdpSubgroups().get(0).getPdpInstances();
        assertThat(pdps).hasSize(1);
        assertThat(pdps.get(0).getInstanceId()).isEqualTo(PDP2);
    }

    protected PfModelException makeException() {
        return new PfModelException(Status.BAD_REQUEST, "expected exception");
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

        ToscaConceptIdentifier ident = new ToscaConceptIdentifier("undeployed", "2.3.4");
        ToscaPolicy policy = mock(ToscaPolicy.class);
        when(policy.getIdentifier()).thenReturn(ident);

        // add some policies to the update
        update.setPoliciesToBeDeployed(Arrays.asList(policy));

        map.addRequest(update);

        /*
         * Reconfigure the request when undeploy() is called. Also arrange for undeploy()
         * to throw an exception.
         */
        Request req = getSingletons(1).get(0);

        doAnswer(ans -> {
            PdpUpdate update2 = new PdpUpdate(update);
            update2.setPoliciesToBeDeployed(Collections.emptyList());
            update2.setPoliciesToBeUndeployed(Arrays.asList(policy.getIdentifier()));
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

        ToscaConceptIdentifier ident = new ToscaConceptIdentifier("msg-unchanged", "8.7.6");
        ToscaPolicy policy = mock(ToscaPolicy.class);
        when(policy.getIdentifier()).thenReturn(ident);

        // add some policies to the update
        update.setPoliciesToBeDeployed(Arrays.asList(policy));

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
        final var request = map.addRequest(change);

        // invoke the method
        invokeLastRetryHandler(1, request);

        verify(requests).stopPublishing();
    }


    /**
     * Invokes the first request's listener.success() method.
     *
     * @param count expected number of requests
     */
    private void invokeSuccessHandler(int count) {
        getListener(getSingletons(count).get(0)).success(PDP1, response);
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
     * @param request request whose count was exhausted
     */
    private void invokeLastRetryHandler(int count, Request request) {
        getListener(getSingletons(count).get(0)).retryCountExhausted(request);
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
        subgroup.setCurrentInstanceCount(pdpNames.length);
        subgroup.setPdpInstances(Arrays.asList(pdpNames).stream().map(this::makePdp).collect(Collectors.toList()));

        return subgroup;
    }

    private Pdp makePdp(String pdpName) {
        Pdp pdp = new Pdp();
        pdp.setInstanceId(pdpName);
        pdp.setLastUpdate(Instant.now());

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

        @Override
        protected PdpStatusMessageHandler makePdpResponseHandler() {
            return responseHandler;
        }
    }
}
