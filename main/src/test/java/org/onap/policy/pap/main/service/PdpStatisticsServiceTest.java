/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.base.Validated;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.pap.main.PolicyPapApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = PolicyPapApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PdpStatisticsServiceTest {

    private static final String NAME3 = "name3";
    private static final String NAME1 = "name1";
    private static final String LIST_IS_NULL = "pdpStatisticsList is marked .*ull but is null";
    private static final String GROUP0 = "group0";
    private static final String GROUP = "group";
    private static final String SUBGROUP = "subgroup";
    private static final Instant TIMESTAMP1 = Instant.ofEpochSecond(1078884319);
    private static final Instant TIMESTAMP2 = Instant.ofEpochSecond(1078884350);
    private static final Integer NUMBER_RECORDS = 10;

    @Autowired
    private PdpStatisticsService pdpStatisticsService;

    private PdpStatistics pdpStatistics1;
    private PdpStatistics pdpStatistics2;
    private PdpStatistics pdpStatistics3;
    private PdpStatistics pdpStatistics4;

    /**
     * Setup for tests.
     *
     * @throws CoderException the exception
     */
    @Before
    public void setup() {
        pdpStatistics1 = generatePdpStatistics(NAME1, TIMESTAMP1, 1L, GROUP, SUBGROUP);
        pdpStatistics2 = generatePdpStatistics("name2", TIMESTAMP1, 2L, GROUP, SUBGROUP);
        pdpStatistics3 = generatePdpStatistics(NAME1, TIMESTAMP2, 3L, GROUP, SUBGROUP);
        pdpStatistics4 = generatePdpStatistics(NAME3, TIMESTAMP2, 4L, GROUP0, SUBGROUP);
    }

    /**
     * Tear down.
     */
    @After
    public void teardown() {

    }

    @Test
    public void testCreatePdpStatisticsSuccess() {
        List<PdpStatistics> createList = List.of(pdpStatistics1, pdpStatistics3, pdpStatistics4, pdpStatistics2);
        List<PdpStatistics> createdPdpStatisticsList = pdpStatisticsService.createPdpStatistics(createList);
        // these should match AND be in the same order
        assertThat(createdPdpStatisticsList).isEqualTo(createList);
    }

    @Test
    public void testCreatePdpStatisticsFailure() {

        assertThatThrownBy(() -> {
            pdpStatisticsService.createPdpStatistics(null);
        }).hasMessageMatching(LIST_IS_NULL);

        PdpStatistics pdpStatisticsErr = new PdpStatistics();
        pdpStatisticsErr.setPdpInstanceId("NULL");
        pdpStatisticsErr.setPdpGroupName(GROUP);

        assertThatThrownBy(() -> {
            pdpStatisticsService.createPdpStatistics(List.of(pdpStatisticsErr));
        }).hasMessageContaining("pdp statistics").hasMessageContaining("key")
            .hasMessageContaining(Validated.IS_A_NULL_KEY);
    }

    @Test
    public void testFetchDatabaseStatistics() {

        List<PdpStatistics> createList = List.of(pdpStatistics1, pdpStatistics3, pdpStatistics4, pdpStatistics2);
        pdpStatisticsService.createPdpStatistics(createList);
        Map<String, Map<String, List<PdpStatistics>>> created =
            pdpStatisticsService.fetchDatabaseStatistics(NUMBER_RECORDS, null, null);
        assertThat(created).hasSize(2);
        assertThat(created.get(GROUP0)).hasSize(1);
        assertThat(created.get(GROUP0).get(SUBGROUP)).hasSize(1);
        assertThat(created.get(GROUP)).hasSize(1);
        assertThat(created.get(GROUP).get(SUBGROUP)).hasSize(3);

        created = pdpStatisticsService.fetchDatabaseStatistics(NUMBER_RECORDS, TIMESTAMP2, TIMESTAMP2);
        assertThat(created).hasSize(2);
        assertThat(created.get(GROUP0)).hasSize(1);
        assertThat(created.get(GROUP0).get(SUBGROUP)).isEqualTo(List.of(pdpStatistics4));
        assertThat(created.get(GROUP)).hasSize(1);
        assertThat(created.get(GROUP).get(SUBGROUP)).isEqualTo(List.of(pdpStatistics3));

        created = pdpStatisticsService.fetchDatabaseStatistics(NUMBER_RECORDS, null, TIMESTAMP1);
        assertThat(created).hasSize(1);
        assertThat(created.get(GROUP)).hasSize(1);
        assertThat(created.get(GROUP).get(SUBGROUP)).hasSize(2);

        created = pdpStatisticsService.fetchDatabaseStatistics(NUMBER_RECORDS, TIMESTAMP2, null);
        assertThat(created).hasSize(2);

        created = pdpStatisticsService.fetchDatabaseStatistics(GROUP0, NUMBER_RECORDS, TIMESTAMP2, TIMESTAMP2);
        assertThat(created).hasSize(1);
        assertThat(created.get(GROUP0)).hasSize(1);
        assertThat(created.get(GROUP0).get(SUBGROUP)).isEqualTo(List.of(pdpStatistics4));

        created = pdpStatisticsService.fetchDatabaseStatistics(GROUP, NUMBER_RECORDS, null, TIMESTAMP1);
        assertThat(created).hasSize(1);
        assertThat(created.get(GROUP)).hasSize(1);
        assertThat(created.get(GROUP).get(SUBGROUP)).hasSize(2);

        created = pdpStatisticsService.fetchDatabaseStatistics(GROUP, NUMBER_RECORDS, TIMESTAMP2, null);
        assertThat(created).hasSize(1);

        created = pdpStatisticsService.fetchDatabaseStatistics(GROUP, SUBGROUP, NUMBER_RECORDS, TIMESTAMP1, TIMESTAMP2);
        assertThat(created).hasSize(1);
        assertThat(created.get(GROUP)).hasSize(1);
        assertThat(created.get(GROUP).get(SUBGROUP)).hasSize(3);

        created = pdpStatisticsService.fetchDatabaseStatistics(GROUP, SUBGROUP, NUMBER_RECORDS, null, TIMESTAMP1);
        assertThat(created).hasSize(1);
        assertThat(created.get(GROUP)).hasSize(1);
        assertThat(created.get(GROUP).get(SUBGROUP)).hasSize(2);

        created = pdpStatisticsService.fetchDatabaseStatistics(GROUP, SUBGROUP, NUMBER_RECORDS, TIMESTAMP2, null);
        assertThat(created).hasSize(1);
        assertThat(created.get(GROUP).get(SUBGROUP)).isEqualTo(List.of(pdpStatistics3));

        created = pdpStatisticsService.fetchDatabaseStatistics(GROUP, SUBGROUP, NAME1, NUMBER_RECORDS, TIMESTAMP1,
            TIMESTAMP2);
        assertThat(created).hasSize(1);
        assertThat(created.get(GROUP)).hasSize(1);
        assertThat(created.get(GROUP).get(SUBGROUP)).hasSize(2);

        created =
            pdpStatisticsService.fetchDatabaseStatistics(GROUP, SUBGROUP, NAME1, NUMBER_RECORDS, null, TIMESTAMP1);
        assertThat(created).hasSize(1);
        assertThat(created.get(GROUP)).hasSize(1);
        assertThat(created.get(GROUP).get(SUBGROUP)).hasSize(1);

        created =
            pdpStatisticsService.fetchDatabaseStatistics(GROUP0, SUBGROUP, NAME3, NUMBER_RECORDS, TIMESTAMP2, null);
        assertThat(created).hasSize(1);
        assertThat(created.get(GROUP0).get(SUBGROUP)).isEqualTo(List.of(pdpStatistics4));
    }

    private PdpStatistics generatePdpStatistics(String pdpInstanceId, Instant date, Long id, String group,
        String subgroup) {
        PdpStatistics pdpStatistics11 = new PdpStatistics();
        pdpStatistics11.setPdpInstanceId(pdpInstanceId);
        pdpStatistics11.setTimeStamp(date);
        pdpStatistics11.setGeneratedId(id);
        pdpStatistics11.setPdpGroupName(group);
        pdpStatistics11.setPdpSubGroupName(subgroup);
        pdpStatistics11.setPolicyDeployCount(2);
        pdpStatistics11.setPolicyDeployFailCount(1);
        pdpStatistics11.setPolicyDeploySuccessCount(1);
        pdpStatistics11.setPolicyExecutedCount(2);
        pdpStatistics11.setPolicyExecutedFailCount(1);
        pdpStatistics11.setPolicyExecutedSuccessCount(1);
        pdpStatistics11.setEngineStats(new ArrayList<>());

        return pdpStatistics11;
    }
}
