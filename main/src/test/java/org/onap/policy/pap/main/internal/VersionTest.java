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

package org.onap.policy.pap.main.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class VersionTest {
    private static final String TYPE = "my-type";
    private static final String NAME = "my-name";

    private static final int MAJOR = 10;
    private static final int MINOR = 2;
    private static final int PATCH = 3;

    private Version vers;

    @Before
    public void setUp() {
        vers = new Version(MAJOR, MINOR, PATCH);
    }

    @Test
    public void testHashCode() {
        int hash = vers.hashCode();
        int hash2 = new Version(MAJOR, MINOR, PATCH + 1).hashCode();
        assertTrue(hash != hash2);
    }

    @Test
    public void testMakeVersion() {
        assertEquals("9.8.7", Version.makeVersion(TYPE, NAME, "9.8.7").toString());
        assertEquals("9.0.0", Version.makeVersion(TYPE, NAME, "9").toString());

        assertNull(Version.makeVersion(TYPE, NAME, ""));
        assertNull(Version.makeVersion(TYPE, NAME, "a.3.4"));
        assertNull(Version.makeVersion(TYPE, NAME, "100."));
        assertNull(Version.makeVersion(TYPE, NAME, "10000000000000000.2.3"));
        assertNull(Version.makeVersion(TYPE, NAME, "1.20000000000000000.3"));
        assertNull(Version.makeVersion(TYPE, NAME, "1.2.30000000000000000"));
    }

    @Test
    public void testNewVersion() {
        vers = vers.newVersion();
        assertEquals("11.0.0", vers.toString());
    }

    @Test
    public void testEquals() {
        assertFalse(vers.equals(null));
        assertFalse(vers.equals(new Object()));

        assertTrue(vers.equals(vers));

        assertTrue(vers.equals(new Version(MAJOR, MINOR, PATCH)));

        assertFalse(vers.equals(new Version(MAJOR + 1, MINOR, PATCH)));
        assertFalse(vers.equals(new Version(MAJOR, MINOR + 1, PATCH)));
        assertFalse(vers.equals(new Version(MAJOR, MINOR, PATCH + 1)));
    }

    @Test
    public void testCompareTo() {
        vers = new Version(101, 201, 301);

        // equals case
        assertTrue(new Version(101, 201, 301).compareTo(vers) == 0);

        // major takes precedence
        assertTrue(new Version(102, 200, 300).compareTo(vers) > 0);

        // minor takes precedence over patch
        assertTrue(new Version(101, 202, 300).compareTo(vers) > 0);

        // compare major
        assertTrue(new Version(100, 201, 301).compareTo(vers) < 0);
        assertTrue(new Version(102, 201, 301).compareTo(vers) > 0);

        // compare minor
        assertTrue(new Version(101, 200, 301).compareTo(vers) < 0);
        assertTrue(new Version(101, 202, 301).compareTo(vers) > 0);

        // compare patch
        assertTrue(new Version(101, 201, 300).compareTo(vers) < 0);
        assertTrue(new Version(101, 201, 302).compareTo(vers) > 0);
    }

    @Test
    public void testToString() {
        assertEquals("10.2.3", vers.toString());
    }

    @Test
    public void testGetMajor() {
        assertEquals(MAJOR, vers.getMajor());
    }

    @Test
    public void testGetMinor() {
        assertEquals(MINOR, vers.getMinor());
    }

    @Test
    public void testGetPatch() {
        assertEquals(PATCH, vers.getPatch());
    }

    @Test
    public void testVersionIntIntInt() {
        assertEquals("5.6.7", new Version(5, 6, 7).toString());
    }

    @Test
    public void testVersion() {
        assertEquals("0.0.0", new Version().toString());
    }
}
