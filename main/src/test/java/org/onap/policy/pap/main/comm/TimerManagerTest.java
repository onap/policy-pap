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

package org.onap.policy.pap.main.comm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.pap.main.comm.TimerManager.Timer;

public class TimerManagerTest extends Threaded {
    private static final String EXPECTED_EXCEPTION = "expected exception";
    private static final String MGR_NAME = "my-manager";
    private static final String NAME1 = "timer-A";
    private static final String NAME2 = "timer-B";
    private static final String NAME3 = "timer-C";

    private static final long MGR_TIMEOUT_MS = 10000;

    private MyManager mgr;
    private LinkedBlockingQueue<String> results = new LinkedBlockingQueue<>();
    private long tcur;

    @BeforeClass
    public static void setUpBeforeClass() {

    }

    @AfterClass
    public static void tearDownAfterClass() {

    }

    /**
     * Sets up.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        mgr = new MyManager(MGR_NAME, MGR_TIMEOUT_MS);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected void stopThread() throws Exception {
        if (mgr != null) {
            mgr.stop();
            mgr.stopSleep();
        }
    }

    @Test
    public void testTimerManager_testStop() throws Exception {
        startThread(mgr);

        mgr.stop();
        assertTrue(waitStop());

        // ensure we can call "stop" a second time
        mgr.stop();
    }

    @Test
    public void testRegister() throws Exception {
        mgr.register(NAME2, this::addToQueue);
        mgr.register(NAME1, this::addToQueue);

        // override previous timer and go to the end of the queue
        mgr.addTime(1);
        mgr.register(NAME2, this::addToQueue);

        startThread(mgr);

        mgr.allowSleep(2);

        assertEquals(NAME1, awaitTimer());
        assertEquals(NAME2, awaitTimer());
    }

    @Test
    public void testRun_Ex() throws Exception {
        startThread(mgr);
        mgr.register(NAME1, this::addToQueue);

        mgr.awaitSleep();

        // background thread is "sleeping" - now we can interrupt it
        interruptThread();

        assertTrue(waitStop());
    }

    @Test
    public void testProcessTimers() throws Exception {
        startThread(mgr);
        mgr.register(NAME1, this::addToQueue);
        mgr.awaitSleep();
        mgr.allowSleep(1);

        mgr.register(NAME2, this::addToQueue);
        mgr.awaitSleep();

        // tell it to stop before returning from "sleep"
        mgr.stop();
        mgr.allowSleep(1);

        assertTrue(waitStop());

        assertEquals(NAME1, results.poll());
        assertNull(results.poll());
    }

    @Test
    public void testGetNextTimer() throws Exception {
        startThread(mgr);
        mgr.register(NAME1, this::addToQueue);
        mgr.awaitSleep();
        mgr.allowSleep(1);

        mgr.register(NAME2, this::addToQueue);
        mgr.awaitSleep();
    }

    @Test
    public void testProcessTimer_StopWhileWaiting() throws Exception {
        startThread(mgr);
        mgr.register(NAME1, this::addToQueue);
        mgr.awaitSleep();
        mgr.allowSleep(1);

        mgr.register(NAME2, this::addToQueue);
        mgr.awaitSleep();

        mgr.stop();
        mgr.allowSleep(1);

        assertTrue(waitStop());

        // should have stopped after processing the first timer
        assertEquals(NAME1, results.poll());
        assertNull(results.poll());
    }

    @Test
    public void testProcessTimer_CancelWhileWaiting() throws Exception {
        startThread(mgr);
        Timer timer = mgr.register(NAME1, this::addToQueue);
        mgr.awaitSleep();

        timer.cancel();
        mgr.allowSleep(1);

        mgr.register(NAME2, this::addToQueue);
        mgr.awaitSleep();
        mgr.allowSleep(1);

        mgr.register(NAME1, this::addToQueue);
        mgr.awaitSleep();

        // should have fired timer 2, but not timer 1
        assertEquals(NAME2, results.poll());
        assertNull(results.poll());
    }

    @Test
    public void testProcessTimer_TimerEx() throws Exception {
        startThread(mgr);
        mgr.register(NAME1, name -> {
            throw new RuntimeException(EXPECTED_EXCEPTION);
        });
        mgr.register(NAME2, this::addToQueue);
        mgr.awaitSleep();

        mgr.addTime(1);
        mgr.allowSleep(2);

        mgr.register(NAME3, this::addToQueue);
        mgr.awaitSleep();

        // timer 1 fired but threw an exception, so only timer 2 should be in the queue
        assertEquals(NAME2, results.poll());
    }

    @Test
    public void testTimerAwait() throws Exception {
        startThread(mgr);

        // same times - only need one sleep
        mgr.register(NAME1, this::addToQueue);
        mgr.register(NAME2, this::addToQueue);
        mgr.awaitSleep();

        tcur = mgr.currentTimeMillis();

        // change time
        mgr.addTime(1);
        mgr.allowSleep(1);

        // next one will have a new timeout, so expect to sleep
        mgr.register(NAME3, this::addToQueue);
        mgr.awaitSleep();

        long tcur2 = mgr.currentTimeMillis();
        assertTrue(tcur2 >= tcur + MGR_TIMEOUT_MS);

        assertEquals(NAME1, results.poll());
        assertEquals(NAME2, results.poll());
        assertNull(results.poll());
    }

    @Test
    public void testTimerCancel_WhileWaiting() throws Exception {
        startThread(mgr);

        Timer timer = mgr.register(NAME1, this::addToQueue);
        mgr.awaitSleep();

        // cancel while sleeping
        timer.cancel();

        mgr.register(NAME2, this::addToQueue);

        // allow it to sleep through both timers
        mgr.allowSleep(2);

        // only timer 2 should have fired
        assertEquals(NAME2, results.poll(MAX_WAIT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testTimerCancel_ViaReplace() throws Exception {
        startThread(mgr);

        mgr.register(NAME1, name -> addToQueue("hello"));
        mgr.awaitSleep();

        // replace the timer while the background thread is sleeping
        mgr.register(NAME1, name -> addToQueue("world"));

        // allow it to sleep through both timers
        mgr.allowSleep(2);

        // only timer 2 should have fired
        assertEquals("world", results.poll(MAX_WAIT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testTimerToString() {
        Timer timer = mgr.register(NAME1, this::addToQueue);
        assertNotNull(timer.toString());
    }

    @Test
    public void testCurrentTimeMillis() {
        long tbeg = System.currentTimeMillis();
        long tcur = new TimerManager(MGR_NAME, MGR_TIMEOUT_MS).currentTimeMillis();
        long tend = System.currentTimeMillis();

        assertTrue(tcur >= tbeg);
        assertTrue(tend >= tcur);
    }

    @Test
    public void testSleep() throws Exception {
        long tbeg = System.currentTimeMillis();
        new TimerManager(MGR_NAME, MGR_TIMEOUT_MS).sleep(10);
        long tend = System.currentTimeMillis();

        assertTrue(tend >= tbeg + 10);
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

    private static class MyManager extends TimerManager {
        private AtomicLong curTime = new AtomicLong(1000);
        private LinkedBlockingQueue<Boolean> sleepEntered = new LinkedBlockingQueue<>();
        private LinkedBlockingQueue<Boolean> shouldStop = new LinkedBlockingQueue<>();

        public MyManager(String name, long waitTimeMs) {
            super(name, waitTimeMs);
        }

        /**
         * Adds to the current time.
         *
         * @param timeMs time to add, in milli-seconds
         */
        public void addTime(long timeMs) {
            curTime.addAndGet(timeMs);
        }

        /**
         * Stops the "sleep".
         */
        public void stopSleep() {
            shouldStop.add(true);
        }

        /**
         * Allows the manager to "sleep" several times.
         *
         * @param ntimes the number of times the manager should sleep
         */
        public void allowSleep(int ntimes) {
            for (int x = 0; x < ntimes; ++x) {
                shouldStop.add(false);
            }
        }

        /**
         * Waits for the manager to "sleep".
         *
         * @throws InterruptedException if the thread is interrupted while waiting for the
         *         background thread to sleep
         */
        public void awaitSleep() throws InterruptedException {
            if (sleepEntered.poll(MAX_WAIT_MS, TimeUnit.MILLISECONDS) == null) {
                fail("background thread failed to sleep");
            }
        }

        @Override
        protected long currentTimeMillis() {
            return curTime.get();
        }

        @Override
        protected void sleep(long timeMs) throws InterruptedException {
            sleepEntered.offer(true);

            if (!shouldStop.take()) {
                // test thread did not request that we stop
                curTime.addAndGet(timeMs);
            }
        }
    }
}
