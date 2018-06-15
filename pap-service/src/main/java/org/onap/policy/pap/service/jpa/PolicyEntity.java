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

package org.onap.policy.pap.service.jpa;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "PolicyEntity")
public class PolicyEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policyId")
    private long policyId;

    @Column(name = "policyName")
    private String policyName;

    @Column(name = "policyVersion")
    private String policyVersion;

    @Column(name = "pdpType")
    private String pdpType;

    @Column(name = "policyMavenArtifact")
    private String policyMavenArtifact;

    public long getPolicyId() {
        return policyId;
    }

    public void setPolicySetId(long policyId) {
        this.policyId = policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicySetName(String policyName) {
        this.policyName = policyName;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getPdpType() {
        return pdpType;
    }

    public void setPdpType(String pdpType) {
        this.pdpType = pdpType;
    }

    public String getPolicyMavenArtifact() {
        return policyMavenArtifact;
    }

    public void setPolicyMavenArtifact(String policyMavenArtifact) {
        this.policyMavenArtifact = policyMavenArtifact;
    }

}

