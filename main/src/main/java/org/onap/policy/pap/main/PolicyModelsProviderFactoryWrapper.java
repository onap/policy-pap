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

import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.provider.PolicyModelsProviderFactory;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;

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
    public PolicyModelsProviderFactoryWrapper(final PolicyModelsProviderParameters params) {
        this.params = params;
        this.factory = new PolicyModelsProviderFactory();
    }

    @Override
    public void close() throws Exception {
        /*
         * PolicyModelsProviderFactory should, in theory, implement AutoCloseable so it can close the entity manager
         * factory and release all data. Since it doesn't this method does nothing for now.
         */
    }

    /**
     * Creates a provider based on models-pap.
     *
     * @return a new provider
     * @throws PolicyPapException in case of errors.
     */
    public PolicyModelsProvider create() throws PolicyPapException {
        try (PolicyModelsProvider modelsProvider = factory.createPolicyModelsProvider(params)) {
            modelsProvider.init();
            return modelsProvider;
        } catch (final Exception exp) {
            throw new PolicyPapException(exp);
        }
    }
}
