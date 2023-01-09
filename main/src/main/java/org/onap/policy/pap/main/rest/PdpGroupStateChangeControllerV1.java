/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2023 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.rest;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PdpGroupStateChangeResponse;
import org.onap.policy.models.pdp.enums.PdpState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to change state of a PDP group.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@RestController
@RequiredArgsConstructor
public class PdpGroupStateChangeControllerV1 extends PapRestControllerV1 implements PdpGroupStateChangeControllerV1Api {

    private static final Logger logger = LoggerFactory.getLogger(PdpGroupHealthCheckControllerV1.class);
    private final PdpGroupStateChangeProvider provider;

    /**
     * Changes state of a PDP group.
     *
     * @param requestId request ID used in ONAP logging
     * @param groupName name of the PDP group to be deleted
     * @param state state of the PDP group
     * @return a response
     * @throws PfModelException Exception thrown by changeGroupState
     */
    @Override
    public ResponseEntity<PdpGroupStateChangeResponse> changeGroupState(
        String groupName, PdpState state, UUID requestId) {
        try {
            final Pair<HttpStatus, PdpGroupStateChangeResponse> pair = provider.changeGroupState(groupName, state);
            return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(pair.getLeft())), requestId)
                .body(pair.getRight());
        } catch (PfModelException e) {
            logger.warn("changeGroupState failed", e);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
