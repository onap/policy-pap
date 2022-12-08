/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Instant;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.common.gson.InstantTypeAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {JacksonAutoConfiguration.class})
@EntityScan(
    basePackages = {"org.onap.policy.models.pap.persistence.concepts",
        "org.onap.policy.models.pdp.persistence.concepts", "org.onap.policy.models.tosca.simple.concepts"})
public class PolicyPapApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolicyPapApplication.class, args);
    }

    /**
     * Configure gson with the required adapters to be used in the application.
     *
     * @return the gson bean
     */
    @Bean
    public Gson gson() {
        GsonBuilder gsonBuilder = GsonMessageBodyHandler.configBuilder(new GsonBuilder());
        gsonBuilder.registerTypeAdapter(Instant.class, new InstantTypeAdapter());
        return gsonBuilder.create();
    }
}
