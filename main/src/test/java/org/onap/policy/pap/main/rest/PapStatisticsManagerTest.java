package org.onap.policy.pap.main.rest;

import static org.junit.Assert.*;
import org.junit.Test;

public class PapStatisticsManagerTest {

    @Test
    public void test() {
        PapStatisticsManager mgr = PapStatisticsManager.getInstance();
        assertNotNull(mgr);

        // should return the same manager
        assertSame(mgr, PapStatisticsManager.getInstance());

        // work with a new object so we don't have to worry about initial counts
        mgr = new PapStatisticsManager();

        // try each update

        assertEquals(0, mgr.getTotalPdpCount());
        assertEquals(1, mgr.updateTotalPdpCount());
        assertEquals(1, mgr.getTotalPdpCount());

        assertEquals(0, mgr.getTotalPdpGroupCount());
        assertEquals(1, mgr.updateTotalPdpGroupCount());
        assertEquals(1, mgr.getTotalPdpGroupCount());

        assertEquals(0, mgr.getTotalPolicyDeployCount());
        assertEquals(1, mgr.updateTotalPolicyDeployCount());
        assertEquals(1, mgr.getTotalPolicyDeployCount());

        assertEquals(0, mgr.getPolicyDeploySuccessCount());
        assertEquals(1, mgr.updatePolicyDeploySuccessCount());
        assertEquals(1, mgr.getPolicyDeploySuccessCount());

        assertEquals(0, mgr.getPolicyDeployFailureCount());
        assertEquals(1, mgr.updatePolicyDeployFailureCount());
        assertEquals(1, mgr.getPolicyDeployFailureCount());

        assertEquals(0, mgr.getTotalPolicyDownloadCount());
        assertEquals(1, mgr.updateTotalPolicyDownloadCount());
        assertEquals(1, mgr.getTotalPolicyDownloadCount());

        assertEquals(0, mgr.getPolicyDownloadSuccessCount());
        assertEquals(1, mgr.updatePolicyDownloadSuccessCount());
        assertEquals(1, mgr.getPolicyDownloadSuccessCount());

        assertEquals(0, mgr.getPolicyDownloadFailureCount());
        assertEquals(1, mgr.updatePolicyDownloadFailureCount());
        assertEquals(1, mgr.getPolicyDownloadFailureCount());

        // now check reset
        mgr.resetAllStatistics();

        assertEquals(0, mgr.getPolicyDeployFailureCount());
        assertEquals(0, mgr.getTotalPdpCount());
        assertEquals(0, mgr.getTotalPdpGroupCount());
        assertEquals(0, mgr.getTotalPolicyDeployCount());
        assertEquals(0, mgr.getPolicyDeploySuccessCount());
        assertEquals(0, mgr.getPolicyDeployFailureCount());
        assertEquals(0, mgr.getTotalPolicyDownloadCount());
        assertEquals(0, mgr.getPolicyDownloadSuccessCount());
        assertEquals(0, mgr.getPolicyDownloadFailureCount());
    }
}
