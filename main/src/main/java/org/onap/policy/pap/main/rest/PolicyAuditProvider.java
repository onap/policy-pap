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

import java.util.Collection;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyAudit;
import org.onap.policy.models.pap.persistence.provider.PolicyAuditProvider.AuditFilter;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Provider for PAP component to query policy audit information.
 */
@Service
public class PolicyAuditProvider {

    /**
     * Factory for PAP DAO.
     */
    private PolicyModelsProviderFactoryWrapper daoFactory;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        this.daoFactory = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
    }

    /**
     * Gets the audit record of all policies.
     *
     * @param auditFilter the filter for the query
     * @return the audit record of all policies
     * @throws PfModelException if a DB error occurs
     */
    public Collection<PolicyAudit> getAuditRecords(AuditFilter auditFilter)
                    throws PfModelException {
        try (PolicyModelsProvider dao = daoFactory.create()) {
            return dao.getAuditRecords(auditFilter);
        }
    }
}
