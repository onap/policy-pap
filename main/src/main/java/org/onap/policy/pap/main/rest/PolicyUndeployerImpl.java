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

package org.onap.policy.pap.main.rest;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifierOptVersion;
import org.onap.policy.pap.main.comm.PolicyUndeployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of policy undeployer.
 */
public class PolicyUndeployerImpl extends ProviderBase implements PolicyUndeployer {
    private static final Logger logger = LoggerFactory.getLogger(PolicyUndeployerImpl.class);


    /**
     * Constructs the object.
     */
    public PolicyUndeployerImpl() {
        super();
    }

    @Override
    public void undeploy(String group, String subgroup, Collection<ToscaPolicyIdentifier> policies)
                    throws PfModelException {

        process(new Info(group, subgroup, policies), this::undeployPolicies);
    }

    /**
     * Undeploys policies from all PDPs within a subgroup.
     *
     * @param data session data
     * @param policyInfo information about the policies to be undeployed
     * @throws PfModelException if an error occurs
     */
    private void undeployPolicies(SessionData data, Info policyInfo) throws PfModelException {
        // get group and subgroup
        PdpGroup group = data.getGroup(policyInfo.group);
        if (group == null) {
            return;
        }

        Optional<PdpSubGroup> optsub = group.getPdpSubgroups().stream()
                        .filter(subgroup -> subgroup.getPdpType().equals(policyInfo.subgroup)).findAny();
        if (!optsub.isPresent()) {
            logger.warn("subgroup {} {} not found", policyInfo.group, policyInfo.subgroup);
            return;
        }

        PdpSubGroup subgroup = optsub.get();

        // remove the policies
        boolean updated = false;
        Set<String> pdps = subgroup.getPdpInstances().stream().map(Pdp::getInstanceId).collect(Collectors.toSet());

        for (ToscaPolicyIdentifier ident : policyInfo.policies) {
            if (!subgroup.getPolicies().remove(ident)) {
                continue;
            }

            logger.info("remove policy {} {} from subgroup {} {} count={}", ident.getName(), ident.getVersion(),
                            group.getName(), subgroup.getPdpType(), subgroup.getPolicies().size());

            updated = true;
            data.trackUndeploy(ident, pdps);
        }

        // push the updates
        if (updated) {
            makeUpdates(data, group, subgroup);
            data.update(group);
        }
    }

    @Override
    protected Updater makeUpdater(SessionData data, ToscaPolicy policy, ToscaPolicyIdentifierOptVersion desiredPolicy) {
        throw new UnsupportedOperationException("makeUpdater should not be invoked");
    }

    private static class Info {
        private String group;
        private String subgroup;
        private Collection<ToscaPolicyIdentifier> policies;

        public Info(String group, String subgroup, Collection<ToscaPolicyIdentifier> policies) {
            this.group = group;
            this.subgroup = subgroup;
            this.policies = policies;
        }
    }
}
