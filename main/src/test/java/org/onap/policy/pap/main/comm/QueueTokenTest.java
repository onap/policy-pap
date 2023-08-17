/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.comm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class QueueTokenTest {
    private static final String STRING1 = "a string";
    private static final String STRING2 = "another string";

    private QueueToken<String> token;

    @Test
    void test() throws Exception {
        token = new QueueToken<>(STRING1);
        assertEquals(STRING1, token.get());

        assertEquals(STRING1, token.replaceItem(STRING2));
        assertEquals(STRING2, token.get());

        assertEquals(STRING2, token.replaceItem(null));
        assertNull(token.get());

        assertNull(token.replaceItem(null));
        assertNull(token.get());

        assertNull(token.replaceItem(STRING1));
        assertNull(token.get());

        /*
         * Now do some mult-threaded tests, hopefully causing some contention.
         */

        token = new QueueToken<>("");

        Set<String> values = ConcurrentHashMap.newKeySet();

        // create and configure the threads
        Thread[] threads = new Thread[100];
        for (int x = 0; x < threads.length; ++x) {
            final int xfinal = x;
            threads[x] = new Thread(() -> values.add(token.replaceItem("me-" + xfinal)));
            threads[x].setDaemon(true);
        }

        // start the threads all at once
        for (Thread thread : threads) {
            thread.start();
        }

        // wait for the threads to stop
        for (Thread thread : threads) {
            thread.join(5000);
        }

        values.add(token.replaceItem(null));

        for (int x = 0; x < threads.length; ++x) {
            String msg = "me-" + x;
            assertTrue(values.contains(msg), msg);
        }
    }

}
