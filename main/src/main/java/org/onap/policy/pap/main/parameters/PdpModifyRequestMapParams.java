/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import lombok.Getter;
import org.onap.policy.common.endpoints.listeners.RequestIdDispatcher;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.TimerManager;


/**
 * Parameters needed to create a {@link PdpModifyRequestMapParams}.
 */
@Getter
public class PdpModifyRequestMapParams extends RequestDataParams {
    private PdpParameters params;
    private TimerManager updateTimers;
    private TimerManager stateChangeTimers;

    public PdpModifyRequestMapParams setParams(PdpParameters params) {
        this.params = params;
        return this;
    }

    public PdpModifyRequestMapParams setUpdateTimers(TimerManager updateTimers) {
        this.updateTimers = updateTimers;
        return this;
    }

    public PdpModifyRequestMapParams setStateChangeTimers(TimerManager stateChangeTimers) {
        this.stateChangeTimers = stateChangeTimers;
        return this;
    }

    @Override
    public PdpModifyRequestMapParams setPublisher(Publisher publisher) {
        super.setPublisher(publisher);
        return this;
    }

    @Override
    public PdpModifyRequestMapParams setResponseDispatcher(RequestIdDispatcher<PdpStatus> responseDispatcher) {
        super.setResponseDispatcher(responseDispatcher);
        return this;
    }

    @Override
    public PdpModifyRequestMapParams setModifyLock(Object modifyLock) {
        super.setModifyLock(modifyLock);
        return this;
    }

    @Override
    public void validate() {
        super.validate();

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
