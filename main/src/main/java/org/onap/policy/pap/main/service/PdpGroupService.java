/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.service;

import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.base.PfReferenceKey;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.pdp.persistence.concepts.JpaPdp;
import org.onap.policy.models.pdp.persistence.concepts.JpaPdpGroup;
import org.onap.policy.models.pdp.persistence.concepts.JpaPdpSubGroup;
import org.onap.policy.pap.main.repository.PdpGroupRepository;
import org.onap.policy.pap.main.repository.PdpRepository;
import org.onap.policy.pap.main.repository.PdpSubGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PdpGroupService {

    private final PdpGroupRepository pdpGroupRepository;
    private final PdpSubGroupRepository pdpSubGroupRepository;
    private final PdpRepository pdpRepository;

    /**
     * Get all PDP groups.
     *
     * @return the PDP groups found
     */
    public List<PdpGroup> getPdpGroups() {
        return asPdpGroups(pdpGroupRepository.findAll());
    }

    /**
     * Get PDP groups by name.
     *
     * @param pdpGroup the name of group
     * @return the PDP groups found
     */
    public List<PdpGroup> getPdpGroups(@NonNull String pdpGroup) {
        return asPdpGroups(pdpGroupRepository.findByKeyName(pdpGroup));
    }

    /**
     * Get PDP groups by state.
     *
     * @param pdpState the state of pdpGroup
     * @return the PDP groups found
     */
    public List<PdpGroup> getPdpGroups(@NonNull PdpState pdpState) {
        return asPdpGroups(pdpGroupRepository.findByPdpGroupState(pdpState));
    }

    /**
     * Get PDP groups by name and state.
     *
     * @param pdpGroup the name of group
     * @param state the state of pdpGroup
     * @return the PDP groups found
     */
    public List<PdpGroup> getPdpGroups(@NonNull String pdpGroup, @NonNull PdpState state) {
        return asPdpGroups(pdpGroupRepository.findByKeyNameAndPdpGroupState(pdpGroup, state));
    }

    /**
     * Get filtered PDP groups.
     *
     * @param filter the filter for the PDP groups to get
     * @return the PDP groups found
     */
    public List<PdpGroup> getFilteredPdpGroups(@NonNull final PdpGroupFilter filter) {
        return filter.filter(asPdpGroups(pdpGroupRepository.findAll()));
    }

    /**
     * Creates PDP groups.
     *
     * @param pdpGroups the PDP groups to create
     * @return the PDP groups created
     */
    public List<PdpGroup> createPdpGroups(@NonNull final List<PdpGroup> pdpGroups) {
        return savePdpGroups(pdpGroups);
    }

    /**
     * Updates PDP groups.
     *
     * @param pdpGroups the PDP groups to create
     * @return the PDP groups created
     */
    public List<PdpGroup> updatePdpGroups(@NonNull final List<PdpGroup> pdpGroups) {
        return savePdpGroups(pdpGroups);
    }

    private List<PdpGroup> savePdpGroups(final List<PdpGroup> pdpGroups) {
        List<PdpGroup> returnPdpGroupList = new ArrayList<>();

        for (PdpGroup pdpGroup : pdpGroups) {
            var jpaPdpGroup = new JpaPdpGroup();
            try {
                jpaPdpGroup.fromAuthorative(pdpGroup);

                BeanValidationResult validationResult = jpaPdpGroup.validate("PDP group");
                if (!validationResult.isValid()) {
                    throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
                }

                returnPdpGroupList.add(pdpGroupRepository.save(jpaPdpGroup).toAuthorative());
            } catch (Exception exc) {
                throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    "Failed saving PdpGroup. " + exc.getMessage(), exc);
            }
        }
        return returnPdpGroupList;
    }

    /**
     * Delete a PDP group.
     *
     * @param pdpGroup the name of the pdpGroup to delete
     */
    public void deletePdpGroup(String pdpGroup) {
        PfConceptKey groupKey = new PfConceptKey(pdpGroup, "0.0.0");
        if (pdpGroupRepository.existsById(groupKey)) {
            pdpGroupRepository.deleteById(groupKey);
        } else {
            String errorMessage = "delete of PDP group \"" + pdpGroup + "\" failed, PDP group does not exist";
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, errorMessage);
        }
    }

    /**
     * Convert JPA PDP group list to an authorative PDP group list.
     *
     * @param jpaPdpGroupList the list to convert
     * @return the authorative list
     */
    private List<PdpGroup> asPdpGroups(List<JpaPdpGroup> jpaPdpGroupList) {
        List<PdpGroup> pdpGroupList = new ArrayList<>(jpaPdpGroupList.size());
        for (JpaPdpGroup jpaPdpGroup : jpaPdpGroupList) {
            pdpGroupList.add(jpaPdpGroup.toAuthorative());
        }
        return pdpGroupList;
    }

    /**
     * Update a PDP.
     *
     * @param pdpGroupName the name of the PDP group of the PDP subgroup
     * @param pdpSubGroup the PDP subgroup to be updated
     * @param pdp the PDP to be updated
     */
    public void updatePdp(@NonNull final String pdpGroupName, @NonNull final String pdpSubGroup,
        @NonNull final Pdp pdp) {

        final var pdpKey = new PfReferenceKey(pdpGroupName, PfKey.NULL_KEY_VERSION, pdpSubGroup, pdp.getInstanceId());
        final var jpaPdp = new JpaPdp(pdpKey);
        jpaPdp.fromAuthorative(pdp);

        BeanValidationResult validationResult = jpaPdp.validate("PDP");
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }

        pdpRepository.save(jpaPdp);
    }

    /**
     * Update a PDP subgroup.
     *
     * @param pdpGroupName the name of the PDP group of the PDP subgroup
     * @param pdpSubGroup the PDP subgroup to be updated
     */
    public void updatePdpSubGroup(@NonNull final String pdpGroupName, @NonNull final PdpSubGroup pdpSubGroup) {

        final var subGroupKey = new PfReferenceKey(pdpGroupName, PfKey.NULL_KEY_VERSION, pdpSubGroup.getPdpType());
        final var jpaPdpSubgroup = new JpaPdpSubGroup(subGroupKey);
        jpaPdpSubgroup.fromAuthorative(pdpSubGroup);

        BeanValidationResult validationResult = jpaPdpSubgroup.validate("PDP sub group");
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        pdpSubGroupRepository.save(jpaPdpSubgroup);
    }

}
