/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.pap.client.monitoring.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.junit.Test;

/**
 * Test the periodic event manager utility.
 */
public class MonitoringRestMainTest {
    @Test
    public void testMonitoringClientBad() {
        try {
            final String[] eventArgs =
                { "-z" };

            PapMonitoringRestMain.main(eventArgs);
        } catch (Exception exc) {
            fail("test should not throw an exception");
        }
    }

    @Test
    public void testMonitoringClientOk() {
        try {
            final String[] eventArgs =
                { "-t", "1" };

            PapMonitoringRestMain.main(eventArgs);
        } catch (Exception exc) {
            fail("test should not throw an exception");
        }
    }

    @Test
    public void testMonitoringClientNoOptions() {
        final String[] eventArgs = new String[]
            {};

        final String outputString = testPapMonitoringRestMainConstructor(eventArgs);

        System.err.println(outputString);
        assertEquals("*** StdOut ***\n\n*** StdErr ***\n", outputString);
    }

    @Test
    public void testMonitoringClientBadOptions() {
        final String[] eventArgs =
            { "-zabbu" };

        try {
            new PapMonitoringRestMain(eventArgs, System.out);
            fail("test should throw an exception");
        } catch (Exception ex) {
            assertEquals("Pap Monitoring REST endpoint (PapMonitoringRestMain: Config=[null], State=STOPPED) "
                            + "parameter error, invalid command line arguments specified "
                            + ": Unrecognized option: -zabbu", ex.getMessage().substring(0, 170));
        }
    }

    @Test
    public void testMonitoringClientHelp() {
        final String[] eventArgs =
            { "-h" };

        try {
            new PapMonitoringRestMain(eventArgs, System.out);
            fail("test should throw an exception");
        } catch (Exception ex) {
            assertEquals("usage: org.onap.policy.pap.client.monitoring.rest.PapMonitoringRestMain [options...]",
                    ex.getMessage().substring(0, 84));
        }
    }

    @Test
    public void testMonitoringClientPortBad() {
        final String[] eventArgs =
            { "-p", "hello" };

        try {
            new PapMonitoringRestMain(eventArgs, System.out);
            fail("test should throw an exception");
        } catch (Exception ex) {
            assertEquals("Pap Monitoring REST endpoint (PapMonitoringRestMain: Config=[null], State=STOPPED) "
                            + "parameter error, error parsing argument \"port\" :For input string: \"hello\"",
                            ex.getMessage().substring(0, 156));
        }
    }

    @Test
    public void testMonitoringClientPortNegative() {
        final String[] eventArgs =
            { "-p", "-1" };

        try {
            new PapMonitoringRestMain(eventArgs, System.out);
            fail("test should throw an exception");
        } catch (Exception ex) {
            assertEquals("Pap Monitoring REST endpoint (PapMonitoringRestMain: Config=[PapMonitoringRestParameters: "
                    + "URI=http://localhost:-1/papservices/, TTL=-1sec], State=STOPPED) parameters invalid, "
                    + "port must be greater than 1023 and less than 65536", ex.getMessage().substring(0, 225));
        }
    }

    @Test
    public void testMonitoringClientTtlTooSmall() {
        final String[] eventArgs =
            { "-t", "-2" };

        try {
            new PapMonitoringRestMain(eventArgs, System.out);
            fail("test should throw an exception");
        } catch (Exception ex) {
            assertEquals("Pap Monitoring REST endpoint (PapMonitoringRestMain: Config=[PapMonitoringRestParameters: "
                    + "URI=http://localhost:18999/papservices/, TTL=-2sec], State=STOPPED) parameters invalid, "
                            + "time to live must be greater than -1 (set to -1 to wait forever)",
                    ex.getMessage().substring(0, 242));
        }
    }

    @Test
    public void testMonitoringClientTooManyPars() {
        final String[] eventArgs =
            { "-t", "10", "-p", "12344", "aaa", "bbb" };

        try {
            new PapMonitoringRestMain(eventArgs, System.out);
            fail("test should throw an exception");
        } catch (Exception ex) {
            assertEquals("Pap Monitoring REST endpoint (PapMonitoringRestMain: Config=[null], State=STOPPED) "
                            + "parameter error, too many command line arguments specified : [aaa, bbb]",
                            ex.getMessage().substring(0, 154));
        }
    }

    @Test
    public void testMonitoringClientTtlNotNumber() {
        final String[] eventArgs =
            { "-t", "timetolive" };

        try {
            new PapMonitoringRestMain(eventArgs, System.out);
            fail("test should throw an exception");
        } catch (Exception ex) {
            assertEquals("Pap Monitoring REST endpoint (PapMonitoringRestMain: Config=[null], State=STOPPED) "
                            + "parameter error, error parsing argument \"time-to-live\" :"
                            + "For input string: \"timetolive\"", ex.getMessage().substring(0, 169));
        }
    }

    @Test
    public void testMonitoringClientPortTooBig() {
        final String[] eventArgs =
            { "-p", "65536" };

        try {
            new PapMonitoringRestMain(eventArgs, System.out);
            fail("test should throw an exception");
        } catch (Exception ex) {
            assertEquals("Pap Monitoring REST endpoint (PapMonitoringRestMain: Config=[PapMonitoringRestParameters: "
                    + "URI=http://localhost:65536/papservices/, TTL=-1sec], State=STOPPED) parameters invalid, "
                    + "port must be greater than 1023 and less than 65536", ex.getMessage().substring(0, 228));
        }
    }

    @Test
    public void testMonitoringClientDefaultPars() {
        try {
            PapMonitoringRest monRest = new PapMonitoringRest();
            monRest.shutdown();

        } catch (Exception ex) {
            fail("test should not throw an exception");
        }
    }

    @Test
    public void testMonitoringOneSecStart() {
        final String[] eventArgs =
            { "-t", "1" };

        try {
            PapMonitoringRestMain monRestMain = new PapMonitoringRestMain(eventArgs, System.out);
            monRestMain.init();
            monRestMain.shutdown();

        } catch (Exception ex) {
            fail("test should not throw an exception");
        }
    }

    @Test
    public void testMonitoringForeverStart() {
        final String[] eventArgs =
            { "-t", "-1" };

        PapMonitoringRestMain monRestMain = new PapMonitoringRestMain(eventArgs, System.out);

        Thread monThread = new Thread() {
            @Override
            public void run() {
                monRestMain.init();
            }
        };

        try {
            monThread.start();
            Thread.sleep(2000);
            monRestMain.shutdown();
        } catch (Exception ex) {
            fail("test should not throw an exception");
        }
    }

    /**
     * Run the application.
     *
     * @param eventArgs the command arguments
     * @return a string containing the command output
     */
    private String testPapMonitoringRestMainConstructor(final String[] eventArgs) {
        final ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
        final ByteArrayOutputStream baosErr = new ByteArrayOutputStream();

        new PapMonitoringRestMain(eventArgs, new PrintStream(baosOut, true));

        InputStream testInput = new ByteArrayInputStream("Test Data for Input to WS".getBytes());
        System.setIn(testInput);

        String outString = baosOut.toString();
        String errString = baosErr.toString();

        return "*** StdOut ***\n" + outString + "\n*** StdErr ***\n" + errString;
    }
}
