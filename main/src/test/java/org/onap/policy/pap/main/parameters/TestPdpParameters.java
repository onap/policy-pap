/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import java.util.Map;
import org.junit.Test;
import org.onap.policy.common.parameters.GroupValidationResult;

public class TestPdpParameters {
    private static CommonTestData testData = new CommonTestData();

    @Test
    public void testGetters() {
        PdpParameters params = testData.toObject(testData.getPdpParametersMap(), PdpParameters.class);

        PdpUpdateParameters update = params.getUpdateParameters();
        assertNotNull(update);
        assertEquals(1, update.getMaxRetryCount());

        PdpStateChangeParameters state = params.getStateChangeParameters();
        assertNotNull(state);
        assertEquals(2, state.getMaxWaitMs());
    }

    @Test
    public void testValidate() {
        // valid
        Map<String, Object> map = testData.getPdpParametersMap();
        GroupValidationResult result = testData.toObject(map, PdpParameters.class).validate();
        assertNull(result.getResult());
        assertTrue(result.isValid());

        // no update params
        map = testData.getPdpParametersMap();
        map.remove("updateParameters");
        result = testData.toObject(map, PdpParameters.class).validate();
        assertFalse(result.isValid());
        assertTrue(result.getResult().contains("field 'updateParameters'".replace('\'', '"')));
        assertTrue(result.getResult().contains("is null"));

        // invalid update params
        map = testData.getPdpParametersMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> updmap = (Map<String, Object>) map.get("updateParameters");
        updmap.put("maxRetryCount", "-2");
        result = testData.toObject(map, PdpParameters.class).validate();
        assertFalse(result.isValid());
        assertTrue(result.getResult().contains("parameter group 'PdpUpdateParameters'".replace('\'', '"')));
        assertTrue(result.getResult().contains(
                        "field 'maxRetryCount' type 'int' value '-2' INVALID, must be >= 0".replace('\'', '"')));

        // no state-change params
        map = testData.getPdpParametersMap();
        map.remove("stateChangeParameters");
        result = testData.toObject(map, PdpParameters.class).validate();
        assertFalse(result.isValid());

        // invalid state-change params
        map = testData.getPdpParametersMap();
        @SuppressWarnings("unchecked")
        Map<String, Object> statemap = (Map<String, Object>) map.get("stateChangeParameters");
        statemap.put("maxRetryCount", "-3");
        result = testData.toObject(map, PdpParameters.class).validate();
        assertFalse(result.isValid());
        System.out.println(result.getResult());
        assertTrue(result.getResult().contains("parameter group 'PdpStateChangeParameters'".replace('\'', '"')));
        assertTrue(result.getResult().contains(
                        "field 'maxRetryCount' type 'int' value '-3' INVALID, must be >= 0".replace('\'', '"')));
    }

}
