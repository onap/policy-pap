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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Response to PDP Group DELETE REST API.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class PdpGroupDeployResponse extends SimpleResponse<org.onap.policy.models.pap.concepts.PdpGroupDeployResponse> {

    public PdpGroupDeployResponse(PdpGroupDeployResponse source) {
        super(source);
    }

    @Override
    public org.onap.policy.models.pap.concepts.PdpGroupDeployResponse makeExternal() {
        return new org.onap.policy.models.pap.concepts.PdpGroupDeployResponse();
    }
}
