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

import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.RollbackException;
import org.onap.policy.common.logging.eelf.PolicyLogger;
import org.onap.policy.pap.service.jpa.DatabaseLockEntity;
import org.onap.policy.pap.service.jpa.PdpEntity;
import org.onap.policy.pap.service.jpa.PdpGroupEntity;
import org.onap.policy.pap.service.jpa.PolicyEntity;
import org.onap.policy.pap.service.jpa.PolicySetToPolicyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyDbDaoTransaction {

    Logger logger = LoggerFactory.getLogger(PolicyDbDaoTransaction.class);
    private EntityManager em;
    private final Object emLock = new Object();
    private boolean operationRun = false;
    private final Thread transactionTimer;

    /**
     * Create a DB transaction.
     * 
     * @param entityManagerFactory the EntityManagerFactory to use for connecting to the DB
     */
    PolicyDbDaoTransaction(EntityManagerFactory entityManagerFactory) {
        this(entityManagerFactory, 100000, 100000);
    }

    /**
     * Create a DB transaction.
     * 
     * @param entityManagerFactory the EntityManagerFactory to use for connecting to the DB
     * @param transactionTimeout how long the transaction can sit before rolling back
     * @param transactionWaitTime how long to wait for the transaction to start before throwing an
     *        exception
     */
    private PolicyDbDaoTransaction(EntityManagerFactory entityManagerFactory,
            int transactionTimeout, int transactionWaitTime) {

        this.em = entityManagerFactory.createEntityManager();
        synchronized (emLock) {
            try {
                startTransactionSynced(this.em, transactionWaitTime);
            } catch (Exception e) {
                throw new PersistenceException("Could not lock transaction within "
                        + transactionWaitTime + " milliseconds");
            }
        }
        class TransactionTimer implements Runnable {

            private int sleepTime;

            public TransactionTimer(int timeout) {
                this.sleepTime = timeout;
            }

            @Override
            public void run() {

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // probably, the transaction was completed, the last thing we want to do is
                    // roll back

                    Thread.currentThread().interrupt();
                    return;
                }
                rollbackTransaction();
            }

        }

        transactionTimer =
                new Thread(new TransactionTimer(transactionTimeout), "transactionTimerThread");
        transactionTimer.start();


    }

    private void checkBeforeOperationRun(boolean justCheckOpen) {
        if (!isTransactionOpen()) {
            logger.error("There is no transaction currently open");
            throw new IllegalStateException("There is no transaction currently open");
        }
        if (operationRun && !justCheckOpen) {
            logger.error(
                    "An operation has already been performed and the current transaction should be committed");
            throw new IllegalStateException(
                    "An operation has already been performed and the current transaction should be committed");
        }
        operationRun = true;
    }

    public void commitTransaction() {
        synchronized (emLock) {
            logger.debug("Commiting transaction");
            if (!isTransactionOpen()) {
                logger.debug("There is no open transaction to commit");
                try {
                    em.close();
                } catch (Exception e) {
                    logger.error("Exception Occured closing EntityManager", e);
                }
                return;
            }
            try {
                em.getTransaction().commit();
            } catch (RollbackException e) {
                logger.error("PolicyDbDao caught RollbackException on em.getTransaction().commit()",
                        e);
                throw new PersistenceException("The commit failed. Message:\n" + e.getMessage());
            }
            em.close();
        }
        if (transactionTimer != null) {
            transactionTimer.interrupt();
        }
    }

    public void rollbackTransaction() {
        synchronized (emLock) {
            if (isTransactionOpen()) {

                try {
                    em.getTransaction().rollback();
                } catch (Exception e) {
                    logger.error("Exception while rolling back transaction", e);
                }
                try {
                    em.close();
                } catch (Exception e) {
                    logger.error("Exception while closing EntityManager", e);
                }

            } else {
                try {
                    em.close();
                } catch (Exception e) {
                    logger.error("Exception while closing EntityManager" + e);
                }
            }

        }
        if (transactionTimer != null) {
            transactionTimer.interrupt();
        }
    }

    /**
     * Check if a transaction is open.
     * 
     * @return <code>true</code> if the transaction is open, <code>false</code> otherwise
     */
    public boolean isTransactionOpen() {
        synchronized (emLock) {
            return em.isOpen() && em.getTransaction().isActive();
        }
    }

    public PolicyEntity getPolicyEntity(int policyId) {
        return getEntity("PolicyEntity", "policyId", policyId);
    }

    public PdpGroupEntity getPdpGroupEntity(long groupId) {
        return getEntity("PdpGroupEntity", "groupId", groupId);
    }

    public PdpGroupEntity getPdpGroupEntity(String groupName) {
        return getEntity("PdpGroupEntity", "groupName", groupName);
    }

    public <T> T getEntity(final String tableName, final String columnName, final Object value) {
        synchronized (emLock) {
            checkBeforeOperationRun(true);
            String queryString =
                    "SELECT p FROM " + tableName + " p WHERE p." + columnName + "=:" + columnName;
            Query query = em.createQuery(queryString);
            query.setParameter(columnName, value);
            List<?> queryResultList;
            try {
                queryResultList = query.getResultList();
            } catch (Exception e) {
                logger.error("Caught Exception trying to get entry in {} with {} equal to {}",
                        tableName, columnName, value);
                throw new PersistenceException("Query failed trying to get entry in " + tableName
                        + " with " + columnName + " equal to " + value);
            }
            if (queryResultList.size() > 1) {
                PolicyLogger.error("More than entry in database matches");
                throw new PersistenceException("More than entry in database matches in " + tableName
                        + " with " + columnName + " equal to " + value);
            }
            return (T) queryResultList.get(0);
        }
    }

    public Collection<PolicySetToPolicyEntity> getPolicySetToPolicyEntities(int policySetId) {
        return getEntities("PolicySetToPolicyEntity", "policySetId", policySetId);
    }

    public <T> Collection<T> getEntities(final String tableName, final String columnName,
            final Object value) {
        synchronized (emLock) {
            checkBeforeOperationRun(true);
            // check if group exists
            String queryString =
                    "SELECT p FROM " + tableName + " p WHERE p." + columnName + "=:" + columnName;
            Query query = em.createQuery(queryString);
            query.setParameter(columnName, value);
            List<?> queryResultList;
            try {
                queryResultList = query.getResultList();
            } catch (Exception e) {
                logger.error("Caught Exception trying to get entries in {} with {} equal to {}",
                        tableName, columnName, value);
                throw new PersistenceException("Query failed trying to get entries in " + tableName
                        + " with " + columnName + " equal to " + value);
            }
            return (Collection<T>) queryResultList;
        }
    }

    public void createPdp(String pdpName, String pdpVersion, String pdpState, String pdpType,
            String pdpEndpoint, int pdpGroupId, int policySetId) {
        synchronized (emLock) {
            checkBeforeOperationRun(true);

            PdpEntity newPdp = new PdpEntity();

            newPdp.setPdpName(pdpName);
            newPdp.setPdpEndpoint(pdpEndpoint);
            newPdp.setPdpGroupId(pdpGroupId);
            newPdp.setPdpState(pdpState);
            newPdp.setPdpType(pdpType);
            newPdp.setPdpVersion(pdpVersion);
            newPdp.setPolicySetId(policySetId);
            em.persist(newPdp);

            em.flush();

        }
    }

    private void startTransactionSynced(EntityManager entityMgr, int waitTime) {
        DatabaseLockEntity lock = null;

        entityMgr.setProperty("javax.persistence.query.timeout", waitTime);
        entityMgr.getTransaction().begin();

        try {
            lock = entityMgr.find(DatabaseLockEntity.class, 1, LockModeType.PESSIMISTIC_WRITE);
        } catch (Exception e) {
            logger.error("Could not get lock entity");
        }
        if (lock == null) {
            throw new IllegalStateException(
                    "The lock row does not exist in the table. Please create a primary key with value = 1.");
        }

    }
}
