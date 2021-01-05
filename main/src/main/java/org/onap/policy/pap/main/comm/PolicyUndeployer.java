/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.pap.main.comm;

import java.util.Collection;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

@FunctionalInterface
public interface PolicyUndeployer {
    /**
     * Undeploys a list of policies.
     *
     * @param group name of the group from which to undeploy the policies
     * @param subgroup name of the subgroup from which to undeploy the policies
     * @param policies policies to be undeployed
     * @throws PfModelException if an error occurs
     */
    public void undeploy(String group, String subgroup, Collection<ToscaConceptIdentifier> policies)
                    throws PfModelException;
}
