/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.pap.pdpclient;

import org.junit.Ignore;
import org.junit.Test;
import org.onap.policy.pap.pdpclient.model.ModeParameters;
import org.onap.policy.pap.pdpclient.model.Policy;
import org.onap.policy.pap.pdpclient.model.PolicyActivationParameters;
import org.onap.policy.pap.pdpclient.model.PolicyRetirementParameters;
import org.onap.policy.pap.pdpclient.model.PolicyRollbackParameters;
import org.onap.policy.pap.pdpclient.model.PolicyUpgradeParameters;

/**
 * API tests for DefaultApi
 */
@Ignore
public class PdpClientTest {

    private final PdpClient api = new PdpClient(new ApiClient());


    /**
     * Activate a policy on the PDP
     *
     * 
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void policyDeploymentActivatePutTest() throws ApiException {
        PolicyActivationParameters policyActivationParameters = null;
        api.policyDeploymentActivatePut(policyActivationParameters);

        // TODO: test validations
    }

    /**
     * Deploys a policy to the PDP
     *
     * 
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void policyDeploymentDeployPutTest() throws ApiException {
        Policy policyDeploymentParameters = null;
        api.policyDeploymentDeployPut(policyDeploymentParameters);

        // TODO: test validations
    }

    /**
     * Set the mode of the PDP
     *
     * 
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void policyDeploymentModePutTest() throws ApiException {
        ModeParameters modeParameters = null;
        api.policyDeploymentModePut(modeParameters);

        // TODO: test validations
    }

    /**
     * Retire a policy from the PDP
     *
     * 
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void policyDeploymentRetirePutTest() throws ApiException {
        PolicyRetirementParameters policyRetirementParameters = null;
        api.policyDeploymentRetirePut(policyRetirementParameters);

        // TODO: test validations
    }

    /**
     * Rollback a policy on the PDP to an older version
     *
     * 
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void policyDeploymentRollbackPutTest() throws ApiException {
        PolicyRollbackParameters policyRollbackParameters = null;
        api.policyDeploymentRollbackPut(policyRollbackParameters);

        // TODO: test validations
    }

    /**
     * Upgrade a policy on the PDP to a newer version
     *
     * 
     *
     * @throws ApiException if the Api call fails
     */
    @Test
    public void policyDeploymentUpgradePutTest() throws ApiException {
        PolicyUpgradeParameters policyUpgradeParameters = null;
        api.policyDeploymentUpgradePut(policyUpgradeParameters);

        // TODO: test validations
    }

}
