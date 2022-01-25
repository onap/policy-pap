/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
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
import org.onap.policy.models.pdp.concepts.PdpGroups;
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
    public List<PdpGroup> getPdpGroupByName(@NonNull String pdpGroup) {
        return asPdpGroups(pdpGroupRepository.findByKeyName(pdpGroup));
    }

    /**
     * Get PDP groups by state.
     *
     * @param pdpState the state of pdpGroup
     * @return the PDP groups found
     */
    public List<PdpGroup> getPdpGroupByState(@NonNull PdpState pdpState) {
        return asPdpGroups(pdpGroupRepository.findByPdpGroupState(pdpState));
    }

    /**
     * Get PDP groups by name and state.
     *
     * @param pdpGroup the name of group
     * @param state the state of pdpGroup
     * @return the PDP groups found
     */
    public List<PdpGroup> getPdpGroupByNameAndState(@NonNull String pdpGroup, @NonNull PdpState state) {
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
    public PdpGroups savePdpGroups(@NonNull final List<PdpGroup> pdpGroups) {

        // Return the created PDP groups
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
        PdpGroups returnPdpGroups = new PdpGroups();
        returnPdpGroups.setGroups(returnPdpGroupList);
        return returnPdpGroups;
    }

    /**
     * Delete a PDP group.
     *
     * @param pdpGroup the name of the pdpGroup to delete
     */
    public void deletePdpGroup(String pdpGroup) {
        try {
            pdpGroupRepository.deleteById(new PfConceptKey(pdpGroup, "0.0.0"));
        } catch (Exception exc) {
            String errorMessage = "delete of PDP group \"" + pdpGroup + "\" failed, PDP group does not exist";
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, errorMessage, exc);
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
