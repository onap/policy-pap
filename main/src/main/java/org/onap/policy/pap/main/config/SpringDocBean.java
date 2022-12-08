/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.pap.main.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocBean {

    /**
     * Bean to configure Springdoc.
     *
     * @return the OpenAPI specification
     */
    @Bean
    public OpenAPI policyFrameworkLifecycleOpenApi() {
        return new OpenAPI()
            .info(new Info().title("Policy Framework Policy Administration (PAP)")
                .description(
                    "The Policy Framework Policy Administration (PAP) allows"
                        + " Policies to be grouped, deployed, and monitored")
                .license(new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0")))
            .externalDocs(new ExternalDocumentation()
                .description("Policy Framework Documentation")
                .url("https://docs.onap.org/projects/onap-policy-parent/en/latest"));
    }
}
