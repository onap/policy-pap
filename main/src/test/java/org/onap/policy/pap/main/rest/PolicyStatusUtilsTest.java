/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Modifications Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.pap.main.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

public class PolicyStatusUtilsTest {
    private PolicyStatusUtils utils;

    @Before
    public void setUp() {
        utils = new PolicyStatusUtils();
    }

    @Test
    public void isRegexNull() {
        final boolean actual = utils.isRegex(null);
        assertFalse(actual);
    }

    @Test
    public void isRegexEmpty() {
        final boolean actual = utils.isRegex("");
        assertFalse(actual);
    }

    @Test
    public void isRegexBlank() {
        final boolean actual = utils.isRegex(" ");
        assertFalse(actual);
    }

    @Test
    public void isRegexRegularString() {
        final String text = RandomStringUtils.randomAlphanumeric(2, 20);
        final boolean actual = utils.isRegex(text);
        assertFalse(actual);
    }

    @Test
    public void isRegexValidChars() {
        final char[] chars = PolicyStatusUtils.REGEX_CHAR_PATTERN.toCharArray();
        for (char letter : chars) {
            final String text =
                RandomStringUtils.randomAlphanumeric(2, 20) + letter + RandomStringUtils.randomAlphanumeric(3, 10);
            final boolean actual = utils.isRegex(text);
            assertTrue("Should be a regex with char " + letter, actual);
        }
    }

    @Test
    public void isRegexRegex() {
        final String text =
            RandomStringUtils.randomAlphanumeric(2, 20) + "o(2,3)" + RandomStringUtils.randomAlphanumeric(3, 10);
        final boolean actual = utils.isRegex(text);
        assertTrue(actual);
    }


}