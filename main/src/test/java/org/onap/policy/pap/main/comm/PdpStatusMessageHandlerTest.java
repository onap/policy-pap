/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023, 2025 OpenInfra Foundation Europe. All rights reserved.
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

import java.io.Serial;
import java.sql.SQLIntegrityConstraintViolationException;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PolicyPapApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = PolicyPapApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"db.initialize=false"})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PdpStatusMessageHandlerTest {

    @BeforeAll
    static void setupClass() {
        Registry.newRegistry();
    }

    @Test
    void testIsDuplicateKeyException() {

        // @formatter:off

        // null exception
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(null, HibernateException.class)).isFalse();

        // plain exception
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new Exception(), HibernateException.class))
            .isFalse();

        // cause is also plain
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new Exception(
                            new Exception()), HibernateException.class))
            .isFalse();

        // dup key
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new SQLIntegrityConstraintViolationException(), HibernateException.class))
            .isTrue();

        // cause is dup key
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new Exception(
                            new SQLIntegrityConstraintViolationException()), HibernateException.class))
            .isTrue();

        // eclipselink exception, no internal exception
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new MyHibernateException(), HibernateException.class))
            .isFalse();

        // eclipselink exception, cause is plain
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new MyHibernateException(
                            new Exception()), HibernateException.class))
            .isFalse();

        // eclipselink exception, cause is dup
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new MyHibernateException(
                            new SQLIntegrityConstraintViolationException()), HibernateException.class))
            .isTrue();

        // multiple cause both inside and outside the eclipselink exception
        assertThat(PdpStatusMessageHandler.isDuplicateKeyException(
                        new Exception(
                            new Exception(
                                new MyHibernateException(
                                    new Exception(
                                        new SQLIntegrityConstraintViolationException())))), HibernateException.class))
            .isTrue();

        // @formatter:on
    }

    public static class MyHibernateException extends HibernateException {
        @Serial
        private static final long serialVersionUID = 1L;

        public MyHibernateException() {
            super("");
        }

        public MyHibernateException(Exception exception) {
            super(exception);
        }
    }
}
