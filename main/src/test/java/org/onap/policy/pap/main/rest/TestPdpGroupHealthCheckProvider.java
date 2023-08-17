/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2023 Nordix Foundation.
 *  Modifications Copyright (C) 2020-2022 Bell Canada. All rights reserved.
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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import org.onap.policy.pap.main.service.PdpGroupService;
import org.springframework.http.HttpStatus;

/**
 * Class to perform unit test of {@link PdpGroupHealthCheckProvider}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@RunWith(MockitoJUnitRunner.class)
class TestPdpGroupHealthCheckProvider {

    @Mock
    private PdpGroupService pdpGroupService;
    private List<PdpGroup> groups;
    private final Coder coder = new StandardCoder();

    AutoCloseable autoCloseable;

    /**
     * Configures DAO and mocks.
     */
    @BeforeEach
    public void setUp() throws Exception {
        autoCloseable = MockitoAnnotations.openMocks(this);
        Registry.newRegistry();
        groups = loadFile().getGroups();

        when(pdpGroupService.getPdpGroups()).thenReturn(groups);
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void testFetchPdpGroupHealthStatus() throws Exception {
        final PdpGroupHealthCheckProvider provider = new PdpGroupHealthCheckProvider(pdpGroupService);
        final Pair<HttpStatus, Pdps> pair = provider.fetchPdpGroupHealthStatus();
        assertEquals(HttpStatus.OK, pair.getLeft());
        verifyPdps(pair.getRight().getPdpList(), groups);
    }

    private void verifyPdps(final List<Pdp> pdpList, final List<PdpGroup> groups) {
        assertEquals(6, pdpList.size());
        boolean containsAll = false;

        do {
            for (final PdpGroup group : groups) {
                for (final PdpSubGroup subGroup : group.getPdpSubgroups()) {
                    containsAll = pdpList.containsAll(subGroup.getPdpInstances());
                }
            }
        } while (!containsAll);

        assertTrue(containsAll);
    }

    private PdpGroups loadFile() {
        final File propFile = new File(ResourceUtils.getFilePath4Resource("rest/" + "pdpGroup.json"));
        try {
            return coder.decode(propFile, PdpGroups.class);

        } catch (final CoderException e) {
            throw new RuntimeException(e);
        }
    }
}
