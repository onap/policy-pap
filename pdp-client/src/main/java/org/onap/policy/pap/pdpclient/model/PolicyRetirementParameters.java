/*-
 * ============LICENSE_START=======================================================
 * pdp-client
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

package org.onap.policy.pap.pdpclient.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;

/**
 * PolicyRetirementParameters
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-05T10:23:28.596Z")
public class PolicyRetirementParameters {
    @SerializedName("policyIdentity")
    private PolicyIdentity policyIdentity = null;

    public PolicyRetirementParameters policyIdentity(PolicyIdentity policyIdentity) {
        this.policyIdentity = policyIdentity;
        return this;
    }

    /**
     * Get policyIdentity
     * 
     * @return policyIdentity
     **/
    @ApiModelProperty(required = true, value = "")
    public PolicyIdentity getPolicyIdentity() {
        return policyIdentity;
    }

    public void setPolicyIdentity(PolicyIdentity policyIdentity) {
        this.policyIdentity = policyIdentity;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PolicyRetirementParameters policyRetirementParameters = (PolicyRetirementParameters) o;
        return Objects.equals(this.policyIdentity, policyRetirementParameters.policyIdentity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyIdentity);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PolicyRetirementParameters {\n");

        sb.append("    policyIdentity: ").append(toIndentedString(policyIdentity)).append("\n");
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

