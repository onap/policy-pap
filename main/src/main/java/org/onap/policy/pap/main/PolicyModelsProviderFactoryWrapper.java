/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.main;

import java.util.List;
import lombok.NonNull;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.provider.PolicyModelsProviderFactory;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.simple.concepts.ToscaServiceTemplate;

/**
 * Wraps a {@link PolicyModelsProviderFactoryWrapper}.
 */
public class PolicyModelsProviderFactoryWrapper implements AutoCloseable {
    private final PolicyModelsProviderParameters params;
    private final PolicyModelsProviderFactory factory;

    /**
     * Constructs the object.
     *
     * @param params DAO configuration parameters
     */
    public PolicyModelsProviderFactoryWrapper(PolicyModelsProviderParameters params) {
        this.params = params;
        this.factory = new PolicyModelsProviderFactory();
    }

    @Override
    public void close() throws Exception {
        /*
         * PolicyModelsProviderFactory should, in theory, implement AutoCloseable so it
         * can close the entity manager factory and release all data. Since it doesn't
         * this method does nothing for now.
         */
    }

    /**
     * Creates a provider based on models-pap.
     *
     * @return a new provider
     * @throws PfModelException if an error occurs
     */
    public org.onap.policy.models.provider.PolicyModelsProvider createPapProvider() throws PfModelException {
        /*
         * TODO rename this method to create() once the provider in policy/models has been
         * updated to use models-pdp instead of models-pap.
         */

        return factory.createPolicyModelsProvider(params);
    }

    /**
     * Creates a provider.
     *
     * @return a new provider
     * @throws PfModelException if an error occurs
     */
    public PolicyModelsProvider create() throws PfModelException {

        return new PolicyModelsProvider() {

            @Override
            public void close() throws Exception {
                // do nothing
            }

            @Override
            public ToscaServiceTemplate getPolicyTypes(@NonNull PfConceptKey policyTypeKey) throws PfModelException {
                throw new UnsupportedOperationException();
            }

            @Override
            public PdpGroups createPdpGroups(@NonNull PdpGroups pdpGroups) throws PfModelException {
                throw new UnsupportedOperationException();
            }

            @Override
            public PdpGroups updatePdpGroups(@NonNull PdpGroups pdpGroups) throws PfModelException {
                throw new UnsupportedOperationException();
            }

            @Override
            public PdpGroups deletePdpGroups(@NonNull String pdpGroupFilter) throws PfModelException {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<PdpGroup> getPdpGroups(@NonNull PfConceptKey groupKey) throws PfModelException {
                throw new UnsupportedOperationException();
            }

            @Override
            public PdpGroup createPdpGroup(@NonNull PdpGroup pdpGroup) throws PfModelException {
                throw new UnsupportedOperationException();
            }

            @Override
            public PdpGroup updatePdpGroup(@NonNull PdpGroup pdpGroup) throws PfModelException {
                throw new UnsupportedOperationException();
            }

            @Override
            public ToscaServiceTemplate getPolicies(@NonNull PfConceptKey policyKey) throws PfModelException {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<PdpGroup> getActivePdpGroupsByPolicy(@NonNull PfConceptKey policyTypeKey)
                            throws PfModelException {
                throw new UnsupportedOperationException();
            }
        };
    }
}
