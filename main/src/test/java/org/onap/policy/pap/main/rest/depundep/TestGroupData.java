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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.onap.policy.models.pdp.concepts.PdpGroup;

public class TestGroupData {
    private static final String NAME = "my-name";

    private PdpGroup oldGroup;
    private PdpGroup newGroup;
    private GroupData data;

    /**
     * Sets up.
     */
    @Before
    public void setUp() {
        oldGroup = new PdpGroup();
        oldGroup.setName(NAME);

        newGroup = new PdpGroup(oldGroup);

        data = new GroupData(oldGroup);
    }

    @Test
    public void testNew() {
        data = new GroupData(oldGroup, true);
        assertSame(oldGroup, data.getGroup());

        assertFalse(data.isUnchanged());
        assertTrue(data.isNew());
        assertFalse(data.isUpdated());

        data.update(newGroup);
        assertFalse(data.isUnchanged());
        assertTrue(data.isNew());
        assertFalse(data.isUpdated());
        assertSame(newGroup, data.getGroup());

        // repeat with a new group
        newGroup = new PdpGroup(oldGroup);
        data.update(newGroup);
        assertFalse(data.isUnchanged());
        assertTrue(data.isNew());
        assertFalse(data.isUpdated());
        assertSame(newGroup, data.getGroup());
    }

    @Test
    public void testUpdateOnly() {
        assertTrue(data.isUnchanged());
        assertFalse(data.isUpdated());
        assertSame(oldGroup, data.getGroup());

        data.update(newGroup);

        assertFalse(data.isUnchanged());
        assertTrue(data.isUpdated());
        assertFalse(data.isNew());
        assertSame(newGroup, data.getGroup());

        // repeat
        newGroup = new PdpGroup(oldGroup);
        data.update(newGroup);
        assertFalse(data.isUnchanged());
        assertTrue(data.isUpdated());
        assertFalse(data.isNew());
        assertSame(newGroup, data.getGroup());

        // incorrect name
        newGroup = new PdpGroup(oldGroup);
        newGroup.setName("other");
        assertThatIllegalArgumentException().isThrownBy(() -> data.update(newGroup))
                        .withMessage("expected group my-name, but received other");
    }
}
