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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.firewall.RuleType;
import org.junit.Before;
import org.junit.Test;

public class AbstractLinuxFirewallTest extends FirewallTestUtils {

    private AbstractLinuxFirewall firewall;

    @Before
    public void setUp() throws IOException {
        setUpMock();
        this.firewall = new TestableLinuxFirewall(this.executor);
        this.firewall.persistenceStore = newTempPersistenceStore();
    }

    @Test
    public void addAndDeleteLocalRuleTest() throws KuraException {
        LocalRule rule = new LocalRule(5400, "tcp", null, "eth0", null, "00:11:22:33:44:55:66", "10100:10200");

        this.firewall.addLocalRules(Arrays.asList(rule));
        assertTrue(this.firewall.getLocalRules().contains(rule));

        this.firewall.deleteLocalRule(rule);
        assertTrue(this.firewall.getLocalRules().isEmpty());
    }

    @Test
    public void addingSameLocalRuleTwiceIsIgnoredTest() throws KuraException {
        LocalRule rule = new LocalRule(22, "tcp", null, null, null, null, null);

        this.firewall.addLocalRules(Arrays.asList(rule));
        this.firewall.addLocalRules(Arrays.asList(rule));

        assertEquals(1, this.firewall.getLocalRules().size());
    }

    @Test
    public void deleteAllLocalRulesTest() throws KuraException {
        this.firewall.addLocalRules(Arrays.asList(new LocalRule(22, "tcp", null, null, null, null, null)));

        this.firewall.deleteAllLocalRules();

        assertTrue(this.firewall.getLocalRules().isEmpty());
    }

    @Test
    public void addPortForwardRuleAndDeleteAllTest() throws KuraException {
        PortForwardRule rule = new PortForwardRule().inboundIface("ppp0").outboundIface("eth0").address("10.0.0.5")
                .protocol("tcp").inPort(8080).outPort(80);

        this.firewall.addPortForwardRules(Arrays.asList(rule));
        assertTrue(this.firewall.getPortForwardRules().contains(rule));

        this.firewall.deleteAllPortForwardRules();
        assertTrue(this.firewall.getPortForwardRules().isEmpty());
    }

    @Test
    public void addAutoNatAndFullNatRulesTest() throws KuraException {
        this.firewall.addAutoNatRules(Arrays.asList(new NATRule("eth0", "ppp0", true, RuleType.GENERIC)));
        this.firewall.addNatRules(Arrays.asList(
                new NATRule("eth0", "ppp0", "tcp", "192.168.1.0/24", "0.0.0.0/0", true, RuleType.IP_FORWARDING)));

        assertEquals(1, this.firewall.getAutoNatRules().size());
        assertEquals(1, this.firewall.getNatRules().size());

        this.firewall.deleteAllAutoNatRules();
        this.firewall.deleteAllNatRules();
        assertTrue(this.firewall.getAutoNatRules().isEmpty());
        assertTrue(this.firewall.getNatRules().isEmpty());
    }

    @Test
    public void replaceAllNatRulesActuallyReplacesAutoNatRulesTest() throws KuraException {
        // Pre-existing naming quirk kept intentionally: despite the name, this
        // replaces autoNatRules, not natRules.
        this.firewall.addNatRules(
                Arrays.asList(new NATRule("eth0", "ppp0", "tcp", "192.168.1.0/24", "0.0.0.0/0", true,
                        RuleType.IP_FORWARDING)));

        Set<NATRule> replacement = new LinkedHashSet<>(
                Arrays.asList(new NATRule("eth1", "ppp1", true, RuleType.GENERIC)));
        this.firewall.replaceAllNatRules(replacement);

        assertEquals(replacement, this.firewall.getAutoNatRules());
        assertEquals(1, this.firewall.getNatRules().size());
    }

    @Test
    public void blockAllPortsDoesNotClearNatRulesTest() throws KuraException {
        // Pre-existing quirk kept intentionally.
        this.firewall.addLocalRules(Arrays.asList(new LocalRule(22, "tcp", null, null, null, null, null)));
        this.firewall.addNatRules(
                Arrays.asList(new NATRule("eth0", "ppp0", "tcp", "192.168.1.0/24", "0.0.0.0/0", true,
                        RuleType.IP_FORWARDING)));

        this.firewall.blockAllPorts();

        assertTrue(this.firewall.getLocalRules().isEmpty());
        assertEquals(1, this.firewall.getNatRules().size());
    }

    @Test
    public void enableAndDisableTest() throws KuraException {
        this.firewall.enable();
        assertTrue(this.executor.lastScript().contains("add table ip filter"));

        this.firewall.disable();
        assertTrue(this.executor.submittedScripts.contains("delete table ip filter"));
        assertTrue(this.executor.submittedScripts.contains("delete table ip nat"));
        assertTrue(this.executor.submittedScripts.contains("delete table ip mangle"));
    }

    @Test
    public void setAdditionalRulesTest() throws KuraException {
        this.firewall.setAdditionalRules(Collections.singleton("add rule ip filter input_kura tcp dport 9 accept"),
                Collections.emptySet(), Collections.emptySet());

        assertTrue(this.executor.lastScript().contains("add rule ip filter input_kura tcp dport 9 accept"));
    }
}
