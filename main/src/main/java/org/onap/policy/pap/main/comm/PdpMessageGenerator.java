/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Nordix Foundation.
 * Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.comm;

import java.util.LinkedList;
import java.util.List;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.notification.DeploymentStatus;
import org.onap.policy.pap.main.notification.PolicyNotifier;
import org.onap.policy.pap.main.parameters.PapParameterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * PDP message generator used to generate PDP-UPDATE and PDP-STATE-CHANGE messages.
 */
public class PdpMessageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdpMessageGenerator.class);

    private static final String PAP_GROUP_PARAMS_NAME = "PapGroup";

    /**
     * Lock used when updating PDPs.
     */
    protected final Object updateLock;

    /**
     * Used to send UPDATE and STATE-CHANGE requests to the PDPs.
     */
    protected final PdpModifyRequestMap requestMap;

    /**
     * Factory for PAP DAO.
     */
    protected final PolicyModelsProviderFactoryWrapper modelProviderWrapper;

    /**
     * Heart beat interval, in milliseconds, to pass to PDPs, or {@code null}.
     */
    private final Long heartBeatMs;

    /**
     * Constructs the object.
     *
     * @param includeHeartBeat if the heart beat interval is to be included in any
     *        PDP-UPDATE messages
     */
    public PdpMessageGenerator(boolean includeHeartBeat) {
        modelProviderWrapper = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        updateLock = Registry.get(PapConstants.REG_PDP_MODIFY_LOCK, Object.class);
        requestMap = Registry.get(PapConstants.REG_PDP_MODIFY_MAP, PdpModifyRequestMap.class);

        if (includeHeartBeat) {
            PapParameterGroup params = ParameterService.get(PAP_GROUP_PARAMS_NAME);
            heartBeatMs = params.getPdpParameters().getHeartBeatMs();

        } else {
            heartBeatMs = null;
        }
    }

    protected PdpUpdate createPdpUpdateMessage(final String pdpGroupName, final PdpSubGroup subGroup,
                    final String pdpInstanceId, final PolicyModelsProvider databaseProvider,
                    final List<ToscaPolicy> policies, final List<ToscaPolicy> policiesToBeDeployed,
                    final List<ToscaConceptIdentifier> policiesToBeUndeployed) throws PfModelException {

        final PdpUpdate update = new PdpUpdate();

        update.setName(pdpInstanceId);
        update.setPdpGroup(pdpGroupName);
        update.setPdpSubgroup(subGroup.getPdpType());
        update.setPolicies(policies);
        update.setPoliciesToBeDeployed(policiesToBeDeployed);
        update.setPoliciesToBeUndeployed(policiesToBeUndeployed);
        update.setPdpHeartbeatIntervalMs(heartBeatMs);

        LOGGER.debug("Created PdpUpdate message - {}", update);
        return update;
    }

    /**
     * Method to return a list of policies.
     *
     * @param subGroup PdpSubGroup to retrieve policies from
     * @param databaseProvider PolicyModelsProvider used to retrieve list of policies
     * @returns a list of ToscaPolicy
     **/
    public List<ToscaPolicy> getToscaPolicies(final PdpSubGroup subGroup,
            final PolicyModelsProvider databaseProvider)
                    throws PfModelException {

        final List<ToscaPolicy> policies = new LinkedList<>();
        for (final ToscaConceptIdentifier policyIdentifier : subGroup.getPolicies()) {
            policies.addAll(databaseProvider.getPolicyList(policyIdentifier.getName(), policyIdentifier.getVersion()));
        }

        LOGGER.debug("Created ToscaPolicy list - {}", policies);
        return policies;
    }

    protected PdpStateChange createPdpStateChangeMessage(final String pdpGroupName, final PdpSubGroup subGroup,
                    final String pdpInstanceId, final PdpState pdpState) {

        final PdpStateChange stateChange = new PdpStateChange();
        stateChange.setName(pdpInstanceId);
        stateChange.setPdpGroup(pdpGroupName);
        stateChange.setPdpSubgroup(subGroup.getPdpType());
        stateChange.setState(pdpState == null ? PdpState.ACTIVE : pdpState);

        LOGGER.debug("Created PdpStateChange message - {}", stateChange);
        return stateChange;
    }

    /**
     * If group state=ACTIVE AND updateMsg has policiesToDeploy, then make sure deployment status is updated
     * If group state=PASSIVE, then delete any deployment information for a PDP.
     *
     * @param pdpGroupName the group name
     * @param pdpType the pdp type
     * @param pdpInstanceId the pdp instance
     * @param pdpState the new state as per the PDP_STATE_CHANGE change message
     * @param databaseProvider the database provider
     * @param policies list of policies as per the PDP_UPDATE message
     * @throws PfModelException the exception
     */
    protected void updateDeploymentStatus(final String pdpGroupName, final String pdpType, final String pdpInstanceId,
        PdpState pdpState, final PolicyModelsProvider databaseProvider, List<ToscaPolicy> policies)
        throws PfModelException {
        DeploymentStatus deploymentStatus = new DeploymentStatus(databaseProvider);
        deploymentStatus.loadByGroup(pdpGroupName);
        if (pdpState.equals(PdpState.PASSIVE)) {
            deploymentStatus.deleteDeployment(pdpInstanceId);
        } else if (pdpState.equals(PdpState.ACTIVE)) {
            for (ToscaPolicy toscaPolicy : policies) {
                deploymentStatus.deploy(pdpInstanceId, toscaPolicy.getIdentifier(), toscaPolicy.getTypeIdentifier(),
                    pdpGroupName, pdpType, true);
            }
        }
        PolicyNotification notification = new PolicyNotification();
        deploymentStatus.flush(notification);
        PolicyNotifier notifier = Registry.get(PapConstants.REG_POLICY_NOTIFIER);
        notifier.publish(notification);
    }
}
