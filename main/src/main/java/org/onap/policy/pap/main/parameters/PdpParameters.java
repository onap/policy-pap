/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import java.util.concurrent.TimeUnit;
import lombok.Getter;
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
public class PdpParameters extends ParameterGroupImpl {

    /**
     * Default maximum message age, in milliseconds, that should be examined. Any message
     * older than this is discarded.
     */
    public static final long DEFAULT_MAX_AGE_MS = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);


    @Min(1)
    private long heartBeatMs;

    @Min(1)
    private long maxMessageAgeMs =  DEFAULT_MAX_AGE_MS;

    private PdpUpdateParameters updateParameters;
    private PdpStateChangeParameters stateChangeParameters;


    /**
     * Constructs the object.
     */
    public PdpParameters() {
        super(PdpParameters.class.getSimpleName());
    }
}
