/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021-2023 Bell Canada. All rights reserved.
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

import lombok.Builder;
import lombok.Getter;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.TimerManager;


/**
 * Parameters needed to create a {@link PdpModifyRequestMapParams}.
 */
@Getter
@Builder
public class PdpModifyRequestMapParams {
    private long maxPdpAgeMs;
    private Publisher<PdpMessage> pdpPublisher;
    private RequestIdDispatcher<PdpStatus> responseDispatcher;
    private Object modifyLock;
    private PdpParameters params;
    private TimerManager updateTimers;
    private TimerManager stateChangeTimers;

    /**
     * Validates the parameters.
     */
    public void validate() {
        if (maxPdpAgeMs < 1) {
            throw new IllegalArgumentException("maxPdpAgeMs must be >= 1");
        }

        if (pdpPublisher == null) {
            throw new IllegalArgumentException("missing publisher");
        }

        if (responseDispatcher == null) {
            throw new IllegalArgumentException("missing responseDispatcher");
        }

        if (modifyLock == null) {
            throw new IllegalArgumentException("missing modifyLock");
        }

        if (params == null) {
            throw new IllegalArgumentException("missing PDP parameters");
        }

        if (updateTimers == null) {
            throw new IllegalArgumentException("missing updateTimers");
        }

        if (stateChangeTimers == null) {
            throw new IllegalArgumentException("missing stateChangeTimers");
        }
    }
}
