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

import lombok.Getter;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.annotations.Min;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;

/**
 * Class to hold all parameters needed for pap rest server.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@NotNull
@NotBlank
@Getter
public class RestServerParameters extends ParameterGroupImpl {
    private String host;

    @Min(value = 1)
    private int port;

    private String userName;
    private String password;
    private boolean https;
    private boolean aaf;

    public RestServerParameters() {
        super("RestServerParameters");
    }

    /**
     * Constructor for instantiating RestServerParameters.
     *
     * @param host the host name
     * @param port the port
     * @param userName the user name
     * @param password the password
     * @param https the https flag
     * @param aaf the aaf flag
     */
    public RestServerParameters(final String host, final int port, final String userName, final String password,
            final boolean https, final boolean aaf) {
        super("RestServerParameters");
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.https = https;
        this.aaf = aaf;
    }
}
