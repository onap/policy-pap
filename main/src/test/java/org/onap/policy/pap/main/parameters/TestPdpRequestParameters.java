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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;

class TestPdpRequestParameters {
    private static final Coder coder = new StandardCoder();

    @Test
    void test() throws Exception {
        PdpRequestParameters params = makeParams(10, 20);
        assertEquals(10, params.getMaxRetryCount());
        assertEquals(20, params.getMaxWaitMs());
    }

    @Test
    void testValidate() throws Exception {
        // valid, zeroes
        PdpRequestParameters params = makeParams(0, 0);
        ValidationResult result = params.validate();
        assertNull(result.getResult());
        assertTrue(result.isValid());

        // valid
        params = makeParams(100, 110);
        result = params.validate();
        assertNull(result.getResult());
        assertTrue(result.isValid());

        // invalid retry count
        params = makeParams(-1, 120);
        result = params.validate();
        assertFalse(result.isValid());
        assertThat(result.getResult()).contains(
                        "'maxRetryCount' value '-1' INVALID, is below the minimum value: 0".replace('\'', '"'));

        // invalid wait time
        params = makeParams(130, -1);
        result = params.validate();
        assertFalse(result.isValid());
        assertThat(result.getResult()).contains(
                        "'maxWaitMs' value '-1' INVALID, is below the minimum value: 0".replace('\'', '"'));
    }

    private PdpRequestParameters makeParams(int maxRetry, long maxWait) throws Exception {
        String json = "{'name':'abc', 'maxRetryCount':" + maxRetry + ", 'maxWaitMs':" + maxWait + "}";
        return coder.decode(json.replace('\'', '"'), PdpRequestParameters.class);
    }
}
