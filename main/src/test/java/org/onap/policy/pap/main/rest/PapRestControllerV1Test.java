/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021, 2023, 2025 OpenInfra Foundation Europe. All rights reserved.
 * Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.ws.rs.core.SecurityContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;

class PapRestControllerV1Test {

    @Mock
    SecurityContext mockSecurityContext;

    private AutoCloseable closeable;
    private BodyBuilder bldr;

    @BeforeEach
    void setUp() {
        bldr = ResponseEntity.ok();
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void after() throws Exception {
        closeable.close();
    }

    @Test
    void testAddVersionControlHeaders() {
        ResponseEntity<Object> resp = PapRestControllerV1.addVersionControlHeaders(bldr).build();
        assertEquals("0", resp.getHeaders().get(PapRestControllerV1.VERSION_MINOR_NAME).get(0));
        assertEquals("0", resp.getHeaders().get(PapRestControllerV1.VERSION_PATCH_NAME).get(0));
        assertEquals("1.0.0", resp.getHeaders().get(PapRestControllerV1.VERSION_LATEST_NAME).get(0));
    }

    @Test
    void testAddLoggingHeaders_Null() {
        ResponseEntity<Object> resp = PapRestControllerV1.addLoggingHeaders(bldr, null).build();
        assertNotNull(resp.getHeaders().get(PapRestControllerV1.REQUEST_ID_NAME));
    }

    @Test
    void testAddLoggingHeaders_NonNull() {
        UUID uuid = UUID.randomUUID();
        ResponseEntity<Object> resp = PapRestControllerV1.addLoggingHeaders(bldr, uuid).build();
        assertEquals(uuid.toString(), resp.getHeaders().get(PapRestControllerV1.REQUEST_ID_NAME).get(0));
    }

    @Test
    void testGetPrincipal() {
        assertThat(new PapRestControllerV1().getPrincipal()).isEmpty();
    }
}
