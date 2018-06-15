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

package org.onap.policy.pap.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.onap.policy.pap.pdpclient.ApiClient;
import org.onap.policy.pap.pdpclient.ApiException;
import org.onap.policy.pap.pdpclient.PdpClient;
import org.onap.policy.pap.pdpclient.model.Policy;
import org.onap.policy.pap.service.dao.PolicyDbDao;
import org.onap.policy.pap.service.jpa.PolicyEntity;
import org.onap.policy.pap.service.jpa.PolicySetToPolicyEntity;
import org.onap.policy.pap.service.nexus.NexusRestSearchParameters;
import org.onap.policy.pap.service.nexus.NexusRestWrapper;
import org.onap.policy.pap.service.nexus.NexusRestWrapperException;
import org.onap.policy.pap.service.nexus.pojo.NexusArtifact;
import org.onap.policy.pap.service.nexus.pojo.NexusSearchResult;
import org.onap.policy.pap.service.plugin.DummyPapPlugin;
import org.onap.policy.pap.service.rest.model.PdpStateEnum;
import org.onap.policy.pap.service.rest.model.PdpStatusParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PapService {

    private static Logger logger = LoggerFactory.getLogger(PapService.class);
    private Properties properties = new Properties();

    @Autowired
    PolicyDbDao policyDbDao;

    /**
     * Instantiate the PAP.
     */
    public PapService() {
        try (InputStream input = new FileInputStream("src/main/resources/pap.properties")) {
            properties.load(input);
        } catch (Exception e) {
            logger.error("Exception Occured while loading properties file" + e);
        }
    }

    /**
     * Handle a PDP status report.
     * 
     * @param pdpStatusParameters the status report paramaters from the PDP
     * @throws NexusRestWrapperException if an error occurs downloading artifacts from maven
     * @throws IOException if an error occurs writing maven artifacts to the local file system
     * @throws ApiException if an error occurs sending a policy to the PDP
     */
    @Transactional
    public void handlePdpStatusReport(final PdpStatusParameters pdpStatusParameters)
            throws NexusRestWrapperException, IOException, ApiException {

        if (pdpStatusParameters.getPdpState().equals(PdpStateEnum.PASSIVE)) {

            long policySetId = policyDbDao.getPolicySetIdForPdpGroup(pdpStatusParameters.getPdpGroupName());
            long pdpGroupId = policyDbDao.getPdpGroupId(pdpStatusParameters.getPdpGroupName());
            final Collection<PolicySetToPolicyEntity> policySetToPolicyEntitys =
                    policyDbDao.getPoliciesForPolicySetId(policySetId);


            for (PolicySetToPolicyEntity policySetToPolicyEntity : policySetToPolicyEntitys) {
                long policyId = policySetToPolicyEntity.getPolicyId();
                logger.debug("PAP: Found policy Id for pdp group {} : {}", pdpStatusParameters.getPdpGroupName(),
                        policyId);

                final PolicyEntity policyEntity = policyDbDao.getPolicyEntity(policyId);
                Collection<File> policyArtifacts = downloadMavenArtifactsForPolicy(policyEntity);
                logger.info("PAP: Sending policy maven artifact etc. to plugin");
                Policy policy = generatePolicy(policyId, policyEntity.getPolicyName(), policyEntity.getPolicyVersion(),
                        policyArtifacts, Collections.<String, String>emptyMap());
                deployPolicyToPdp(pdpStatusParameters.getPdpEndpoint(), policy);
            }

            logger.info("PAP: Add new PDP to database");
            policyDbDao.createPdp(pdpStatusParameters.getPdpName(), pdpStatusParameters.getPdpVersion(),
                    pdpStatusParameters.getPdpState().toString(), pdpStatusParameters.getPdpType(),
                    pdpStatusParameters.getPdpEndpoint(), pdpGroupId, policySetId);
        }
    }

    private Collection<File> downloadMavenArtifactsForPolicy(final PolicyEntity policyEntity)
            throws NexusRestWrapperException, IOException {
        String mavenArtifactString = policyEntity.getPolicyMavenArtifact();
        String[] mavenArtifactDetails = mavenArtifactString.split(":");

        NexusRestSearchParameters nexusRestSearchParameters = new NexusRestSearchParameters();
        nexusRestSearchParameters.useFilterSearch(mavenArtifactDetails[0], mavenArtifactDetails[1],
                mavenArtifactDetails[2], null, null);

        NexusRestWrapper nexusRestWrapper = new NexusRestWrapper(properties.getProperty("nexus_server_url"));
        NexusSearchResult nexusSearchResult = nexusRestWrapper.findArtifact(nexusRestSearchParameters);
        List<NexusArtifact> nexusArtifacts = nexusSearchResult.getArtifactList();

        Collection<File> artifacts = new HashSet<>();
        for (NexusArtifact nexusArtifact : nexusArtifacts) {
            final String urlPath = nexusArtifact.getUrlPath();
            logger.info("PAP: Downloading artifact: {}", urlPath);

            final String fileName = urlPath.substring(urlPath.lastIndexOf('/') + 1);
            final File file = new File(properties.getProperty("nexus_download_dir") + fileName + ".jar");
            artifacts.add(file);

            FileUtils.copyURLToFile(new URL(urlPath + ".jar"), file);

            logger.debug("PAP: Downloaded artifact to: ", file.getAbsolutePath());
        }
        return artifacts;
    }

    private Policy generatePolicy(long policyId, final String policyName, final String policyVersion,
            final Collection<File> policyArtifacts, final Map<String, String> policyMetadata) {
        DummyPapPlugin plugin = new DummyPapPlugin();
        return plugin.generatePolicy(policyId, policyName, policyVersion, policyArtifacts, policyMetadata);
    }

    private void deployPolicyToPdp(String pdpEndpopint, Policy policy) throws ApiException {
        logger.info("PAP: Sending policy to PDP", policy);
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(pdpEndpopint);
        PdpClient pdpClient = new PdpClient(apiClient);
        pdpClient.policyDeploymentDeployPut(policy);
    }

}
