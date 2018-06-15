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
@Table(name = "PdpGroupEntity")
public class PdpGroupEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "groupId")
    private long groupId;

    @Column(name = "groupName")
    private String groupName;

    @Column(name = "groupVersion")
    private String groupVersion;

    @Column(name = "pdpType")
    private String pdpType;

    @Column(name = "pdpServiceEndpoint")
    private String pdpServiceEndpoint;

    @Column(name = "policySetId")
    private long policySetId;

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupVersion() {
        return groupName;
    }

    public void setGroupVersion(String groupVersion) {
        this.groupName = groupVersion;
    }

    public String getPdpType() {
        return pdpType;
    }

    public void setPdpType(String pdpType) {
        this.pdpType = pdpType;
    }

    public String getPdpServiceEndpoint() {
        return pdpServiceEndpoint;
    }

    public void setPdpServiceEndpoint(String pdpServiceEndpoint) {
        this.pdpServiceEndpoint = pdpServiceEndpoint;
    }

    public long getPolicySetId() {
        return policySetId;
    }

    public void setPolicySetId(long policySetId) {
        this.policySetId = policySetId;
    }
}

