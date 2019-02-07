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

import org.onap.policy.pap.main.startstop.PapActivator;

/**
 * Class to fetch statistics of pap component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class StatisticsProvider {

    /**
     * Returns the current statistics of pap component.
     *
     * @return Report containing statistics of pap component
     */
    public StatisticsReport fetchCurrentStatistics() {
        final StatisticsReport report = new StatisticsReport();
        report.setCode(PapActivator.isAlive() ? 200 : 500);
        report.setTotalPdpCount(PapStatisticsManager.getTotalPdpCount());
        report.setTotalPdpGroupCount(PapStatisticsManager.getTotalPdpGroupCount());
        report.setTotalPolicyDownloadCount(PapStatisticsManager.getTotalPolicyDownloadCount());
        report.setPolicyDownloadSuccessCount(PapStatisticsManager.getPolicyDownloadSuccessCount());
        report.setPolicyDownloadFailureCount(PapStatisticsManager.getPolicyDownloadFailureCount());
        report.setTotalPolicyDeployCount(PapStatisticsManager.getTotalPolicyDeployCount());
        report.setPolicyDeploySuccessCount(PapStatisticsManager.getPolicyDeploySuccessCount());
        report.setPolicyDeployFailureCount(PapStatisticsManager.getPolicyDeployFailureCount());
        return report;
    }
}
