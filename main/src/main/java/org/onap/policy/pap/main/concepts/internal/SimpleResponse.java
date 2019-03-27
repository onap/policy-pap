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
 * Response returned when no extra output fields are needed.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public abstract class SimpleResponse<E extends org.onap.policy.models.pap.concepts.SimpleResponse> {

    /**
     * Optional detailed message in error cases.
     */
    private String errorDetails;


    public SimpleResponse(SimpleResponse<E> source) {
        this.errorDetails = source.errorDetails;
    }

    /**
     * Converts this to the corresponding class in models-pap.
     *
     * @return an external object, populated with data from this object
     */
    public E toExternal() {
        return copyTo(makeExternal());
    }

    /**
     * Copies fields from "this" to the target.
     *
     * @param target target to which to copy the fields
     * @return the target
     */
    public E copyTo(E target) {
        target.setErrorDetails(errorDetails);
        return target;
    }

    /**
     * Makes an external object that corresponds to "this" object's class.
     *
     * @return a new external object
     */
    public abstract E makeExternal();
}
