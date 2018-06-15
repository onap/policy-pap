/*-
 * ============LICENSE_START=======================================================
 * pap-service
 * ================================================================================
 * Copyright (C) 2018 Ericsson Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.service.plugin;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import org.onap.policy.pap.pdpclient.model.Policy;

/**
 * Interface to be implemented by domain specific plugins to the PAP.
 */
public interface PapPlugin {

    /**
     * Generate a Policy
     * 
     * @param policyId the policy ID
     * @param policyName the policy name
     * @param policyVersion the policy version
     * @param policyArtifacts the artifacts for the policy
     * @param policyMetadata the policy metadata
     * @return the created Policy
     */
    Policy generatePolicy(int policyId, final String policyName, final String policyVersion,
            Collection<File> policyArtifacts, final Map<String, String> policyMetadata);

}
