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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager of timers. All of the timers for a given manager have the same wait time, which
 * makes it possible to use a linked hash map to track the timers. As a result, timers can
 * be quickly added and removed. In addition, the expiration time of any new timers is
 * always greater than or equal to the timers that are already in the map. Consequently,
 * the map's iterator will go in ascending order from the minimum expiration time to
 * maximum expiration time.
 */
public class TimerManager implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TimerManager.class);

    /**
     * Name of this manager, used for logging purposes.
     */
    private final String name;

    /**
     * Time that each new timer should wait.
     */
    private final long waitTimeMs;

    /**
     * When the map is empty, the timer thread will block waiting for this semaphore. When
     * a new timer is added to the map, the semaphore will be released, thus allowing the
     * timer thread to progress.
     */
    private final Semaphore sem = new Semaphore(0);

    /**
     * This is decremented to indicate that this manager should be stopped.
     */
    private final CountDownLatch stopper = new CountDownLatch(1);

    /**
     * Used to lock updates to the map.
     */
    private final Object lockit = new Object();

    /**
     * Maps a timer name to a timer.
     */
    private final Map<String, Timer> name2timer = new LinkedHashMap<>();

    /**
     * Constructs the object.
     *
     * @param name name of this manager, used for logging purposes
     * @param waitTimeMs time that each new timer should wait
     */
    public TimerManager(String name, long waitTimeMs) {
        this.name = name;
        this.waitTimeMs = waitTimeMs;
    }

    /**
     * Stops the timer thread.
     */
    public void stop() {
        logger.info("timer manager {} stopping", name);

        // must decrement the latch BEFORE releasing the semaphore
        stopper.countDown();
        sem.release();
    }

    /**
     * Registers a timer with the given name. When the timer expires, it is automatically
     * unregistered and then executed.
     *
     * @param name name of the timer to register
     * @param runner action to run when the timer expires
     * @return the timer
     */
    public Timer register(String name, Runnable runner) {

        synchronized (lockit) {
            Timer timer = new Timer(name, runner);

            // always remove existing entry so that new entry goes at the end of the map
            name2timer.remove(name);
            name2timer.put(name, timer);

            if (name2timer.size() == 1) {
                // release the timer thread
                sem.release();
            }

            logger.info("{} timer registered {}", TimerManager.this.name, timer);
            return timer;
        }
    }

    /**
     * Continuously processes timers until {@link #stop()} is invoked.
     */
    @Override
    public void run() {
        logger.info("timer manager {} started", name);

        while (stopper.getCount() > 0) {

            try {
                sem.acquire();
                sem.drainPermits();

                processTimers();

            } catch (InterruptedException e) {
                logger.warn("timer manager {} stopping due to interrupt", name);
                stopper.countDown();
                Thread.currentThread().interrupt();
            }
        }

        logger.info("timer manager {} stopped", name);
    }

    /**
     * Process all timers, continuously, as long as a timer remains in the map (and
     * {@link #stop()} has not been called).
     *
     * @throws InterruptedException if the thread is interrupted
     */
    private void processTimers() throws InterruptedException {
        Timer timer;
        while ((timer = getNextTimer()) != null && stopper.getCount() > 0) {
            processTimer(timer);
        }
    }

    /**
     * Gets the timer that will expire first.
     *
     * @return the timer that will expire first, or {@code null} if there are no timers
     */
    private Timer getNextTimer() {

        synchronized (lockit) {
            if (name2timer.isEmpty()) {
                return null;
            }

            // use an iterator to get the first timer in the map
            return name2timer.values().iterator().next();
        }
    }

    /**
     * Process a timer, waiting until it expires, unregistering it, and then executing its
     * action.
     *
     * @param timer timer to process
     * @throws InterruptedException if the thread is interrupted
     */
    private void processTimer(Timer timer) throws InterruptedException {
        // wait for it to expire, if necessary
        long tleft = timer.expireMs - System.currentTimeMillis();
        if (tleft > 0) {
            if (stopper.await(waitTimeMs, TimeUnit.MILLISECONDS)) {
                // stop() was called
                return;
            }
        }

        synchronized (lockit) {
            if (name2timer.get(name) != timer) {
                // timer was cancelled while we were waiting
                return;
            }

            // remove the timer from the map
            if (!timer.cancel()) {
                return;
            }
        }


        // run the timer
        try {
            logger.info("{} timer expired {}", TimerManager.this.name, timer);
            timer.runner.run();
        } catch (RuntimeException e) {
            logger.warn("{} timer threw an exception {}", TimerManager.this.name, timer, e);
        }
    }

    /**
     * Timer info.
     */
    public class Timer {
        /**
         * The timer's name.
         */
        private String name;

        /**
         * Time, in milliseconds, when the timer will expire.
         */
        private long expireMs;

        /**
         * Action to take when the timer expires.
         */
        private Runnable runner;


        private Timer(String name, Runnable runner) {
            this.name = name;
            this.expireMs = waitTimeMs + System.currentTimeMillis();
            this.runner = runner;
        }

        /**
         * Cancels the timer.
         *
         * @return {@code true} if the timer was cancelled, {@code false} if the timer was
         *         not running
         */
        public boolean cancel() {

            synchronized (lockit) {
                Timer original = name2timer.remove(name);
                if (original != this) {
                    // timer is no longer in the map - don't run it; put the old one back
                    logger.info("{} timer discarded {}", TimerManager.this.name, this);

                    if (original != null) {
                        name2timer.put(name, original);
                    }

                    return false;
                }

                logger.debug("{} timer cancelled {}", TimerManager.this.name, this);
                name2timer.remove(name);
                return true;
            }
        }

        @Override
        public String toString() {
            return "Timer [name=" + name + ", expireMs=" + expireMs + "]";
        }
    }
}
