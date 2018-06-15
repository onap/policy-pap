/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.pap.service.dao;

import java.util.Collection;
import org.onap.policy.pap.service.jpa.PdpEntity;
import org.onap.policy.pap.service.jpa.PdpEntityRepository;
import org.onap.policy.pap.service.jpa.PdpGroupEntity;
import org.onap.policy.pap.service.jpa.PdpGroupEntityRepository;
import org.onap.policy.pap.service.jpa.PolicyEntity;
import org.onap.policy.pap.service.jpa.PolicyEntityRepository;
import org.onap.policy.pap.service.jpa.PolicySetToPolicyEntity;
import org.onap.policy.pap.service.jpa.PolicySetToPolicyEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * DAO for access the policy DB.
 */
@Service
public class PolicyDbDao {

    @Autowired
    PdpEntityRepository pdpEntityRepository;
    @Autowired
    PdpGroupEntityRepository pdpGroupEntityRepository;
    @Autowired
    PolicySetToPolicyEntityRepository policySetToPolicyEntityRepository;
    @Autowired
    PolicyEntityRepository policyEntityRepository;

    public long getPolicySetIdForPdpGroup(final String pdpGroupName) {
        PdpGroupEntity pdpGroupEntity = pdpGroupEntityRepository.findByGroupName(pdpGroupName);
        return pdpGroupEntity.getPolicySetId();
    }

    public Collection<PolicySetToPolicyEntity> getPoliciesForPolicySetId(final long policySetId) {
        return policySetToPolicyEntityRepository.findByPolicySetId(policySetId);
    }

    public long getPdpGroupId(final String pdpGroupName) {
        return pdpGroupEntityRepository.findByGroupName(pdpGroupName).getGroupId();
    }

    public PolicyEntity getPolicyEntity(long policyId) {
        return policyEntityRepository.findOne(policyId);
    }

    public void createPdp(String pdpName, String pdpVersion, String pdpState, String pdpType, String pdpEndpoint,
            long pdpGroupId, long policySetId) {
        PdpEntity newPdp = new PdpEntity();

        newPdp.setPdpName(pdpName);
        newPdp.setPdpEndpoint(pdpEndpoint);
        newPdp.setPdpGroupId(pdpGroupId);
        newPdp.setPdpState(pdpState);
        newPdp.setPdpType(pdpType);
        newPdp.setPdpVersion(pdpVersion);
        newPdp.setPolicySetId(policySetId);

        pdpEntityRepository.save(newPdp);
    }

}
