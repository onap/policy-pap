/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.main.comm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Setter;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.msgdata.Request;
import org.onap.policy.pap.main.comm.msgdata.RequestListener;
import org.onap.policy.pap.main.comm.msgdata.StateChangeReq;
import org.onap.policy.pap.main.comm.msgdata.UpdateReq;
import org.onap.policy.pap.main.notification.DeploymentStatus;
import org.onap.policy.pap.main.notification.PolicyNotifier;
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
     * Used to notify when policy updates completes.
     */
    private final PolicyNotifier policyNotifier;

    /**
     * Used to undeploy policies from the system, when they cannot be deployed to a PDP.
     *
     * <p/>
     * Note: there's a "catch-22" here. The request map needs an undeployer, but the
     * undeployer needs the request map. Consequently, the request map is created first,
     * then the undeployer, and finally, this field is set.
     */
    @Setter
    private PolicyUndeployer policyUndeployer;


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
        this.policyNotifier = params.getPolicyNotifier();
    }

    /**
     * Determines if the map contains any requests.
     *
     * @return {@code true} if the map is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return pdp2requests.isEmpty();
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

        } else if (stateChange == null) {
            addRequest(update);

        } else if (stateChange.getState() == PdpState.ACTIVE) {
            // publish update before activating
            synchronized (modifyLock) {
                addRequest(update);
                addRequest(stateChange);
            }

        } else {
            // deactivate before publishing update
            synchronized (modifyLock) {
                addRequest(stateChange);
                addRequest(update);
            }
        }
    }

    /**
     * Adds an UPDATE request to the map.
     *
     * @param update the UPDATE request or {@code null}
     * @return the new request (his should only be used by junit tests)
     */
    public Request addRequest(PdpUpdate update) {
        if (update == null) {
            return null;
        }

        if (isBroadcast(update)) {
            throw new IllegalArgumentException(UNEXPECTED_BROADCAST + update);
        }

        // @formatter:off
        RequestParams reqparams = new RequestParams()
            .setMaxRetryCount(params.getParams().getUpdateParameters().getMaxRetryCount())
            .setTimers(params.getUpdateTimers())
            .setModifyLock(params.getModifyLock())
            .setPdpPublisher(params.getPdpPublisher())
            .setResponseDispatcher(params.getResponseDispatcher());
        // @formatter:on

        String name = update.getName() + " " + PdpUpdate.class.getSimpleName();
        var request = new UpdateReq(reqparams, name, update);

        addSingleton(request);
        return request;
    }

    /**
     * Adds a STATE-CHANGE request to the map.
     *
     * @param stateChange the STATE-CHANGE request or {@code null}
     * @return the new request (his should only be used by junit tests)
     */
    public Request addRequest(PdpStateChange stateChange) {
        if (stateChange == null) {
            return null;
        }

        if (isBroadcast(stateChange)) {
            throw new IllegalArgumentException(UNEXPECTED_BROADCAST + stateChange);
        }

        // @formatter:off
        RequestParams reqparams = new RequestParams()
            .setMaxRetryCount(params.getParams().getStateChangeParameters().getMaxRetryCount())
            .setTimers(params.getStateChangeTimers())
            .setModifyLock(params.getModifyLock())
            .setPdpPublisher(params.getPdpPublisher())
            .setResponseDispatcher(params.getResponseDispatcher());
        // @formatter:on

        String name = stateChange.getName() + " " + PdpStateChange.class.getSimpleName();
        var request = new StateChangeReq(reqparams, name, stateChange);

        addSingleton(request);
        return request;
    }

    /**
     * Determines if a message is a broadcast message.
     *
     * @param message the message to examine
     * @return {@code true} if the message is a broadcast message, {@code false} if
     *         destined for a single PDP
     */
    private boolean isBroadcast(PdpMessage message) {
        return (message.getName() == null);
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
     * Removes expired PDPs from all active groups.
     */
    public void removeExpiredPdps() {

        synchronized (modifyLock) {
            logger.info("check for PDP records older than {}ms", params.getMaxPdpAgeMs());

            try (PolicyModelsProvider dao = daoFactory.create()) {

                PdpGroupFilter filter = PdpGroupFilter.builder().groupState(PdpState.ACTIVE).build();
                List<PdpGroup> groups = dao.getFilteredPdpGroups(filter);
                List<PdpGroup> updates = new ArrayList<>(1);

                var status = new DeploymentStatus(dao);

                Instant minAge = Instant.now().minusMillis(params.getMaxPdpAgeMs());

                for (PdpGroup group : groups) {
                    Set<String> pdps = removeFromGroup(minAge, group);
                    if (!pdps.isEmpty()) {
                        updates.add(group);
                        status.loadByGroup(group.getName());
                        pdps.forEach(status::deleteDeployment);
                    }
                }

                if (!updates.isEmpty()) {
                    dao.updatePdpGroups(updates);

                    var notification = new PolicyNotification();
                    status.flush(notification);

                    policyNotifier.publish(notification);
                }

            } catch (PfModelException e) {
                logger.warn("failed to remove expired PDPs", e);
            }
        }
    }

    /**
     * Removes expired PDPs from a group.
     *
     * @param minAge minimum age for active PDPs
     * @param group group from which expired PDPs should be removed
     * @return he expired PDPs
     */
    private Set<String> removeFromGroup(Instant minAge, PdpGroup group) {
        Set<String> pdps = new HashSet<>();
        for (PdpSubGroup subgrp : group.getPdpSubgroups()) {
            removeFromSubgroup(minAge, group, subgrp, pdps);
        }

        return pdps;
    }

    /**
     * Removes expired PDPs from a subgroup.
     *
     * @param minAge minimum age for active PDPs
     * @param group group from which to attempt to remove the PDP
     * @param subgrp subgroup from which to attempt to remove the PDP
     * @param pdps where to place the expired PDPs
     */
    private void removeFromSubgroup(Instant minAge, PdpGroup group, PdpSubGroup subgrp, Set<String> pdps) {

        Iterator<Pdp> iter = subgrp.getPdpInstances().iterator();

        while (iter.hasNext()) {
            Pdp instance = iter.next();

            if (instance.getLastUpdate().isBefore(minAge)) {
                String pdpName = instance.getInstanceId();
                logger.info("removed {} from group={} subgroup={}", pdpName, group.getName(), subgrp.getPdpType());
                iter.remove();
                subgrp.setCurrentInstanceCount(subgrp.getPdpInstances().size());
                pdps.add(pdpName);
            }
        }
    }

    /**
     * Creates a new set of requests for a PDP. May be overridden by junit tests.
     *
     * @param pdpName PDP name
     * @return a new set of requests
     */
    protected PdpRequests makePdpRequests(String pdpName) {
        return new PdpRequests(pdpName, policyNotifier);
    }

    /**
     * Listener for singleton request events.
     */
    private class SingletonListener implements RequestListener {
        private final PdpRequests requests;
        private final Request request;
        private final String pdpName;

        public SingletonListener(PdpRequests requests, Request request) {
            this.requests = requests;
            this.request = request;
            this.pdpName = requests.getPdpName();
        }

        @Override
        public void failure(String responsePdpName, String reason) {
            Collection<ToscaConceptIdentifier> undeployPolicies = requestCompleted(responsePdpName);
            if (undeployPolicies.isEmpty()) {
                // nothing to undeploy
                return;
            }

            /*
             * Undeploy the extra policies. Note: this will likely cause a new message to
             * be assigned to the request, thus we must re-start it after making the
             * change.
             */
            PdpMessage oldmsg = request.getMessage();

            try {
                logger.warn("undeploy policies from {}:{} that failed to deploy: {}", oldmsg.getPdpGroup(),
                                oldmsg.getPdpSubgroup(), undeployPolicies);
                policyUndeployer.undeploy(oldmsg.getPdpGroup(), oldmsg.getPdpSubgroup(), undeployPolicies);
            } catch (PfModelException | RuntimeException e) {
                logger.error("cannot undeploy policies {}", undeployPolicies, e);
            }

            if (request.getMessage() == oldmsg) {
                // message is unchanged - start the next request
                startNextRequest(request);
            } else {
                // message changed - restart the request
                request.startPublishing();
            }
        }

        @Override
        public void success(String responsePdpName) {
            requestCompleted(responsePdpName);
        }

        /**
         * Handles a request completion, starting the next request, if there is one.
         *
         * @param responsePdpName name of the PDP provided in the response
         * @return a list of policies to be undeployed
         */
        private Collection<ToscaConceptIdentifier> requestCompleted(String responsePdpName) {
            if (!pdpName.equals(responsePdpName)) {
                return Collections.emptyList();
            }

            if (pdp2requests.get(pdpName) != requests) {
                logger.info("discard old requests for {}", responsePdpName);
                requests.stopPublishing();
                return Collections.emptyList();
            }

            if (!requests.isFirstInQueue(request)) {
                logger.error("request is not first in the queue {}", request.getMessage());
                return Collections.emptyList();
            }

            Collection<ToscaConceptIdentifier> undeployPolicies = request.getUndeployPolicies();
            if (undeployPolicies.isEmpty()) {
                // nothing to undeploy - just start the next request
                startNextRequest(request);
            }

            return undeployPolicies;
        }

        @Override
        public void retryCountExhausted(Request request) {
            if (pdp2requests.get(pdpName) == requests) {
                requests.stopPublishing();
                startNextRequest(request);
            }
        }

        /**
         * Starts the next request associated with a PDP.
         *
         * @param request the request that just completed
         */
        private void startNextRequest(Request request) {
            if (!requests.startNextRequest(request)) {
                pdp2requests.remove(pdpName, requests);
            }
        }
    }
}
