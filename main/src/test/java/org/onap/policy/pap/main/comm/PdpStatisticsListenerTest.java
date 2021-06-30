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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.models.pdp.concepts.PdpStatus;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.parameters.PdpParameters;

@RunWith(MockitoJUnitRunner.class)
public class PdpStatisticsListenerTest {
    private static final long MAX_MSG_AGE_MS = 5000;

    private static final String GROUP = "myGroup";
    private static final String PDP_TYPE = "mySubgroup";
    private static final String MY_NAME = "myName";
    private static final CommInfrastructure INFRA = CommInfrastructure.DMAAP;
    private static final String TOPIC = "MyTopic";

    private static final long COUNT = 10;

    @Mock
    private PdpParameters params;

    @Mock
    private PolicyModelsProviderFactoryWrapper provFactory;

    @Mock
    private PolicyModelsProvider provider;

    private PdpStatus msg;
    private PdpStatistics stats;

    private PdpStatisticsListener listener;


    @AfterClass
    public static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    /**
     * Sets up.
     */
    @Before
    public void setUp() throws PfModelException {
        when(params.getMaxMessageAgeMs()).thenReturn(MAX_MSG_AGE_MS);
        when(provFactory.create()).thenReturn(provider);
        when(provider.getFilteredPdpGroups(any())).thenReturn(List.of());

        Registry.newRegistry();

        Registry.register(PapConstants.REG_PAP_DAO_FACTORY, provFactory);
        Registry.register(PapConstants.REG_PDP_MODIFY_LOCK, new Object());

        stats = new PdpStatistics();
        stats.setPolicyDeployCount(COUNT);
        stats.setPolicyDeployFailCount(COUNT);
        stats.setPolicyDeploySuccessCount(COUNT);
        stats.setPolicyExecutedCount(COUNT);
        stats.setPolicyExecutedFailCount(COUNT);
        stats.setPolicyExecutedSuccessCount(COUNT);

        msg = new PdpStatus();
        msg.setPdpGroup(GROUP);
        msg.setPdpType(PDP_TYPE);
        msg.setName(MY_NAME);
        msg.setStatistics(stats);
        msg.setTimestampMs(System.currentTimeMillis());

        listener = new PdpStatisticsListener(params);
    }

    @Test
    public void testOnTopicEvent() throws PfModelException {
        listener.onTopicEvent(INFRA, TOPIC, msg);
        verify(provFactory).create();
    }

    @Test
    public void testOnTopicEventOutOfDate() throws PfModelException {
        msg.setTimestampMs(System.currentTimeMillis() - MAX_MSG_AGE_MS - 1);
        listener.onTopicEvent(INFRA, TOPIC, msg);

        verify(provFactory, never()).create();
    }

    @Test
    public void testOnTopicEventDbException() throws PfModelException {
        when(provFactory.create()).thenThrow(new RuntimeException("expected exception"));
        assertThatCode(() -> listener.onTopicEvent(INFRA, TOPIC, msg)).doesNotThrowAnyException();
    }

    @Test
    public void testHandleStatistics() throws PfModelException {
        // no matching group
        listener.onTopicEvent(INFRA, TOPIC, msg);
        verify(provFactory).create();
        verify(provider, never()).createPdpStatistics(any());

        // matches
        final Pdp pdp = new Pdp();
        pdp.setInstanceId(MY_NAME);

        // doesn't match
        final Pdp pdp2 = new Pdp();
        pdp2.setInstanceId(MY_NAME + "aaa");

        // matches, but PDP doesn't match
        final PdpSubGroup subgrp = new PdpSubGroup();
        subgrp.setPdpType(PDP_TYPE);
        subgrp.setPdpInstances(List.of(pdp2));

        // doesn't match, but has matching PDP
        final PdpSubGroup subgrp2 = new PdpSubGroup();
        subgrp2.setPdpType(PDP_TYPE + "bbb");
        subgrp2.setPdpInstances(List.of(pdp));

        // has matching subgroup
        final PdpGroup group = new PdpGroup();
        group.setPdpSubgroups(List.of(subgrp2, subgrp));

        // no matching subgroup
        final PdpGroup group2 = new PdpGroup();
        group2.setPdpSubgroups(List.of(subgrp2));

        when(provider.getFilteredPdpGroups(any())).thenReturn(List.of(group2, group));

        // nothing matches, so nothing should be inserted
        listener.onTopicEvent(INFRA, TOPIC, msg);
        verify(provider, never()).createPdpStatistics(any());

        // add a matching pdp to the matching subgroup
        subgrp.setPdpInstances(List.of(pdp, pdp));

        // now it should update the statistics
        listener.onTopicEvent(INFRA, TOPIC, msg);
        verify(provider).createPdpStatistics(eq(List.of(stats)));
    }

    @Test
    public void testValidStatistics() throws PfModelException {
        validateStats(msg::getState, msg::setState, PdpState.TERMINATED);
        validateStats(msg::getStatistics, msg::setStatistics, null);
        validateStats(msg::getPdpGroup, msg::setPdpGroup, null);
        validateStats(msg::getPdpType, msg::setPdpType, null);
        validateStats(msg::getName, msg::setName, null);
        validateStats(stats::getPolicyDeployCount, stats::setPolicyDeployCount, -1L);
        validateStats(stats::getPolicyDeployFailCount, stats::setPolicyDeployFailCount, -1L);
        validateStats(stats::getPolicyDeploySuccessCount, stats::setPolicyDeploySuccessCount, -1L);
        validateStats(stats::getPolicyExecutedCount, stats::setPolicyExecutedCount, -1L);
        validateStats(stats::getPolicyExecutedFailCount, stats::setPolicyExecutedFailCount, -1L);
        validateStats(stats::getPolicyExecutedSuccessCount, stats::setPolicyExecutedSuccessCount, -1L);

        // verify that all zeroes are OK
        msg.setStatistics(new PdpStatistics());
        listener.onTopicEvent(INFRA, TOPIC, msg);
        verify(provFactory).create();
    }

    /**
     * Verifies that the provider is never created when one of the message's fields is set
     * to an invalid value.
     *
     * @param <T> value type
     * @param getter method to get the current value of the field
     * @param setter method to change the field
     * @param invalidValue invalid value for the field
     * @throws PfModelException if an error occurs
     */
    private <T> void validateStats(Supplier<T> getter, Consumer<T> setter, T invalidValue) throws PfModelException {
        final T saved = getter.get();
        setter.accept(invalidValue);

        listener.onTopicEvent(INFRA, TOPIC, msg);
        verify(provFactory, never()).create();

        setter.accept(saved);
    }
}
