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

package org.onap.policy.pap.main.concepts.internal;

import org.apache.commons.lang3.StringUtils;
import org.onap.policy.models.base.PfConceptKey;

/**
 * Utilities to manipulate PolicyIdentXxx classes.
 */
public class IdentUtil {
    public static final String NULL_NAME_INTERNAL = PfConceptKey.getNullKey().getName();
    public static final String NULL_VERSION_INTERNAL = PfConceptKey.getNullKey().getVersion();

    public static final String NULL_NAME_EXTERNAL = "";
    public static final String NULL_VERSION_EXTERNAL = null;

    private IdentUtil() {
        super();
    }

    /**
     * Converts a name from an internal form to an external form.
     *
     * @param name the name to be converted
     * @return the external form of the name
     */
    public static String nameToExternal(String name) {
        return (NULL_NAME_INTERNAL.equals(name) ? "" : name);
    }

    /**
     * Converts a version from an internal form to an external form.
     *
     * @param version the name to be converted
     * @return the external form of the version
     */
    public static String versionToExternal(String version) {
        return (NULL_VERSION_INTERNAL.equals(version) ? null : version);
    }

    /**
     * Converts an external name-value pair to its internal form.
     *
     * @param name the external name
     * @param version the external version
     * @param target where to place the internal data
     */
    public static void toInternal(String name, String version, PfConceptKey target) {
        target.setName(StringUtils.isBlank(name) ? NULL_NAME_INTERNAL : name);
        target.setVersion(StringUtils.isBlank(version) ? NULL_VERSION_INTERNAL : version);
    }
}
