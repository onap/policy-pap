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

package org.onap.policy.pap.main.rest.depundep;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.common.utils.validation.Version;
import org.onap.policy.models.pdp.concepts.PdpGroup;

public class TestGroupData {
    private static final String NEW_VERSION = "2.0.0";
    private static final String NAME = "my-name";

    private PdpGroup oldGroup;
    private PdpGroup newGroup;
    private GroupData data;
    private Version version;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        oldGroup = new PdpGroup();
        oldGroup.setName(NAME);

        newGroup = new PdpGroup(oldGroup);

        version = new Version(1, 2, 3);

        data = new GroupData(oldGroup);
    }

    @Test
    public void test() {
        assertFalse(data.isNew());
        assertSame(oldGroup, data.getOldGroup());
        assertSame(oldGroup, data.getCurrentGroup());

        data.setLatestVersion(version);
        data.setNewGroup(newGroup);

        assertTrue(data.isNew());
        assertSame(oldGroup, data.getOldGroup());
        assertSame(newGroup, data.getCurrentGroup());
        assertEquals(NEW_VERSION, data.getLatestVersion().toString());
        assertEquals(NEW_VERSION, newGroup.getVersion());

        // repeat
        newGroup = new PdpGroup(oldGroup);
        data.setNewGroup(newGroup);
        assertSame(oldGroup, data.getOldGroup());
        assertSame(newGroup, data.getCurrentGroup());
        assertEquals(NEW_VERSION, data.getLatestVersion().toString());
        assertEquals(NEW_VERSION, newGroup.getVersion());
    }

    @Test
    public void testSetNewGroup_DifferentName() {
        newGroup.setName("different-name");

        data.setLatestVersion(version);
        assertThatIllegalArgumentException().isThrownBy(() -> data.setNewGroup(newGroup))
                        .withMessage("attempt to change group name from my-name to different-name");
    }

    @Test
    public void testSetNewGroup_VersionNotSet() {
        assertThatIllegalStateException().isThrownBy(() -> data.setNewGroup(newGroup))
                        .withMessage("latestVersion not set for group: my-name");
    }
}
