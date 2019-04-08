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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.msgdata.Request;
import org.onap.policy.pap.main.comm.msgdata.RequestListener;
import org.onap.policy.pap.main.comm.msgdata.StateChangeReq;
import org.onap.policy.pap.main.comm.msgdata.UpdateReq;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;
import org.onap.policy.pap.main.parameters.RequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps a PDP name to requests that modify PDPs.
 */
public class PdpModifyRequestMap {
    private static final Logger logger = LoggerFactory.getLogger(PdpModifyRequestMap.class);

    private static final String UNEXPECTED_BROADCAST = "unexpected broadcast message: ";

    /**
     * Maps a PDP name to its outstanding requests.
     */
    private final Map<String, PdpRequests> pdp2requests = new HashMap<>();

    /**
     * PDP modification lock.
     */
    private final Object modifyLock;

    /**
     * The configuration parameters.
     */
    private final PdpModifyRequestMapParams params;

    /**
     * Factory for PAP DAO.
     */
    private final PolicyModelsProviderFactoryWrapper daoFactory;


    /**
     * Constructs the object.
     *
     * @param params configuration parameters
     *
     * @throws IllegalArgumentException if a required parameter is not set
     */
    public PdpModifyRequestMap(PdpModifyRequestMapParams params) {
        params.validate();

        this.params = params;
        this.modifyLock = params.getModifyLock();
        this.daoFactory = params.getDaoFactory();
    }

    /**
     * Stops publishing requests to the given PDP.
     *
     * @param pdpName PDP name
     */
    public void stopPublishing(String pdpName) {
        synchronized (modifyLock) {
            PdpRequests requests = pdp2requests.remove(pdpName);
            if (requests != null) {
                requests.stopPublishing();
            }
        }
    }

    /**
     * Adds a pair of requests to the map.
     *
     * @param update the UPDATE request or {@code null}
     * @param stateChange the STATE-CHANGE request or {@code null}
     */
    public void addRequest(PdpUpdate update, PdpStateChange stateChange) {
        if (update == null) {
            addRequest(stateChange);

        } else {
            synchronized (modifyLock) {
                addRequest(update);
                addRequest(stateChange);
            }
        }
    }

    /**
     * Adds an UPDATE request to the map.
     *
     * @param update the UPDATE request or {@code null}
     */
    public void addRequest(PdpUpdate update) {
        if (update == null) {
            return;
        }

        if (isBroadcast(update)) {
            throw new IllegalArgumentException(UNEXPECTED_BROADCAST + update);
        }

        // @formatter:off
        RequestParams reqparams = new RequestParams()
            .setMaxRetryCount(params.getParams().getUpdateParameters().getMaxRetryCount())
            .setTimers(params.getUpdateTimers())
            .setModifyLock(params.getModifyLock())
            .setPublisher(params.getPublisher())
            .setResponseDispatcher(params.getResponseDispatcher());
        // @formatter:on

        String name = update.getName() + " " + PdpUpdate.class.getSimpleName();
        UpdateReq request = new UpdateReq(reqparams, name, update);

        addSingleton(request);
    }

    /**
     * Adds a STATE-CHANGE request to the map.
     *
     * @param stateChange the STATE-CHANGE request or {@code null}
     */
    public void addRequest(PdpStateChange stateChange) {
        if (stateChange == null) {
            return;
        }

        if (isBroadcast(stateChange)) {
            throw new IllegalArgumentException(UNEXPECTED_BROADCAST + stateChange);
        }

        // @formatter:off
        RequestParams reqparams = new RequestParams()
            .setMaxRetryCount(params.getParams().getStateChangeParameters().getMaxRetryCount())
            .setTimers(params.getStateChangeTimers())
            .setModifyLock(params.getModifyLock())
            .setPublisher(params.getPublisher())
            .setResponseDispatcher(params.getResponseDispatcher());
        // @formatter:on

        String name = stateChange.getName() + " " + PdpStateChange.class.getSimpleName();
        StateChangeReq request = new StateChangeReq(reqparams, name, stateChange);

        addSingleton(request);
    }

    /**
     * Determines if a message is a broadcast message.
     *
     * @param message the message to examine
     * @return {@code true} if the message is a broadcast message, {@code false} if
     *         destined for a single PDP
     */
    private boolean isBroadcast(PdpMessage message) {
        return StringUtils.isBlank(message.getName());
    }

    /**
     * Configures and adds a request to the map.
     *
     * @param request the request to be added
     */
    private void addSingleton(Request request) {

        synchronized (modifyLock) {
            PdpRequests requests = pdp2requests.computeIfAbsent(request.getMessage().getName(), this::makePdpRequests);

            request.setListener(new SingletonListener(requests, request));
            requests.addSingleton(request);
        }
    }

    /**
     * Starts the next request associated with a PDP.
     *
     * @param requests current set of requests, or {@code null} if there are no requests
     *        for the PDP
     * @param request the request that just completed
     */
    private void startNextRequest(PdpRequests requests, Request request) {
        if (!requests.startNextRequest(request)) {
            pdp2requests.remove(requests.getPdpName(), requests);
        }
    }

    /**
     * Disables a PDP by removing it from its subgroup and then sending it a PASSIVE
     * request.
     *
     * @param requests the requests associated with the PDP to be disabled
     */
    private void disablePdp(PdpRequests requests) {

        // remove the requests from the map
        if (!pdp2requests.remove(requests.getPdpName(), requests)) {
            // don't have the info we need to disable it
            return;
        }

        requests.stopPublishing();

        // don't do anything if we don't have a group
        String name = requests.getLastGroupName();
        if (name == null) {
            return;
        }

        // remove the PDP from the group
        removeFromGroup(requests.getPdpName(), name);

        // send the state change
        PdpStateChange change = new PdpStateChange();
        change.setName(requests.getPdpName());
        change.setState(PdpState.PASSIVE);
        addRequest(change);
    }

    /**
     * Removes a PDP from its group.
     *
     * @param pdpName name of the PDP to be removed
     * @param groupName name of the group from which it should be removed
     */
    private void removeFromGroup(String pdpName, String groupName) {

        try (PolicyModelsProvider dao = daoFactory.create()) {

            PdpGroupFilter filter = PdpGroupFilter.builder().name(groupName).groupState(PdpState.ACTIVE)
                            .version(PdpGroupFilter.LATEST_VERSION).build();

            List<PdpGroup> groups = dao.getFilteredPdpGroups(filter);
            if (groups.isEmpty()) {
                return;
            }

            PdpGroup group = groups.get(0);

            for (PdpSubGroup subgrp : group.getPdpSubgroups()) {
                if (removeFromSubgroup(pdpName, group, subgrp)) {
                    dao.updatePdpGroups(Collections.singletonList(group));
                    return;
                }
            }

        } catch (PfModelException e) {
            logger.info("unable to remove PDP {} from subgroup", pdpName, e);
        }
    }

    /**
     * Removes a PDP from a group.
     *
     * @param pdpName name of the PDP to be removed
     * @param group group from which to attempt to remove the PDP
     * @param subgrp subgroup from which to attempt to remove the PDP
     * @return {@code true} if the PDP was removed, {@code false} if the PDP was not in
     *         the group
     * @throws PfModelException if a DB error occurs
     */
    private boolean removeFromSubgroup(String pdpName, PdpGroup group, PdpSubGroup subgrp) throws PfModelException {

        Iterator<Pdp> iter = subgrp.getPdpInstances().iterator();

        while (iter.hasNext()) {
            Pdp instance = iter.next();

            if (pdpName.equals(instance.getInstanceId())) {
                iter.remove();
                subgrp.setCurrentInstanceCount(subgrp.getPdpInstances().size());
                return true;
            }
        }

        return false;
    }

    /**
     * Creates a new set of requests for a PDP. May be overridden by junit tests.
     *
     * @param pdpName PDP name
     * @return a new set of requests
     */
    protected PdpRequests makePdpRequests(String pdpName) {
        return new PdpRequests(pdpName);
    }

    /**
     * Listener for singleton request events.
     */
    private class SingletonListener implements RequestListener {
        private final PdpRequests requests;
        private final Request request;

        public SingletonListener(PdpRequests requests, Request request) {
            this.requests = requests;
            this.request = request;
        }

        @Override
        public void failure(String pdpName, String reason) {
            if (requests.getPdpName().equals(pdpName)) {
                disablePdp(requests);
            }
        }

        @Override
        public void success(String pdpName) {
            if (requests.getPdpName().equals(pdpName)) {
                if (pdp2requests.get(requests.getPdpName()) == requests) {
                    startNextRequest(requests, request);

                } else {
                    requests.stopPublishing();
                }
            }
        }

        @Override
        public void retryCountExhausted() {
            disablePdp(requests);
        }
    }
}
