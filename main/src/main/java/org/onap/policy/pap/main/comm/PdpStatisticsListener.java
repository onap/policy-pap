/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.TypedMessageListener;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.parameters.PdpParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for PDP statistics, found within {@link PdpStatus} messages.
 */
public class PdpStatisticsListener implements TypedMessageListener<PdpStatus> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdpStatisticsListener.class);

    /**
     * Maximum message age, in milliseconds - anything older than this is discarded.
     */
    private final long maxMessageAgeMs;

    /**
     * Lock used when updating PDPs.
     */
    private final Object updateLock;

    /**
     * Factory for PAP DAO.
     */
    private final PolicyModelsProviderFactoryWrapper modelProviderWrapper;

    /**
     * Constructs the object.
     *
     * @param params PDP parameters
     */
    public PdpStatisticsListener(PdpParameters params) {
        maxMessageAgeMs = params.getMaxMessageAgeMs();
        modelProviderWrapper = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);
        updateLock = Registry.get(PapConstants.REG_PDP_MODIFY_LOCK, Object.class);
    }

    @Override
    public void onTopicEvent(CommInfrastructure infra, String topic, PdpStatus message) {
        long diffms = System.currentTimeMillis() - message.getTimestampMs();
        if (diffms > maxMessageAgeMs) {
            long diffsec = TimeUnit.SECONDS.convert(diffms, TimeUnit.MILLISECONDS);
            LOGGER.info("discarding statistics message from {} age {}s", message.getName(), diffsec);
            return;
        }

        if (!validStatistics(message)) {
            LOGGER.info("discarding invalid/null statistics message from {}", message.getName());
            return;
        }

        synchronized (updateLock) {
            try (PolicyModelsProvider databaseProvider = modelProviderWrapper.create()) {
                handleStatistics(message, databaseProvider);
            } catch (final Exception exp) {
                LOGGER.error("database provider error", exp);
            }
        }
    }

    private void handleStatistics(final PdpStatus message, final PolicyModelsProvider databaseProvider)
                    throws PfModelException {

        final String pdpType = message.getPdpType();
        final String pdpName = message.getName();
        final PdpGroupFilter filter =
                        PdpGroupFilter.builder().name(message.getPdpGroup()).groupState(PdpState.ACTIVE).build();
        boolean pdpFound = databaseProvider.getFilteredPdpGroups(filter).stream()
                        .flatMap(grp -> grp.getPdpSubgroups().stream())
                        .filter(subgrp -> subgrp.getPdpType().equals(pdpType))
                        .flatMap(subgrp -> subgrp.getPdpInstances().stream())
                        .anyMatch(pdp -> pdp.getInstanceId().equals(pdpName));
        if (pdpFound) {
            databaseProvider.createPdpStatistics(List.of(message.getStatistics()));
            LOGGER.debug("Created PdpStatistics in DB for {}", pdpName);
        } else {
            LOGGER.info("discarding statistics message from unknown PDP {}", message.getName());
        }
    }

    private boolean validStatistics(final PdpStatus message) {
        PdpStatistics stats = message.getStatistics();
        if (stats == null) {
            return false;
        }

        // @formatter:off
        return new EqualsBuilder()
            .append(PdpState.TERMINATED.equals(message.getState()), false)
            .append(message.getPdpGroup() != null, true)
            .append(message.getPdpType() != null, true)
            .append(message.getName() != null, true)
            .append(stats.getPdpGroupName(), message.getPdpGroup())
            .append(stats.getPdpSubGroupName(), message.getPdpType())
            .append(stats.getPdpInstanceId(), message.getName())
            .append(stats.getPolicyDeployCount() >= 0, true)
            .append(stats.getPolicyDeployFailCount() >= 0, true)
            .append(stats.getPolicyDeploySuccessCount() >= 0, true)
            .append(stats.getPolicyExecutedCount() >= 0, true)
            .append(stats.getPolicyExecutedFailCount() >= 0, true)
            .append(stats.getPolicyExecutedSuccessCount() >= 0, true)
            .isEquals();
        // @formatter:on
    }
}
