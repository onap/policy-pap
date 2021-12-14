/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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
import lombok.Setter;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.annotations.Min;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;

/**
 * Parameters for communicating with PDPs.
 */
@NotNull
@NotBlank
@Getter
@Setter
public class PdpRequestParameters extends ParameterGroupImpl {

    // NOTE: these fields must not be "private" or the validator will skip them

    /**
     * Maximum number of times to re-send a request to a PDP.
     */
    @Min(value = 0)
    protected int maxRetryCount;

    /**
     * Maximum time to wait, in milliseconds, for a PDP response.
     */
    @Min(value = 0)
    protected long maxWaitMs;


    /**
     * Constructs the object.
     *
     * @param name name of this set of parameters
     */
    public PdpRequestParameters(String name) {
        super(name);
    }
}
