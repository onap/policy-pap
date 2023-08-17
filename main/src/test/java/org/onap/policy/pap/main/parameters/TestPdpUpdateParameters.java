/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.parameters;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * As {@link TestPdpRequestParameters} tests the "getXxx()" methods, all we need to verify
 * here is that the object is valid after loading from JSON.
 */
public class TestPdpUpdateParameters {
    private static final Coder coder = new StandardCoder();

    @Test
    public void testValidate() throws Exception {
        // valid, zeroes
        PdpUpdateParameters params = makeParams(10, 20);
        ValidationResult result = params.validate();
        assertNull(result.getResult());
        assertTrue(result.isValid());
    }

    private PdpUpdateParameters makeParams(int maxRetry, long maxWait) throws Exception {
        String json = "{'maxRetryCount':" + maxRetry + ", 'maxWaitMs':" + maxWait + "}";
        return coder.decode(json.replace('\'', '"'), PdpUpdateParameters.class);
    }
}
