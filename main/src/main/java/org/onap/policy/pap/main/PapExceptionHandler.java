/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada. All rights reserved.
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

import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.errors.concepts.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PapExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(PapExceptionHandler.class);

    @ExceptionHandler({PfModelException.class, PfModelRuntimeException.class})
    public ResponseEntity<ErrorResponse> pfModelExceptionHandler(PfModelException exp) {
        logger.warn(exp.getMessage(), exp);
        return ResponseEntity.status(exp.getErrorResponse().getResponseCode().getStatusCode())
            .body(exp.getErrorResponse());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException exp) {
        String errorMessage = exp.getClass().getName() + " " +exp.getMessage();
        logger.warn(exp.getMessage(), exp);
        return ResponseEntity.badRequest().body(errorMessage);
    }
}
