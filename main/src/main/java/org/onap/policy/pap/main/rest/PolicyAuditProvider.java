/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.rest;

import java.time.Instant;
import java.util.Collection;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.persistence.provider.PolicyAuditProvider.AuditFilter;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;

/**
 * Provider for PAP component to query policy audit information.
 */
public class PolicyAuditProvider {

    /**
     * Factory for PAP DAO.
     */
    private final PolicyModelsProviderFactoryWrapper daoFactory;


    /**
     * Constructs the object.
     */
    public PolicyAuditProvider() {
        this.daoFactory = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
    }

    /**
     * Gets the audit record of all policies.
     *
     * @param recordCount number of records to fetch
     * @param fromDate the starting date for the query
     * @param toDate the ending date for the query
     * @return the audit record of all policies
     * @throws PfModelException if a DB error occurs
     */
    public Collection<PolicyAudit> getAuditRecords(int recordCount, Instant fromDate, Instant toDate)
                    throws PfModelException {
        try (PolicyModelsProvider dao = daoFactory.create()) {
            return dao.getAuditRecords(
                            AuditFilter.builder().recordNum(recordCount).fromDate(fromDate).toDate(toDate).build());
        }
    }

    /**
     * Gets the audit record of policies in a PdpGroup.
     *
     * @param recordCount number of records to fetch
     * @param fromDate the starting date for the query
     * @param toDate the ending date for the query
     * @param pdpGroupName the pdp group name for the query
     * @return the audit record of all policies
     * @throws PfModelException if a DB error occurs
     */
    public Collection<PolicyAudit> getAuditRecords(int recordCount, Instant fromDate, Instant toDate,
                    String pdpGroupName) throws PfModelException {
        try (PolicyModelsProvider dao = daoFactory.create()) {
            return dao.getAuditRecords(AuditFilter.builder().recordNum(recordCount).fromDate(fromDate).toDate(toDate)
                            .pdpGroup(pdpGroupName).build());
        }
    }

    /**
     * Gets the audit record of a policy in a PdpGroup.
     *
     * @param recordCount number of records to fetch
     * @param fromDate the starting date for the query
     * @param toDate the ending date for the query
     * @param pdpGroupName the pdp group name for the query
     * @param policyIdent the identifier of policy for the query
     * @return the audit record of all policies
     * @throws PfModelException if a DB error occurs
     */
    public Collection<PolicyAudit> getAuditRecords(int recordCount, Instant fromDate, Instant toDate,
                    String pdpGroupName, ToscaConceptIdentifier policyIdent) throws PfModelException {
        try (PolicyModelsProvider dao = daoFactory.create()) {
            return dao.getAuditRecords(AuditFilter.builder().recordNum(recordCount).fromDate(fromDate).toDate(toDate)
                            .pdpGroup(pdpGroupName).name(policyIdent.getName()).version(policyIdent.getVersion())
                            .build());
        }
    }

    /**
     * Gets the audit record of a policy.
     *
     * @param recordCount number of records to fetch
     * @param fromDate the starting date for the query
     * @param toDate the ending date for the query
     * @param policyIdent the identifier of policy for the query
     * @return the audit record of all policies
     * @throws PfModelException if a DB error occurs
     */
    public Collection<PolicyAudit> getAuditRecords(int recordCount, Instant fromDate, Instant toDate,
                    ToscaConceptIdentifier policyIdent) throws PfModelException {
        try (PolicyModelsProvider dao = daoFactory.create()) {
            return dao.getAuditRecords(AuditFilter.builder().recordNum(recordCount).fromDate(fromDate).toDate(toDate)
                            .name(policyIdent.getName()).version(policyIdent.getVersion()).build());
        }
    }
}
