/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLIntegrityConstraintViolationException;
import org.eclipse.persistence.exceptions.EclipseLinkException;
import org.junit.Test;

public class PdpStatusMessageHandlerTest {

    @Test
    public void testIsDuplicateKeyException() {

        // @formatter:off

        // null exception
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(null)).isFalse();

        // plain exception
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new Exception()))
            .isFalse();

        // cause is also plain
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new Exception(
                            new Exception())))
            .isFalse();

        // dup key
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new SQLIntegrityConstraintViolationException()))
            .isTrue();

        // cause is dup key
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new Exception(
                            new SQLIntegrityConstraintViolationException())))
            .isTrue();

        // eclipselink exception, no internal exception
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new MyEclipseLinkException()))
            .isFalse();

        // eclipselink exception, cause is plain
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new MyEclipseLinkException(
                            new Exception())))
            .isFalse();

        // eclipselink exception, cause is dup
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new MyEclipseLinkException(
                            new SQLIntegrityConstraintViolationException())))
            .isTrue();

        // multiple cause both inside and outside of the eclipselink exception
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new Exception(
                            new Exception(
                                new MyEclipseLinkException(
                                    new Exception(
                                        new SQLIntegrityConstraintViolationException()))))))
            .isTrue();

        // @formatter:on
    }

    public static class MyEclipseLinkException extends EclipseLinkException {
        private static final long serialVersionUID = 1L;

        public MyEclipseLinkException() {
            // do nothing
        }

        public MyEclipseLinkException(Exception exception) {
            setInternalException(exception);
        }
    }
}
