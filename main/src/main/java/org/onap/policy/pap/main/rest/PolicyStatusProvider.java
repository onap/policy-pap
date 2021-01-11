/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.main.rest;

import java.util.Collection;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.notification.DeploymentTracker;

/**
 * Provider for PAP component to query policy deployment status.
 */
public class PolicyStatusProvider {

    /**
     * Factory for PAP DAO.
     */
    private final PolicyModelsProviderFactoryWrapper daoFactory;


    /**
     * Constructs the object. Loads all deployed policies into the internal cache.
     */
    public PolicyStatusProvider() {
        this.daoFactory = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
    }

    /**
     * Gets the deployment status of all policies.
     *
     * @return the deployment status of all policies
     * @throws PfModelException if a DB error occurs
     */
    public Collection<PolicyStatus> getStatus() throws PfModelException {
        try (PolicyModelsProvider dao = daoFactory.create()) {
            return accumulate(dao.getAllPolicyStatus());
        }
    }

    /**
     * Gets the deployment status of a policy.
     *
     * @param policy policy of interest
     * @return the deployment status of all policies
     * @throws PfModelException if a DB error occurs
     */
    public Collection<PolicyStatus> getStatus(ToscaConceptIdentifierOptVersion policy) throws PfModelException {
        try (PolicyModelsProvider dao = daoFactory.create()) {
            return accumulate(dao.getAllPolicyStatus(policy));
        }
    }

    /**
     * Accumulates the deployment status of individual PDP/policy pairs into a status for
     * a policy.
     *
     * @param source PDP/policy pairs
     * @return the deployment status of the policies
     */
    private Collection<PolicyStatus> accumulate(Collection<PdpPolicyStatus> source) {
        DeploymentTracker tracker = new DeploymentTracker();

        for (PdpPolicyStatus status : source) {
            if (status.isDeploy()) {
                tracker.add(status);
            }
        }

        return tracker.getDeploymentStatus();
    }
}
