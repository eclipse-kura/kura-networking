/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.linux.net.nftables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.kura.KuraException;
import org.junit.Test;

/**
 * Regression coverage for the family-independence bug: the
 * FirewallConfigurationService (IPv4) and FirewallConfigurationServiceIPv6
 * OSGi services each obtain their own {@code AbstractLinuxFirewall} instance
 * - {@link LinuxFirewall} and {@link LinuxFirewallIPv6} respectively - and
 * must never share rule state or a live nft table, or one family's
 * getFirewallConfiguration()/setFirewallOpenPortConfiguration() call corrupts
 * or crashes on the other's rules.
 */
public class LinuxFirewallIPv6Test extends FirewallTestUtils {

    private LinuxFirewallIPv6 getFreshInstance() throws IOException {
        setUpMock();
        LinuxFirewallIPv6 firewall = LinuxFirewallIPv6.getInstance(this.executor);
        firewall.persistenceStore = newTempPersistenceStore();
        firewall.getLocalRules().clear();
        return firewall;
    }

    @Test
    public void isDistinctFromIPv4SingletonTest() throws IOException {
        LinuxFirewall v4 = LinuxFirewall.getInstance(this.executor);
        LinuxFirewallIPv6 v6 = getFreshInstance();

        assertNotSame(v4, v6);
    }

    @Test
    public void applyRulesTargetsIp6FamilyTest() throws KuraException, IOException {
        LinuxFirewallIPv6 firewall = getFreshInstance();

        firewall.addLocalRules(Arrays.asList(new LocalRule(22, "tcp", null, null, null, null, null)));

        assertTrue(this.executor.lastScript().contains("add table ip6 filter"));
        assertFalse(this.executor.lastScript().contains("add table ip filter"));
    }

    @Test
    public void v4AndV6RuleSetsAreIndependentTest() throws KuraException, IOException {
        RecordingNftExecutor v4Executor = new RecordingNftExecutor();
        RecordingNftExecutor v6Executor = new RecordingNftExecutor();

        LinuxFirewall v4 = LinuxFirewall.getInstance(v4Executor);
        v4.persistenceStore = newTempPersistenceStore();
        v4.getLocalRules().clear();

        LinuxFirewallIPv6 v6 = LinuxFirewallIPv6.getInstance(v6Executor);
        v6.persistenceStore = newTempPersistenceStore();
        v6.getLocalRules().clear();

        LocalRule v4Rule = new LocalRule(80, "tcp", null, null, null, null, null);
        v4.addLocalRules(Arrays.asList(v4Rule));

        // Adding a v4-only rule must never appear in the v6 instance's rule set,
        // and deleting all v6 rules (a no-op here) must never touch v4's.
        assertEquals(0, v6.getLocalRules().size());

        LocalRule v6Rule = new LocalRule(443, "tcp", null, null, null, null, null);
        v6.addLocalRules(Arrays.asList(v6Rule));
        v6.deleteAllLocalRules();

        assertTrue(v4.getLocalRules().contains(v4Rule));
        assertTrue(v6.getLocalRules().isEmpty());
    }
}
