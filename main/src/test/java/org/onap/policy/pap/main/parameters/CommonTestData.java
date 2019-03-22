/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property.
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

package org.onap.policy.pap.main.parameters;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.onap.policy.common.parameters.ParameterGroup;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * Class to hold/create all parameters for test cases.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class CommonTestData {

    private static final String REST_SERVER_PASSWORD = "zb!XztG34";
    private static final String REST_SERVER_USER = "healthcheck";
    private static final int REST_SERVER_PORT = 6969;
    private static final String REST_SERVER_HOST = "0.0.0.0";
    private static final boolean REST_SERVER_HTTPS = true;
    private static final boolean REST_SERVER_AAF = false;
    public static final String PAP_GROUP_NAME = "PapGroup";

    private static final Coder coder = new StandardCoder();

    /**
     * Converts the contents of a map to a parameter class.
     *
     * @param source property map
     * @param clazz class of object to be created from the map
     * @return a new object represented by the map
     */
    public <T extends ParameterGroup> T toObject(Map<String, Object> source, Class<T> clazz) {
        try {
            return coder.decode(coder.encode(source), clazz);

        } catch (CoderException e) {
            throw new RuntimeException("cannot create " + clazz.getName() + " from map", e);
        }
    }

    /**
     * Returns a property map for a PapParameterGroup map for test cases.
     * @param name name of the parameters
     *
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getPapParameterGroupMap(String name) {
        Map<String,Object> map = new TreeMap<>();

        map.put("name", name);
        map.put("restServerParameters", getRestServerParametersMap(false));
        map.put("pdpParameters", getPdpParametersMap());

        return map;
    }

    /**
     * Returns a property map for a RestServerParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public Map<String,Object> getRestServerParametersMap(final boolean isEmpty) {
        Map<String,Object> map = new TreeMap<>();
        map.put("https", REST_SERVER_HTTPS);
        map.put("aaf", REST_SERVER_AAF);

        if (!isEmpty) {
            map.put("host", REST_SERVER_HOST);
            map.put("port", REST_SERVER_PORT);
            map.put("userName", REST_SERVER_USER);
            map.put("password", REST_SERVER_PASSWORD);
        }

        return map;
    }

    /**
     * Returns a property map for a PdpParameters map for test cases.
     * @return a property map suitable for constructing an object
     */
    public Map<String,Object> getPdpParametersMap() {
        Map<String,Object> map = new TreeMap<>();

        map.put("name", "PdpParameters");
        map.put("updateParameters", getPdpUpdateParametersMap());
        map.put("stateChangeParameters", getPdpStateChangeParametersMap());

        return map;
    }

    /**
     * Returns a property map for a PdpUpdateParameters map for test cases.
     * @return a property map suitable for constructing an object
     */
    public Map<String,Object> getPdpUpdateParametersMap() {
        Map<String, Object> map = getPdpRequestParametersMap();

        map.put("name", "PdpUpdateParameters");

        return map;
    }

    /**
     * Returns a property map for a PdpStateChangeParameters map for test cases.
     * @return a property map suitable for constructing an object
     */
    public Map<String,Object> getPdpStateChangeParametersMap() {
        Map<String, Object> map = getPdpRequestParametersMap();

        map.put("name", "PdpStateChangeParameters");

        return map;
    }

    /**
     * Returns a property map for a PdpParameters map for test cases.
     * @return a property map suitable for constructing an object
     */
    public Map<String,Object> getPdpRequestParametersMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("maxRetryCount", "0");
        map.put("maxWaitMs", "0");

        return map;
    }

    /**
     * Returns a property map for a PdpGroupDeploymentParameters map for test cases.
     *
     * @return a property map suitable for constructing an object
     */
    public Map<String,Object> getPdpGroupDeploymentParametersMap() {
        Map<String,Object> map = new TreeMap<>();
        map.put("waitResponseMs", "1");

        return map;
    }
}
