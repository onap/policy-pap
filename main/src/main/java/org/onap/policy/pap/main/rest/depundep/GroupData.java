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
import lombok.Setter;
import org.onap.policy.common.utils.validation.Version;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.enums.PdpState;

/**
 * PdpGroup data, which includes the old group, that's in the DB, and possibly a new
 * group, that must be added to the DB.
 */
@Getter
public class GroupData {
    private final PdpGroup oldGroup;

    /**
     * Starts out pointing to {@link #oldGroup}, but then changed to point to the new
     * group, if {@link #makeNewGroup()} is invoked.
     */
    private PdpGroup currentGroup;

    /**
     * Latest version of this group.
     */
    @Setter
    private Version latestVersion;


    /**
     * Constructs the object.
     *
     * @param group the group that is in the DB
     */
    public GroupData(PdpGroup group) {
        this.oldGroup = group;
        this.currentGroup = group;
    }

    /**
     * Determines if a new version of this group has been created (i.e.,
     * {@link #makeNewGroup()} has been invoked.
     *
     * @return {@code true} if a new version of the group has been created, {@code false}
     *         otherwise
     */
    public boolean isNew() {
        return (currentGroup != oldGroup);
    }

    /**
     * Indicates that there is a new group.
     *
     * @throws IllegalArgumentException if the new group has a different name than the old
     *         group
     * @throws IllegalStateException if {@link #setLatestVersion(Version)} has not been
     *         invoked yet
     */
    public void setNewGroup(PdpGroup newGroup) {
        if (!currentGroup.getName().equals(newGroup.getName())) {
            throw new IllegalArgumentException("attempt to change group name from " + currentGroup.getName() + " to "
                            + newGroup.getName());
        }

        if (currentGroup == oldGroup) {
            // first time to create a new group - bump the version
            if (latestVersion == null) {
                throw new IllegalStateException("latestVersion not set for group: " + oldGroup.getName());
            }

            latestVersion = latestVersion.newVersion();
            oldGroup.setPdpGroupState(PdpState.PASSIVE);
        }

        currentGroup = newGroup;
        currentGroup.setVersion(latestVersion.toString());
    }
}
