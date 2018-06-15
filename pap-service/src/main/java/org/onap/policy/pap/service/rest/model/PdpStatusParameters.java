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

package org.onap.policy.pap.service.rest.model;

import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;

/**
 * PdpStatusParameters
 */
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-06-13T13:56:19.747Z")

public class PdpStatusParameters {
    private String pdpName = null;

    private String pdpVersion = null;

    private PdpStateEnum pdpState = null;

    private String pdpType = null;

    private String pdpGroupName = null;

    private String pdpEndpoint = null;

    public PdpStatusParameters pdpName(String pdpName) {
        this.pdpName = pdpName;
        return this;
    }

    /**
     * Get pdpName
     * 
     * @return pdpName
     **/
    @ApiModelProperty(value = "")


    public String getPdpName() {
        return pdpName;
    }

    public void setPdpName(String pdpName) {
        this.pdpName = pdpName;
    }

    public PdpStatusParameters pdpVersion(String pdpVersion) {
        this.pdpVersion = pdpVersion;
        return this;
    }

    /**
     * Get pdpVersion
     * 
     * @return pdpVersion
     **/
    @ApiModelProperty(value = "")


    public String getPdpVersion() {
        return pdpVersion;
    }

    public void setPdpVersion(String pdpVersion) {
        this.pdpVersion = pdpVersion;
    }

    public PdpStatusParameters pdpState(PdpStateEnum pdpState) {
        this.pdpState = pdpState;
        return this;
    }

    /**
     * Get pdpState
     * 
     * @return pdpState
     **/
    @ApiModelProperty(value = "")


    public PdpStateEnum getPdpState() {
        return pdpState;
    }

    public void setPdpState(PdpStateEnum pdpState) {
        this.pdpState = pdpState;
    }

    public PdpStatusParameters pdpType(String pdpType) {
        this.pdpType = pdpType;
        return this;
    }

    /**
     * Get pdpType
     * 
     * @return pdpType
     **/
    @ApiModelProperty(value = "")


    public String getPdpType() {
        return pdpType;
    }

    public void setPdpType(String pdpType) {
        this.pdpType = pdpType;
    }

    public PdpStatusParameters pdpGroupName(String pdpGroupName) {
        this.pdpGroupName = pdpGroupName;
        return this;
    }

    /**
     * Get pdpGroupName
     * 
     * @return pdpGroupName
     **/
    @ApiModelProperty(value = "")


    public String getPdpGroupName() {
        return pdpGroupName;
    }

    public void setPdpGroupName(String pdpGroupName) {
        this.pdpGroupName = pdpGroupName;
    }

    public PdpStatusParameters pdpEndpoint(String pdpEndpoint) {
        this.pdpEndpoint = pdpEndpoint;
        return this;
    }

    /**
     * Get pdpEndpoint
     * 
     * @return pdpEndpoint
     **/
    @ApiModelProperty(value = "")


    public String getPdpEndpoint() {
        return pdpEndpoint;
    }

    public void setPdpEndpoint(String pdpEndpoint) {
        this.pdpEndpoint = pdpEndpoint;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PdpStatusParameters pdpStatusParameters = (PdpStatusParameters) o;
        return Objects.equals(this.pdpName, pdpStatusParameters.pdpName)
                && Objects.equals(this.pdpVersion, pdpStatusParameters.pdpVersion)
                && Objects.equals(this.pdpState, pdpStatusParameters.pdpState)
                && Objects.equals(this.pdpType, pdpStatusParameters.pdpType)
                && Objects.equals(this.pdpGroupName, pdpStatusParameters.pdpGroupName)
                && Objects.equals(this.pdpEndpoint, pdpStatusParameters.pdpEndpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pdpName, pdpVersion, pdpState, pdpType, pdpGroupName, pdpEndpoint);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PdpStatusParameters {\n");

        sb.append("    pdpName: ").append(toIndentedString(pdpName)).append("\n");
        sb.append("    pdpVersion: ").append(toIndentedString(pdpVersion)).append("\n");
        sb.append("    pdpState: ").append(toIndentedString(pdpState)).append("\n");
        sb.append("    pdpType: ").append(toIndentedString(pdpType)).append("\n");
        sb.append("    pdpGroupName: ").append(toIndentedString(pdpGroupName)).append("\n");
        sb.append("    pdpEndpoint: ").append(toIndentedString(pdpEndpoint)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first
     * line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

