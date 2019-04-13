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

package org.onap.policy.pap.main.startstop;

import java.util.List;

import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.provider.PolicyModelsProviderFactory;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.pap.main.PolicyPapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates initial PdpGroup/SubGroup in the database.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class PapDatabaseInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PapDatabaseInitializer.class);

    private StandardCoder standardCoder;
    private PolicyModelsProviderFactory factory;

    /**
     * Constructs the object.
     */
    public PapDatabaseInitializer() {
        factory = new PolicyModelsProviderFactory();
        standardCoder = new StandardCoder();
    }

    /**
     * Initializes database.
     *
     * @param policyModelsProviderParameters the database parameters
     * @throws PolicyPapException in case of errors.
     */
    public void initializePapDatabase(final PolicyModelsProviderParameters policyModelsProviderParameters)
            throws PolicyPapException {

        try (PolicyModelsProvider databaseProvider =
                factory.createPolicyModelsProvider(policyModelsProviderParameters)) {
            final String originalJson = ResourceUtils.getResourceAsString("PapDb.json");
            final PdpGroups pdpGroupsToCreate = standardCoder.decode(originalJson, PdpGroups.class);
            final List<PdpGroup> pdpGroupsFromDb = databaseProvider.getPdpGroups(
                    pdpGroupsToCreate.getGroups().get(0).getName());
            if (pdpGroupsFromDb.isEmpty()) {
                databaseProvider.createPdpGroups(pdpGroupsToCreate.getGroups());
                LOGGER.debug("Created initial pdpGroup in DB - {}", pdpGroupsToCreate);
            } else {
                LOGGER.debug("Initial pdpGroup already exists in DB, skipping create - {}", pdpGroupsFromDb);
            }
        } catch (final PfModelException | CoderException exp) {
            throw new PolicyPapException(exp);
        }
    }
}
