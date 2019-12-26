/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property.
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
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
    private static final String DESC_ORDER = "DESC";

    /**
     * Returns the current statistics of pap component.
     *
     * @return Report containing statistics of pap component
     */
    public StatisticsReport fetchCurrentStatistics() {
        final StatisticsReport report = new StatisticsReport();
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
     * @return Report containing statistics of pdp component
     * @throws PfModelException when database can not found
     */
    public Map<String, Map<String, List<PdpStatistics>>> fetchDatabaseStatistics(String groupName, String subType,
            String pdpName) throws PfModelException {
        final PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        Map<String, Map<String, List<PdpStatistics>>> pair;
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            String dbPdpName = pdpName;
            Date startTime = null;
            Date endTime = null;

            if (groupName == null) {
                pair = generatePdpStatistics(databaseProvider.getPdpStatistics(dbPdpName, startTime));
            } else {
                pair = generatePdpStatistics(databaseProvider.getFilteredPdpStatistics(dbPdpName, groupName, subType,
                        startTime, endTime, DESC_ORDER, 0));
            }
        } catch (final PfModelException exp) {
            String errorMessage = GET_STATISTICS_ERR_MSG + "groupName:" + groupName + "subType:" + subType + "pdpName:"
                    + pdpName + exp.getMessage();
            LOGGER.debug(errorMessage);
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, errorMessage);
        }
        return pair;
    }

    /**
     * Returns Latest statistics of pdp component from database.
     *
     * @param groupName name of the PDP group
     * @param subType type of the sub PDP group
     * @param pdpName the name of the PDP
     * @param recordCount the total number for latest record to get
     * @return Report containing statistics of pdp component
     * @throws PfModelException when database can not found
     */
    public List<PdpStatistics> fetchLatestDatabaseStatistics(String groupName, String subType, String pdpName,
            int recordCount) throws PfModelException {
        List<PdpStatistics> sortedList = new ArrayList<>();
        final PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            String dbPdpName = pdpName;
            Date startTime = null;
            Date endTime = null;

            sortedList = databaseProvider.getFilteredPdpStatistics(dbPdpName, groupName, subType, startTime, endTime,
                    DESC_ORDER, recordCount);

        } catch (final PfModelException exp) {
            String errorMessage = GET_STATISTICS_ERR_MSG + "groupName:" + groupName + "subType:" + subType + "pdpName:"
                    + pdpName + exp.getMessage();
            LOGGER.debug(errorMessage);
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, errorMessage);
        }
        return sortedList;
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
                groupMap.putIfAbsent(curGroup, new HashMap<>());
                Map<String, List<PdpStatistics>> curGroupMap = groupMap.get(curGroup);
                curGroupMap.putIfAbsent(curSubGroup, new ArrayList<>());
                List<PdpStatistics> curSubGroupList = curGroupMap.get(curSubGroup);
                curSubGroupList.add(s);
            });
        }
        return groupMap;
    }
}


