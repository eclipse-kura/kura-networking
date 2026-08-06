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

import org.eclipse.kura.net.firewall.RuleType;
import org.junit.Test;

public class NatPreroutingChainRuleTest {

    @Test
    public void toNftStatementIPv4WithRestrictionsTest() {
        NatPreroutingChainRule rule = new NatPreroutingChainRule().inputInterface("eth0").protocol("tcp")
                .externalPort(8080).internalPort(80).srcPortFirst(1024).srcPortLast(2048)
                .dstIpAddress("10.0.0.5").permittedNetwork("192.168.1.0").permittedNetworkMask((short) 24)
                .permittedMacAddress("AA:BB:CC:DD:EE:FF").type(RuleType.PORT_FORWARDING);

        assertEquals(
                "prerouting_kura_pf ip saddr 192.168.1.0/24 iifname \"eth0\" ether saddr AA:BB:CC:DD:EE:FF tcp sport 1024-2048 tcp dport 8080 counter dnat to 10.0.0.5:80",
                rule.toString());
    }

    @Test
    public void toNftStatementIPv6DestinationIsBracketedTest() {
        NatPreroutingChainRule rule = new NatPreroutingChainRule().inputInterface("eth0").protocol("tcp")
                .externalPort(8080).internalPort(80).dstIpAddress("fd00::5").type(RuleType.PORT_FORWARDING);

        assertEquals(
                "prerouting_kura_pf iifname \"eth0\" tcp dport 8080 counter dnat to [fd00::5]:80",
                rule.toString());
    }

    @Test
    public void noRestrictionSentinelSuppressesSaddrClauseTest() {
        NatPreroutingChainRule rule = new NatPreroutingChainRule().inputInterface("eth0").protocol("tcp")
                .externalPort(8080).internalPort(80).dstIpAddress("10.0.0.5").permittedNetwork("0.0.0.0")
                .permittedNetworkMask((short) 0).type(RuleType.PORT_FORWARDING);

        assertEquals("prerouting_kura_pf iifname \"eth0\" tcp dport 8080 counter dnat to 10.0.0.5:80",
                rule.toString());
    }
}
