/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.rest;

import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;

/**
 * Class to hold statistical data for pap component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapStatisticsManager {

    @Getter
    private static final PapStatisticsManager instance = new PapStatisticsManager();

    private final AtomicLong totalPdpCount = new AtomicLong(0);
    private final AtomicLong totalPdpGroupCount = new AtomicLong(0);
    private final AtomicLong totalPolicyDeployCount = new AtomicLong(0);
    private final AtomicLong policyDeploySuccessCount = new AtomicLong(0);
    private final AtomicLong policyDeployFailureCount = new AtomicLong(0);
    private final AtomicLong totalPolicyDownloadCount = new AtomicLong(0);
    private final AtomicLong policyDownloadSuccessCount = new AtomicLong(0);
    private final AtomicLong policyDownloadFailureCount = new AtomicLong(0);

    /**
     * Constructs the object.
     */
    protected PapStatisticsManager() {
        super();
    }

    /**
     * Method to update the total pdp count.
     *
     * @return the updated value of totalPdpCount
     */
    public long updateTotalPdpCount() {
        return totalPdpCount.incrementAndGet();
    }

    /**
     * Method to update the total pdp group count.
     *
     * @return the updated value of totalPdpGroupCount
     */
    public long updateTotalPdpGroupCount() {
        return totalPdpGroupCount.incrementAndGet();
    }

    /**
     * Method to update the total policy deploy count.
     *
     * @return the updated value of totalPolicyDeployCount
     */
    public long updateTotalPolicyDeployCount() {
        return totalPolicyDeployCount.incrementAndGet();
    }

    /**
     * Method to update the policy deploy success count.
     *
     * @return the updated value of policyDeploySuccessCount
     */
    public long updatePolicyDeploySuccessCount() {
        return policyDeploySuccessCount.incrementAndGet();
    }

    /**
     * Method to update the policy deploy failure count.
     *
     * @return the updated value of policyDeployFailureCount
     */
    public long updatePolicyDeployFailureCount() {
        return policyDeployFailureCount.incrementAndGet();
    }

    /**
     * Method to update the total policy download count.
     *
     * @return the updated value of totalPolicyDownloadCount
     */
    public long updateTotalPolicyDownloadCount() {
        return totalPolicyDownloadCount.incrementAndGet();
    }

    /**
     * Method to update the policy download success count.
     *
     * @return the updated value of policyDownloadSuccessCount
     */
    public long updatePolicyDownloadSuccessCount() {
        return policyDownloadSuccessCount.incrementAndGet();
    }

    /**
     * Method to update the policy download failure count.
     *
     * @return the updated value of policyDownloadFailureCount
     */
    public long updatePolicyDownloadFailureCount() {
        return policyDownloadFailureCount.incrementAndGet();
    }

    /**
     * Reset all the statistics counts to 0.
     */
    public void resetAllStatistics() {
        totalPdpCount.set(0L);
        totalPdpGroupCount.set(0L);
        totalPolicyDeployCount.set(0L);
        policyDeploySuccessCount.set(0L);
        policyDeployFailureCount.set(0L);
        totalPolicyDownloadCount.set(0L);
        policyDownloadSuccessCount.set(0L);
        policyDownloadFailureCount.set(0L);
    }

    public long getTotalPdpCount() {
        return totalPdpCount.get();
    }

    public long getTotalPdpGroupCount() {
        return totalPdpGroupCount.get();
    }

    public long getTotalPolicyDeployCount() {
        return totalPolicyDeployCount.get();
    }

    public long getPolicyDeploySuccessCount() {
        return policyDeploySuccessCount.get();
    }

    public long getPolicyDeployFailureCount() {
        return policyDeployFailureCount.get();
    }

    public long getTotalPolicyDownloadCount() {
        return totalPolicyDownloadCount.get();
    }

    public long getPolicyDownloadSuccessCount() {
        return policyDownloadSuccessCount.get();
    }

    public long getPolicyDownloadFailureCount() {
        return policyDownloadFailureCount.get();
    }
}
