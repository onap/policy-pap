/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2024 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.common.utils.report.HealthCheckReport;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.startstop.PapActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Class to fetch health check of PAP service.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@Service
public class HealthCheckProvider {

    private static final String NOT_ALIVE = "not alive";
    private static final String ALIVE = "alive";
    private static final String URL = NetworkUtil.getHostname();
    private static final String NAME = "Policy PAP";

    @Autowired
    private PolicyStatusProvider policyStatusProvider;
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckProvider.class);

    /**
     * Performs the health check of PAP service.
     *
     * @param checkDbConnectivity flag to enable pap to db connectivity verification
     * @return Report containing health check status
     */
    public HealthCheckReport performHealthCheck(boolean checkDbConnectivity) {
        final var report = new HealthCheckReport();
        report.setName(NAME);
        report.setUrl(URL);

        boolean alive = Registry.get(PapConstants.REG_PAP_ACTIVATOR, PapActivator.class).isAlive();

        if (alive && checkDbConnectivity) {
            alive = verifyPapDbConnectivity();
        }

        report.setHealthy(alive);
        report.setCode(alive ? 200 : 503);
        report.setMessage(alive ? ALIVE : NOT_ALIVE);
        return report;
    }

    /**
     * Verifies the connectivity between pap component & policy database.
     *
     * @return boolean signaling the verification result
     */
    private boolean verifyPapDbConnectivity() {
        try {
            policyStatusProvider.getPolicyStatus();
            return true;
        } catch (PfModelRuntimeException pfModelRuntimeException) {
            LOGGER.warn("Policy pap to database connection check failed. Details - ", pfModelRuntimeException);
            return false;
        }
    }
}
