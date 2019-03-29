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
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.tosca.simple.concepts.ToscaServiceTemplate;

/**
 * TODO: This is a fake and will be deleted once the one in policy/models is modified to
 * use the classes in models-pdp instead of models-pap.
 */
public interface PolicyModelsProvider extends AutoCloseable {

    /**
     * Get policy types.
     *
     * @param policyTypeKey the policy type key for the policy types to be retrieved. A
     *        null key name returns all policy types. A null key version returns all
     *        versions of the policy type name specified in the key.
     * @return the policy types found
     * @throws PfModelException on errors getting policy types
     */
    public ToscaServiceTemplate getPolicyTypes(@NonNull final PfConceptKey policyTypeKey) throws PfModelException;

    /**
     * Get policies.
     *
     * @param policyKey the policy key for the policies to be retrieved. The parent name
     *        and version must be specified. A null local name returns all policies for a
     *        parent policy type.
     * @return the policies found
     * @throws PfModelException on errors getting policies
     */
    public ToscaServiceTemplate getPolicies(@NonNull final PfConceptKey policyKey) throws PfModelException;

    /**
     * Get PDP groups.
     *
     * @param groupKey the group key for the groups to be retrieved. A null key version
     *        returns all versions of the group name specified in the key.
     * @return the PDP groups found
     * @throws PfModelException on errors getting PDP groups
     */
    public List<PdpGroup> getPdpGroups(@NonNull final PfConceptKey groupKey) throws PfModelException;

    /**
     * Gets the PDP groups that are active and that have at least one subgroup, with an
     * active PDP, supporting the specified policy type.
     *
     * @param policyTypeKey the policy type identifier, including the version
     * @return the PDP groups found
     * @throws PfModelException on errors getting PDP groups
     */
    public List<PdpGroup> getActivePdpGroupsByPolicy(@NonNull final PfConceptKey policyTypeKey) throws PfModelException;

    /**
     * Creates PDP groups.
     *
     * @param pdpGroups a specification of the PDP groups to create
     * @return the PDP groups created
     * @throws PfModelException on errors creating PDP groups
     */
    public PdpGroups createPdpGroups(@NonNull final PdpGroups pdpGroups) throws PfModelException;

    /**
     * Updates PDP groups.
     *
     * @param pdpGroups a specification of the PDP groups to update
     * @return the PDP groups updated
     * @throws PfModelException on errors updating PDP groups
     */
    public PdpGroups updatePdpGroups(@NonNull final PdpGroups pdpGroups) throws PfModelException;

    /**
     * Delete PDP groups.
     *
     * @param pdpGroupFilter a filter for the get
     * @return the PDP groups deleted
     * @throws PfModelException on errors deleting PDP groups
     */
    public PdpGroups deletePdpGroups(@NonNull final String pdpGroupFilter) throws PfModelException;

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
