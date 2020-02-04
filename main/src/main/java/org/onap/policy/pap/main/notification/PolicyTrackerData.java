/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyTypeIdentifier;

/**
 * Data associated with a policy, used by PolicyTracker. PDPs start in
 * {@link #incompletePdps} and are moved to either {@link #successPdps} or
 * {@link #failPdps}, depending on their response. Objects of this type are not
 * multi-thread safe.
 */
public class PolicyTrackerData {
    /**
     * The policy type associated with the policy.
     */
    @Getter
    private final ToscaPolicyTypeIdentifier policyType;

    /**
     * PDPs that have successfully completed an update of the policy.
     */
    private final Set<String> successPdps = new HashSet<>();

    /**
     * PDPs that have failed to complete an update of the policy.
     */
    private final Set<String> failPdps = new HashSet<>();

    /**
     * PDPs for which we're still awaiting a response.
     */
    private final Set<String> incompletePdps = new HashSet<>();


    /**
     * Constructs the object.
     *
     * @param policyType policy type
     */
    public PolicyTrackerData(ToscaPolicyTypeIdentifier policyType) {
        this.policyType = policyType;
    }

    /**
     * Determines if this is complete (i.e., it is not waiting for responses from any
     * other PDPs).
     *
     * @return {@code true} if this is complete, {@code false} otherwise
     */
    public boolean isComplete() {
        return incompletePdps.isEmpty();
    }

    /**
     * Determines if everything has succeeded.
     *
     * @return {@code true} if this is complete <i>and</i> nothing has failed,
     *         {@code false} otherwise
     */
    public boolean allSucceeded() {
        return (failPdps.isEmpty() && incompletePdps.isEmpty());
    }

    /**
     * Determines if all of the sets within the data are empty (i.e., contain no PDPs).
     *
     * @return {@code true} if the data is completely empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return (successPdps.isEmpty() && failPdps.isEmpty() && incompletePdps.isEmpty());
    }

    /**
     * Puts the values from this data into the given status object.
     *
     * @param status object whose values are to be set
     */
    public void putValuesInto(PolicyStatus status) {
        status.setFailureCount(failPdps.size());
        status.setIncompleteCount(incompletePdps.size());
        status.setSuccessCount(successPdps.size());
    }

    /**
     * Adds PDPs to {@link #incompletePdps}, removing them from any other sets in which
     * they appear.
     *
     * @param pdps PDPs to be added
     */
    public void addPdps(Collection<String> pdps) {
        successPdps.removeAll(pdps);
        failPdps.removeAll(pdps);

        incompletePdps.addAll(pdps);
    }

    /**
     * Removes PDPs from the sets.
     *
     * @param pdps PDPs to be removed
     * @return {@code true} if anything changed and the policy is now complete, {@code false} otherwise
     */
    public boolean removePdps(Collection<String> pdps) {
        boolean changed = successPdps.removeAll(pdps);
        changed = failPdps.removeAll(pdps) || changed;
        changed = incompletePdps.removeAll(pdps) || changed;

        return (changed && incompletePdps.isEmpty());
    }

    /**
     * Removes a PDP from all sets.
     *
     * @param pdp PDP to be removed
     * @return {@code true} if anything changed and the policy is now complete, {@code false} otherwise
     */
    public boolean removePdp(String pdp) {
        boolean changed = successPdps.remove(pdp);
        changed = failPdps.remove(pdp) || changed;
        changed = incompletePdps.remove(pdp) || changed;

        return (changed && incompletePdps.isEmpty());
    }

    /**
     * Indicates that a PDP has successfully processed this policy.
     *
     * @param pdp the PDP of interest
     * @return {@code true} if the policy is now complete, {@code false} otherwise
     */
    public boolean success(String pdp) {
        return complete(pdp, successPdps, failPdps);
    }

    /**
     * Indicates that a PDP has processed this policy, but was unsuccessful.
     *
     * @param pdp the PDP of interest
     * @return {@code true} if the policy is now complete, {@code false} otherwise
     */
    public boolean fail(String pdp) {
        return complete(pdp, failPdps, successPdps);
    }

    /**
     * Indicates that a PDP has processed this policy, either successfully or
     * unsuccessfully.
     *
     * @param pdp the PDP of interest
     * @param addSet set to which the PDP should be added
     * @param removeSet set from which the PDP should be removed
     * @return {@code true} if the policy is now complete, {@code false} otherwise
     */
    private boolean complete(String pdp, Set<String> addSet, Set<String> removeSet) {
        if (incompletePdps.remove(pdp) || removeSet.remove(pdp)) {
            // successfully removed from one of the sets
            addSet.add(pdp);
            return incompletePdps.isEmpty();
        }

        /*
         * Else: wasn't in either set, thus it's already in the "addSet" or it isn't
         * relevant to this policy. Either way, just discard it without triggering any new
         * notification.
         */
        return false;
    }
}
