/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
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

package org.onap.policy.pap.service.nexus;

import com.google.gson.Gson;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import org.onap.policy.pap.service.nexus.pojo.NexusArtifact;
import org.onap.policy.pap.service.nexus.pojo.NexusRepository;
import org.onap.policy.pap.service.nexus.pojo.NexusSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class NexusRestWrapper provides a Java API to a Nexus repository, wrapping the Nexus REST
 * interface.
 */
public class NexusRestWrapper {

    private static final Logger logger = LoggerFactory.getLogger(NexusRestWrapper.class);

    // A web client for issuing REST requests to the Nexus server
    private final Client client;

    // The URL of the Nexus server
    private final String nexusServerUrl;

    // Credentials for Nexus server logins
    private String nexusUser;
    private String nexusPassword;

    /**
     * Instantiates a new Nexus REST agent.
     *
     * @param nexusServerUrl the URL of the Nexus server as a string
     * @throws NexusRestWrapperException on errors on the Nexus server URL
     */
    public NexusRestWrapper(final String nexusServerUrl) throws NexusRestWrapperException {
        if (isNullOrBlank(nexusServerUrl)) {
            throw new NexusRestWrapperException("nexusServerUrl must be specified for the Nexus server");
        }

        this.nexusServerUrl = nexusServerUrl;

        // Create a client for RST calls towards the Nexus server
        client = ClientBuilder.newClient();
    }

    /**
     * Instantiates a new Nexus REST agent with credentials.
     *
     * @param nexusServerUrl the URL of the Nexus server as a string
     * @param nexusUser the Nexus userid
     * @param nexusPassword the Nexus password
     * @throws NexusRestWrapperException on parameter exceptions
     */
    public NexusRestWrapper(final String nexusServerUrl, final String nexusUser, final String nexusPassword)
            throws NexusRestWrapperException {

        if (isNullOrBlank(nexusServerUrl)) {
            throw new NexusRestWrapperException("nexusServerUrl must be specified for the Nexus server");
        }

        if (isNullOrBlank(nexusUser) || isNullOrBlank(nexusPassword)) {
            throw new NexusRestWrapperException("nexuusUser and nexusPassword must both be specified");
        }

        this.nexusServerUrl = nexusServerUrl;
        this.nexusUser = nexusUser;
        this.nexusPassword = nexusPassword;

        // Create a client for RST calls towards the Nexus server
        client = ClientBuilder.newClient();

    }

    /**
     * Close the REST client.
     */
    public void close() {

        // Close the web client
        client.close();

    }

    /**
     * Find an artifact in the Nexus server.
     *
     * @param searchParameters the search parameters to use for the search
     * @return the list of artifacts found that match the requested artifact
     * @throws NexusRestWrapperException Exceptions accessing the Nexus server
     */
    public NexusSearchResult findArtifact(final NexusRestSearchParameters searchParameters)
            throws NexusRestWrapperException {

        // Issue the REST request to perform the search
        URI searchUri = searchParameters.getSearchUri(nexusServerUrl);

        logger.info("Performing Nexus search, URI: {}", searchUri);

        // Compose the REST request
        Builder requestBuilder = client.target(searchUri).request("application/json");
        getAuthorizationHeader(requestBuilder);

        // Issue the REST request
        Response response = requestBuilder.get();

        logger.info("Search response is: {} reason: {}", response.getStatus(),
                response.getStatusInfo().getReasonPhrase());

        // Check the HTTP response code for the search
        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
            logger.error("PAP: search to URI {} failed, response was: {}", searchUri, response);
            throw new NexusRestWrapperException("query to Nexus failed with message: " + response.toString());
        }

        try {
            // Get the JSON string with the the search result
            String responseString = response.readEntity(String.class);

            // Parse the returned JSON into result POJOs
            NexusSearchResult searchResult = new Gson().fromJson(responseString, NexusSearchResult.class);

            // We now need to expand the release and snapshot URL paths for each artifact
            expandArtifactUrlPaths(searchResult);

            return searchResult;
        } catch (Exception e) {
            logger.error("PAP: processing of result from search to URI {}  failed with message {}", searchUri,
                    e.getMessage());
            throw new NexusRestWrapperException(
                    "processing of result from query to Nexus failed with message: " + e.getMessage(), e);
        }
    }

    /**
     * Get the authorisation header for the user name and password.
     * 
     * @param requestBuilder the request builder to add authorization to
     * @return the authorisation header
     */
    private Builder getAuthorizationHeader(Builder requestBuilder) {
        if (null != nexusUser && null != nexusPassword) {
            String userPassString = nexusUser + ":" + nexusPassword;
            requestBuilder.header("Authorization",
                    "Basic " + java.util.Base64.getEncoder().encodeToString(userPassString.getBytes()));
        }

        return requestBuilder;
    }

    /**
     * Use the Repository URLs in the search result to create a release and snapshot URL path for
     * each artifact.
     * 
     * @param searchResult the results of a Nexus server search
     */
    private void expandArtifactUrlPaths(NexusSearchResult searchResult) {
        // Create a map of repositories for doing lookups
        Map<String, NexusRepository> repositoryMap = new HashMap<>();

        for (NexusRepository repository : searchResult.getRepoDetailsList()) {
            repositoryMap.put(repository.getRepositoryId(), repository);
        }

        for (NexusArtifact artifact : searchResult.getArtifactList()) {
            artifact.setUrlPath(composeArtifactUrlPath(repositoryMap, artifact));
        }
    }

    /**
     * Compose an artifact URL path using the repository and artifact details for the artifact.
     * 
     * @param repositoryMap the available repositories
     * @param artifact the artifact
     * @return the URL path
     */
    private String composeArtifactUrlPath(Map<String, NexusRepository> repositoryMap, NexusArtifact artifact) {
        // We always have one hit
        NexusRepository repository = repositoryMap.get(artifact.getArtifactHits().get(0).getRepositoryId());

        return new StringBuilder().append(repository.getRepositoryUrl()).append("/content/")
                .append(artifact.getGroupId().replace('.', '/')).append('/').append(artifact.getArtifactId())
                .append('/').append(artifact.getVersion()).append('/').append(artifact.getArtifactId()).append('-')
                .append(artifact.getVersion()).toString();
    }

    /**
     * Check if a string is null or all white space.
     */
    private boolean isNullOrBlank(final String parameter) {
        return null == parameter || parameter.trim().isEmpty();
    }
}

