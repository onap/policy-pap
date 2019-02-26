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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * JSON encoder and decoder.
 */
public class Coder {

    /**
     * Gson object used to encode and decode messages.
     */
    private static final Gson GSON = new Gson();

    /**
     * Constructs the object.
     */
    public Coder() {
        super();
    }

    /**
     * Encodes an object into json.
     *
     * @param object object to be encoded
     * @return a json string representing the object
     * @throws CoderException if an error occurs
     */
    public String encode(Object object) throws CoderException {
        try {
            return toJson(object);

        } catch (JsonParseException e) {
            throw new CoderException(e);
        }
    }

    /**
     * Decodes a json string into an object.
     *
     * @param json json string to be decoded
     * @param clazz class of object to be decoded
     * @return the object represented by the given json string
     * @throws CoderException if an error occurs
     */
    public <T> T decode(String json, Class<T> clazz) throws CoderException {
        try {
            return fromJson(json, clazz);

        } catch (JsonParseException e) {
            throw new CoderException(e);
        }
    }

    // the remaining methods are wrappers that can be overridden by junit tests

    /**
     * Encodes an object into json, without catching exception.
     *
     * @param object object to be encoded
     * @return a json string representing the object
     */
    protected String toJson(Object object) {
        return GSON.toJson(object);
    }

    /**
     * Decodes a json string into an object, without catching exception.
     *
     * @param json json string to be decoded
     * @param clazz class of object to be decoded
     * @return the object represented by the given json string
     */
    protected <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
}
