/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2022 Bell Canada. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.TimerManager;
import org.onap.policy.pap.main.parameters.PdpModifyRequestMapParams.PdpModifyRequestMapParamsBuilder;

public class TestPdpModifyRequestMapParams {
    private static final long MAX_PDP_AGE_MS = 100;
    private PdpModifyRequestMapParamsBuilder builder;
    private Publisher<PdpMessage> pub;
    private RequestIdDispatcher<PdpStatus> disp;
    private Object lock;
    private PdpParameters pdpParams;
    private TimerManager updTimers;
    private TimerManager stateTimers;

    /**
     * Sets up the objects and creates an empty {@link #builder}.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        pub = mock(Publisher.class);
        disp = mock(RequestIdDispatcher.class);
        lock = new Object();
        pdpParams = mock(PdpParameters.class);
        updTimers = mock(TimerManager.class);
        stateTimers = mock(TimerManager.class);

        builder = PdpModifyRequestMapParams.builder().modifyLock(lock).pdpPublisher(pub).responseDispatcher(disp)
                        .params(pdpParams).stateChangeTimers(stateTimers).updateTimers(updTimers)
                        .maxPdpAgeMs(MAX_PDP_AGE_MS);
    }

    @Test
    public void testGettersSetters() {
        PdpModifyRequestMapParams params = builder.build();
        assertThat(params.getMaxPdpAgeMs()).isEqualTo(MAX_PDP_AGE_MS);
        assertSame(pub, params.getPdpPublisher());
        assertSame(disp, params.getResponseDispatcher());
        assertSame(lock, params.getModifyLock());
        assertSame(pdpParams, params.getParams());
        assertSame(updTimers, params.getUpdateTimers());
        assertSame(stateTimers, params.getStateChangeTimers());
    }

    @Test
    public void testValidate() {
        assertThatCode(builder.build()::validate).doesNotThrowAnyException();
    }

    @Test
    public void testValidate_InvalidMaxPdpAge() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder.maxPdpAgeMs(0).build().validate())
                        .withMessageContaining("maxPdpAgeMs");
        assertThatIllegalArgumentException().isThrownBy(() -> builder.maxPdpAgeMs(-1).build().validate())
                        .withMessageContaining("maxPdpAgeMs");

        assertThatCode(builder.maxPdpAgeMs(1).build()::validate).doesNotThrowAnyException();
    }

    @Test
    public void testValidate_MissingPublisher() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder.pdpPublisher(null).build().validate())
                        .withMessageContaining("publisher");
    }

    @Test
    public void testValidate_MissingDispatcher() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder.responseDispatcher(null).build().validate())
                        .withMessageContaining("Dispatch");
    }

    @Test
    public void testValidate_MissingLock() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder.modifyLock(null).build().validate())
                        .withMessageContaining("Lock");
    }

    @Test
    public void testValidate_MissingPdpParams() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder.params(null).build().validate())
                        .withMessageContaining("PDP param");
    }

    @Test
    public void testValidate_MissingStateChangeTimers() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder.stateChangeTimers(null).build().validate())
                        .withMessageContaining("state");
    }

    @Test
    public void testValidate_MissingUpdateTimers() {
        assertThatIllegalArgumentException().isThrownBy(() -> builder.updateTimers(null).build().validate())
                        .withMessageContaining("update");
    }
}
