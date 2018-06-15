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

package org.onap.policy.pap.service.dao;

import javax.persistence.EntityManagerFactory;

/**
 * DAO for access the policy DB.
 */
public class PolicyDbDao {

    private static PolicyDbDao currentInstance = null;
    private EntityManagerFactory entityManagerFactory;

    private PolicyDbDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Get an instance of a PolicyDbDao. It creates one if it does not exist. Only one instance is
     * allowed to be created per server.
     * 
     * @param entityManagerFactory The EntityFactoryManager to be used for database connections
     * @return The new instance of PolicyDbDao
     * @throws IllegalArgumentException if entityManagerFactory is null.
     */
    public static PolicyDbDao getPolicyDbDaoInstance(EntityManagerFactory entityManagerFactory) {

        if (currentInstance == null) {
            if (entityManagerFactory != null) {
                currentInstance = new PolicyDbDao(entityManagerFactory);
                return currentInstance;
            }
            throw new IllegalArgumentException("The EntityManagerFactory is Null");
        }
        return currentInstance;
    }

    public PolicyDbDaoTransaction getNewTransaction() {
        return new PolicyDbDaoTransaction(entityManagerFactory);
    }

}
