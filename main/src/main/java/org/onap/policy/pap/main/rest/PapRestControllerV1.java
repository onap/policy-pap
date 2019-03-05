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

package org.onap.policy.pap.main.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.BasicAuthDefinition;
import io.swagger.annotations.Info;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import java.net.HttpURLConnection;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Version v1 common superclass to provide REST endpoints for PAP component.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@Path("/policy/pap/v1")
@Api(value = "Policy Administration (PAP) API")
@Produces(MediaType.APPLICATION_JSON)
@SwaggerDefinition(
    info = @Info(description =
                    "Policy Administration is responsible for the deployment life cycle of policies as well as "
                    + "interworking with the mechanisms required to orchestrate the nodes and containers on which "
                    + "policies run. It is also responsible for the administration of policies at run time;"
                    + " ensuring that policies are available to users, that policies are executing correctly,"
                    + " and that the state and status of policies is monitored", version = "v1.0",
                    title = "Policy Administration"),
    consumes = {MediaType.APPLICATION_JSON}, produces = {MediaType.APPLICATION_JSON},
    schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
    tags = {@Tag(name = "policy-administration", description = "Policy Administration Service Operations")},
    securityDefinition = @SecurityDefinition(
                    basicAuthDefinitions = {@BasicAuthDefinition(key = "basicAuth")}))
public class PapRestControllerV1 {

    public static final String AUTHORIZATION_TYPE = "basicAuth";

    public static final int AUTHENTICATION_ERROR_CODE = HttpURLConnection.HTTP_UNAUTHORIZED;
    public static final int AUTHORIZATION_ERROR_CODE = HttpURLConnection.HTTP_FORBIDDEN;
    public static final int SERVER_ERROR_CODE = HttpURLConnection.HTTP_INTERNAL_ERROR;

    public static final String AUTHENTICATION_ERROR_MESSAGE = "Authentication Error";
    public static final String AUTHORIZATION_ERROR_MESSAGE = "Authorization Error";
    public static final String SERVER_ERROR_MESSAGE = "Internal Server Error";
}
