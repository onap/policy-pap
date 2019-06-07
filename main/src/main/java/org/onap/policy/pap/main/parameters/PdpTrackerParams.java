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
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.PdpRequestMap;


/**
 * Parameters needed to create a {@link PdpTracker}.
 */
@Getter
public class PdpTrackerParams {
    private PdpRequestMap requestMap;
    private Object modifyLock;
    private long heartBeatMs;
    private PolicyModelsProviderFactoryWrapper daoFactory;

    public PdpTrackerParams setRequestMap(PdpRequestMap requestMap) {
        this.requestMap = requestMap;
        return this;
    }

    public PdpTrackerParams setModifyLock(Object modifyLock) {
        this.modifyLock = modifyLock;
        return this;
    }

    public PdpTrackerParams setHeartBeatMs(long heartBeatMs) {
        this.heartBeatMs = heartBeatMs;
        return this;
    }

    public PdpTrackerParams setDaoFactory(PolicyModelsProviderFactoryWrapper daoFactory) {
        this.daoFactory = daoFactory;
        return this;
    }

    /**
     * Validates the parameters.
     */
    public void validate() {
        if (requestMap == null) {
            throw new IllegalArgumentException("missing requestMap");
        }

        if (modifyLock == null) {
            throw new IllegalArgumentException("missing modifyLock");
        }

        if (heartBeatMs < 1) {
            throw new IllegalArgumentException("invalid heartBeatMs");
        }

        if (daoFactory == null) {
            throw new IllegalArgumentException("missing daoFactory");
        }
    }
}
