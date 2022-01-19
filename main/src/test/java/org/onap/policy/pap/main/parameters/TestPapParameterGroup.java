/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
 *  Modification Copyright 2022. Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * Class to perform unit test of {@link PapParameterGroup}.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class TestPapParameterGroup {
    private static final Coder coder = new StandardCoder();

    CommonTestData commonTestData = new CommonTestData();

    @Test
    public void testPapParameterGroup_Named() {
        final PapParameterGroup papParameters = new PapParameterGroup("my-name");
        assertEquals("my-name", papParameters.getName());
    }

    @Test
    public void testPapParameterGroup() {
        final PapParameterGroup papParameters = commonTestData.getPapParameterGroup(1);
        final TopicParameterGroup topicParameterGroup = papParameters.getTopicParameterGroup();
        final ValidationResult validationResult = papParameters.validate();
        assertTrue(validationResult.isValid());
        assertEquals(CommonTestData.PAP_GROUP_NAME, papParameters.getName());
        assertEquals(topicParameterGroup.getTopicSinks(), papParameters.getTopicParameterGroup().getTopicSinks());
        assertEquals(topicParameterGroup.getTopicSources(), papParameters.getTopicParameterGroup().getTopicSources());
    }

    @Test
    public void testPapParameterGroup_Postgres() throws CoderException {
        String json = commonTestData.getPapPostgresParameterGroupAsString(1);
        final PapParameterGroup papPostgresParameters = coder.decode(json, PapParameterGroup.class);
        final TopicParameterGroup topicParameterGroup = papPostgresParameters.getTopicParameterGroup();
        final ValidationResult validationResult = papPostgresParameters.validate();
        assertTrue(validationResult.isValid());
        assertEquals(CommonTestData.PAP_GROUP_NAME, papPostgresParameters.getName());
        assertEquals(topicParameterGroup.getTopicSinks(),
                papPostgresParameters.getTopicParameterGroup().getTopicSinks());
        assertEquals(topicParameterGroup.getTopicSources(),
                papPostgresParameters.getTopicParameterGroup().getTopicSources());
    }

    @Test
    public void testPapParameterGroup_NullName() throws Exception {
        String json = commonTestData.getPapParameterGroupAsString(1).replace("\"PapGroup\"", "null");
        final PapParameterGroup papParameters = coder.decode(json, PapParameterGroup.class);
        final ValidationResult validationResult = papParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals(null, papParameters.getName());
        assertThat(validationResult.getResult()).contains("is null");
    }

    @Test
    public void testPapParameterGroup_EmptyName() throws Exception {
        String json = commonTestData.getPapParameterGroupAsString(1).replace(CommonTestData.PAP_GROUP_NAME, "");
        final PapParameterGroup papParameters = coder.decode(json, PapParameterGroup.class);
        final ValidationResult validationResult = papParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals("", papParameters.getName());
        assertThat(validationResult.getResult()).contains("\"name\" value \"\" INVALID, " + "is blank");
    }

    @Test
    public void testPapParameterGroup_SetName() {
        final PapParameterGroup papParameters = commonTestData.getPapParameterGroup(1);
        papParameters.setName("PapNewGroup");
        final ValidationResult validationResult = papParameters.validate();
        assertTrue(validationResult.isValid());
        assertEquals("PapNewGroup", papParameters.getName());
    }

}
