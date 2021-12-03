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
