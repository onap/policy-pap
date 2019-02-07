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

package org.onap.policy.pap.main.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onap.policy.common.parameters.GroupValidationResult;

/**
 * Class to perform unit test of {@link PapParameterGroup}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class TestPapParameterGroup {
    CommonTestData commonTestData = new CommonTestData();

    @Test
    public void testPapParameterGroup() {
        final RestServerParameters restServerParameters = commonTestData.getRestServerParameters(false);
        final PapParameterGroup papParameters =
                new PapParameterGroup(CommonTestData.PAP_GROUP_NAME, restServerParameters);
        final GroupValidationResult validationResult = papParameters.validate();
        assertTrue(validationResult.isValid());
        assertEquals(CommonTestData.PAP_GROUP_NAME, papParameters.getName());
        assertEquals(restServerParameters.getHost(), papParameters.getRestServerParameters().getHost());
        assertEquals(restServerParameters.getPort(), papParameters.getRestServerParameters().getPort());
        assertEquals(restServerParameters.getUserName(), papParameters.getRestServerParameters().getUserName());
        assertEquals(restServerParameters.getPassword(), papParameters.getRestServerParameters().getPassword());
        assertFalse(papParameters.getRestServerParameters().isHttps());
        assertFalse(papParameters.getRestServerParameters().isAaf());
    }

    @Test
    public void testPapParameterGroup_NullName() {
        final RestServerParameters restServerParameters = commonTestData.getRestServerParameters(false);
        final PapParameterGroup papParameters = new PapParameterGroup(null, restServerParameters);
        final GroupValidationResult validationResult = papParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals(null, papParameters.getName());
        assertTrue(validationResult.getResult().contains(
                "field \"name\" type \"java.lang.String\" value \"null\" INVALID, " + "must be a non-blank string"));
    }

    @Test
    public void testPapParameterGroup_EmptyName() {
        final RestServerParameters restServerParameters = commonTestData.getRestServerParameters(false);
        final PapParameterGroup papParameters = new PapParameterGroup("", restServerParameters);
        final GroupValidationResult validationResult = papParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals("", papParameters.getName());
        assertTrue(validationResult.getResult().contains(
                "field \"name\" type \"java.lang.String\" value \"\" INVALID, " + "must be a non-blank string"));
    }

    @Test
    public void testPapParameterGroup_SetName() {
        final RestServerParameters restServerParameters = commonTestData.getRestServerParameters(false);
        final PapParameterGroup papParameters =
                new PapParameterGroup(CommonTestData.PAP_GROUP_NAME, restServerParameters);
        papParameters.setName("PapNewGroup");
        final GroupValidationResult validationResult = papParameters.validate();
        assertTrue(validationResult.isValid());
        assertEquals("PapNewGroup", papParameters.getName());
    }

    @Test
    public void testApiParameterGroup_EmptyRestServerParameters() {
        final RestServerParameters restServerParameters = commonTestData.getRestServerParameters(true);

        final PapParameterGroup papParameters =
                new PapParameterGroup(CommonTestData.PAP_GROUP_NAME, restServerParameters);
        final GroupValidationResult validationResult = papParameters.validate();
        assertFalse(validationResult.isValid());
        assertTrue(validationResult.getResult()
                .contains("\"org.onap.policy.pap.main.parameters.RestServerParameters\" INVALID, "
                        + "parameter group has status INVALID"));
    }
}
