/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2024 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main;

import java.io.Serial;

/**
 * This runtime exception will be called if a runtime error occurs when using policy pap.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PolicyPapRuntimeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -8507246953751956974L;

    /**
     * Instantiates a new policy pap runtime exception with a message.
     *
     * @param message the message
     */
    public PolicyPapRuntimeException(final String message) {
        super(message);
    }

    /**
     * Instantiates a new policy pap runtime exception with a caused by exception.
     *
     * @param exp the exception that caused this exception to be thrown
     */
    public PolicyPapRuntimeException(final Exception exp) {
        super(exp);
    }

    /**
     * Instantiates a new policy pap runtime exception with a message and a caused by exception.
     *
     * @param message the message
     * @param exp the exception that caused this exception to be thrown
     */
    public PolicyPapRuntimeException(final String message, final Exception exp) {
        super(message, exp);
    }
}
