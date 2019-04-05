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

package org.onap.policy.pap.main.rest.depundep;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpUpdate;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;

/**
 * Super class for TestPdpGroupDeployProviderXxx classes.
 */
public class ProviderSuper {
    private static final Coder coder = new StandardCoder();

    @Mock
    protected PolicyModelsProvider dao;

    /**
     * Used to capture input to dao.createPdpGroups().
     */
    @Captor
    private ArgumentCaptor<List<PdpGroup>> createCaptor;


    /**
     * Used to capture input to dao.updatePdpGroups().
     */
    @Captor
    private ArgumentCaptor<List<PdpGroup>> updateCaptor;

    protected Object lockit;
    protected PdpModifyRequestMap reqmap;
    protected PolicyModelsProviderFactoryWrapper daofact;
    protected ToscaPolicy policy1;


    /**
     * Configures DAO, captors, and various mocks.
     */
    @Before
    public void setUp() throws Exception {

        Registry.newRegistry();

        MockitoAnnotations.initMocks(this);

        reqmap = mock(PdpModifyRequestMap.class);

        lockit = new Object();
        daofact = mock(PolicyModelsProviderFactoryWrapper.class);
        policy1 = loadFile("policy.json", ToscaPolicy.class);

        when(daofact.create()).thenReturn(dao);

        List<PdpGroup> groups = loadGroups("groups.json");

        when(dao.getFilteredPdpGroups(any())).thenReturn(groups);

        when(dao.createPdpGroups(any())).thenAnswer(answer -> answer.getArgumentAt(0, List.class));
        when(dao.updatePdpGroups(any())).thenAnswer(answer -> answer.getArgumentAt(0, List.class));

        Registry.register(PapConstants.REG_PDP_MODIFY_LOCK, lockit);
        Registry.register(PapConstants.REG_PDP_MODIFY_MAP, reqmap);
        Registry.register(PapConstants.REG_PAP_DAO_FACTORY, daofact);
    }

    protected void assertGroup(List<PdpGroup> groups, String name, String version) {
        PdpGroup group = groups.remove(0);

        assertEquals(name, group.getName());
        assertEquals(version, group.getVersion());
    }

    protected void assertUpdateIgnorePolicy(List<PdpUpdate> updates, String groupName, String pdpType, String pdpName) {

        PdpUpdate update = updates.remove(0);

        assertEquals(groupName, update.getPdpGroup());
        assertEquals(pdpType, update.getPdpSubgroup());
        assertEquals(pdpName, update.getName());
    }

    /**
     * Gets the input to the method.
     *
     * @param count the number of times the method is expected to have been called.
     * @return the input that was passed to the dao.createPdpGroups() method
     * @throws Exception if an error occurred
     */
    protected List<List<PdpGroup>> getGroupCreates(int count) throws Exception {
        verify(dao, times(count)).createPdpGroups(createCaptor.capture());

        return copyLists(createCaptor.getAllValues());
    }

    /**
     * Gets the input to the method.
     *
     * @param count the number of times the method is expected to have been called
     * @return the input that was passed to the dao.updatePdpGroups() method
     * @throws Exception if an error occurred
     */
    protected List<List<PdpGroup>> getGroupUpdates(int count) throws Exception {
        verify(dao, times(count)).updatePdpGroups(updateCaptor.capture());

        return copyLists(updateCaptor.getAllValues());
    }

    /**
     * Gets the updates that were added to the request map.
     *
     * @param count the number of times the method is expected to have been called
     * @return the updates that were added to the request map
     */
    protected List<PdpUpdate> getUpdateRequests(int count) {
        ArgumentCaptor<PdpUpdate> captor = ArgumentCaptor.forClass(PdpUpdate.class);

        verify(reqmap, times(count)).addRequest(captor.capture());

        return new ArrayList<>(captor.getAllValues());
    }

    /**
     * Makes a partly deep copy of the list. Also sorts each list in order by group name.
     *
     * @param source source list to copy
     * @return a copy of the source list
     */
    private List<List<PdpGroup>> copyLists(List<List<PdpGroup>> source) {
        List<List<PdpGroup>> target = new ArrayList<>(source.size());

        for (List<PdpGroup> lst : source) {
            List<PdpGroup> newlst = new ArrayList<>(lst);
            Collections.sort(newlst, (left, right) -> left.getName().compareTo(right.getName()));
            target.add(newlst);
        }

        return target;
    }

    /**
     * Loads a list of groups.
     *
     * @param fileName name of the file from which to load
     * @return a list of groups
     */
    protected List<PdpGroup> loadGroups(String fileName) {
        return loadFile(fileName, org.onap.policy.models.pdp.concepts.PdpGroups.class).getGroups();
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
     * Wraps a list of policies. The decoder doesn't work with generic lists, so we wrap
     * the list and decode it into the wrapper before extracting the list contents.
     */
    private static class PolicyList {
        private List<ToscaPolicy> policies;
    }
}
