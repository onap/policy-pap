/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2022-2023 Nordix Foundation. All rights reserved.
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

package org.onap.policy.pap.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaNodeTemplate;
import org.onap.policy.pap.main.repository.ToscaNodeTemplateRepository;

@RunWith(MockitoJUnitRunner.class)
class ToscaNodeTemplateServiceTest {

    private static final String NODE_TEMPLATE_NAME = "tca_metadata";
    private static final String NODE_TEMPLATE_VERSION = "1.0.0";

    @Mock
    private ToscaNodeTemplateRepository nodeTemplateRepository;

    @InjectMocks
    private ToscaNodeTemplateService nodeTemplateService;

    private ToscaNodeTemplate nodeTemplate = new ToscaNodeTemplate();

    private final StandardCoder coder = new StandardYamlCoder();

    AutoCloseable autoCloseable;

    /**
     * Set up for tests.
     */
    @BeforeEach
    public void setup() throws CoderException {
        autoCloseable = MockitoAnnotations.openMocks(this);
        coder.decode(ResourceUtils.getResourceAsString("e2e/policyMetadataSet.yaml"),
                ToscaServiceTemplate.class).getToscaTopologyTemplate().getNodeTemplates()
            .forEach((key, value) -> nodeTemplate = value);

        Mockito.when(nodeTemplateRepository.findById(new PfConceptKey(NODE_TEMPLATE_NAME, NODE_TEMPLATE_VERSION)))
            .thenReturn(Optional.of(new JpaToscaNodeTemplate(nodeTemplate)));
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void testGetToscaNodeTemplate() {
        assertDoesNotThrow(() -> nodeTemplateService.getToscaNodeTemplate(NODE_TEMPLATE_NAME, NODE_TEMPLATE_VERSION));

        assertThat(nodeTemplateService.getToscaNodeTemplate(NODE_TEMPLATE_NAME, NODE_TEMPLATE_VERSION).getMetadata())
            .containsEntry("policyModel", nodeTemplate.getMetadata().get("policyModel"));

        assertThatThrownBy(() -> nodeTemplateService.getToscaNodeTemplate("invalid_name", "1.0.0"))
            .isInstanceOf(PfModelRuntimeException.class)
            .hasMessage("node template for invalid_name:1.0.0 do not exist in the database");

    }

}
