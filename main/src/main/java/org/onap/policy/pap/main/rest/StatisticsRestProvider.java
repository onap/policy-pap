/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2021 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.models.pdp.persistence.provider.PdpFilterParameters;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.startstop.PapActivator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Class to fetch statistics of pap component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@Service
public class StatisticsRestProvider {
    private static final String GET_STATISTICS_ERR_MSG = "database query failed";

    @Autowired
    private PapActivator papActivator;

    /**
     * Returns the current statistics of pap component.
     *
     * @return Report containing statistics of pap component
     */
    public StatisticsReport fetchCurrentStatistics() {
        final var report = new StatisticsReport();
        report.setCode(papActivator.isAlive() ? 200 : 500);

        PapStatisticsManager mgr = Registry.get(PapConstants.REG_STATISTICS_MANAGER, PapStatisticsManager.class);
        report.setTotalPdpCount(mgr.getTotalPdpCount());
        report.setTotalPdpGroupCount(mgr.getTotalPdpGroupCount());
        report.setTotalPolicyDownloadCount(mgr.getTotalPolicyDownloadCount());
        report.setPolicyDownloadSuccessCount(mgr.getPolicyDownloadSuccessCount());
        report.setPolicyDownloadFailureCount(mgr.getPolicyDownloadFailureCount());
        report.setTotalPolicyDeployCount(mgr.getTotalPolicyDeployCount());
        report.setPolicyDeploySuccessCount(mgr.getPolicyDeploySuccessCount());
        report.setPolicyDeployFailureCount(mgr.getPolicyDeployFailureCount());

        return report;
    }

    /**
     * Returns statistics of pdp component from database.
     *
     * @param filter record filter
     * @return Report containing statistics of pdp component
     * @throws PfModelException when database can not found
     */
    public Map<String, Map<String, List<PdpStatistics>>> fetchDatabaseStatistics(PdpFilterParameters filter)
                    throws PfModelException {
        final PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                        Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            return generatePdpStatistics(databaseProvider.getFilteredPdpStatistics(filter));

        } catch (final PfModelException exp) {
            throw new PfModelRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, GET_STATISTICS_ERR_MSG);
        }
    }

    /**
     * generate the statistics of pap component by group/subgroup.
     *
     */
    private Map<String, Map<String, List<PdpStatistics>>> generatePdpStatistics(List<PdpStatistics> pdpStatisticsList) {
        Map<String, Map<String, List<PdpStatistics>>> groupMap = new HashMap<>();
        if (pdpStatisticsList != null) {
            pdpStatisticsList.stream().forEach(s -> {
                String curGroup = s.getPdpGroupName();
                String curSubGroup = s.getPdpSubGroupName();
                groupMap.computeIfAbsent(curGroup, curGroupMap -> new HashMap<>())
                        .computeIfAbsent(curSubGroup, curSubGroupList -> new ArrayList<>()).add(s);
            });
        }
        return groupMap;
    }
}
