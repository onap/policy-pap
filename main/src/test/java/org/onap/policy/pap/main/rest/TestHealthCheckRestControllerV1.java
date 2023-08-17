/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2022-2023 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2022 Bell Canada. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.SyncInvoker;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Class to perform unit test of {@link HealthCheckRestControllerV1}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@ActiveProfiles({"test", "default"})
class TestHealthCheckRestControllerV1 extends CommonPapRestServer {

    private static final String HEALTHCHECK_ENDPOINT = "healthcheck";

    @MockBean
    private PolicyStatusProvider policyStatusProvider;

    @Test
    void testSwagger() throws Exception {
        super.testSwagger(HEALTHCHECK_ENDPOINT);
    }

    @Test
    void testHealthCheckSuccess() throws Exception {
        final Invocation.Builder invocationBuilder = sendRequest(HEALTHCHECK_ENDPOINT);
        final HealthCheckReport report = invocationBuilder.get(HealthCheckReport.class);
        validateHealthCheckReport(true, 200, ALIVE, report);

        // verify it fails when no authorization info is included
        checkUnauthRequest(HEALTHCHECK_ENDPOINT, SyncInvoker::get);
    }

    @Test
    void testHealthCheckActivatorFailure() throws Exception {

        markActivatorDead();

        final Invocation.Builder invocationBuilder = sendRequest(HEALTHCHECK_ENDPOINT);
        var response = invocationBuilder.get();
        var report = response.readEntity(HealthCheckReport.class);
        assertThat(response.getStatus()).isEqualTo(503);
        validateHealthCheckReport(false, 503, NOT_ALIVE, report);
    }

    @Test
    void testHealthCheckDbConnectionFailure() throws Exception {
        when(policyStatusProvider.getPolicyStatus()).thenThrow(PfModelRuntimeException.class);
        final Invocation.Builder invocationBuilder = sendRequest(HEALTHCHECK_ENDPOINT);
        var response = invocationBuilder.get();
        var report = response.readEntity(HealthCheckReport.class);
        assertThat(response.getStatus()).isEqualTo(503);
        validateHealthCheckReport(false, 503, NOT_ALIVE, report);
    }

    private void validateHealthCheckReport(final boolean healthy, final int code,
                                           final String message, final HealthCheckReport report) {
        assertEquals(CommonPapRestServer.NAME, report.getName());
        assertEquals(CommonPapRestServer.SELF, report.getUrl());
        assertEquals(healthy, report.isHealthy());
        assertEquals(code, report.getCode());
        assertEquals(message, report.getMessage());
    }
}
