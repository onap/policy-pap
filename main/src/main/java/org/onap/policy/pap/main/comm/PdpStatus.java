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

package org.onap.policy.pap.main.comm;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// TODO delete this class once the real once is available

/**
 * PDP Status message. This is a stub class that will be replaced once the real classes
 * are available.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class PdpStatus {

    /**
     * Name of the PDP from which this message originated.
     */
    private String name;

    /**
     * ID from the original request, or {@code null} if this is an autonomous message.
     */
    private String requestId;

    /**
     * Constructs the object.
     *
     * @param name name of the PDP from which the message originated
     */
    public PdpStatus(String name) {
        this.name = name;
    }

    /**
     * Constructs the object.
     *
     * @param name name of the PDP from which the message originated
     * @param requestId the ID from the original request, or {@code null} if this is an
     *        autonomous message
     */
    public PdpStatus(String name, String requestId) {
        this.name = name;
        this.requestId = requestId;
    }
}
