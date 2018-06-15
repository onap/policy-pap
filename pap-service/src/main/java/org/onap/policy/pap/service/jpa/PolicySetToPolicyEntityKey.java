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
import java.util.Objects;

public class PolicySetToPolicyEntityKey implements Serializable {

    private static final long serialVersionUID = -5755578105045734880L;
    private long policySetId;
    private long policyId;

    public PolicySetToPolicyEntityKey() {}

    public PolicySetToPolicyEntityKey(long policySetId, long policyId) {
        this.policySetId = policySetId;
        this.policyId = policyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PolicySetToPolicyEntityKey key1 = (PolicySetToPolicyEntityKey) o;
        if (policySetId != key1.policySetId) {
            return false;
        }
        return policyId == key1.policyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(policySetId, policyId);
    }

}
