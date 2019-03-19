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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onap.policy.pdp.common.models.PdpMessage;
import org.onap.policy.pdp.common.models.PdpStateChange;
import org.onap.policy.pdp.common.models.PdpStatus;
import org.onap.policy.pdp.common.models.PdpUpdate;
import org.onap.policy.pdp.common.models.Policy;

/**
 * Maps a PDP name to requests that modify PDPs.
 */
public class PdpModifyRequestMap {

    /**
     * Maps a PDP name to its request data. An entry is removed once all of the requests
     * within the data have been completed.
     */
    private final Map<String, ModifyReqData> name2data = new HashMap<>();

    /**
     * Constructs the data.
     */
    public PdpModifyRequestMap() {
        super();
    }

    /**
     * Adds a pair of requests to the map.
     *
     * @param comm communication objects
     * @param update the UPDATE request or {@code null}
     * @param stateChange the STATE-CHANGE request or {@code null}
     */
    public void addRequest(PdpCommObjects comm, PdpUpdate update, PdpStateChange stateChange) {

        synchronized (comm.getPdpLock()) {
            String pdpName = getPdpName(update, stateChange);
            if (pdpName == null) {
                return;
            }

            ModifyReqData data = name2data.get(pdpName);
            if (data != null) {
                // update the existing request
                data.add(update);
                data.add(stateChange);

            } else {
                data = new ModifyReqData(comm, update, stateChange);
                name2data.put(pdpName, data);
                data.startPublishing();
            }
        }
    }

    /**
     * Gets the PDP name from two requests.
     *
     * @param update the update request, or {@code null}
     * @param stateChange the state-change request, or {@code null}
     * @return the PDP name, or {@code null} if both requests are {@code null}
     */
    private static String getPdpName(PdpUpdate update, PdpStateChange stateChange) {
        String pdpName = null;

        if (update != null) {
            if ((pdpName = update.getName()) == null) {
                throw new IllegalArgumentException("missing name in " + update);
            }

            if (stateChange != null && !pdpName.equals(stateChange.getName())) {
                throw new IllegalArgumentException(
                                "name " + stateChange.getName() + " does not match " + pdpName + " " + stateChange);
            }

        } else if (stateChange != null) {
            if ((pdpName = stateChange.getName()) == null) {
                throw new IllegalArgumentException("missing name in " + stateChange);
            }
        }

        return pdpName;
    }

    /**
     * Determines if two requests are the "same", which is does not necessarily mean
     * "equals".
     *
     * @param first first request to check
     * @param second second request to check
     * @return {@code true} if the requests are the "same", {@code false} otherwise
     */
    protected static boolean isSame(PdpUpdate first, PdpUpdate second) {
        if (first.getPolicies().size() != second.getPolicies().size()) {
            return false;
        }

        if (!first.getPdpGroup().equals(second.getPdpGroup())) {
            return false;
        }

        if (!first.getPdpSubgroup().equals(second.getPdpSubgroup())) {
            return false;
        }

        // see if the other has any policies that this does not have
        ArrayList<Policy> lst = new ArrayList<>(second.getPolicies());
        lst.removeAll(first.getPolicies());

        return lst.isEmpty();
    }

    /**
     * Determines if two requests are the "same", which is does not necessarily mean
     * "equals".
     *
     * @param first first request to check
     * @param second second request to check
     * @return {@code true} if this update subsumes the other, {@code false} otherwise
     */
    protected static boolean isSame(PdpStateChange first, PdpStateChange second) {
        return (first.getState() == second.getState());
    }

    /**
     * Request data, which contains an UPDATE or a STATE-CHANGE request, or both. The
     * UPDATE is always published before the STATE-CHANGE. In addition, both requests may
     * be changed at any point, possibly triggering a restart of the publishing.
     */
    public class ModifyReqData extends RequestData {

        /**
         * The UPDATE message to be published, or {@code null}.
         */
        private PdpUpdate update;

        /**
         * The STATE-CHANGE message to be published, or {@code null}.
         */
        private PdpStateChange stateChange;


        /**
         * Constructs the object.
         *
         * @param comm objects used to communicate with the PDPs
         * @param update the UPDATE message to be sent, or {@code null}
         * @param stateChange the STATE-CHANGE message to be sent, or {@code null}
         */
        public ModifyReqData(PdpCommObjects comm, PdpUpdate update, PdpStateChange stateChange) {
            super(comm);

            if (update != null) {
                this.stateChange = stateChange;
                setName(update.getName());
                configure(new UpdateWrapper(update));

            } else {
                this.update = null;
                setName(stateChange.getName());
                configure(new StateChangeWrapper(stateChange));
            }
        }

        /**
         * Determines if this request is still in the map.
         */
        @Override
        protected boolean isActive() {
            return (name2data.get(getName()) == this);
        }

        /**
         * Removes this request from the map.
         */
        @Override
        protected void allCompleted() {
            name2data.remove(getName(), this);
        }

        /**
         * Adds an UPDATE to the request data, replacing any existing UPDATE, if
         * appropriate. If the UPDATE is replaced, then publishing is restarted.
         *
         * @param newRequest the new UPDATE request
         */
        private void add(PdpUpdate newRequest) {
            if (newRequest == null) {
                return;
            }

            if (!getName().equals(newRequest.getName())) {
                throw new IllegalArgumentException(
                                "request for PDP " + newRequest.getName() + ", but expecting " + getName());
            }

            synchronized (comm.getPdpLock()) {
                if (update != null && isSame(update, newRequest)) {
                    // already have this update - discard it
                    return;
                }

                // must restart from scratch
                stopPublishing();

                configure(new UpdateWrapper(newRequest));

                startPublishing();
            }
        }

        /**
         * Adds a STATE-CHANGE to the request data, replacing any existing UPDATE, if
         * appropriate. If the STATE-CHANGE is replaced, and we're currently publishing
         * the STATE-CHANGE, then publishing is restarted.
         *
         * @param newRequest the new STATE-CHANGE request
         */
        private void add(PdpStateChange newRequest) {
            if (newRequest == null) {
                return;
            }

            if (!getName().equals(newRequest.getName())) {
                throw new IllegalArgumentException(
                                "request for PDP " + newRequest.getName() + ", but expecting " + getName());
            }

            synchronized (comm.getPdpLock()) {
                if (stateChange != null && isSame(stateChange, newRequest)) {
                    // already have this update - discard it
                    return;
                }

                if (getWrapper() instanceof StateChangeWrapper) {
                    // we were publishing STATE-CHANGE, thus must restart it
                    stopPublishing();

                    configure(new StateChangeWrapper(newRequest));

                    startPublishing();

                } else {
                    // haven't started publishing STATE-CHANGE yet, just replace it
                    stateChange = newRequest;
                }
            }
        }

        /**
         * Indicates that the retry count was exhausted.
         */
        protected void retryCountExhausted() {
            // remove this request data from the PDP request map
            allCompleted();

            // TODO should we stop the PDP if we receive no DMaaP response back?
            comm.stopPdp(getName(), "retry count exhausted for " + getWrapper().getType() + " request");
        }

        /**
         * Wraps an UPDATE.
         */
        private class UpdateWrapper implements MessageWrapper {

            public UpdateWrapper(PdpUpdate message) {
                update = message;
            }

            @Override
            public PdpMessage getMessage() {
                return update;
            }

            @Override
            public String getType() {
                return update.getClass().getSimpleName();
            }

            @Override
            public int getMaxRetryCount() {
                return comm.getConfig().getUpdateParameters().getMaxRetryCount();
            }

            @Override
            public TimerManager getTimers() {
                return comm.getUpdateTimers();
            }

            @Override
            public String checkResponse(PdpStatus response) {
                if (!update.getName().equals(response.getName())) {
                    return "name does not match";
                }

                if (!update.getPdpGroup().equals(response.getPdpGroup())) {
                    return "group does not match";
                }

                if (!update.getPdpSubgroup().equals(response.getPdpSubgroup())) {
                    return "subgroup does not match";
                }

                // see if the other has any policies that this does not have
                ArrayList<Policy> lst = new ArrayList<>(response.getPolicies());
                List<Policy> mypolicies = update.getPolicies();

                if (mypolicies.size() != lst.size()) {
                    return "policies do not match";
                }

                lst.removeAll(update.getPolicies());
                if (!lst.isEmpty()) {
                    return "policies do not match";
                }

                return null;
            }

            @Override
            public void completed() {
                if (stateChange == null) {
                    // no STATE-CHANGE request - we're done
                    allCompleted();

                } else {
                    // now process the STATE-CHANGE request
                    configure(new StateChangeWrapper(stateChange));
                    startPublishing();
                }
            }
        }

        /**
         * Wraps a STATE-CHANGE.
         */
        private class StateChangeWrapper implements MessageWrapper {

            public StateChangeWrapper(PdpStateChange message) {
                stateChange = message;
            }

            @Override
            public PdpMessage getMessage() {
                return stateChange;
            }

            @Override
            public String getType() {
                return update.getClass().getSimpleName();
            }

            @Override
            public int getMaxRetryCount() {
                return comm.getConfig().getStateChangeParameters().getMaxRetryCount();
            }

            @Override
            public TimerManager getTimers() {
                return comm.getStateChangeTimers();
            }

            @Override
            public String checkResponse(PdpStatus response) {
                if (!getName().equals(response.getName())) {
                    return "name does not match";
                }

                if (response.getState() != stateChange.getState()) {
                    return "state is " + response.getState() + ", but expected " + stateChange.getState();
                }

                return null;
            }

            @Override
            public void completed() {
                allCompleted();
            }
        }
    }
}
