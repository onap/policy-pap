/*
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2021 Nordix Foundation.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.UUID;
import javax.ws.rs.core.SecurityContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;

public class PapRestControllerV1Test {

    @Mock
    SecurityContext mockSecurityContext;

    @InjectMocks
    PapRestControllerV1 mockController;

    private AutoCloseable closeable;
    private BodyBuilder bldr;

    @Before
    public void setUp() {
        bldr = ResponseEntity.ok();
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void after() throws Exception {
        closeable.close();
    }

    @Test
    public void testAddVersionControlHeaders() {
        ResponseEntity<Object> resp = mockController.addVersionControlHeaders(bldr).build();
        assertEquals("0", resp.getHeaders().get(PapRestControllerV1.VERSION_MINOR_NAME).get(0));
        assertEquals("0", resp.getHeaders().get(PapRestControllerV1.VERSION_PATCH_NAME).get(0));
        assertEquals("1.0.0", resp.getHeaders().get(PapRestControllerV1.VERSION_LATEST_NAME).get(0));
    }

    @Test
    public void testAddLoggingHeaders_Null() {
        ResponseEntity<Object> resp = mockController.addLoggingHeaders(bldr, null).build();
        assertNotNull(resp.getHeaders().get(PapRestControllerV1.REQUEST_ID_NAME));
    }

    @Test
    public void testAddLoggingHeaders_NonNull() {
        String uuid = UUID.randomUUID().toString();
        ResponseEntity<Object> resp = mockController.addLoggingHeaders(bldr, uuid).build();
        assertEquals(uuid.toString(), resp.getHeaders().get(PapRestControllerV1.REQUEST_ID_NAME).get(0));
    }

    @Test
    public void testGetPrincipal() {
        assertThat(new PapRestControllerV1().getPrincipal()).isEmpty();

        Principal mockUser = mock(Principal.class, new Returns("myFakeUser"));
        when(mockSecurityContext.getUserPrincipal()).thenReturn(mockUser);

        assertEquals("myFakeUser", mockController.getPrincipal());
    }
}
