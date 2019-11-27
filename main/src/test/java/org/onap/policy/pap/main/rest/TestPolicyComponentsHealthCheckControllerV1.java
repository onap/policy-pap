/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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

import org.junit.Test;

/**
 * Class to perform unit test of {@link PolicyComponentsHealthCheckControllerV1}.
 *
 * @author Yehui Wang (yehui.wang@est.tech)
 */
public class TestPolicyComponentsHealthCheckControllerV1 extends CommonPapRestServer {

    private static final String ENDPOINT = "components/healthcheck";

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(ENDPOINT);
    }
}
