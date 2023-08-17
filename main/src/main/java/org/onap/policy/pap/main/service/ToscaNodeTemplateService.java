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

import jakarta.ws.rs.core.Response;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaNodeTemplate;
import org.onap.policy.pap.main.repository.ToscaNodeTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ToscaNodeTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ToscaNodeTemplateService.class);

    private final ToscaNodeTemplateRepository nodeTemplateRepository;

    /**
     * Get node templates.
     *
     * @param name the name of the node template to get
     * @param version the version of the node template to get
     * @return the node templates found
     * @throws PfModelRuntimeException on errors getting node templates
     */
    public ToscaNodeTemplate getToscaNodeTemplate(final String name, final String version)
        throws PfModelRuntimeException {

        LOGGER.debug("->getNodeTemplate: name={}, version={}", name, version);

        Optional<JpaToscaNodeTemplate> jpaToscaNodeTemplate = nodeTemplateRepository
            .findById(new PfConceptKey(name, version));
        if (jpaToscaNodeTemplate.isPresent()) {
            var nodeTemplate = jpaToscaNodeTemplate.get().toAuthorative();
            LOGGER.debug("<-NodeTemplate: name={}, version={}, nodeTemplate={}", name, version, nodeTemplate);
            return nodeTemplate;
        } else {
            throw new PfModelRuntimeException(Response.Status.NOT_ACCEPTABLE,
                "node template for " + name + ":" + version + " do not exist in the database");
        }
    }

}
