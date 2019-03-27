/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.main.concepts.internal;

import lombok.NoArgsConstructor;

/**
 * Internal class providing conversion to/from the corresponding class in models-pap.
 */
@NoArgsConstructor
public class PolicyIdent extends org.onap.policy.models.pdp.concepts.PolicyIdent {
    private static final long serialVersionUID = 1L;

    public PolicyIdent(PolicyIdent source) {
        super(source);
    }

    /**
     * Constructs the object, populating fields from the external source.
     * @param source source from which to copy the data
     */
    public PolicyIdent(org.onap.policy.models.pap.concepts.PolicyIdent source) {
        IdentUtil.toInternal(source.getName(), source.getVersion(), this);
    }

    /**
     * Converts this to the corresponding class in models-pap.
     *
     * @return an external object, populated with data from this object
     */
    public org.onap.policy.models.pap.concepts.PolicyIdent toExternal() {

        org.onap.policy.models.pap.concepts.PolicyIdent target =
                        new org.onap.policy.models.pap.concepts.PolicyIdent();
        copyTo(target);
        return target;
    }

    public void copyTo(org.onap.policy.models.pap.concepts.PolicyIdent target) {
        target.setName(IdentUtil.nameToExternal(this.getName()));
        target.setVersion(IdentUtil.versionToExternal(this.getVersion()));
    }
}
