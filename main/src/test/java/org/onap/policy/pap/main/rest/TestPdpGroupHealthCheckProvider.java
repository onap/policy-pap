/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2020-2021 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.main.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.concepts.Pdps;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.springframework.http.HttpStatus;

/**
 * Class to perform unit test of {@link PdpGroupHealthCheckProvider}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@RunWith(MockitoJUnitRunner.class)
public class TestPdpGroupHealthCheckProvider {

    @Mock
    private PolicyModelsProvider dao;
    private PolicyModelsProviderFactoryWrapper daofact;
    private List<PdpGroup> groups;
    private Coder coder = new StandardCoder();

    /**
     * Configures DAO and mocks.
     */
    @Before
    public void setUp() throws Exception {

        Registry.newRegistry();
        daofact = mock(PolicyModelsProviderFactoryWrapper.class);
        when(daofact.create()).thenReturn(dao);

        groups = loadFile("pdpGroup.json").getGroups();

        when(dao.getPdpGroups(any())).thenReturn(groups);

        Registry.register(PapConstants.REG_PAP_DAO_FACTORY, daofact);
    }

    @Test
    public void testFetchPdpGroupHealthStatus() throws Exception {
        final PdpGroupHealthCheckProvider provider = new PdpGroupHealthCheckProvider();
        final Pair<HttpStatus, Pdps> pair = provider.fetchPdpGroupHealthStatus();
        assertEquals(HttpStatus.OK, pair.getLeft());
        verifyPdps(pair.getRight().getPdpList(), groups);
    }

    private void verifyPdps(final List<Pdp> pdpList, final List<PdpGroup> groups) {
        assertEquals(6, pdpList.size());
        for (final PdpGroup group : groups) {
            for (final PdpSubGroup subGroup : group.getPdpSubgroups()) {
                pdpList.containsAll(subGroup.getPdpInstances());
            }
        }
    }

    private PdpGroups loadFile(final String fileName) {
        final File propFile = new File(ResourceUtils.getFilePath4Resource("rest/" + fileName));
        try {
            return coder.decode(propFile, PdpGroups.class);

        } catch (final CoderException e) {
            throw new RuntimeException(e);
        }
    }
}
