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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onap.policy.models.base.PfConceptKey;

public class IdentUtilTest {

    @Test
    public void testConstants() {
        PfConceptKey key = new PfConceptKey(IdentUtil.NULL_NAME_INTERNAL, IdentUtil.NULL_VERSION_INTERNAL);
        assertTrue(key.isNullKey());
        assertTrue(key.isNullVersion());
    }

    @Test
    public void testNameToExternal() {
        assertEquals(IdentUtil.NULL_NAME_EXTERNAL, IdentUtil.nameToExternal(IdentUtil.NULL_NAME_INTERNAL));

        assertEquals("my-name", IdentUtil.nameToExternal("my-name"));
    }

    @Test
    public void testVersionToExternal() {
        assertEquals(IdentUtil.NULL_VERSION_EXTERNAL, IdentUtil.versionToExternal(IdentUtil.NULL_VERSION_INTERNAL));

        assertEquals("my-version", IdentUtil.versionToExternal("my-version"));
    }

    @Test
    public void testToInternal() {
        PfConceptKey target = new PfConceptKey();

        IdentUtil.toInternal("name-a", "version-a", target);
        assertEquals("name-a", target.getName());
        assertEquals("version-a", target.getVersion());

        // try with null name
        IdentUtil.toInternal(null, "version-a", target);
        assertEquals(IdentUtil.NULL_NAME_INTERNAL, target.getName());
        assertEquals("version-a", target.getVersion());

        // try with null version
        IdentUtil.toInternal("name-a", null, target);
        assertEquals("name-a", target.getName());
        assertEquals(IdentUtil.NULL_VERSION_INTERNAL, target.getVersion());

        // both empty
        IdentUtil.toInternal("", "", target);
        assertEquals(IdentUtil.NULL_NAME_INTERNAL, target.getName());
        assertEquals(IdentUtil.NULL_VERSION_INTERNAL, target.getVersion());
    }

}
