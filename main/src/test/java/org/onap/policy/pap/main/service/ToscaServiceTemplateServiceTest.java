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

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pap.main.repository.ToscaServiceTemplateRepository;

class ToscaServiceTemplateServiceTest {

    private static final String VERSION_1 = "1.0.0";

    private static final String VERSION = "version";

    private static final String NAME = "name";

    private static final String INVALID_VERSION_ERR_MSG =
        "parameter \"version\": value \"version\", does not match regular expression \"^(\\d+.){2}\\d+$\"";

    private static final String NODE_TEMPLATE_NAME = "tca_metadata";
    private static final String NODE_TEMPLATE_VERSION = "1.0.0";

    @Mock
    private ToscaServiceTemplateRepository toscaRepository;

    @InjectMocks
    private ToscaServiceTemplateService toscaService;

    @Mock
    private ToscaNodeTemplateService nodeTemplateService;

    private ToscaNodeTemplate nodeTemplate;

    private final StandardCoder coder = new StandardYamlCoder();

    AutoCloseable autoCloseable;

    /**
     * Set up for tests.
     *
     * @throws CoderException the exception
     */
    @BeforeEach
    public void setup() throws CoderException {
        autoCloseable = MockitoAnnotations.openMocks(this);
        coder.decode(ResourceUtils.getResourceAsString("e2e/policyMetadataSet.yaml"),
                ToscaServiceTemplate.class).getToscaTopologyTemplate().getNodeTemplates()
            .forEach((key, value) -> nodeTemplate = value);

        ToscaServiceTemplate toscaPolicyType =
            coder.decode(ResourceUtils.getResourceAsString("e2e/monitoring.policy-type.yaml"),
                ToscaServiceTemplate.class);
        ToscaServiceTemplate toscaPolicy =
            coder.decode(ResourceUtils.getResourceAsString("e2e/monitoring.policy.yaml"),
                ToscaServiceTemplate.class);
        // Add metadataSet reference to the policy metadata
        toscaPolicy.getToscaTopologyTemplate().getPolicies().forEach(e -> e.entrySet().iterator().next().getValue()
            .getMetadata().putAll(Map.of("metadataSetName", NODE_TEMPLATE_NAME,
                "metadataSetVersion", NODE_TEMPLATE_VERSION)));
        ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate(toscaPolicyType);
        serviceTemplate.setToscaTopologyTemplate(toscaPolicy.getToscaTopologyTemplate());
        Mockito
            .when(toscaRepository.findById(
                new PfConceptKey(JpaToscaServiceTemplate.DEFAULT_NAME, JpaToscaServiceTemplate.DEFAULT_VERSION)))
            .thenReturn(Optional.of(new JpaToscaServiceTemplate(serviceTemplate)));

        Mockito
            .when(nodeTemplateService.getToscaNodeTemplate(NODE_TEMPLATE_NAME, NODE_TEMPLATE_VERSION))
            .thenReturn(nodeTemplate);
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void testGetPolicyList() throws PfModelException {
        assertThatThrownBy(() -> toscaService.getPolicyList(NAME, VERSION))
            .isInstanceOf(PfModelRuntimeException.class).hasRootCauseMessage(INVALID_VERSION_ERR_MSG);

        assertThat(toscaService.getPolicyList(NAME, VERSION_1)).isEmpty();

        assertThat(toscaService.getPolicyList("onap.restart.tca", VERSION_1)).hasSize(1);
    }

    @Test
    void testPolicyForMetadataSet() throws PfModelException {
        List<ToscaPolicy> policies = toscaService.getPolicyList("onap.restart.tca", VERSION_1);

        assertThat(policies.get(0).getMetadata()).containsEntry("metadataSet", nodeTemplate.getMetadata());

        Mockito
            .when(nodeTemplateService.getToscaNodeTemplate(NODE_TEMPLATE_NAME, NODE_TEMPLATE_VERSION))
            .thenThrow(new PfModelRuntimeException(Response.Status.NOT_ACCEPTABLE,
                "node template for onap.restart.tca:1.0.0 do not exist in the database"));

        assertThatThrownBy(() -> toscaService.getPolicyList("onap.restart.tca", VERSION_1))
            .isInstanceOf(PfModelRuntimeException.class)
            .hasMessage("node template for onap.restart.tca:1.0.0 do not exist in the database");
    }

    @Test
    void testGetPolicyTypeList() throws PfModelException {
        assertThatThrownBy(() -> toscaService.getPolicyTypeList(NAME, VERSION))
            .isInstanceOf(PfModelRuntimeException.class).hasRootCauseMessage(INVALID_VERSION_ERR_MSG);

        assertThat(toscaService.getPolicyTypeList(NAME, VERSION_1)).isEmpty();

        assertThat(toscaService.getPolicyTypeList("onap.policies.monitoring.cdap.tca.hi.lo.app", VERSION_1)).hasSize(2);
        assertThat(toscaService.getPolicyTypeList("onap.policies.Monitoring", VERSION_1)).hasSize(1);
    }
}
