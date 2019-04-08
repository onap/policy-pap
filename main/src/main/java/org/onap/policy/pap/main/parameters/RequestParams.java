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
 * Parameters needed to create a Request.
 */
@Getter
public class RequestParams {
    private Publisher publisher;
    private RequestIdDispatcher<PdpStatus> responseDispatcher;
    private Object modifyLock;
    private TimerManager timers;
    private int maxRetryCount;


    public RequestParams setPublisher(Publisher publisher) {
        this.publisher = publisher;
        return this;
    }

    public RequestParams setResponseDispatcher(RequestIdDispatcher<PdpStatus> responseDispatcher) {
        this.responseDispatcher = responseDispatcher;
        return this;
    }

    public RequestParams setModifyLock(Object modifyLock) {
        this.modifyLock = modifyLock;
        return this;
    }

    public RequestParams setTimers(TimerManager timers) {
        this.timers = timers;
        return this;
    }

    public RequestParams setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }

    /**
     * Validates the parameters.
     */
    public void validate() {
        if (publisher == null) {
            throw new IllegalArgumentException("missing publisher");
        }

        if (responseDispatcher == null) {
            throw new IllegalArgumentException("missing responseDispatcher");
        }

        if (modifyLock == null) {
            throw new IllegalArgumentException("missing modifyLock");
        }

        if (timers == null) {
            throw new IllegalArgumentException("missing timers");
        }
    }
}
