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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.firewall.RuleType;
import org.junit.Before;
import org.junit.Test;

public class NftablesConfigTest {

    private RecordingNftExecutor executor;
    private NftablesConfig configV4;
    private NftablesConfig configV6;

    @Before
    public void setUp() {
        this.executor = new RecordingNftExecutor();
        this.configV4 = new NftablesConfig(this.executor, NftablesConfigConstants.FAMILY_V4);
        this.configV6 = new NftablesConfig(this.executor, NftablesConfigConstants.FAMILY_V6);
    }

    @Test
    public void applyRulesBootstrapsTableAndBaselineTest() throws KuraException {
        this.configV4.applyRules();

        String script = this.executor.lastScript();
        assertTrue(script.contains("add table ip filter"));
        assertTrue(script.contains("flush table ip filter"));
        assertTrue(script.contains("add table ip nat"));
        assertTrue(script.contains("flush table ip nat"));
        assertTrue(script.contains("add table ip mangle"));
        assertTrue(script.contains("flush table ip mangle"));
        assertTrue(script
                .contains("add chain ip filter input { type filter hook input priority filter; policy drop; }"));
        assertTrue(script.contains("add rule ip filter input jump input_kura"));
        assertTrue(script.contains("add rule ip filter input_kura iifname \"lo\" accept"));
        assertTrue(script.contains("add rule ip filter input_kura ct state established,related accept"));
        assertTrue(script.contains("icmp type { echo-request, echo-reply } accept"));
        assertFalse(script.contains("icmpv6"));
    }

    @Test
    public void applyRulesUsesIp6FamilyAndIcmpv6AllowListTest() throws KuraException {
        this.configV6.applyRules();

        String script = this.executor.lastScript();
        assertTrue(script.contains("add table ip6 filter"));
        assertTrue(script.contains("flush table ip6 filter"));
        assertTrue(script.contains("add rule ip6 filter input_kura icmpv6 type"));
        assertFalse(script.contains("icmp type { echo-request, echo-reply } accept"));
    }

    @Test
    public void applyRulesOmitsIcmpAllowListWhenDisabledTest() throws KuraException {
        this.configV4.setAllowIcmp(false);

        this.configV4.applyRules();

        assertFalse(this.executor.lastScript().contains("icmp type"));
    }

    @Test
    public void applyRulesIncludesLocalRuleStatementTest() throws KuraException {
        Set<LocalRule> localRules = new LinkedHashSet<>();
        localRules.add(new LocalRule(22, "tcp", null, null, null, null, null));
        this.configV4.setLocalRules(localRules);

        this.configV4.applyRules();

        assertTrue(this.executor.lastScript().contains("add rule ip filter input_kura tcp dport 22 accept"));
    }

    @Test
    public void applyRulesIncludesPortForwardStatementsTest() throws KuraException {
        Set<PortForwardRule> portForwardRules = new LinkedHashSet<>();
        portForwardRules.add(new PortForwardRule().inboundIface("ppp0").outboundIface("eth0").address("10.0.0.5")
                .protocol("tcp").inPort(8080).outPort(80).masquerade(true));
        this.configV4.setPortForwardRules(portForwardRules);

        this.configV4.applyRules();

        String script = this.executor.lastScript();
        assertTrue(script.contains("dnat to 10.0.0.5:80"));
        assertTrue(script.contains("postrouting_kura_pf oifname \"eth0\" meta l4proto tcp ip daddr 10.0.0.5/32"));
        assertTrue(script.contains("forward_kura_pf"));
    }

    @Test
    public void applyRulesIncludesAdditionalRulesVerbatimTest() throws KuraException {
        this.configV4.setAdditionalFilterRules(
                Collections.singleton("add rule ip filter forward_kura tcp flags syn limit rate 10/second accept"));

        this.configV4.applyRules();

        assertTrue(this.executor.lastScript()
                .contains("add rule ip filter forward_kura tcp flags syn limit rate 10/second accept"));
    }

    @Test(expected = KuraException.class)
    public void applyRulesThrowsOnExecutorFailureTest() throws KuraException {
        this.executor.nextResultSuccess = false;
        this.executor.nextErrorBuffer = "boom";

        this.configV4.applyRules();
    }

    @Test
    public void clearAllChainsDeletesAllThreeTablesIndependentlyTest() throws KuraException {
        this.configV4.clearAllChains();
        assertTrue(this.executor.submittedScripts.contains("delete table ip filter"));
        assertTrue(this.executor.submittedScripts.contains("delete table ip nat"));
        assertTrue(this.executor.submittedScripts.contains("delete table ip mangle"));
        assertFalse(this.executor.submittedScripts.stream().anyMatch(s -> s.contains("ip6")));

        this.executor.submittedScripts.clear();
        this.configV6.clearAllChains();
        assertTrue(this.executor.submittedScripts.contains("delete table ip6 filter"));
        assertTrue(this.executor.submittedScripts.contains("delete table ip6 nat"));
        assertTrue(this.executor.submittedScripts.contains("delete table ip6 mangle"));
    }

    @Test
    public void clearAllChainsTolerantOfMissingTableTest() throws KuraException {
        this.executor.nextResultSuccess = false;
        this.executor.nextErrorBuffer = "No such file or directory";

        this.configV4.clearAllChains();
    }

    @Test(expected = KuraException.class)
    public void clearAllChainsThrowsOnOtherFailuresTest() throws KuraException {
        this.executor.nextResultSuccess = false;
        this.executor.nextErrorBuffer = "permission denied";

        this.configV4.clearAllChains();
    }

    @Test
    public void applyBlockPolicyOnlyAllowsLoopbackAndEstablishedTest() throws KuraException {
        this.configV4.applyBlockPolicy();

        String script = this.executor.lastScript();
        assertTrue(script.contains("add rule ip filter input_kura iifname \"lo\" accept"));
        assertTrue(script.contains("add rule ip filter input_kura ct state established,related accept"));
        assertFalse(script.contains("icmp"));
    }

    @Test
    public void autoNatRuleWithoutMasqueradeStillEmitsForwardAcceptTest() throws KuraException {
        Set<NATRule> autoNatRules = new LinkedHashSet<>();
        autoNatRules.add(new NATRule("eth0", "ppp0", false, RuleType.GENERIC));
        this.configV4.setAutoNatRules(autoNatRules);

        this.configV4.applyRules();

        String script = this.executor.lastScript();
        assertFalse(script.contains("masquerade"));
        assertTrue(script.contains("forward_kura iifname \"eth0\" oifname \"ppp0\" counter accept"));
    }
}
