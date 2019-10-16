/*-
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

package org.onap.policy.pap.main.notification;

/**
 * Tracker for policy undeployments from PDPs.
 *
 * <p/>
 * Policies are removed from the internal map when they are no longer waiting for
 * responses from any PDPs.
 */
public class PolicyUndeployTracker extends PolicyCommonTracker {

    /**
     * Constructs the object.
     */
    public PolicyUndeployTracker() {
        super();
    }

    @Override
    protected boolean updateData(String pdp, PolicyTrackerData data, boolean stillActive) {
        // note: still active means the policy wasn't undeployed, thus it's a failure
        return (stillActive ? data.fail(pdp) : data.success(pdp));
    }

    @Override
    protected boolean shouldRemove(PolicyTrackerData data) {
        return data.isComplete();
    }
}
