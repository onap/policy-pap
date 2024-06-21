/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.pap.main.exception;

import jakarta.ws.rs.core.Response;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

@Aspect
@Component
public class ServiceExceptionHandler {

    /**
     * Handle any exceptions that are not already handled.
     * For e.g., runtime exceptions that could happen during SQL query execution related to data integrity etc.
     *
     * @param exception the exception
     */
    @AfterThrowing(pointcut = "execution(* org.onap.policy.pap.main.service.*.*(..))", throwing = "exception")
    public void handleServiceException(RuntimeException exception) {
        if (exception instanceof PfModelRuntimeException pfModelException) {
            throw pfModelException;
        } else {
            throw new PfModelRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
        }
    }

    /**
     * Handle DB Transaction related exceptions.
     * All service classes in org.onap.policy.pap.main.service are transactional now,
     * Autowiring these service classes can cause TransactionException.
     * For e.g., JDBC connection failure occurs and failed to open transaction at service level
     *
     * @param exception the exception
     */
    @AfterThrowing(pointcut = "execution(* org.onap.policy.pap.main.*.*.*(..))", throwing = "exception")
    public void handleTransactionException(TransactionException exception) {
        throw new PfModelRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
    }
}
