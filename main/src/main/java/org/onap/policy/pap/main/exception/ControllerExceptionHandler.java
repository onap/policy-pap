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

package org.onap.policy.pap.main.exception;

import java.util.UUID;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.errors.concepts.ErrorResponse;
import org.onap.policy.pap.main.rest.PapRestControllerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class ControllerExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    /**
     * Handle PfModelException.
     *
     * @return ResponseEntity the response
     */
    @ExceptionHandler(PfModelException.class)
    public ResponseEntity<ErrorResponse> pfModelExceptionHandler(PfModelException exp, WebRequest req) {
        return handlePfModelException(exp, exp.getErrorResponse(), req);
    }

    /**
     * Handle PfModelRuntimeException.
     *
     * @return ResponseEntity the response
     */
    @ExceptionHandler(PfModelRuntimeException.class)
    public ResponseEntity<ErrorResponse> pfModelRuntimeExceptionHandler(PfModelRuntimeException exp, WebRequest req) {
        return handlePfModelException(exp, exp.getErrorResponse(), req);
    }

    private ResponseEntity<ErrorResponse> handlePfModelException(Exception exp, ErrorResponse errorResponse,
        WebRequest req) {
        logger.warn(exp.getMessage(), exp);
        String requestId = req.getHeader(PapRestControllerV1.REQUEST_ID_NAME);
        return PapRestControllerV1.addLoggingHeaders(
            PapRestControllerV1
                .addVersionControlHeaders(ResponseEntity.status(errorResponse.getResponseCode().getStatusCode())),
            requestId != null ? UUID.fromString(requestId) : null).body(errorResponse);
    }

    /**
     * Handle IllegalArgumentException.
     *
     * @return ResponseEntity the response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException exp, WebRequest req) {
        String errorMessage = exp.getClass().getName() + " " + exp.getMessage();
        logger.warn(exp.getMessage(), exp);
        String requestId = req.getHeader(PapRestControllerV1.REQUEST_ID_NAME);
        return PapRestControllerV1.addLoggingHeaders(
            PapRestControllerV1.addVersionControlHeaders(ResponseEntity.status(HttpStatus.BAD_REQUEST)),
            requestId != null ? UUID.fromString(requestId) : null).body(errorMessage);
    }
}
