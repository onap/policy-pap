/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada. All rights reserved.
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

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pap.main.repository.ToscaServiceTemplateRepository;

@RunWith(MockitoJUnitRunner.class)
public class ToscaServiceTemplateServiceTest {

    private static final String INVALID_VERSION_ERR_MSG =
        "parameter \"version\": value \"version\", does not match regular expression \"^(\\d+.){2}\\d+$\"";

    @Mock
    private ToscaServiceTemplateRepository toscaRepository;

    @InjectMocks
    private ToscaServiceTemplateService toscaService;

    private ToscaServiceTemplate serviceTemplate;

    /**
     * Set up for tests.
     *
     * @throws CoderException the exception
     */
    @Before
    public void setup() throws CoderException {
        StandardCoder coder = new StandardYamlCoder();
        ToscaServiceTemplate toscaPolicyType = coder
            .decode(ResourceUtils.getResourceAsString("e2e/monitoring.policy-type.yaml"), ToscaServiceTemplate.class);
        ToscaServiceTemplate toscaPolicy =
            coder.decode(ResourceUtils.getResourceAsString("e2e/monitoring.policy.yaml"), ToscaServiceTemplate.class);
        serviceTemplate = new ToscaServiceTemplate(toscaPolicyType);
        serviceTemplate.setToscaTopologyTemplate(toscaPolicy.getToscaTopologyTemplate());
        Mockito
            .when(toscaRepository.findById(
                new PfConceptKey(JpaToscaServiceTemplate.DEFAULT_NAME, JpaToscaServiceTemplate.DEFAULT_VERSION)))
            .thenReturn(Optional.of(new JpaToscaServiceTemplate(serviceTemplate)));
    }


    @Test
    public void testGetPolicyList() throws PfModelException {
        assertThatThrownBy(() -> toscaService.getPolicyList("name", "version"))
            .isInstanceOf(PfModelRuntimeException.class).hasRootCauseMessage(INVALID_VERSION_ERR_MSG);

        assertThat(toscaService.getPolicyList("name", "1.0.0")).hasSize(0);

        assertThat(toscaService.getPolicyList("onap.restart.tca", "1.0.0")).hasSize(1);
    }

    @Test
    public void testGetPolicyTypeList() throws PfModelException {
        assertThatThrownBy(() -> toscaService.getPolicyTypeList("name", "version"))
            .isInstanceOf(PfModelRuntimeException.class).hasRootCauseMessage(INVALID_VERSION_ERR_MSG);

        assertThat(toscaService.getPolicyTypeList("name", "1.0.0")).hasSize(0);

        assertThat(toscaService.getPolicyTypeList("onap.policies.monitoring.cdap.tca.hi.lo.app", "1.0.0")).hasSize(2);
        assertThat(toscaService.getPolicyTypeList("onap.policies.Monitoring", "1.0.0")).hasSize(1);
    }
}
