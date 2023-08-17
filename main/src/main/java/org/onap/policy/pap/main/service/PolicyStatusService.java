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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.pdp.persistence.concepts.JpaPdpPolicyStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifierOptVersion;
import org.onap.policy.pap.main.repository.PolicyStatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PolicyStatusService {

    private final PolicyStatusRepository policyStatusRepository;

    /**
     * Gets all status for policies in a group.
     *
     * @param pdpGroup the group's name
     * @return the policy status list found
     */
    public List<PdpPolicyStatus> getGroupPolicyStatus(@NonNull String pdpGroup) {
        return asPolicyStatusList(policyStatusRepository.findByPdpGroup(pdpGroup));
    }

    /**
     * Gets all status for policies.
     *
     * @return the policy status list found
     */
    public List<PdpPolicyStatus> getAllPolicyStatus() {
        return asPolicyStatusList(policyStatusRepository.findAll());
    }

    /**
     * Gets all status for a policy.
     *
     * @param policy the policy
     * @return the policy status list found
     */
    public List<PdpPolicyStatus> getAllPolicyStatus(@NonNull ToscaConceptIdentifierOptVersion policy) {

        if (policy.getVersion() != null) {
            return asPolicyStatusList(policyStatusRepository
                .findByKeyParentKeyNameAndKeyParentKeyVersion(policy.getName(), policy.getVersion()));

        } else {
            return asPolicyStatusList(policyStatusRepository.findByKeyParentKeyName(policy.getName()));
        }
    }

    /**
     * Gets all status for a policy in a group.
     *
     * @param pdpGroup the group's name
     * @param policy the policy
     * @return the policy status list found
     */
    public List<PdpPolicyStatus> getAllPolicyStatus(@NonNull String pdpGroup,
        @NonNull ToscaConceptIdentifierOptVersion policy) {
        if (policy.getVersion() != null) {
            return asPolicyStatusList(policyStatusRepository.findByPdpGroupAndKeyParentKeyNameAndKeyParentKeyVersion(
                pdpGroup, policy.getName(), policy.getVersion()));
        } else {
            return asPolicyStatusList(
                policyStatusRepository.findByPdpGroupAndKeyParentKeyName(pdpGroup, policy.getName()));
        }
    }

    /**
     * Creates, updates, and deletes collections of policy status.
     *
     * @param createObjs the objects to create
     * @param updateObjs the objects to update
     * @param deleteObjs the objects to delete
     */
    public void cudPolicyStatus(Collection<PdpPolicyStatus> createObjs, Collection<PdpPolicyStatus> updateObjs,
        Collection<PdpPolicyStatus> deleteObjs) {
        try {
            policyStatusRepository.deleteAll(fromAuthorativeStatus(deleteObjs, "deletePdpPolicyStatusList"));
            policyStatusRepository.saveAll(fromAuthorativeStatus(createObjs, "createPdpPolicyStatusList"));
            policyStatusRepository.saveAll(fromAuthorativeStatus(updateObjs, "updatePdpPolicyStatusList"));
        } catch (Exception exc) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                "Policy status operation failed." + exc.getMessage(), exc);
        }
    }

    /**
     * Converts a collection of authorative policy status to a collection of JPA policy
     * status. Validates the resulting list.
     *
     * @param objs authorative policy status to convert
     * @param fieldName name of the field containing the collection
     * @return a list of JPA policy status
     */
    private List<JpaPdpPolicyStatus> fromAuthorativeStatus(Collection<PdpPolicyStatus> objs, String fieldName) {
        if (objs == null) {
            return Collections.emptyList();
        }

        List<JpaPdpPolicyStatus> jpas = objs.stream().map(JpaPdpPolicyStatus::new).collect(Collectors.toList());

        // validate the objects
        var result = new BeanValidationResult(fieldName, jpas);

        var count = 0;
        for (JpaPdpPolicyStatus jpa : jpas) {
            result.addResult(jpa.validate(String.valueOf(count++)));
        }

        if (!result.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, result.getResult());
        }

        return jpas;
    }

    private List<PdpPolicyStatus> asPolicyStatusList(List<JpaPdpPolicyStatus> jpaPdpPolicyStatusList) {
        return jpaPdpPolicyStatusList.stream().map(JpaPdpPolicyStatus::toAuthorative).collect(Collectors.toList());
    }
}
