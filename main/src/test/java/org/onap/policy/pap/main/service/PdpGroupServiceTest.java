/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2022-2023 Nordix Foundation.
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

package org.onap.policy.pap.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.pdp.concepts.Pdp;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpSubGroup;
import org.onap.policy.models.pdp.enums.PdpHealthStatus;
import org.onap.policy.models.pdp.enums.PdpState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.pap.main.rest.CommonPapRestServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PdpGroupServiceTest extends CommonPapRestServer {

    private static final String FIELD_IS_NULL = "%s is marked non-null but is null";

    private static final String DEFAULT_GROUP = "defaultGroup";

    private static final String CREATE_GROUPS = "createGroups";

    private static final String TYPE = "type";

    private static final String NAME = "name";

    private static final String LOCALNAME_IS_NULL = "parameter \"localName\" is null";

    @Autowired
    private PdpGroupService pdpGroupService;

    private PdpGroups groupsToCreate;

    private final StandardCoder coder = new StandardCoder();

    /**
     * Setup before tests.
     *
     * @throws Exception the exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        PdpGroups defaultGroup = coder.decode(ResourceUtils.getResourceAsString("e2e/PdpGroups.json"), PdpGroups.class);
        pdpGroupService.createPdpGroups(defaultGroup.getGroups());
        groupsToCreate = coder.decode(ResourceUtils.getResourceAsString("e2e/createGroups.json"), PdpGroups.class);
    }

    @Test
    void testPdpGroupsCrudSuccess() {

        List<PdpGroup> pdpGroups = pdpGroupService.getPdpGroups();
        assertThat(pdpGroups).hasSize(1);
        assertThat(pdpGroups.get(0).getName()).isEqualTo(DEFAULT_GROUP);

        pdpGroupService.createPdpGroups(groupsToCreate.getGroups());

        assertThat(pdpGroupService.getPdpGroups()).hasSize(2);

        pdpGroups = pdpGroupService.getPdpGroups(CREATE_GROUPS);
        assertThat(pdpGroups).hasSize(1);
        assertThat(pdpGroups.get(0).getName()).isEqualTo(CREATE_GROUPS);

        assertThat(pdpGroupService.getPdpGroups(PdpState.PASSIVE)).isEqualTo(pdpGroups);

        List<PdpGroup> activePdpGroups = pdpGroupService.getPdpGroups(PdpState.ACTIVE);
        assertThat(activePdpGroups).hasSize(1);
        assertThat(activePdpGroups.get(0).getPdpSubgroups()).hasSize(3);

        assertThat(pdpGroupService.getPdpGroups(CREATE_GROUPS, PdpState.PASSIVE)).hasSize(1);
        assertThat(pdpGroupService.getPdpGroups("invalid-group", PdpState.PASSIVE)).isEmpty();
        assertThat(pdpGroupService.getPdpGroups(DEFAULT_GROUP, PdpState.ACTIVE)).hasSize(1);

        PdpGroupFilter filter = PdpGroupFilter.builder()
            .policyTypeList(
                Collections.singletonList(new ToscaConceptIdentifier("onap.policies.native.Xacml", "1.0.0")))
            .groupState(PdpState.ACTIVE).build();
        List<PdpGroup> filteredGroups = pdpGroupService.getFilteredPdpGroups(filter);
        assertThat(filteredGroups).hasSize(1);
        assertThat(filteredGroups.get(0).getName()).isEqualTo(DEFAULT_GROUP);

        pdpGroupService.deletePdpGroup(CREATE_GROUPS);
        pdpGroups = pdpGroupService.getPdpGroups();
        assertThat(pdpGroups).hasSize(1);
        assertThat(pdpGroups.get(0).getName()).isEqualTo(DEFAULT_GROUP);
    }

    @Test
    void testPdpGroupsCrudFailure() {
        PdpState pdpState = null;
        assertThatThrownBy(() -> pdpGroupService.getPdpGroups(pdpState))
            .hasMessage(String.format(FIELD_IS_NULL, "pdpState"));
        pdpGroupService.createPdpGroups(groupsToCreate.getGroups());
        assertThatThrownBy(() -> pdpGroupService.deletePdpGroup("invalid-group"))
            .hasMessage("delete of PDP group \"invalid-group\" failed, PDP group does not exist");
        assertThat(pdpGroupService.getPdpGroups()).hasSize(2);

        assertThatThrownBy(() -> pdpGroupService.createPdpGroups(null))
            .hasMessage(String.format(FIELD_IS_NULL, "pdpGroups"));

        PdpGroup invalidPdpGroup = new PdpGroup(groupsToCreate.getGroups().get(0));
        invalidPdpGroup.setName("invalidPdpGroup");
        invalidPdpGroup.setPdpGroupState(null);
        assertThatThrownBy(() -> pdpGroupService.createPdpGroups(List.of(invalidPdpGroup)))
            .hasMessageContaining("Failed saving PdpGroup.")
            .hasMessageContaining("item \"pdpGroupState\" value \"null\" INVALID, is null");
        pdpGroupService.deletePdpGroup(CREATE_GROUPS);
    }

    @Test
    void testUpdatePdp() {
        assertThatThrownBy(() -> pdpGroupService.updatePdp(null, null, new Pdp()))
            .hasMessage(String.format(FIELD_IS_NULL, "pdpGroupName"));

        assertThatThrownBy(() -> pdpGroupService.updatePdp(NAME, null, new Pdp()))
            .hasMessage(String.format(FIELD_IS_NULL, "pdpSubGroup"));

        assertThatThrownBy(() -> pdpGroupService.updatePdp(NAME, TYPE, null))
            .hasMessage(String.format(FIELD_IS_NULL, "pdp"));

        assertThatThrownBy(() -> pdpGroupService.updatePdp(NAME, TYPE, new Pdp())).hasMessage(LOCALNAME_IS_NULL);

        pdpGroupService.createPdpGroups(groupsToCreate.getGroups());
        assertThat(pdpGroupService.getPdpGroups()).hasSize(2);
        PdpGroup pdpGroup = pdpGroupService.getPdpGroups(CREATE_GROUPS).get(0);
        Pdp pdp = pdpGroup.getPdpSubgroups().get(0).getPdpInstances().get(0);
        assertThat(pdp.getHealthy()).isEqualTo(PdpHealthStatus.HEALTHY);

        // now update and test
        pdp.setHealthy(PdpHealthStatus.NOT_HEALTHY);
        pdpGroupService.updatePdp(CREATE_GROUPS, "pdpTypeA", pdp);
        PdpGroup updatGroup = pdpGroupService.getPdpGroups(CREATE_GROUPS).get(0);
        assertThat(updatGroup.getPdpSubgroups().get(0).getPdpInstances().get(0).getHealthy())
            .isEqualTo(PdpHealthStatus.NOT_HEALTHY);
        pdpGroupService.deletePdpGroup(CREATE_GROUPS);
    }

    @Test
    void testUpdateSubGroup() {
        assertThatThrownBy(() -> pdpGroupService.updatePdpSubGroup(null, null))
            .hasMessage(String.format(FIELD_IS_NULL, "pdpGroupName"));

        assertThatThrownBy(() -> pdpGroupService.updatePdpSubGroup(NAME, null))
            .hasMessage(String.format(FIELD_IS_NULL, "pdpSubGroup"));

        assertThatThrownBy(() -> pdpGroupService.updatePdpSubGroup(NAME, new PdpSubGroup()))
            .hasMessage(LOCALNAME_IS_NULL);

        pdpGroupService.createPdpGroups(groupsToCreate.getGroups());
        assertThat(pdpGroupService.getPdpGroups()).hasSize(2);
        PdpGroup pdpGroup = pdpGroupService.getPdpGroups(CREATE_GROUPS).get(0);
        PdpSubGroup pdpSubGroup = pdpGroup.getPdpSubgroups().get(0);
        assertThat(pdpSubGroup.getDesiredInstanceCount()).isEqualTo(2);

        // now update and test
        pdpSubGroup.setDesiredInstanceCount(1);
        pdpGroupService.updatePdpSubGroup(CREATE_GROUPS, pdpSubGroup);
        PdpGroup updatGroup = pdpGroupService.getPdpGroups(CREATE_GROUPS).get(0);
        assertThat(updatGroup.getPdpSubgroups().get(0).getDesiredInstanceCount()).isEqualTo(1);
        pdpGroupService.deletePdpGroup(CREATE_GROUPS);
    }
}
