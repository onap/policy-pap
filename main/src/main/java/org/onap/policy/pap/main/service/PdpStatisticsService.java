/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.models.pdp.persistence.concepts.JpaPdpStatistics;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.repository.PdpStatisticsRepository;
import org.onap.policy.pap.main.rest.PapStatisticsManager;
import org.onap.policy.pap.main.rest.StatisticsReport;
import org.onap.policy.pap.main.startstop.PapActivator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PdpStatisticsService {

    private static final String TIMESTAMP = "timeStamp";
    private static final int DEFAULT_RECORD_COUNT = 10;
    private static final int MAX_RECORD_COUNT = 100;

    private final PdpStatisticsRepository pdpStatisticsRepository;

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
     * Creates PDP statistics.
     *
     * @param pdpStatisticsList a specification of the PDP statistics to create
     * @return the PDP statistics created
     */
    public List<PdpStatistics> createPdpStatistics(@NonNull final List<PdpStatistics> pdpStatisticsList) {
        for (PdpStatistics pdpStatistics : pdpStatisticsList) {
            var jpaPdpStatistics = new JpaPdpStatistics();
            jpaPdpStatistics.fromAuthorative(pdpStatistics);
            BeanValidationResult validationResult = jpaPdpStatistics.validate("pdp statistics");
            if (!validationResult.isValid()) {
                throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
            }
            pdpStatisticsRepository.saveAndFlush(jpaPdpStatistics);
            pdpStatistics.setGeneratedId(jpaPdpStatistics.getGeneratedId());
        }

        // Return the created PDP statistics
        List<PdpStatistics> pdpStatistics = new ArrayList<>(pdpStatisticsList.size());

        for (PdpStatistics pdpStatisticsItem : pdpStatisticsList) {
            @SuppressWarnings("deprecation")
            var jpaPdpStatistics = pdpStatisticsRepository.getById(pdpStatisticsItem.getGeneratedId());
            pdpStatistics.add(jpaPdpStatistics.toAuthorative());
        }
        return pdpStatistics;
    }

    /**
     * Fetch PdpStatistics from db.
     *
     * @param pdpGroup the name of the group
     * @param pdpSubGroup the name of the subgroup
     * @param pdp the pdp instance id
     * @param recordCount the number of records to return
     * @param startTime start time of the records to be returned
     * @param endTime end time of the records to be returned
     * @return pdpStatistics grouped by pdpGroup
     */
    public Map<String, Map<String, List<PdpStatistics>>> fetchDatabaseStatistics(@NonNull String pdpGroup,
        @NonNull String pdpSubGroup, @NonNull String pdp, int recordCount, Instant startTime, Instant endTime) {

        Pageable recordSize = getRecordSize(recordCount);
        if (startTime != null && endTime != null) {
            return generatePdpStatistics(asPdpStatisticsList(pdpStatisticsRepository
                .findByPdpGroupNameAndPdpSubGroupNameAndNameAndTimeStampBetween(pdpGroup, pdpSubGroup, pdp,
                    convertInstantToDate(startTime), convertInstantToDate(endTime), recordSize)));
        } else if (startTime == null && endTime == null) {
            return generatePdpStatistics(
                asPdpStatisticsList(pdpStatisticsRepository.findByPdpGroupNameAndPdpSubGroupNameAndName(pdpGroup,
                    pdpSubGroup, pdp, recordSize)));
        } else if (startTime != null) {
            return generatePdpStatistics(asPdpStatisticsList(
                pdpStatisticsRepository.findByPdpGroupNameAndPdpSubGroupNameAndNameAndTimeStampGreaterThanEqual(
                    pdpGroup, pdpSubGroup, pdp, convertInstantToDate(startTime), recordSize)));
        } else {
            return generatePdpStatistics(asPdpStatisticsList(
                pdpStatisticsRepository.findByPdpGroupNameAndPdpSubGroupNameAndNameAndTimeStampLessThanEqual(
                    pdpGroup, pdpSubGroup, pdp, convertInstantToDate(endTime), recordSize)));
        }
    }

    /**
     * Fetch PdpStatistics from db.
     *
     * @param pdpGroup the name of the group
     * @param pdpSubGroup the name of the subgroup
     * @param recordCount the number of records to return
     * @param startTime start time of the records to be returned
     * @param endTime end time of the records to be returned
     * @return pdpStatistics grouped by pdpGroup
     */
    public Map<String, Map<String, List<PdpStatistics>>> fetchDatabaseStatistics(@NonNull String pdpGroup,
        @NonNull String pdpSubGroup, int recordCount, Instant startTime, Instant endTime) {

        Pageable recordSize = getRecordSize(recordCount);
        if (startTime != null && endTime != null) {
            return generatePdpStatistics(asPdpStatisticsList(
                pdpStatisticsRepository.findByPdpGroupNameAndPdpSubGroupNameAndTimeStampBetween(pdpGroup, pdpSubGroup,
                    convertInstantToDate(startTime), convertInstantToDate(endTime), recordSize)));
        } else if (startTime == null && endTime == null) {
            return generatePdpStatistics(asPdpStatisticsList(pdpStatisticsRepository
                .findByPdpGroupNameAndPdpSubGroupName(pdpGroup, pdpSubGroup, recordSize)));
        } else if (startTime != null) {
            return generatePdpStatistics(asPdpStatisticsList(
                pdpStatisticsRepository.findByPdpGroupNameAndPdpSubGroupNameAndTimeStampGreaterThanEqual(pdpGroup,
                    pdpSubGroup, convertInstantToDate(startTime), recordSize)));
        } else {
            return generatePdpStatistics(asPdpStatisticsList(
                pdpStatisticsRepository.findByPdpGroupNameAndPdpSubGroupNameAndTimeStampLessThanEqual(pdpGroup,
                    pdpSubGroup, convertInstantToDate(endTime), recordSize)));
        }
    }

    /**
     * Fetch PdpStatistics from db.
     *
     * @param pdpGroup the name of the group
     * @param recordCount the number of records to return
     * @param startTime start time of the records to be returned
     * @param endTime end time of the records to be returned
     * @return pdpStatistics grouped by pdpGroup
     */
    public Map<String, Map<String, List<PdpStatistics>>> fetchDatabaseStatistics(@NonNull String pdpGroup,
        int recordCount, Instant startTime, Instant endTime) {

        Pageable recordSize = getRecordSize(recordCount);
        if (startTime != null && endTime != null) {
            return generatePdpStatistics(
                asPdpStatisticsList(pdpStatisticsRepository.findByPdpGroupNameAndTimeStampBetween(pdpGroup,
                    convertInstantToDate(startTime), convertInstantToDate(endTime), recordSize)));
        } else if (startTime == null && endTime == null) {
            return generatePdpStatistics(
                asPdpStatisticsList(pdpStatisticsRepository.findByPdpGroupName(pdpGroup, recordSize)));
        } else if (startTime != null) {
            return generatePdpStatistics(
                asPdpStatisticsList(pdpStatisticsRepository.findByPdpGroupNameAndTimeStampGreaterThanEqual(pdpGroup,
                    convertInstantToDate(startTime), recordSize)));
        } else {
            return generatePdpStatistics(
                asPdpStatisticsList(pdpStatisticsRepository.findByPdpGroupNameAndTimeStampLessThanEqual(pdpGroup,
                    convertInstantToDate(endTime), recordSize)));
        }
    }

    /**
     * Fetch PdpStatistics from db.
     *
     * @param recordCount the number of records to return
     * @param startTime start time of the records to be returned
     * @param endTime end time of the records to be returned
     * @return pdpStatistics grouped by pdpGroup
     */
    public Map<String, Map<String, List<PdpStatistics>>> fetchDatabaseStatistics(int recordCount, Instant startTime,
        Instant endTime) {

        Pageable recordSize = getRecordSize(recordCount);
        if (startTime != null && endTime != null) {
            return generatePdpStatistics(asPdpStatisticsList(pdpStatisticsRepository.findByTimeStampBetween(
                convertInstantToDate(startTime), convertInstantToDate(endTime), recordSize)));
        } else if (startTime == null && endTime == null) {
            return generatePdpStatistics(
                asPdpStatisticsList(pdpStatisticsRepository.findAll(recordSize).toList()));
        } else if (startTime != null) {
            return generatePdpStatistics(asPdpStatisticsList(pdpStatisticsRepository
                .findByTimeStampGreaterThanEqual(convertInstantToDate(startTime), recordSize)));
        } else {
            return generatePdpStatistics(asPdpStatisticsList(pdpStatisticsRepository
                .findByTimeStampLessThanEqual(convertInstantToDate(endTime), recordSize)));
        }
    }

    private Pageable getRecordSize(int recordCount) {
        if (recordCount < 1) {
            recordCount = DEFAULT_RECORD_COUNT;
        } else if (recordCount > MAX_RECORD_COUNT) {
            recordCount = MAX_RECORD_COUNT;
        }
        return PageRequest.of(0, recordCount, Sort.by(TIMESTAMP).descending());
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

    /**
     * Convert JPA PDP statistics list to an PDP statistics list.
     *
     * @param jpaPdpStatisticsList the list to convert
     * @return the PDP statistics list
     */
    private List<PdpStatistics> asPdpStatisticsList(List<JpaPdpStatistics> jpaPdpStatisticsList) {
        return jpaPdpStatisticsList.stream().map(JpaPdpStatistics::toAuthorative).collect(Collectors.toList());
    }

    private Date convertInstantToDate(Instant instant) {
        return (instant == null ? null : Date.from(instant));
    }
}
