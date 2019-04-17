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

package org.onap.policy.pap.main.rest.e2e;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.pap.main.PapConstants;
import org.onap.policy.pap.main.PolicyModelsProviderFactoryWrapper;
import org.onap.policy.pap.main.PolicyPapRuntimeException;
import org.onap.policy.pap.main.rest.CommonPapRestServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class End2EndBase extends CommonPapRestServer {
    private static final Logger logger = LoggerFactory.getLogger(End2EndBase.class);

    private static final Coder coder = new StandardCoder();
    private static final Yaml yaml = new Yaml();

    /**
     * DB connection. This is kept open until {@link #stop()} is invoked so that the
     * in-memory DB is not destroyed.
     */
    private static PolicyModelsProvider dbConn;

    /**
     * DAO provider factory.
     */
    private static PolicyModelsProviderFactoryWrapper daoFactory;

    /**
     * Context - should be initialized by setUp() method.
     */
    protected End2EndContext context = null;


    /**
     * Starts Main and connects to the DB.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonPapRestServer.setUpBeforeClass();

        daoFactory = Registry.get(PapConstants.REG_PAP_DAO_FACTORY, PolicyModelsProviderFactoryWrapper.class);

        try {
            dbConn = daoFactory.create();
        } catch (PfModelException e) {
            throw new PolicyPapRuntimeException("cannot connect to DB", e);
        }
    }

    /**
     * Tears down.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        try {
            dbConn.close();
        } catch (PfModelException e) {
            logger.warn("failed to close the DB", e);
        }

        try {
            daoFactory.close();
        } catch (Exception e) {
            logger.warn("failed to close DAO factory", e);
        }

        CommonPapRestServer.teardownAfterClass();
    }

    /**
     * Tears down.
     */
    @After
    public void tearDown() {
        if (context != null) {
            try {
                context.stop();
            } catch (Exception e) {
                logger.warn("failed to stop end-to-end context", e);
            }
            context = null;
        }

        super.tearDown();
    }

    /**
     * Adds Tosca Policy Types to the DB.
     *
     * @param yamlFile name of the YAML file specifying the data to be loaded
     * @throws PfModelException if a DAO error occurs
     */
    public static void addToscaPolicyTypes(String yamlFile) throws PfModelException {
        ToscaServiceTemplate serviceTemplate = loadYamlFile(yamlFile, ToscaServiceTemplate.class);
        dbConn.createPolicyTypes(serviceTemplate);
    }

    /**
     * Adds Tosca Policies to the DB.
     *
     * @param yamlFile name of the YAML file specifying the data to be loaded
     * @throws PfModelException if a DAO error occurs
     */
    public static void addToscaPolicies(String yamlFile) throws PfModelException {
        ToscaServiceTemplate serviceTemplate = loadYamlFile(yamlFile, ToscaServiceTemplate.class);
        dbConn.createPolicies(serviceTemplate);
    }

    /**
     * Adds PDP groups to the DB.
     *
     * @param jsonFile name of the JSON file specifying the data to be loaded
     * @throws PfModelException if a DAO error occurs
     */
    public static void addGroups(String jsonFile) throws PfModelException {
        PdpGroups groups = loadJsonFile(jsonFile, PdpGroups.class);

        ValidationResult result = groups.validatePapRest();
        if (!result.isValid()) {
            throw new PolicyPapRuntimeException("cannot init DB groups from " + jsonFile + ":\n" + result.getResult());
        }

        dbConn.createPdpGroups(groups.getGroups());
    }

    /**
     * Loads an object from a YAML file.
     *
     * @param fileName name of the file from which to load
     * @param clazz the class of the object to be loaded
     * @return the object that was loaded from the file
     */
    protected static <T> T loadYamlFile(String fileName, Class<T> clazz) {
        File propFile = new File(ResourceUtils.getFilePath4Resource("e2e/" + fileName));

        try (FileInputStream input = new FileInputStream(propFile)) {
            Object yamlObject = yaml.load(input);
            String json = coder.encode(yamlObject);
            T result = coder.decode(json, clazz);

            if (result == null) {
                throw new PolicyPapRuntimeException("cannot decode " + clazz.getSimpleName() + " from " + fileName);
            }

            return result;

        } catch (FileNotFoundException e) {
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
    protected static <T> T loadJsonFile(String fileName, Class<T> clazz) {
        String fileName2 = (fileName.startsWith("src/") ? fileName : "e2e/" + fileName);
        File propFile = new File(ResourceUtils.getFilePath4Resource(fileName2));
        try {
            T result = coder.decode(propFile, clazz);

            if (result == null) {
                throw new PolicyPapRuntimeException("cannot decode " + clazz.getSimpleName() + " from " + fileName);
            }

            return result;

        } catch (CoderException e) {
            throw new RuntimeException(e);
        }
    }
}
