/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.Pdps;
import org.onap.policy.pap.main.service.PdpGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Provider for PAP component to to fetch health status of all PDPs registered with PAP.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@Service
@RequiredArgsConstructor
public class PdpGroupHealthCheckProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdpGroupHealthCheckProvider.class);

    private PdpGroupService pdpGroupService;

    /**
     * Returns health status of all PDPs.
     *
     * @return a pair containing the status and the response
     * @throws PfModelException in case of errors
     */
    public Pair<HttpStatus, Pdps> fetchPdpGroupHealthStatus() throws PfModelException {

        final var pdps = new Pdps();
        final List<PdpGroup> groups = pdpGroupService.getPdpGroups();
        final List<Pdp> pdpList = new ArrayList<>();
        for (final PdpGroup group : groups) {
            for (final PdpSubGroup subGroup : group.getPdpSubgroups()) {
                pdpList.addAll(subGroup.getPdpInstances());
            }
        }
        pdps.setPdpList(pdpList);
        LOGGER.debug("PdpGroup HealthCheck Response - {}", pdps);
        return Pair.of(HttpStatus.OK, pdps);
    }
}
