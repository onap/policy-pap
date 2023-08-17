/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.rest.stub;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.models.pdp.concepts.DeploymentGroups;
import org.onap.policy.pap.main.rest.PapRestControllerV1;
import org.onap.policy.pap.main.rest.PdpGroupDeployControllerV1Api;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile("stub")
public class PdpGroupDeployControllerV1Stub extends PapRestControllerV1
    implements PdpGroupDeployControllerV1Api {

    private final StubUtils stubUtils;

    @Override
    public ResponseEntity<PdpGroupDeployResponse> deployPolicies(
            UUID requestId, @Valid PdpDeployPolicies body) {
        return stubUtils.getStubbedResponse(PdpGroupDeployResponse.class);
    }

    @Override
    public ResponseEntity<PdpGroupDeployResponse> updateGroupPolicies(
            UUID requestId, @Valid DeploymentGroups body) {
        return stubUtils.getStubbedResponse(PdpGroupDeployResponse.class);
    }

}
