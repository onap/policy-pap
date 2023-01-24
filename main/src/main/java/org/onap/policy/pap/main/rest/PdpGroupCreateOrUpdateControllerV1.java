/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2023 Nordix Foundation.
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.rest;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pap.concepts.PdpGroupUpdateResponse;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to create or update PDP groups.
 */
@RestController
@RequiredArgsConstructor
@Profile("default")
public class PdpGroupCreateOrUpdateControllerV1 extends PapRestControllerV1
    implements PdpGroupCreateOrUpdateControllerV1Api {

    private static final Logger logger = LoggerFactory.getLogger(PdpGroupCreateOrUpdateControllerV1.class);

    private final PdpGroupCreateOrUpdateProvider provider;

    /**
     * Creates or updates one or more PDP groups.
     *
     * @param requestId request ID used in ONAP logging
     * @param groups PDP group configuration
     * @return a response
     */
    @Override
    public ResponseEntity<PdpGroupUpdateResponse> createOrUpdateGroups(UUID requestId, PdpGroups groups) {

        return doOperation(requestId, "create groups failed", () -> provider.createOrUpdateGroups(groups));
    }

    /**
     * Invokes an operation.
     *
     * @param requestId request ID
     * @param errmsg error message to log if the operation throws an exception
     * @param runnable operation to invoke
     * @return a {@link PdpGroupUpdateResponse} response entity
     */
    private ResponseEntity<PdpGroupUpdateResponse> doOperation(UUID requestId, String errmsg,
        RunnableWithPfEx runnable) {
        try {
            runnable.run();
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.ok()), requestId)
                .body(new PdpGroupUpdateResponse());
        } catch (PfModelException | PfModelRuntimeException e) {
            logger.warn(errmsg, e);
            var resp = new PdpGroupUpdateResponse();
            resp.setErrorDetails(e.getErrorResponse().getErrorMessage());
            return addLoggingHeaders(
                addVersionControlHeaders(ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode())),
                requestId).body(resp);
        }
    }
}
