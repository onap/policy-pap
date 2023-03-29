/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2022 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021-2022 Nordix Foundation.
 * Modifications Copyright (C) 2022-2023 Bell Canada. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.pap.concepts.PolicyNotification;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpStateChange;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;
import org.onap.policy.pap.main.notification.PolicyNotifier;
import org.onap.policy.pap.main.service.PdpGroupService;
import org.onap.policy.pap.main.service.PolicyAuditService;
import org.onap.policy.pap.main.service.PolicyStatusService;
import org.onap.policy.pap.main.service.ToscaServiceTemplateService;

/**
 * Super class for TestPdpGroupDeployProviderXxx classes.
 */
public class ProviderSuper {
    private static final Coder coder = new StandardCoder();
    public static final String DEFAULT_USER = "PAP_TEST";

    @Mock
    protected PdpGroupService pdpGroupService;

    @Mock
    protected PolicyStatusService policyStatusService;

    @Mock
    protected PolicyAuditService policyAuditService;

    @Mock
    protected ToscaServiceTemplateService toscaService;

    @Mock
    protected PolicyNotifier notifier;

    /**
     * Used to capture input to dao.updatePdpGroups() and dao.createPdpGroups().
     */
    @Captor
    private ArgumentCaptor<List<PdpGroup>> updateCaptor;

    protected Object lockit;
    protected PdpModifyRequestMap reqmap;
    protected ToscaPolicy policy1;
    protected MeterRegistry meterRegistry;

    /**
     * Configures DAO, captors, and various mocks.
     */
    @Before
    public void setUp() throws Exception {

        Registry.newRegistry();

        MockitoAnnotations.openMocks(this);

        reqmap = mock(PdpModifyRequestMap.class);

        lockit = new Object();
        policy1 = loadPolicy("policy.json");

        meterRegistry = mock(MeterRegistry.class);

        List<PdpGroup> groups = loadGroups("groups.json");

        when(pdpGroupService.getFilteredPdpGroups(any())).thenReturn(groups);

        when(pdpGroupService.createPdpGroups(any())).thenAnswer(answer -> answer.getArgument(0, List.class));
        when(pdpGroupService.updatePdpGroups(any())).thenAnswer(answer -> answer.getArgument(0, List.class));

        Registry.register(PapConstants.REG_PDP_MODIFY_LOCK, lockit);
        Registry.register(PapConstants.REG_PDP_MODIFY_MAP, reqmap);
        Registry.register(PapConstants.REG_METER_REGISTRY, meterRegistry);

    }

    /**
     * Initialize services to the provider for tests.
     *
     * @param prov the provider
     */
    public void initialize(ProviderBase prov) {
        prov.setPdpGroupService(pdpGroupService);
        prov.setPolicyAuditService(policyAuditService);
        prov.setPolicyStatusService(policyStatusService);
        prov.setToscaService(toscaService);
        prov.setPolicyNotifier(notifier);
        prov.initialize();
    }

    protected void assertGroup(List<PdpGroup> groups, String name) {
        PdpGroup group = groups.remove(0);

        assertEquals(name, group.getName());
    }

    protected void assertUpdateIgnorePolicy(List<PdpUpdate> updates, String groupName, String pdpType, String pdpName) {

        PdpUpdate update = updates.remove(0);

        assertEquals(groupName, update.getPdpGroup());
        assertEquals(pdpType, update.getPdpSubgroup());
        assertEquals(pdpName, update.getName());
    }

    /**
     * Gets the input to the create() method.
     *
     * @return the input that was passed to the dao.updatePdpGroups() method
     */
    protected List<PdpGroup> getGroupCreates() {
        verify(pdpGroupService).createPdpGroups(updateCaptor.capture());

        return copyList(updateCaptor.getValue());
    }

    /**
     * Gets the input to the update() method.
     *
     * @return the input that was passed to the dao.updatePdpGroups() method
     */
    protected List<PdpGroup> getGroupUpdates() {
        verify(pdpGroupService).updatePdpGroups(updateCaptor.capture());

        return copyList(updateCaptor.getValue());
    }

    /**
     * Gets the state-changes that were added to the request map.
     *
     * @param count the number of times the method is expected to have been called
     * @return the state-changes that were added to the request map
     */
    protected List<PdpStateChange> getStateChangeRequests(int count) {
        ArgumentCaptor<PdpStateChange> captor = ArgumentCaptor.forClass(PdpStateChange.class);

        verify(reqmap, times(count)).addRequest(any(), captor.capture());

        return captor.getAllValues().stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Gets the updates that were added to the request map.
     *
     * @param count the number of times the method is expected to have been called
     * @return the updates that were added to the request map
     */
    protected List<PdpUpdate> getUpdateRequests(int count) {
        ArgumentCaptor<PdpUpdate> captor = ArgumentCaptor.forClass(PdpUpdate.class);

        verify(reqmap, times(count)).addRequest(captor.capture(), any());

        return captor.getAllValues().stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Copies a list and sorts it by group name.
     *
     * @param source source list to copy
     * @return a copy of the source list
     */
    private List<PdpGroup> copyList(List<PdpGroup> source) {
        List<PdpGroup> newlst = new ArrayList<>(source);
        newlst.sort(Comparator.comparing(PdpGroup::getName));
        return newlst;
    }

    /**
     * Loads a list of groups.
     *
     * @param fileName name of the file from which to load
     * @return a list of groups
     */
    protected List<PdpGroup> loadGroups(String fileName) {
        return loadPdpGroups(fileName).getGroups();
    }

    /**
     * Loads a PdpGroups.
     *
     * @param fileName name of the file from which to load
     * @return a PdpGroups
     */
    protected PdpGroups loadPdpGroups(String fileName) {
        return loadFile(fileName, PdpGroups.class);
    }

    /**
     * Loads a group.
     *
     * @param fileName name of the file from which to load
     * @return a group
     */
    protected PdpGroup loadGroup(String fileName) {
        return loadFile(fileName, PdpGroup.class);
    }

    /**
     * Loads a list of policies.
     *
     * @param fileName name of the file from which to load
     * @return a list of policies
     */
    protected List<ToscaPolicy> loadPolicies(String fileName) {
        return loadFile(fileName, PolicyList.class).policies;
    }

    /**
     * Loads a policy.
     *
     * @param fileName name of the file from which to load
     * @return a policy
     */
    protected ToscaPolicy loadPolicy(String fileName) {
        return loadFile(fileName, ToscaPolicy.class);
    }

    /**
     * Loads a policy type.
     *
     * @param fileName name of the file from which to load
     * @return a policy type
     */
    protected ToscaPolicyType loadPolicyType(String fileName) {
        return loadFile(fileName, ToscaPolicyType.class);
    }

    /**
     * Loads an object from a JSON file.
     *
     * @param fileName name of the file from which to load
     * @param clazz the class of the object to be loaded
     * @return the object that was loaded from the file
     */
    protected <T> T loadFile(String fileName, Class<T> clazz) {
        File propFile = new File(ResourceUtils.getFilePath4Resource("simpleDeploy/" + fileName));
        try {
            return coder.decode(propFile, clazz);

        } catch (CoderException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies that an empty notification was published.
     */
    protected void checkEmptyNotification() {
        ArgumentCaptor<PolicyNotification> captor = ArgumentCaptor.forClass(PolicyNotification.class);
        verify(notifier).publish(captor.capture());
        assertThat(captor.getValue().isEmpty()).isTrue();
    }

    /**
     * Wraps a list of policies. The decoder doesn't work with generic lists, so we wrap
     * the list and decode it into the wrapper before extracting the list contents.
     */
    private static class PolicyList {
        private List<ToscaPolicy> policies;
    }
}
