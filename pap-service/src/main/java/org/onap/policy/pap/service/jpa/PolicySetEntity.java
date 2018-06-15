/*-
 * ============LICENSE_START=======================================================
 * pap-service
 * ================================================================================
 * Copyright (C) 2018 Ericsson Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.service.jpa;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "PolicySetEntity")
public class PolicySetEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policySetId")
    private int policySetId;

    @Column(name = "policySetName")
    private String policySetName;

    @Column(name = "policySetVersion")
    private String policySetVersion;

    @Column(name = "pdpType")
    private String pdpType;

    @Column(name = "pdpServiceEndpoint")
    private String pdpServiceEndpoint;


    public int getPolicySetId() {
        return policySetId;
    }

    public void setPolicySetId(int policySetId) {
        this.policySetId = policySetId;
    }

    public String getPolicySetName() {
        return policySetName;
    }

    public void setPolicySetName(String policySetName) {
        this.policySetName = policySetName;
    }

    public String getPolicySetVersion() {
        return policySetVersion;
    }

    public void setPolicySetVersion(String policySetVersion) {
        this.policySetVersion = policySetVersion;
    }

}

