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

package org.onap.policy.pap.main.concepts.internal;

public class PdpGroupDeployResponseTest extends
                SimpleResponseBase<PdpGroupDeployResponse, org.onap.policy.models.pap.concepts.PdpGroupDeployResponse> {

    @Override
    protected PdpGroupDeployResponse makeInternal() {
        return new PdpGroupDeployResponse();
    }

    @Override
    protected PdpGroupDeployResponse makeInternal(PdpGroupDeployResponse source) {
        return new PdpGroupDeployResponse(source);
    }

    @Override
    protected org.onap.policy.models.pap.concepts.PdpGroupDeployResponse makeExternal() {
        return new org.onap.policy.models.pap.concepts.PdpGroupDeployResponse();
    }
}
