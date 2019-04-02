/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Nordix Foundation.
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
import java.util.Map;

import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap.ModifyReqData;
import org.onap.policy.pap.main.comm.msgdata.StateChangeData;
import org.onap.policy.pap.main.comm.msgdata.UpdateData;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams;

/**
 * Maps a PDP name to requests that modify PDPs.
 */
public class PdpModifyRequestMap {

    /**
     * Maps a PDP name to its request data. An entry is removed once all of the requests within the data have been
     * completed.
     */
    private final Map<String, ModifyReqData> name2data = new HashMap<>();

    /**
     * PDP modification lock.
     */
    private final Object modifyLock;

    /**
     * The configuration parameters.
     */
    private final PdpModifyRequestMapParams params;

    /**
     * Constructs the data.
     *
     * @param params configuration parameters
     *
     * @throws IllegalArgumentException if a required parameter is not set
     */
    public PdpModifyRequestMap(PdpModifyRequestMapParams params) {
        params.validate();

        this.params = params;
        this.modifyLock = params.getModifyLock();
    }

    /**
     * Adds an UPDATE request to the map.
     *
     * @param update the UPDATE request or {@code null}
     */
    public void addRequest(PdpUpdate update) {
        addRequest(update, null);
    }

    /**
     * Adds STATE-CHANGE request to the map.
     *
     * @param stateChange the STATE-CHANGE request or {@code null}
     */
    public void addRequest(PdpStateChange stateChange) {
        addRequest(null, stateChange);
    }

    /**
     * Adds a pair of requests to the map.
     *
     * @param update the UPDATE request or {@code null}
     * @param stateChange the STATE-CHANGE request or {@code null}
     */
    public void addRequest(PdpUpdate update, PdpStateChange stateChange) {
        if (update == null && stateChange == null) {
            return;
        }

        synchronized (modifyLock) {
            String pdpName = getPdpName(update, stateChange);

            ModifyReqData data = name2data.get(pdpName);
            if (data != null) {
                // update the existing request
                data.add(update);
                data.add(stateChange);

            } else {
                data = makeRequestData(update, stateChange);
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
        String pdpName;

        if (update != null) {
            if ((pdpName = update.getName()) == null) {
                throw new IllegalArgumentException("missing name in " + update);
            }

            if (stateChange != null && !pdpName.equals(stateChange.getName())) {
                throw new IllegalArgumentException(
                        "name " + stateChange.getName() + " does not match " + pdpName + " " + stateChange);
            }

        } else {
            if ((pdpName = stateChange.getName()) == null) {
                throw new IllegalArgumentException("missing name in " + stateChange);
            }
        }

        return pdpName;
    }

    /**
     * Determines if two requests are the "same", which is does not necessarily mean "equals".
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
        ArrayList<ToscaPolicy> lst = new ArrayList<>(second.getPolicies());
        lst.removeAll(first.getPolicies());

        return lst.isEmpty();
    }

    /**
     * Determines if two requests are the "same", which is does not necessarily mean "equals".
     *
     * @param first first request to check
     * @param second second request to check
     * @return {@code true} if this update subsumes the other, {@code false} otherwise
     */
    protected static boolean isSame(PdpStateChange first, PdpStateChange second) {
        return (first.getState() == second.getState());
    }

    /**
     * Request data, which contains an UPDATE or a STATE-CHANGE request, or both. The UPDATE is always published before
     * the STATE-CHANGE. In addition, both requests may be changed at any point, possibly triggering a restart of the
     * publishing.
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
         * @param newUpdate the UPDATE message to be sent, or {@code null}
         * @param newState the STATE-CHANGE message to be sent, or {@code null}
         */
        public ModifyReqData(PdpUpdate newUpdate, PdpStateChange newState) {
            super(params);

            if (newUpdate != null) {
                this.stateChange = newState;
                setName(newUpdate.getName());
                update = newUpdate;
                configure(new ModUpdateData(newUpdate));

            } else {
                this.update = null;
                setName(newState.getName());
                stateChange = newState;
                configure(new ModStateChangeData(newState));
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
         * Adds an UPDATE to the request data, replacing any existing UPDATE, if appropriate. If the UPDATE is replaced,
         * then publishing is restarted.
         *
         * @param newRequest the new UPDATE request
         */
        private void add(PdpUpdate newRequest) {
            if (newRequest == null) {
                return;
            }

            synchronized (modifyLock) {
                if (update != null && isSame(update, newRequest)) {
                    // already have this update - discard it
                    return;
                }

                // must restart from scratch
                stopPublishing();

                update = newRequest;
                configure(new ModUpdateData(newRequest));

                startPublishing();
            }
        }

        /**
         * Adds a STATE-CHANGE to the request data, replacing any existing UPDATE, if appropriate. If the STATE-CHANGE
         * is replaced, and we're currently publishing the STATE-CHANGE, then publishing is restarted.
         *
         * @param newRequest the new STATE-CHANGE request
         */
        private void add(PdpStateChange newRequest) {
            if (newRequest == null) {
                return;
            }

            synchronized (modifyLock) {
                if (stateChange != null && isSame(stateChange, newRequest)) {
                    // already have this update - discard it
                    return;
                }

                if (getWrapper() instanceof StateChangeData) {
                    // we were publishing STATE-CHANGE, thus must restart it
                    stopPublishing();

                    stateChange = newRequest;
                    configure(new ModStateChangeData(newRequest));

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
        @Override
        protected void retryCountExhausted() {
            // remove this request data from the PDP request map
            allCompleted();

            // TODO what to do?
        }

        /**
         * Indicates that a response did not match the data.
         *
         * @param reason the reason for the mismatch
         */
        protected void mismatch(String reason) {
            // remove this request data from the PDP request map
            allCompleted();

            // TODO what to do?
        }

        /**
         * Wraps an UPDATE.
         */
        private class ModUpdateData extends UpdateData {

            public ModUpdateData(PdpUpdate message) {
                super(message, params);
            }

            @Override
            public void mismatch(String reason) {
                ModifyReqData.this.mismatch(reason);
            }

            @Override
            public void completed() {
                if (stateChange == null) {
                    // no STATE-CHANGE request - we're done
                    allCompleted();

                } else {
                    // now process the STATE-CHANGE request
                    configure(new ModStateChangeData(stateChange));
                    startPublishing();
                }
            }
        }

        /**
         * Wraps a STATE-CHANGE.
         */
        private class ModStateChangeData extends StateChangeData {

            public ModStateChangeData(PdpStateChange message) {
                super(message, params);
            }

            @Override
            public void mismatch(String reason) {
                ModifyReqData.this.mismatch(reason);
            }

            @Override
            public void completed() {
                allCompleted();
            }
        }
    }

    // these may be overridden by junit tests

    protected ModifyReqData makeRequestData(PdpUpdate update, PdpStateChange stateChange) {
        return new ModifyReqData(update, stateChange);
    }
}
