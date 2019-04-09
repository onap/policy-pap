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

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider for PAP component to query details of all PDP groups.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PdpGroupQueryProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdpGroupQueryProvider.class);

    /**
     * Queries details of all PDP groups.
     *
     * @return a pair containing the status and the response
     * @throws PfModelException in case of errors
     */
    public Pair<Response.Status, PdpGroups> fetchPdpGroupDetails() throws PfModelException {

        final PdpGroups pdpGroups = new PdpGroups();
        final PolicyModelsProviderFactoryWrapper modelProviderWrapper =
                Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
            pdpGroups.setGroups(databaseProvider.getPdpGroups(null, null));
        }
        LOGGER.debug("PdpGroup Query Response - {}", pdpGroups);
        return Pair.of(Response.Status.OK, pdpGroups);
    }
}
