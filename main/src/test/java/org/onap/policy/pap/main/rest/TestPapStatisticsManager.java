/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property.  All rights reserved.
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

import static org.junit.Assert.assertEquals;

import java.util.function.ToLongFunction;
import org.junit.Test;

public class TestPapStatisticsManager {

    @Test
    public void testTotalPdpCount() {
        verifyCount(PapStatisticsManager::getTotalPdpCount,
                        PapStatisticsManager::updateTotalPdpCount);
    }

    @Test
    public void testTotalPdpGroupCount() {
        verifyCount(PapStatisticsManager::getTotalPdpGroupCount,
                        PapStatisticsManager::updateTotalPdpGroupCount);
    }

    @Test
    public void testTotalPolicyDeployCount() {
        verifyCount(PapStatisticsManager::getTotalPolicyDeployCount,
                        PapStatisticsManager::updateTotalPolicyDeployCount);
    }

    @Test
    public void testPolicyDeploySuccessCount() {
        verifyCount(PapStatisticsManager::getPolicyDeploySuccessCount,
                        PapStatisticsManager::updatePolicyDeploySuccessCount);
    }

    @Test
    public void testPolicyDeployFailureCount() {
        verifyCount(PapStatisticsManager::getPolicyDeployFailureCount,
                        PapStatisticsManager::updatePolicyDeployFailureCount);
    }

    @Test
    public void testTotalPolicyDownloadCount() {
        verifyCount(PapStatisticsManager::getTotalPolicyDownloadCount,
                        PapStatisticsManager::updateTotalPolicyDownloadCount);
    }

    @Test
    public void testPolicyDownloadSuccessCount() {
        verifyCount(PapStatisticsManager::getPolicyDownloadSuccessCount,
                        PapStatisticsManager::updatePolicyDownloadSuccessCount);
    }

    @Test
    public void testPolicyDownloadFailureCount() {
        verifyCount(PapStatisticsManager::getPolicyDownloadFailureCount,
                        PapStatisticsManager::updatePolicyDownloadFailureCount);
    }

    private void verifyCount(ToLongFunction<PapStatisticsManager> getCount,
                    ToLongFunction<PapStatisticsManager> updateCount) {

        PapStatisticsManager mgr = new PapStatisticsManager();

        assertEquals(0, getCount.applyAsLong(mgr));
        assertEquals(1, updateCount.applyAsLong(mgr));
        assertEquals(1, getCount.applyAsLong(mgr));

        assertEquals(2, updateCount.applyAsLong(mgr));
        assertEquals(2, getCount.applyAsLong(mgr));

        // now check reset
        mgr.resetAllStatistics();

        assertEquals(0, getCount.applyAsLong(mgr));
    }
}
