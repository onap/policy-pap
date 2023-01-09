/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2023 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.pap.main.service.PdpGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for PAP component to query details of all PDP groups.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@RestController
@RequiredArgsConstructor
public class PdpGroupQueryControllerV1 extends PapRestControllerV1 implements PdpGroupQueryControllerV1Api {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdpGroupQueryControllerV1.class);

    private final PdpGroupService pdpGroupService;

    /**
     * Queries details of all PDP groups.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     * @throws PfModelException the exception
     */
    @Override
    public ResponseEntity<PdpGroups> queryGroupDetails(UUID requestId) {
        final var pdpGroups = new PdpGroups();
        pdpGroups.setGroups(pdpGroupService.getPdpGroups());
        LOGGER.debug("PdpGroup Query Response - {}", pdpGroups);
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(HttpStatus.OK)), requestId)
            .body(pdpGroups);
    }
}
