/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019-2020, 2022 Nordix Foundation.
 * Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.rest.e2e;

import io.micrometer.core.instrument.MeterRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.junit.After;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.PrometheusUtils;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus;
import org.onap.policy.models.pdp.concepts.PdpPolicyStatus.State;
import org.onap.policy.models.pdp.concepts.PdpStatistics;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.repository.ToscaServiceTemplateRepository;
import org.onap.policy.pap.main.rest.CommonPapRestServer;
import org.onap.policy.pap.main.service.PdpGroupService;
import org.onap.policy.pap.main.service.PdpStatisticsService;
import org.onap.policy.pap.main.service.PolicyStatusService;
import org.onap.policy.pap.main.service.ToscaServiceTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.yaml.snakeyaml.Yaml;

@ActiveProfiles({ "test-e2e", "default" })
public abstract class End2EndBase extends CommonPapRestServer {
    private static final Logger logger = LoggerFactory.getLogger(End2EndBase.class);

    private static final Coder coder = new StandardCoder();
    private static final Yaml yaml = new Yaml();

    /**
     * Context - should be initialized by setUp() method.
     */
    protected End2EndContext context = null;

    @Autowired
    public PdpGroupService pdpGroupService;

    @Autowired
    public PdpStatisticsService pdpStatisticsService;

    @Autowired
    private ToscaServiceTemplateRepository serviceTemplateRepository;

    @Autowired
    public PolicyStatusService policyStatusService;

    @Autowired
    public ToscaServiceTemplateService toscaService;

    @Autowired
    public MeterRegistry meterRegistry;

    @Getter
    private final String topicPolicyPdpPap = "pdp-pap-topic";

    @Getter
    private final String topicPolicyNotification = "notification-topic";

    public String deploymentsCounterName = "pap_" + PrometheusUtils.POLICY_DEPLOYMENTS_METRIC;
    public String[] deploymentSuccessTag = {PrometheusUtils.OPERATION_METRIC_LABEL, PrometheusUtils.DEPLOY_OPERATION,
        PrometheusUtils.STATUS_METRIC_LABEL, State.SUCCESS.name()};
    public String[] unDeploymentSuccessTag = {PrometheusUtils.OPERATION_METRIC_LABEL,
        PrometheusUtils.UNDEPLOY_OPERATION, PrometheusUtils.STATUS_METRIC_LABEL, State.SUCCESS.name()};

    /**
     * Tears down.
     */
    @Override
    @After
    public void tearDown() {
        if (context != null) {
            try {
                context.stop();
            } catch (final Exception e) {
                logger.warn("failed to stop end-to-end context", e);
            }
            context = null;
        }
        meterRegistry.clear();
        super.tearDown();
    }

    /**
     * Adds Tosca Policy Types to the DB.
     *
     * @param yamlFile name of the YAML file specifying the data to be loaded
     */
    public void addToscaPolicyTypes(final String yamlFile) {
        final ToscaServiceTemplate serviceTemplate = loadYamlFile(yamlFile, ToscaServiceTemplate.class);
        JpaToscaServiceTemplate jpaToscaServiceTemplate = mergeWithExistingTemplate(serviceTemplate);
        serviceTemplateRepository.save(jpaToscaServiceTemplate);
    }

    /**
     * Adds Tosca Policies to the DB.
     *
     * @param yamlFile name of the YAML file specifying the data to be loaded
     */
    public void addToscaPolicies(final String yamlFile) {
        final ToscaServiceTemplate serviceTemplate = loadYamlFile(yamlFile, ToscaServiceTemplate.class);
        JpaToscaServiceTemplate jpaToscaServiceTemplate = mergeWithExistingTemplate(serviceTemplate);
        serviceTemplateRepository.save(jpaToscaServiceTemplate);
    }

    private JpaToscaServiceTemplate mergeWithExistingTemplate(ToscaServiceTemplate serviceTemplate) {
        JpaToscaServiceTemplate jpaToscaServiceTemplate = new JpaToscaServiceTemplate(serviceTemplate);
        Optional<JpaToscaServiceTemplate> dbServiceTemplateOpt = serviceTemplateRepository
            .findById(new PfConceptKey(JpaToscaServiceTemplate.DEFAULT_NAME, JpaToscaServiceTemplate.DEFAULT_VERSION));
        if (dbServiceTemplateOpt.isPresent()) {
            JpaToscaServiceTemplate dbServiceTemplate = dbServiceTemplateOpt.get();
            if (dbServiceTemplate.getPolicyTypes() != null) {
                jpaToscaServiceTemplate.setPolicyTypes(dbServiceTemplate.getPolicyTypes());
            }
            if (dbServiceTemplate.getDataTypes() != null) {
                jpaToscaServiceTemplate.setDataTypes(dbServiceTemplate.getDataTypes());
            }
            if (dbServiceTemplate.getTopologyTemplate() != null) {
                jpaToscaServiceTemplate.setTopologyTemplate(dbServiceTemplate.getTopologyTemplate());
            }
        }

        return jpaToscaServiceTemplate;
    }

    /**
     * Adds PDP groups to the DB.
     *
     * @param jsonFile name of the JSON file specifying the data to be loaded
     */
    public void addGroups(final String jsonFile) {
        final PdpGroups groups = loadJsonFile(jsonFile, PdpGroups.class);

        final ValidationResult result = groups.validatePapRest();
        if (!result.isValid()) {
            throw new PolicyPapRuntimeException("cannot init DB groups from " + jsonFile + ":\n" + result.getResult());
        }

        pdpGroupService.createPdpGroups(groups.getGroups());
    }

    /**
     * Fetch PDP groups from the DB.
     *
     * @param name name of the pdpGroup
     */
    public List<PdpGroup> fetchGroups(final String name) {
        return pdpGroupService.getPdpGroups(name);
    }

    /**
     * Fetch PDP statistics from the DB.
     *
     * @param instanceId name of the pdpStatistics
     * @param groupName name of the pdpGroup
     * @param subGroupName name of the pdpSubGroup
     */
    public Map<String, Map<String, List<PdpStatistics>>> fetchPdpStatistics(final String instanceId,
        final String groupName, final String subGroupName) {
        return pdpStatisticsService.fetchDatabaseStatistics(groupName, subGroupName, instanceId, 100, null, null);
    }

    /**
     * Adds PdpPolicyStatus records to the DB.
     *
     * @param jsonFile name of the JSON file specifying the data to be loaded
     */
    public void addPdpPolicyStatus(final String jsonFile) {
        final PolicyStatusRecords data = loadJsonFile(jsonFile, PolicyStatusRecords.class);
        policyStatusService.cudPolicyStatus(data.records, null, null);
    }

    /**
     * Loads an object from a YAML file.
     *
     * @param fileName name of the file from which to load
     * @param clazz the class of the object to be loaded
     * @return the object that was loaded from the file
     */
    protected static <T> T loadYamlFile(final String fileName, final Class<T> clazz) {
        final File propFile = new File(ResourceUtils.getFilePath4Resource("e2e/" + fileName));

        try (FileInputStream input = new FileInputStream(propFile)) {
            final Object yamlObject = yaml.load(input);
            final String json = coder.encode(yamlObject);
            final T result = coder.decode(json, clazz);

            if (result == null) {
                throw new PolicyPapRuntimeException("cannot decode " + clazz.getSimpleName() + " from " + fileName);
            }

            return result;

        } catch (final FileNotFoundException e) {
            throw new PolicyPapRuntimeException("cannot find " + fileName, e);

        } catch (IOException | CoderException e) {
            throw new PolicyPapRuntimeException("cannot decode " + fileName, e);
        }
    }

    /**
     * Loads an object from a JSON file.
     *
     * @param fileName name of the file from which to load
     * @param clazz the class of the object to be loaded
     * @return the object that was loaded from the file
     */
    protected static <T> T loadJsonFile(final String fileName, final Class<T> clazz) {
        final String fileName2 = (fileName.startsWith("src/") ? fileName : "e2e/" + fileName);
        final File propFile = new File(ResourceUtils.getFilePath4Resource(fileName2));
        try {
            final T result = coder.decode(propFile, clazz);

            if (result == null) {
                throw new PolicyPapRuntimeException("cannot decode " + clazz.getSimpleName() + " from " + fileName);
            }

            return result;

        } catch (final CoderException e) {
            throw new RuntimeException(e);
        }
    }

    public class PolicyStatusRecords {
        private List<PdpPolicyStatus> records;
    }
}
