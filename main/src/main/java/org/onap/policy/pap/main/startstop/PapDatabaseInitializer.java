/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2023-2024 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property.
 *  Modifications Copyright (C) 2021-2022 Bell Canada. All rights reserved.
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

package org.onap.policy.pap.main.startstop;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroups;
import org.onap.policy.pap.main.PolicyPapException;
import org.onap.policy.pap.main.service.PdpGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * This class creates initial PdpGroup/SubGroup in the database.
 *
 * @author Ram Krishna Verma (ram.krishna.verma@est.tech)
 */
@Component
@ConditionalOnProperty(value = "db.initialize", havingValue = "true", matchIfMissing = true)
public class PapDatabaseInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PapDatabaseInitializer.class);

    private final StandardCoder standardCoder;

    private final PdpGroupService pdpGroupService;

    @Value("${group-config-file:PapDb.json}")
    private String groupConfigFile;

    /**
     * Constructs the object.
     */
    public PapDatabaseInitializer(PdpGroupService pdpGroupService) {
        this.pdpGroupService = pdpGroupService;
        standardCoder = new StandardCoder();
    }

    /**
     * Initializes database with group information.
     *
     * @param groupsJson the group file path
     * @throws PolicyPapException in case of errors.
     */
    private void initializePapDatabase(String groupsJson) throws PolicyPapException {

        try {
            final var originalJson = ResourceUtils.getResourceAsString(groupsJson);
            final var pdpGroupsToCreate = standardCoder.decode(originalJson, PdpGroups.class);
            final List<PdpGroup> pdpGroupsFromDb =
                pdpGroupService.getPdpGroups(pdpGroupsToCreate.getGroups().get(0).getName());
            if (pdpGroupsFromDb.isEmpty()) {
                ValidationResult result = pdpGroupsToCreate.validatePapRest();
                if (!result.isValid()) {
                    throw new PolicyPapException(result.getResult());
                }
                pdpGroupService.createPdpGroups(pdpGroupsToCreate.getGroups());
                LOGGER.info("Created initial pdpGroup in DB - {} from {}", pdpGroupsToCreate, groupsJson);
            } else {
                LOGGER.info("Initial pdpGroup already exists in DB, skipping create - {} from {}", pdpGroupsFromDb,
                    groupsJson);
            }
        } catch (final CoderException | RuntimeException exp) {
            throw new PolicyPapException(exp);
        }
    }

    /**
     * Initializes database with group information.
     */
    @PostConstruct
    public void loadData() throws PolicyPapException {
        initializePapDatabase(groupConfigFile);
    }
}
