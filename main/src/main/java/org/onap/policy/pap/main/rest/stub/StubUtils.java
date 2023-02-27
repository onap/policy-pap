/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.pap.main.rest.stub;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.pap.main.rest.PapRestControllerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Profile("stub")
class StubUtils {
    private static final Logger log = LoggerFactory.getLogger(StubUtils.class);
    private static final String APPLICATION_JSON = "application/json";
    private static final String SERIALIZE_RESPONSE_FAILURE_MSG =
            "Couldn't serialize response for content type application/json";
    private final HttpServletRequest request;
    private static final String ACCEPT = "Accept";
    private static final String PAP_DB =
            "/PapDb.json";
    private static final Gson JSON_TRANSLATOR = new Gson();

    <T> ResponseEntity<T> getStubbedResponse(Class<T> clazz) {
        var accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            final var resource = new ClassPathResource(PAP_DB);
            try (var inputStream = resource.getInputStream()) {
                final var string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                var targetObject = JSON_TRANSLATOR.fromJson(string, clazz);
                return new ResponseEntity<>(targetObject, HttpStatus.OK);
            } catch (IOException e) {
                log.error(SERIALIZE_RESPONSE_FAILURE_MSG, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    <T> ResponseEntity<List<T>> getStubbedResponseList(Class<T> clazz) {
        var accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            final var resource = new ClassPathResource(PAP_DB);
            try (var inputStream = resource.getInputStream()) {
                final var string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                var targetObject = Arrays.asList(JSON_TRANSLATOR.fromJson(string, clazz));
                return new ResponseEntity<>(targetObject, HttpStatus.OK);
            } catch (IOException e) {
                log.error(SERIALIZE_RESPONSE_FAILURE_MSG, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    ResponseEntity<Map<String, Object>> getStubbedResponseMap() {
        var accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            final var resource = new ClassPathResource(PAP_DB);
            try (var inputStream = resource.getInputStream()) {
                Map<String, Object> map = new HashMap<>();
                final var string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                map.put(PapRestControllerV1.API_VERSION_NAME,
                        JSON_TRANSLATOR.fromJson(string, Object.class));
                return new ResponseEntity<>(map, HttpStatus.OK);
            } catch (IOException e) {
                log.error(SERIALIZE_RESPONSE_FAILURE_MSG, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    ResponseEntity<Map<String, Map<String, List<PdpStatistics>>>> getStubbedResponseStatistics() {
        var accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            Map<String, Map<String, List<PdpStatistics>>> map = new HashMap<>();
            return new ResponseEntity<>(map, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}
