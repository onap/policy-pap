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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Super class for tests that run a background thread.
 */
public abstract class Threaded {

    /**
     * Max time to wait, in milliseconds, for a thread to terminate or for a message to be
     * published.
     */
    public static final long MAX_WAIT_MS = 5000;

    /**
     * The current background thread.
     */
    private Thread thread;

    /**
     * Indicates that a test is about to begin.
     *
     * @throws Exception if an error occurs
     */
    @BeforeEach
    public void setUp() throws Exception {
        thread = null;
    }

    /**
     * Invokes the "stopper" function to tell the background thread to exit and then waits
     * for it to terminate.
     *
     * @throws Exception if an error occurs
     */
    @AfterEach
    public void tearDown() throws Exception {
        stopThread();
        waitStop();
    }

    /**
     * Signals the background thread to stop.
     *
     * @throws Exception if an error occurs
     */
    protected abstract void stopThread() throws Exception;

    /**
     * Starts a background thread.
     *
     * @param runner what should be executed in the background thread
     * @throws IllegalStateException if a background thread is already running
     */
    protected void startThread(Runnable runner) {
        if (thread != null) {
            throw new IllegalStateException("a background thread is already running");
        }

        thread = new Thread(runner);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Interrupts the background thread.
     */
    protected void interruptThread() {
        thread.interrupt();
    }

    /**
     * Waits for the background thread to stop.
     *
     * @return {@code true} if the thread has stopped, {@code false} otherwise
     * @throws InterruptedException if this thread is interrupted while waiting
     */
    protected boolean waitStop() throws InterruptedException {
        if (thread != null) {
            Thread thread2 = thread;
            thread = null;

            thread2.join(MAX_WAIT_MS);

            return !thread2.isAlive();
        }

        return true;
    }
}
