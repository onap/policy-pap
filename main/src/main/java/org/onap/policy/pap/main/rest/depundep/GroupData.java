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

package org.onap.policy.pap.main.rest.depundep;

import lombok.Getter;
import org.onap.policy.models.pdp.concepts.PdpGroup;

/**
 * PdpGroup data, only the latest copy of the group is retained.
 */
public class GroupData {
    @Getter
    private PdpGroup group;

    private enum State {
        UNCHANGED, CREATED, UPDATED
    }

    /**
     * State of the group.
     */
    private State state;


    /**
     * Constructs the object, for an existing group.
     *
     * @param group the group that is in the DB
     */
    public GroupData(PdpGroup group) {
        this(group, false);
    }

    /**
     * Constructs the object.
     *
     * @param group the group that is in, or to be added to, the DB
     * @param isNew {@code true} if this is a new group, {@code false} otherwise
     */
    public GroupData(PdpGroup group, boolean isNew) {
        this.group = group;
        this.state = (isNew ? State.CREATED : State.UNCHANGED);
    }

    /**
     * Determines if the group is new.
     *
     * @return {@code true} if the group is new, {@code false} otherwise
     */
    public boolean isNew() {
        return (state == State.CREATED);
    }

    /**
     * Determines if the group has been updated.
     *
     * @return {@code true} if the group has been updated, {@code false} otherwise
     */
    public boolean isUpdated() {
        return (state == State.UPDATED);
    }

    /**
     * Updates the group to the new value.
     *
     * @param newGroup the updated group
     */
    public void update(PdpGroup newGroup) {
        if (!this.group.getName().equals(newGroup.getName())) {
            throw new IllegalArgumentException(
                            "expected group " + this.group.getName() + ", but received " + newGroup.getName());
        }

        this.group = newGroup;

        if (state == State.UNCHANGED) {
            state = State.UPDATED;
        }
    }
}
