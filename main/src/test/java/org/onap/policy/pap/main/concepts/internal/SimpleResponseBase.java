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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Test super class for classes derived from {@link SimpleResponse}.
 *
 * @param <I> the internal class being tested
 * @param <E> the external class that corresponds to the internal class
 */
public abstract class SimpleResponseBase
                    <I extends SimpleResponse<E>, E extends org.onap.policy.models.pap.concepts.SimpleResponse> {

    protected static final String ERROR_MSG = "an error occurred";

    protected I resp;
    protected I resp2;
    protected E extresp;

    @Before
    public void setUp() {
        resp = makeInternal();
        extresp = makeExternal();
    }

    @Test
    public void testToString() {
        assertNotNull(resp.toString());

        populate(resp);
        assertNotNull(resp.toString());
    }

    @Test
    public void testCopyConstructor() {
        // first try with null
        resp2 = makeInternal(resp);
        assertNullResponse("testCopyConstructor null", resp2);

        // populate and try again
        populate(resp);
        resp2 = makeInternal(resp);
        assertPopulated("testCopyConstructor", resp2);
    }

    @Test
    public void testToExternal() {
        // first check with null values
        extresp = resp.toExternal();
        assertNullResponse("testToExternal null", extresp);

        // populate it and check again
        populate(resp);
        extresp = resp.toExternal();
        resp.copyTo(extresp);
        assertPopulated("testToExternal populated", extresp);
    }

    @Test
    public void testCopyTo() {
        // first check with null values
        resp.copyTo(extresp);
        assertNullResponse("testCopyTo null", extresp);

        // populate it and check again
        populate(resp);
        extresp = makeExternal();
        resp.copyTo(extresp);
        assertPopulated("testCopyTo populated", extresp);
    }

    /**
     * Verifies that the response contains null data.
     *
     * @param testName the test name
     * @param resp the response to validate
     */
    protected void assertNullResponse(String testName, SimpleResponse<E> resp) {
        assertNull(testName, resp.getErrorDetails());
    }

    /**
     * Verifies that the response contains null data.
     *
     * @param testName the test name
     * @param resp the response to validate
     */
    protected void assertNullResponse(String testName, E resp) {
        assertNull(testName, resp.getErrorDetails());
    }

    /**
     * Verifies that the response is populated.
     *
     * @param testName the test name
     * @param resp the response to validate
     */
    protected void assertPopulated(String testName, SimpleResponse<E> resp) {
        assertEquals(testName, ERROR_MSG, resp.getErrorDetails());
    }

    /**
     * Verifies that the response is populated.
     *
     * @param testName the test name
     * @param resp the response to validate
     */
    protected void assertPopulated(String testName, E resp) {
        assertEquals(testName, ERROR_MSG, resp.getErrorDetails());
    }

    /**
     * Populates all fields in the target.
     *
     * @param target target whose fields are to be set
     */
    protected void populate(SimpleResponse<E> target) {
        target.setErrorDetails(ERROR_MSG);
    }

    /**
     * Creates an internal object.
     *
     * @return a new internal object
     */
    protected abstract I makeInternal();

    /**
     * Creates an internal object using the copy constructor.
     *
     * @param source source from which to copy the data
     * @return a new internal object
     */
    protected abstract I makeInternal(I source);

    /**
     * Creates an external object.
     *
     * @return a new external object
     */
    protected abstract E makeExternal();
}
