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
@Table(name = "PdpEntity")
public class PdpEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pdpId")
    private long pdpId;

    @Column(name = "pdpName")
    private String pdpName;

    @Column(name = "pdpVersion")
    private String pdpVersion;

    @Column(name = "pdpType")
    private String pdpType;

    @Column(name = "pdpEndpoint")
    private String pdpEndpoint;

    @Column(name = "pdpGroupId")
    private long pdpGroupId;

    @Column(name = "policySetId")
    private long policySetId;

    @Column(name = "pdpState")
    private String pdpState;

    public long getPdpId() {
        return pdpId;
    }

    public void setPdpId(long pdpId) {
        this.pdpId = pdpId;
    }

    public String getPdpName() {
        return pdpName;
    }

    public void setPdpName(String pdpName) {
        this.pdpName = pdpName;
    }

    public String getPdpVersion() {
        return pdpVersion;
    }

    public void setPdpVersion(String pdpVersion) {
        this.pdpVersion = pdpVersion;
    }

    public String getPdpType() {
        return pdpType;
    }

    public void setPdpType(String pdpType) {
        this.pdpType = pdpType;
    }

    public String getPdpEndpoint() {
        return pdpEndpoint;
    }

    public void setPdpEndpoint(String pdpEndpoint) {
        this.pdpEndpoint = pdpEndpoint;
    }

    public long getPdpGroupId() {
        return pdpGroupId;
    }

    public void setPdpGroupId(long pdpGroupId) {
        this.pdpGroupId = pdpGroupId;
    }

    public long getPolicySetId() {
        return policySetId;
    }

    public void setPolicySetId(long policySetId) {
        this.policySetId = policySetId;
    }

    public String getPdpState() {
        return pdpState;
    }

    public void setPdpState(String pdpState) {
        this.pdpState = pdpState;
    }
}

