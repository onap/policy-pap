/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.pap.main.comm.TimerManager.Timer;

class TimerManagerTest extends Threaded {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String MGR_NAME = "my-manager";
    private static final String NAME1 = "timer-A";
    private static final String NAME2 = "timer-B";
    private static final String NAME3 = "timer-C";

    private static final long MGR_TIMEOUT_MS = 10000;

    private MyManager mgr;

    /*
     * This is a field rather than a local variable to prevent checkstyle from complaining
     * about the distance between its assignment and its use.
     */
    private long tcur1;

    /**
     * Sets up.
     *
     * @throws Exception if an error occurs
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        mgr = new MyManager(MGR_NAME, MGR_TIMEOUT_MS);
    }

    @Override
    protected void stopThread() {
        if (mgr != null) {
            mgr.stop();
            mgr.allowSleep(10);
        }
    }

    @Test
    void testTimerManager_testStop() throws Exception {
        startThread(mgr);

        mgr.stop();
        assertTrue(waitStop());

        // ensure we can call "stop" a second time
        mgr.stop();
    }

    @Test
    void testRegister() throws Exception {
        mgr.register(NAME2, mgr::addToQueue);
        mgr.registerNewTime(NAME1, mgr::addToQueue);

        // goes to the end of the queue
        mgr.registerNewTime(NAME2, mgr::addToQueue);

        startThread(mgr);

        mgr.allowSleep(2);

        assertEquals(NAME1, mgr.awaitTimer());
        assertEquals(NAME2, mgr.awaitTimer());
    }

    @Test
    void testRun_Ex() throws Exception {
        startThread(mgr);
        mgr.register(NAME1, mgr::addToQueue);

        mgr.awaitSleep();

        // background thread is "sleeping" - now we can interrupt it
        interruptThread();

        assertTrue(waitStop());
    }

    @Test
    void testProcessTimers() throws Exception {
        startThread(mgr);
        mgr.register(NAME1, mgr::addToQueue);
        mgr.awaitSleep();
        mgr.allowSleep(1);

        mgr.registerNewTime(NAME2, mgr::addToQueue);
        mgr.awaitSleep();

        // tell it to stop before returning from "sleep"
        mgr.stop();
        mgr.allowSleep(1);

        assertTrue(waitStop());

        assertEquals(NAME1, mgr.pollTimer());
        assertNull(mgr.pollTimer());
    }

    @Test
    void testGetNextTimer() throws Exception {
        startThread(mgr);
        mgr.register(NAME1, mgr::addToQueue);
        mgr.awaitSleep();
        mgr.allowSleep(1);

        mgr.registerNewTime(NAME2, mgr::addToQueue);
        mgr.awaitSleep();
    }

    @Test
    void testProcessTimer_CancelWhileWaiting() throws Exception {
        startThread(mgr);
        Timer timer = mgr.register(NAME1, mgr::addToQueue);
        mgr.awaitSleep();

        timer.cancel();
        mgr.allowSleep(1);

        mgr.registerNewTime(NAME2, mgr::addToQueue);
        mgr.awaitSleep();
        mgr.allowSleep(1);

        mgr.registerNewTime(NAME1, mgr::addToQueue);
        mgr.awaitSleep();

        // should have fired timer 2, but not timer 1
        assertEquals(NAME2, mgr.pollTimer());
        assertNull(mgr.pollTimer());
    }

    @Test
    void testProcessTimer_TimerEx() throws Exception {

        mgr.register(NAME1, name -> {
            throw new RuntimeException(EXPECTED_EXCEPTION);
        });

        mgr.register(NAME2, mgr::addToQueue);

        // same times, so only need one sleep
        startThread(mgr);
        mgr.awaitSleep();
        mgr.allowSleep(1);

        mgr.registerNewTime(NAME3, mgr::addToQueue);
        mgr.awaitSleep();

        // timer 1 fired but threw an exception, so only timer 2 should be in the queue
        assertEquals(NAME2, mgr.pollTimer());
    }

    @Test
    void testTimerAwait() throws Exception {
        startThread(mgr);

        // same times - should only sleep once
        mgr.register(NAME1, mgr::addToQueue);
        mgr.register(NAME2, mgr::addToQueue);
        mgr.awaitSleep();

        tcur1 = mgr.currentTimeMillis();

        mgr.allowSleep(1);

        // next one will have a new timeout, so expect to sleep again
        mgr.registerNewTime(NAME3, mgr::addToQueue);
        mgr.awaitSleep();

        long tcur2 = mgr.currentTimeMillis();
        assertTrue(tcur2 >= tcur1 + MGR_TIMEOUT_MS);

        assertEquals(NAME1, mgr.pollTimer());
        assertEquals(NAME2, mgr.pollTimer());
        assertNull(mgr.pollTimer());
    }

    @Test
    void testTimerCancel_WhileWaiting() throws Exception {
        startThread(mgr);

        Timer timer = mgr.register(NAME1, mgr::addToQueue);
        mgr.awaitSleep();

        // cancel while sleeping
        timer.cancel();

        mgr.registerNewTime(NAME2, mgr::addToQueue);

        // allow it to sleep through both timers
        mgr.allowSleep(2);

        // only timer 2 should have fired
        assertEquals(NAME2, mgr.awaitTimer());
    }

    @Test
    void testTimerCancel_ViaReplace() throws Exception {
        startThread(mgr);

        mgr.register(NAME1, name -> mgr.addToQueue("hello"));
        mgr.awaitSleep();

        // replace the timer while the background thread is sleeping
        mgr.registerNewTime(NAME1, name -> mgr.addToQueue("world"));

        // allow it to sleep through both timers
        mgr.allowSleep(2);

        // only timer 2 should have fired
        assertEquals("world", mgr.awaitTimer());
    }

    @Test
    void testTimerToString() {
        Timer timer = mgr.register(NAME1, mgr::addToQueue);
        assertNotNull(timer.toString());
        assertTrue(timer.toString().contains(NAME1));
    }

    @Test
    void testCurrentTimeMillis() {
        long tbeg = System.currentTimeMillis();
        long tcur = new TimerManager(MGR_NAME, MGR_TIMEOUT_MS).currentTimeMillis();
        long tend = System.currentTimeMillis();

        assertTrue(tcur >= tbeg);
        assertTrue(tend >= tcur);
    }

    @Test
    void testSleep() throws Exception {
        long tbeg = System.currentTimeMillis();
        new TimerManager(MGR_NAME, MGR_TIMEOUT_MS).sleep(10);
        long tend = System.currentTimeMillis();

        assertTrue(tend >= tbeg + 10);
    }


    /**
     * Timer Manager whose notions of time are controlled here. It also overrides the
     * {@link #sleep(long)} method so that the test thread can control when the background
     * timer thread finishes sleeping.
     */
    private static class MyManager extends TimerManager {
        private final Object lockit = new Object();
        private long curTime = 1000;
        private int offset = 0;
        private final Semaphore sleepEntered = new Semaphore(0);
        private final Semaphore sleepsAllowed = new Semaphore(0);
        private final LinkedBlockingQueue<String> results = new LinkedBlockingQueue<>();

        public MyManager(String name, long waitTimeMs) {
            super(name, waitTimeMs);
        }

        /**
         * Registers a timer with a new starting time. Because the manager uses the
         * current time when determining the expiration time, we have to temporarily
         * fiddle with {@link #curTime}, but we leave it unchanged when we're done.
         * Increases the {@link #offset} each time it's invoked.
         */
        public void registerNewTime(String timerName, Consumer<String> action) {
            synchronized (lockit) {
                offset++;

                curTime += offset;
                super.register(timerName, action);
                curTime -= offset;

            }
        }

        /**
         * Allows the manager to "sleep" several times.
         *
         * @param ntimes the number of times the manager should sleep
         */
        public void allowSleep(int ntimes) {
            sleepsAllowed.release(ntimes);
        }

        /**
         * Waits for the manager to "sleep".
         *
         * @throws InterruptedException if the thread is interrupted while waiting for the
         *         background thread to sleep
         */
        public void awaitSleep() throws InterruptedException {
            if (!sleepEntered.tryAcquire(MAX_WAIT_MS, TimeUnit.MILLISECONDS)) {
                fail("background thread failed to sleep");
            }
        }

        @Override
        protected long currentTimeMillis() {
            synchronized (lockit) {
                return curTime;
            }
        }

        @Override
        protected void sleep(long timeMs) throws InterruptedException {
            sleepEntered.release();
            sleepsAllowed.acquire();

            synchronized (lockit) {
                curTime += timeMs;
            }
        }

        /**
         * Waits for a timer to fire.
         *
         * @return the message the timer added to {@link #results}
         * @throws InterruptedException if this thread is interrupted while waiting
         */
        private String awaitTimer() throws InterruptedException {
            return results.poll(MAX_WAIT_MS, TimeUnit.MILLISECONDS);
        }

        /**
         * Adds a name to the queue.
         *
         * @param name the name to add
         */
        private void addToQueue(String name) {
            results.add(name);
        }

        /**
         * Polls to see if a timer has fired.
         *
         * @return the message the timer added to {@link #results}, or {@code null} if no
         *         timer has fired yet
         */
        private String pollTimer() {
            return results.poll();
        }
    }
}
