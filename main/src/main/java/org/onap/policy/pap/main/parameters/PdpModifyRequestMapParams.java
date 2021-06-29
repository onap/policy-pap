/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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
import org.onap.policy.models.pdp.concepts.PdpMessage;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.Publisher;
import org.onap.policy.pap.main.comm.TimerManager;
import org.onap.policy.pap.main.notification.PolicyNotifier;


/**
 * Parameters needed to create a {@link PdpModifyRequestMapParams}.
 */
@Getter
public class PdpModifyRequestMapParams {
    private Publisher<PdpMessage> pdpPublisher;
    private RequestIdDispatcher<PdpStatus> responseDispatcher;
    private Object modifyLock;
    private PdpParameters params;
    private TimerManager updateTimers;
    private TimerManager stateChangeTimers;
    private PolicyModelsProviderFactoryWrapper daoFactory;
    private PolicyNotifier policyNotifier;

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

    public PdpModifyRequestMapParams setDaoFactory(PolicyModelsProviderFactoryWrapper daoFactory) {
        this.daoFactory = daoFactory;
        return this;
    }

    public PdpModifyRequestMapParams setPolicyNotifier(PolicyNotifier policyNotifier) {
        this.policyNotifier = policyNotifier;
        return this;
    }

    public PdpModifyRequestMapParams setPdpPublisher(Publisher<PdpMessage> pdpPublisher) {
        this.pdpPublisher = pdpPublisher;
        return this;
    }

    public PdpModifyRequestMapParams setResponseDispatcher(RequestIdDispatcher<PdpStatus> responseDispatcher) {
        this.responseDispatcher = responseDispatcher;
        return this;
    }

    public PdpModifyRequestMapParams setModifyLock(Object modifyLock) {
        this.modifyLock = modifyLock;
        return this;
    }

    /**
     * Validates the parameters.
     */
    public void validate() {
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

        if (daoFactory == null) {
            throw new IllegalArgumentException("missing DAO factory");
        }

        if (policyNotifier == null) {
            throw new IllegalArgumentException("missing policy notifier");
        }
    }
}
