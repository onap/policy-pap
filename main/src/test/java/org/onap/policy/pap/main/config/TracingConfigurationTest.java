/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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

package org.onap.policy.pap.main.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PolicyPapApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the OpenTelemetry tracing auto-configuration is actually on the classpath and that
 * the {@code management.*} tracing properties in application.yaml are the ones it binds to. Spring
 * Boot 4 moved tracing auto-configuration out of the actuator into separate modules, so without
 * spring-boot-starter-opentelemetry the tracing configuration is silently inert.
 */
@SpringBootTest(classes = PolicyPapApplication.class,
    properties = {"db.initialize=false", "management.tracing.export.enabled=true"})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TracingConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @BeforeAll
    static void setupClass() {
        Registry.newRegistry();
    }

    @Test
    void testTracerIsBackedByOpenTelemetry() {
        assertThat(context.getBean(Tracer.class)).isInstanceOf(OtelTracer.class);
    }

    @Test
    void testSpansAreExportedOverOtlpGrpc() {
        assertThat(context.getBeansOfType(OtlpGrpcSpanExporter.class)).isNotEmpty();
    }

    @Test
    void testServiceNameIsReportedAsPolicyPap() {
        assertThat(context.getBean(Resource.class).getAttribute(AttributeKey.stringKey("service.name")))
            .isEqualTo("policy-pap");
    }

    @Test
    void testOtlpMetricsPushIsNotEnabledAlongWithTracing() {
        assertThat(context.getBeansOfType(OtlpMeterRegistry.class)).isEmpty();
    }
}
