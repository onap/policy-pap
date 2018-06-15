/*-
 * ============LICENSE_START=======================================================
 * pap-service
 * ================================================================================
 * Copyright (C) 2018 Ericsson Intellectual Property. All rights reserved.
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

package org.onap.policy.pap.service.nexus.pojo;

import java.util.List;

/**
 * The Class NexusArtifactHit is a POJO that holds information on the occurrences of an Artifact in
 * a Maven repository. It is populated from the JSON response to a query on the repository. See:
 * {@linktourl https://repository.sonatype.org/nexus-indexer-lucene-plugin/default/docs/path__lucene_search.html}
 */
public class NexusArtifactHit {
    private List<NexusArtifactLink> artifactLinks;
    private String repositoryId;

    public List<NexusArtifactLink> getArtifactLinks() {
        return artifactLinks;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    @Override
    public String toString() {
        return "NexusArtifactHit [artifactLinks=" + artifactLinks + ", repositoryId=" + repositoryId
                + "]";
    }
}
