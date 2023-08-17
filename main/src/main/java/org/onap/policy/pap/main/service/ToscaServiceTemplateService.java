/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2022-2023 Nordix Foundation.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntity;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.provider.SimpleToscaProvider;
import org.onap.policy.models.tosca.utils.ToscaUtils;
import org.onap.policy.pap.main.repository.ToscaServiceTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ToscaServiceTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToscaServiceTemplateService.class);

    private static final String METADATASET_NAME = "metadataSetName";
    private static final String METADATASET_VERSION = "metadataSetVersion";
    private static final String METADATASET = "metadataSet";

    private final ToscaServiceTemplateRepository serviceTemplateRepository;

    private final ToscaNodeTemplateService nodeTemplateService;

    /**
     * Get policies.
     *
     * @param name the name of the policy to get, null to get all policies
     * @param version the version of the policy to get, null to get all versions of a policy
     * @return the policies found
     * @throws PfModelException on errors getting policies
     */
    public List<ToscaPolicy> getPolicyList(final String name, final String version) throws PfModelException {

        LOGGER.debug("->getPolicyList: name={}, version={}", name, version);

        List<ToscaPolicy> policyList;

        try {
            List<Map<String, ToscaPolicy>> policies = getToscaServiceTemplate(name, version, "policy").toAuthorative()
                .getToscaTopologyTemplate().getPolicies();
            policyList = policies.stream().flatMap(policy -> policy.values().stream()).collect(Collectors.toList());
            populateMetadataSet(policyList);
        } catch (PfModelRuntimeException pfme) {
            return handlePfModelRuntimeException(pfme);
        } catch (Exception exc) {
            String errorMsg = "Failed to fetch policy with name " + name + " and version " + version + ".";
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, errorMsg, exc);
        }

        LOGGER.debug("<-getPolicyList: name={}, version={}, policyList={}", name, version, policyList);
        return policyList;
    }

    /**
     * Get filtered policies.
     *
     * @param filter the filter for the policies to get
     * @return the policies found
     * @throws PfModelException on errors getting policies
     */
    public List<ToscaPolicy> getFilteredPolicyList(ToscaTypedEntityFilter<ToscaPolicy> filter) throws PfModelException {
        String version = ToscaTypedEntityFilter.LATEST_VERSION.equals(filter.getVersion()) ? null : filter.getVersion();
        return filter.filter(getPolicyList(filter.getName(), version));
    }

    /**
     * Get policy types.
     *
     * @param name the name of the policy type to get, set to null to get all policy types
     * @param version the version of the policy type to get, set to null to get all versions
     * @return the policy types found
     * @throws PfModelException on errors getting policy types
     */
    public List<ToscaPolicyType> getPolicyTypeList(final String name, final String version) throws PfModelException {

        LOGGER.debug("->getPolicyTypeList: name={}, version={}", name, version);

        List<ToscaPolicyType> policyTypeList;

        try {
            policyTypeList = new ArrayList<>(
                getToscaServiceTemplate(name, version, "policyType").toAuthorative().getPolicyTypes().values());
        } catch (PfModelRuntimeException pfme) {
            return handlePfModelRuntimeException(pfme);
        } catch (Exception exc) {
            String errorMsg = "Failed to fetch policy type with name " + name + " and version " + version + ".";
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, errorMsg, exc);
        }

        LOGGER.debug("<-getPolicyTypeList: name={}, version={}, policyTypeList={}", name, version, policyTypeList);
        return policyTypeList;
    }

    private JpaToscaServiceTemplate getToscaServiceTemplate(final String name, final String version, final String type)
        throws PfModelException {

        Optional<JpaToscaServiceTemplate> serviceTemplate = serviceTemplateRepository
            .findById(new PfConceptKey(JpaToscaServiceTemplate.DEFAULT_NAME, JpaToscaServiceTemplate.DEFAULT_VERSION));
        if (serviceTemplate.isEmpty()) {
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, "service template not found in database");
        }

        LOGGER.debug("<-getServiceTemplate: serviceTemplate={}", serviceTemplate.get());
        JpaToscaServiceTemplate dbServiceTemplate = serviceTemplate.get();

        JpaToscaServiceTemplate returnServiceTemplate;
        if (type.equals("policy")) {
            returnServiceTemplate = getToscaPolicies(name, version, dbServiceTemplate);
        } else {
            returnServiceTemplate = getToscaPolicyTypes(name, version, dbServiceTemplate);
        }
        return returnServiceTemplate;
    }

    private JpaToscaServiceTemplate getToscaPolicies(final String name, final String version,
        JpaToscaServiceTemplate dbServiceTemplate) throws PfModelException {
        if (!ToscaUtils.doPoliciesExist(dbServiceTemplate)) {
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND,
                "policies for " + name + ":" + version + " do not exist");
        }

        JpaToscaServiceTemplate returnServiceTemplate =
            new SimpleToscaProvider().getCascadedPolicies(dbServiceTemplate, name, version);

        LOGGER.debug("<-getPolicies: name={}, version={}, serviceTemplate={}", name, version, returnServiceTemplate);
        return returnServiceTemplate;
    }

    private JpaToscaServiceTemplate getToscaPolicyTypes(final String name, final String version,
        JpaToscaServiceTemplate dbServiceTemplate) throws PfModelException {
        if (!ToscaUtils.doPolicyTypesExist(dbServiceTemplate)) {
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND,
                "policy types for " + name + ":" + version + " do not exist");
        }

        JpaToscaServiceTemplate returnServiceTemplate =
            new SimpleToscaProvider().getCascadedPolicyTypes(dbServiceTemplate, name, version);

        LOGGER.debug("<-getPolicyTypes: name={}, version={}, serviceTemplate={}", name, version, returnServiceTemplate);
        return returnServiceTemplate;
    }

    /**
     * Handle a PfModelRuntimeException on a list call.
     *
     * @param pfme the model exception
     * @return an empty list on 404
     */
    private <T extends ToscaEntity> List<T> handlePfModelRuntimeException(final PfModelRuntimeException pfme) {
        if (Response.Status.NOT_FOUND.equals(pfme.getErrorResponse().getResponseCode())) {
            LOGGER.trace("request did not find any results", pfme);
            return Collections.emptyList();
        } else {
            throw pfme;
        }
    }

    /**
     * Populates metadataSet in policy->metadata if metadataSet reference is provided.
     *
     * @param policies List of policies
     */
    private void populateMetadataSet(List<ToscaPolicy> policies) {
        for (ToscaPolicy policy : policies) {
            if (policy.getMetadata().keySet().containsAll(List.of(METADATASET_NAME, METADATASET_VERSION))) {
                var name = String.valueOf(policy.getMetadata().get(METADATASET_NAME));
                var version = String.valueOf(policy.getMetadata().get(METADATASET_VERSION));
                policy.getMetadata().putIfAbsent(METADATASET,
                    nodeTemplateService.getToscaNodeTemplate(name, version).getMetadata());
            }
        }
    }

}
