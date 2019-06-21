/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.Pdps;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PAP component to to fetch health status of all PDPs registered with PAP.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpGroupHealthCheckProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdpGroupHealthCheckProvider.class);

    /**
     * Returns health status of all PDPs.
     *
     * @return a pair containing the status and the response
     * @throws PfModelException in case of errors
     */
    public Pair<Response.Status, Pdps> fetchPdpGroupHealthStatus() throws PfModelException {

        final Pdps pdps = new Pdps();
        final PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            final List<PdpGroup> groups = databaseProvider.getPdpGroups(null);
            final List<Pdp> pdpList = new ArrayList<>();
            for (final PdpGroup group : groups) {
                for (final PdpSubGroup subGroup : group.getPdpSubgroups()) {
                    pdpList.addAll(subGroup.getPdpInstances());
                }
            }
            pdps.setPdpList(pdpList);
        }
        LOGGER.debug("PdpGroup HealthCheck Response - {}", pdps);
        return Pair.of(Response.Status.OK, pdps);
    }
}
