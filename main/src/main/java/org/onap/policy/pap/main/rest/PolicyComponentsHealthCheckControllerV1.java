/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019,2021 Nordix Foundation.
 *  Modifications Copyright (C) 2020 AT&T Intellectual Property.
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

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end point for PAP component to fetch all policy components, including PAP,
 * API, Distribution, and PDPs.
 *
 * @author Yehui Wang (yehui.wang@est.tech)
 */
@RestController
@RequiredArgsConstructor
public class PolicyComponentsHealthCheckControllerV1 extends PapRestControllerV1
    implements PolicyComponentsHealthCheckControllerV1Api {

    private final PolicyComponentsHealthCheckProvider provider;


    /**
     * Returns health status of all Policy components, including PAP, API, Distribution, and PDPs.
     *
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    @Override
    public ResponseEntity<Map<String, Object>> policyComponentsHealthCheck(UUID requestId) {
        final Pair<HttpStatus, Map<String, Object>> pair = provider.fetchPolicyComponentsHealthStatus();
        return addLoggingHeaders(addVersionControlHeaders(ResponseEntity.status(pair.getLeft())), requestId)
            .body(pair.getRight());
    }
}
