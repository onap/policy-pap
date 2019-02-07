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

import lombok.Getter;

/**
 * Class to hold statistical data for pap component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapStatisticsManager {

    @Getter
    private static long totalPdpCount;
    @Getter
    private static long totalPdpGroupCount;
    @Getter
    private static long totalPolicyDeployCount;
    @Getter
    private static long policyDeploySuccessCount;
    @Getter
    private static long policyDeployFailureCount;
    @Getter
    private static long totalPolicyDownloadCount;
    @Getter
    private static long policyDownloadSuccessCount;
    @Getter
    private static long policyDownloadFailureCount;

    private PapStatisticsManager() {
        throw new IllegalStateException("Instantiation of the class is not allowed");
    }

    /**
     * Method to update the total pdp count.
     *
     * @return the updated value of totalPdpCount
     */
    public static long updateTotalPdpCount() {
        return ++totalPdpCount;
    }

    /**
     * Method to update the total pdp group count.
     *
     * @return the updated value of totalPdpGroupCount
     */
    public static long updateTotalPdpGroupCount() {
        return ++totalPdpGroupCount;
    }

    /**
     * Method to update the total policy deploy count.
     *
     * @return the updated value of totalPolicyDeployCount
     */
    public static long updateTotalPolicyDeployCount() {
        return ++totalPolicyDeployCount;
    }

    /**
     * Method to update the policy deploy success count.
     *
     * @return the updated value of policyDeploySuccessCount
     */
    public static long updatePolicyDeploySuccessCount() {
        return ++policyDeploySuccessCount;
    }

    /**
     * Method to update the policy deploy failure count.
     *
     * @return the updated value of policyDeployFailureCount
     */
    public static long updatePolicyDeployFailureCount() {
        return ++policyDeployFailureCount;
    }

    /**
     * Method to update the total policy download count.
     *
     * @return the updated value of totalPolicyDownloadCount
     */
    public static long updateTotalPolicyDownloadCount() {
        return ++totalPolicyDownloadCount;
    }

    /**
     * Method to update the policy download success count.
     *
     * @return the updated value of policyDownloadSuccessCount
     */
    public static long updatePolicyDownloadSuccessCount() {
        return ++policyDownloadSuccessCount;
    }

    /**
     * Method to update the policy download failure count.
     *
     * @return the updated value of policyDownloadFailureCount
     */
    public static long updatePolicyDownloadFailureCount() {
        return ++policyDownloadFailureCount;
    }

    /**
     * Reset all the statistics counts to 0.
     */
    public static void resetAllStatistics() {
        totalPdpCount = 0L;
        totalPdpGroupCount = 0L;
        totalPolicyDeployCount = 0L;
        policyDeploySuccessCount = 0L;
        policyDeployFailureCount = 0L;
        totalPolicyDownloadCount = 0L;
        policyDownloadSuccessCount = 0L;
        policyDownloadFailureCount = 0L;
    }
}
