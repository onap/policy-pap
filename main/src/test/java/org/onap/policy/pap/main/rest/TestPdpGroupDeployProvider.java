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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.pap.concepts.PdpGroupDeployResponse;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProvider;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.comm.PdpModifyRequestMap;

public class TestPdpGroupDeployProvider {
    private static final Status ERROR_STATUS = Status.INTERNAL_SERVER_ERROR;

    private PdpGroupDeployProvider prov;
    private PdpModifyRequestMap reqmap;
    private PolicyModelsProviderFactoryWrapper daoFactory;
    private PolicyModelsProvider dao;

    @AfterClass
    public static void tearDownAfterClass() {
        Registry.newRegistry();
    }

    /**
     * Sets up mocks and an initial deploy provider.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        Registry.newRegistry();

        reqmap = mock(PdpModifyRequestMap.class);
        daoFactory = mock(PolicyModelsProviderFactoryWrapper.class);
        dao = mock(PolicyModelsProvider.class);

        when(daoFactory.create()).thenReturn(dao);

        Registry.register(PapConstants.REG_PDP_MODIFY_LOCK, new Object());
        Registry.register(PapConstants.REG_PDP_MODIFY_MAP, reqmap);
        Registry.register(PapConstants.REG_PAP_DAO_FACTORY, daoFactory);

        prov = new PdpGroupDeployProvider();
    }

    @Test
    public void testPdpGroupDeployProvider() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeployGroup() {
        org.onap.policy.models.pap.concepts.PdpGroups groups = new org.onap.policy.models.pap.concepts.PdpGroups();

        Pair<Status, PdpGroupDeployResponse> result = prov.deployGroup(groups);
        assertEquals(ERROR_STATUS, result.getLeft());
        assertEquals("not implemented yet", result.getRight().getErrorDetails());
    }

    @Test
    public void testDeployPolicies() {
        org.onap.policy.models.pap.concepts.PdpDeployPolicies policies =
                        new org.onap.policy.models.pap.concepts.PdpDeployPolicies();

        Pair<Status, PdpGroupDeployResponse> result = prov.deployPolicies(policies);
        assertEquals(ERROR_STATUS, result.getLeft());
        assertEquals("request failed", result.getRight().getErrorDetails());
    }

    @Test
    public void testDeploy() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeployGroups() {
        fail("Not yet implemented");
    }

    @Test
    public void testDeploySimplePolicies() {
        fail("Not yet implemented");
    }

    @Test
    public void testProcessPolicy() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetPolicy() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetGroup() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpgradeGroup() {
        fail("Not yet implemented");
    }

    @Test
    public void testMakeUpdate() {
        fail("Not yet implemented");
    }

    @Test
    public void testUpgradeGroupVersion() {
        fail("Not yet implemented");
    }

    @Test
    public void testMakeNewVersion() {
        fail("Not yet implemented");
    }

}
