/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onap.policy.common.parameters.GroupValidationResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;

public class TestPdpParameters {
    private static final Coder coder = new StandardCoder();
    private static final CommonTestData testData = new CommonTestData();

    @Test
    public void testGetters() {
        PdpParameters params = testData.getPapParameterGroup(1).getPdpParameters();

        PdpUpdateParameters update = params.getUpdateParameters();
        assertNotNull(update);
        assertEquals(1, update.getMaxRetryCount());

        PdpStateChangeParameters state = params.getStateChangeParameters();
        assertNotNull(state);
        assertEquals(5, state.getMaxWaitMs());

        assertEquals(60000L, params.getHeartBeatMs());
    }

    @Test
    public void testValidate() throws Exception {
        String json = testData.getPapParameterGroupAsString(1);

        // valid
        String json2 = json;
        GroupValidationResult result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertNull(result.getResult());
        assertTrue(result.isValid());

        // invalid heart beat
        json2 = json.replaceFirst(": 6", ": 0");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());
        assertTrue(result.getResult().contains(
                        "field 'heartBeatMs' type 'long' value '0' INVALID, must be >= 1".replace('\'', '"')));

        // no update params
        json2 = testData.nullifyField(json, "updateParameters");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());
        assertTrue(result.getResult().contains("field 'updateParameters'".replace('\'', '"')));
        assertTrue(result.getResult().contains("is null"));

        // invalid update params
        json2 = json.replaceFirst(": 2", ": -2");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());
        assertTrue(result.getResult().contains("parameter group 'PdpUpdateParameters'".replace('\'', '"')));
        assertTrue(result.getResult().contains(
                        "field 'maxWaitMs' type 'long' value '-2' INVALID, must be >= 0".replace('\'', '"')));

        // no state-change params
        json2 = testData.nullifyField(json, "stateChangeParameters");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());

        // invalid state-change params
        json2 = json.replaceFirst(": 5", ": -5");
        result = coder.decode(json2, PapParameterGroup.class).getPdpParameters().validate();
        assertFalse(result.isValid());
        assertTrue(result.getResult().contains("parameter group 'PdpStateChangeParameters'".replace('\'', '"')));
        assertTrue(result.getResult().contains(
                        "field 'maxWaitMs' type 'long' value '-5' INVALID, must be >= 0".replace('\'', '"')));
    }

}
