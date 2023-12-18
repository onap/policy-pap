/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
 *  Modification Copyright 2022. Nordix Foundation.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.rest.e2e.End2EndBase;

/**
 * Class to hold/create all parameters for test cases.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
public class CommonTestData {
    public static final String PAP_GROUP_NAME = "PapGroup";

    private static final Coder coder = new StandardCoder();

    public static int dbNum = 0;

    public static void newDb() {
        ++dbNum;
    }

    /**
     * Gets the standard PAP parameters.
     *
     * @param port port to be inserted into the parameters
     * @return the standard PAP parameters
     */
    public PapParameterGroup getPapParameterGroup(int port) {
        try {
            return coder.decode(getPapParameterGroupAsString(port), PapParameterGroup.class);

        } catch (CoderException e) {
            throw new PolicyPapRuntimeException("cannot read PAP parameters", e);
        }
    }

    /**
     * Gets the standard PAP parameters, as a String.
     *
     * @param port port to be inserted into the parameters
     * @return the standard PAP parameters
     */
    public String getPapParameterGroupAsString(int port) {

        try {
            File file = new File(getParamFile());
            String json = Files.readString(file.toPath());

            json = json.replace("${port}", String.valueOf(port));
            json = json.replace("${dbName}", "jdbc:h2:mem:testdb" + dbNum);

            return json;

        } catch (IOException e) {
            throw new PolicyPapRuntimeException("cannot read PAP parameters", e);
        }
    }

    /**
     * Gets the postgres PAP parameters, as a String.
     *
     * @param port port to be inserted into the parameters
     * @return the postgres PAP parameters
     */
    public String getPapPostgresParameterGroupAsString(int port) {

        try {
            String json = new String(Files.readAllBytes(Paths.get(
                    "src/test/resources/parameters/PapConfigParameters_Postgres.json")));

            json = json.replace("${port}", String.valueOf(port));
            json = json.replace("${dbName}", "jdbc:h2:mem:testdb" + dbNum);

            return json;

        } catch (IOException e) {
            throw new PolicyPapRuntimeException("cannot read PAP parameters", e);
        }
    }

    /**
     * Gets the full path to the parameter file, which may vary depending on whether
     * this is an end-to-end test.
     *
     * @return the parameter file name
     */
    private String getParamFile() {
        String paramFile = "src/test/resources/parameters/PapConfigParametersStd.json";

        for (StackTraceElement stack : Thread.currentThread().getStackTrace()) {
            String classnm = stack.getClassName();
            if (End2EndBase.class.getName().equals(classnm)) {
                paramFile = "src/test/resources/e2e/PapConfigParameters.json";
                break;
            }
        }
        return paramFile;
    }

    /**
     * Nulls out a field within a JSON string.
     * @param json JSON string
     * @param field field to be nulled out
     * @return a new JSON string with the field nulled out
     */
    public String nullifyField(String json, String field) {
        return json.replace(field + "\"", field + "\":null, \"" + field + "Xxx\"");
    }
}
