/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;

class TestPdpParameters {
    private static final Coder coder = new StandardCoder();
    private static final CommonTestData testData = new CommonTestData();

    @Test
    void testGetters() {
        PdpParameters params = testData.getPapParameterGroup(1).getPdpParameters();

        PdpUpdateParameters update = params.getUpdateParameters();
        assertNotNull(update);
        assertEquals(1, update.getMaxRetryCount());

        PdpStateChangeParameters state = params.getStateChangeParameters();
        assertNotNull(state);
        assertEquals(5, state.getMaxWaitMs());

        assertEquals(6000L, params.getHeartBeatMs());

        assertEquals(20000L, params.getMaxMessageAgeMs());

        // check default value
        assertEquals(600000L, new PdpParameters().getMaxMessageAgeMs());
    }

    @Test
    void testValidate() throws Exception {
        String json = testData.getPapParameterGroupAsString(1);

        // valid
        String json2 = json;
        ValidationResult result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertNull(result.getResult());
        assertTrue(result.isValid());

        // invalid heart beat
        json2 = json.replaceFirst(": 6", ": 0");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());
        assertThat(result.getResult()).contains(
                        "'heartBeatMs' value '0' INVALID, is below the minimum value: 1".replace('\'', '"'));

        // invalid max message age
        json2 = json.replaceFirst(": 20000", ": 0");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());
        assertThat(result.getResult()).contains(
                        "'maxMessageAgeMs' value '0' INVALID, is below the minimum value: 1".replace('\'', '"'));

        // no update params
        json2 = testData.nullifyField(json, "updateParameters");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());
        assertThat(result.getResult()).contains("\"updateParameters\"", "is null");

        // invalid update params
        json2 = json.replaceFirst(": 2", ": -2");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());
        assertThat(result.getResult()).contains("\"PdpUpdateParameters\"",
                        "'maxWaitMs' value '-2' INVALID, is below the minimum value: 0".replace('\'', '"'));

        // no state-change params
        json2 = testData.nullifyField(json, "stateChangeParameters");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());

        // invalid state-change params
        json2 = json.replaceFirst(": 5", ": -5");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());
        assertThat(result.getResult()).contains("\"PdpStateChangeParameters\"",
                        "'maxWaitMs' value '-5' INVALID, is below the minimum value: 0".replace('\'', '"'));
    }

}
