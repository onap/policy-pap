/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.main.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;

@Getter
@AllArgsConstructor
public class StatusAction {
    public enum Action {
        // existing record; matches what is in the DB
        UNCHANGED,
        // new record; to be added to the DB
        CREATED,
        // existing record; to be updated
        UPDATED,
        // existing record; to be deleted
        DELETED,
    }

    @Setter
    private Action action;

    private PdpPolicyStatus status;

    /**
     * Sets the action to indicate that a field within the contained status has changed.
     */
    public void setChanged() {
        /*
         * if it's a new record (i.e., action=CREATED), then it should remain new. In all
         * other cases (even action=DELETED), it should be marked for update
         */
        if (action != Action.CREATED) {
            action = Action.UPDATED;
        }
    }
}
