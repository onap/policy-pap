/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2021 Nordix Foundation.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PolicyStatusUtils {
    private static Pattern isRegexPattern;
    static final String REGEX_CHAR_PATTERN = "<>()\\[]{}^|$&!?*+.";

    /**
     * Checks, if string contains any character, that is used in regular expressions.
     * This method does not check, if expression is valid
     * Currently supported characters are:
     * < > ( ) [ ] { } \ ^ | $ & ! ? * + .
     *
     * @param text to be checked
     * @return true, if regex character is found in String, false otherwise
     */
    boolean isRegex(String text) {
        // Assume, that empty string is not the regex we are interested in
        if (text == null || text.isBlank()) {
            return false;
        }
        // create regex is heavy operation, so make sure it is done only once - with first request
        if (isRegexPattern == null) {
            synchronized (this) {
                if (isRegexPattern == null) {
                    isRegexPattern = Pattern.compile(".*[\\Q" + REGEX_CHAR_PATTERN + "\\E].*");
                }
            }
        }

        final Matcher matcher = isRegexPattern.matcher(text);
        return matcher.matches();
    }
}
