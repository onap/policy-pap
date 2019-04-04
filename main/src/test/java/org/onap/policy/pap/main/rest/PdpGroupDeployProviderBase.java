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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.ArrayList;
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
import org.onap.policy.models.pap.concepts.PdpDeployPolicies;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

/**
 * Based class for TestPdpGroupDeployProviderXxx classes.
 */
public class PdpGroupDeployProviderBase {
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


    /**
     * Configures DAO and captors.
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
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
     * @param count the number of times the method is expected to have been called.
     * @return the input that was passed to the dao.updatePdpGroups() method
     * @throws Exception if an error occurred
     */
    protected List<List<PdpGroup>> getGroupUpdates(int count) throws Exception {
        verify(dao, times(count)).updatePdpGroups(updateCaptor.capture());

        return copyLists(updateCaptor.getAllValues());
    }

    /**
     * Makes a partly deep copy of the list.
     *
     * @param source source list to copy
     * @return a copy of the source list
     */
    private List<List<PdpGroup>> copyLists(List<List<PdpGroup>> source) {
        List<List<PdpGroup>> target = new ArrayList<>(source.size());

        for (List<PdpGroup> lst : source) {
            target.add(new ArrayList<>(lst));
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
     * Loads a standard request.
     *
     * @return a standard request
     */
    protected PdpDeployPolicies loadRequest() {
        return loadRequest("request.json");
    }

    /**
     * Loads a request from a JSON file.
     *
     * @param fileName name of the file from which to load
     * @return the request that was loaded
     */
    protected PdpDeployPolicies loadRequest(String fileName) {
        return loadFile(fileName, PdpDeployPolicies.class);
    }

    /**
     * Loads an empty request.
     *
     * @return an empty request
     */
    protected PdpDeployPolicies loadEmptyRequest() {
        return loadRequest("emptyRequest.json");
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
