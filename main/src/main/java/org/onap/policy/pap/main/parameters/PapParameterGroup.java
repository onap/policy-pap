/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2023 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.parameters;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.common.endpoints.parameters.RestClientParameters;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.common.parameters.validation.ParameterGroupConstraint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Class to hold all parameters needed for pap component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@NotNull
@NotBlank
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pap")
public class PapParameterGroup extends ParameterGroupImpl {
    @Valid
    @ParameterGroupConstraint
    private PdpParameters pdpParameters;
    @Valid
    @ParameterGroupConstraint
    private TopicParameterGroup topicParameterGroup;
    // API, Distribution Health Check REST client parameters.
    @ParameterGroupConstraint
    private List<@NotNull @Valid RestClientParameters> healthCheckRestClientParameters;

    public PapParameterGroup() {
        super();
    }

    /**
     * Create the pap parameter group.
     *
     * @param name the parameter group name
     */
    public PapParameterGroup(final String name) {
        super(name);
    }
}
