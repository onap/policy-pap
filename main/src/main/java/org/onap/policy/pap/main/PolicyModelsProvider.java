/*
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
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.tosca.simple.concepts.ToscaPolicy;

/**
 * TODO: This is a fake and will be deleted once the one in policy/models is modified to
 * use the classes in models-pdp instead of models-pap.
 */
public interface PolicyModelsProvider extends AutoCloseable {

    /**
     * Get policies.
     *
     * @param name policy name/id
     * @param version policy version, or "", which matches all versions
     * @return the policies found
     * @throws PfModelException on errors getting policies
     */
    public List<ToscaPolicy> getPolicies(@NonNull String name, @NonNull String version) throws PfModelException;

    /**
     * Get policy with the maximum version.
     *
     * @param name policy name/id
     * @return the policy found or {@code null} if not found
     * @throws PfModelException on errors getting policies
     */
    public ToscaPolicy getPolicyMaxVersion(@NonNull String name) throws PfModelException;

    /**
     * Get PDP groups.
     *
     * @param name group name
     * @param version group version, or "", which matches all versions
     * @return the PDP groups found
     * @throws PfModelException on errors getting PDP groups
     */
    public List<PdpGroup> getPdpGroups(@NonNull String name, @NonNull String version) throws PfModelException;

    /**
     * Gets the PDP groups that are active and that have at least one subgroup, with an
     * active PDP, supporting the specified policy type.
     *
     * @param policyType the policy type
     * @param policyTypeVersion the policy type version
     * @return the PDP groups found
     * @throws PfModelException on errors getting PDP groups
     */
    public List<PdpGroup> getActivePdpGroupsByPolicy(@NonNull String policyType, @NonNull String policyTypeVersion)
                    throws PfModelException;

    /**
     * Get PDP group with the maximum version.
     *
     * @param name group name
     * @return the PDP group found, or {@code null} if not found
     * @throws PfModelException on errors getting PDP groups
     */
    public PdpGroup getPdpGroupMaxVersion(@NonNull String name) throws PfModelException;

    /**
     * Creates a PDP group.
     *
     * @param pdpGroup a specification of the PDP group to create
     * @return the PDP group created
     * @throws PfModelException on errors creating PDP groups
     */
    public PdpGroup createPdpGroup(@NonNull final PdpGroup pdpGroup) throws PfModelException;

    /**
     * Updates a PDP group.
     *
     * @param pdpGroup a specification of the PDP group to update
     * @return the PDP group updated
     * @throws PfModelException on errors updating PDP groups
     */
    public PdpGroup updatePdpGroup(@NonNull final PdpGroup pdpGroup) throws PfModelException;
}
