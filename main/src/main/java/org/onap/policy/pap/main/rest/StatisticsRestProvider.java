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

import java.time.Instant;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to fetch statistics of pap component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class StatisticsRestProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsRestProvider.class);
    private static final String GET_STATISTICS_ERR_MSG = "fetch database failed";
    private static final String DEFAULT_GROUP = "defaultGroup";
    private static final int MIN_RECORD_COUNT = 1;
    private static final int MAX_RECORD_COUNT = 100;

    /**
     * Returns the current statistics of pap component.
     *
     * @return Report containing statistics of pap component
     */
    public StatisticsReport fetchCurrentStatistics() {
        final var report = new StatisticsReport();
        report.setCode(Registry.get(PapConstants.REG_PAP_ACTIVATOR, PapActivator.class).isAlive() ? 200 : 500);

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
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @param pdpName the name of the PDP
     * @param recordCount the count to query from database
     * @return Report containing statistics of pdp component
     * @throws PfModelException when database can not found
     */
    public Map<String, Map<String, List<PdpStatistics>>> fetchDatabaseStatistics(String groupName, String subType,
            String pdpName, int recordCount) throws PfModelException {
        final PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            Instant startTime = null;
            Instant endTime = null;

            /*
             * getFilteredPdpStatistics() will throw an NPE if a group name is not specified, so we
             * provide a default value
             */
            String grpnm = (groupName != null ? groupName : DEFAULT_GROUP);

            int nrecords = Math.min(MAX_RECORD_COUNT, Math.max(MIN_RECORD_COUNT, recordCount));

            return generatePdpStatistics(databaseProvider.getFilteredPdpStatistics(
                            PdpFilterParameters.builder().name(pdpName).group(grpnm)
                            .subGroup(subType).startTime(startTime).endTime(endTime)
                            .recordNum(nrecords).build()));

        } catch (final PfModelException exp) {
            String errorMessage = GET_STATISTICS_ERR_MSG + "groupName:" + groupName + "subType:" + subType + "pdpName:"
                    + pdpName + exp.getMessage();
            LOGGER.debug(errorMessage, exp);
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, errorMessage);
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


