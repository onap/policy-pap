/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.pap.main.startstop;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.service.PdpGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@ActiveProfiles("test-db")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestPapDatabaseInitializer {

    @Autowired
    private PapDatabaseInitializer papDatabaseInitializer;

    @Autowired
    private PdpGroupService pdpGroupService;

    @BeforeAll
    static void before() {
        Registry.newRegistry();
    }

    @Order(1)
    @Test
    void testDatabaseCreated() {
        assertNotNull(pdpGroupService.getPdpGroups());

        assertDoesNotThrow(() -> papDatabaseInitializer.loadData());
    }

    @Order(2)
    @Test
    void testDatabase_Exception() {
        ReflectionTestUtils.setField(papDatabaseInitializer, "groupConfigFile", "invalid.json");
        assertThrows(PolicyPapException.class, () -> papDatabaseInitializer.loadData());


        ReflectionTestUtils.setField(papDatabaseInitializer, "groupConfigFile", "simpleDeploy/emptyGroups.json");
        assertThrows(PolicyPapException.class, () -> papDatabaseInitializer.loadData());

        ReflectionTestUtils.setField(papDatabaseInitializer, "groupConfigFile",
            "simpleDeploy/createGroupDuplicateSubGroups.json");
        assertThrows(PolicyPapException.class, () -> papDatabaseInitializer.loadData());
    }
}
