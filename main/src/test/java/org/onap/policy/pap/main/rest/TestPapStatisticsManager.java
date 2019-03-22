/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 AT&T Intellectual Property.  All rights reserved.
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

import org.junit.Test;

public class TestPapStatisticsManager {

    @Test
    public void test() {
        PapStatisticsManager mgr = new PapStatisticsManager();

        // try each update

        assertEquals(0, mgr.getTotalPdpCount());
        assertEquals(1, mgr.updateTotalPdpCount());
        assertEquals(1, mgr.getTotalPdpCount());

        assertEquals(0, mgr.getTotalPdpGroupCount());
        assertEquals(1, mgr.updateTotalPdpGroupCount());
        assertEquals(1, mgr.getTotalPdpGroupCount());

        assertEquals(0, mgr.getTotalPolicyDeployCount());
        assertEquals(1, mgr.updateTotalPolicyDeployCount());
        assertEquals(1, mgr.getTotalPolicyDeployCount());

        assertEquals(0, mgr.getPolicyDeploySuccessCount());
        assertEquals(1, mgr.updatePolicyDeploySuccessCount());
        assertEquals(1, mgr.getPolicyDeploySuccessCount());

        assertEquals(0, mgr.getPolicyDeployFailureCount());
        assertEquals(1, mgr.updatePolicyDeployFailureCount());
        assertEquals(1, mgr.getPolicyDeployFailureCount());

        assertEquals(0, mgr.getTotalPolicyDownloadCount());
        assertEquals(1, mgr.updateTotalPolicyDownloadCount());
        assertEquals(1, mgr.getTotalPolicyDownloadCount());

        assertEquals(0, mgr.getPolicyDownloadSuccessCount());
        assertEquals(1, mgr.updatePolicyDownloadSuccessCount());
        assertEquals(1, mgr.getPolicyDownloadSuccessCount());

        assertEquals(0, mgr.getPolicyDownloadFailureCount());
        assertEquals(1, mgr.updatePolicyDownloadFailureCount());
        assertEquals(1, mgr.getPolicyDownloadFailureCount());

        // now check reset
        mgr.resetAllStatistics();

        assertEquals(0, mgr.getPolicyDeployFailureCount());
        assertEquals(0, mgr.getTotalPdpCount());
        assertEquals(0, mgr.getTotalPdpGroupCount());
        assertEquals(0, mgr.getTotalPolicyDeployCount());
        assertEquals(0, mgr.getPolicyDeploySuccessCount());
        assertEquals(0, mgr.getPolicyDeployFailureCount());
        assertEquals(0, mgr.getTotalPolicyDownloadCount());
        assertEquals(0, mgr.getPolicyDownloadSuccessCount());
        assertEquals(0, mgr.getPolicyDownloadFailureCount());
    }
}
