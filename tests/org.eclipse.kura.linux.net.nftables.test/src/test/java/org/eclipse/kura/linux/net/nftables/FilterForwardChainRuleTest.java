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

import java.util.List;

import org.eclipse.kura.net.firewall.RuleType;
import org.junit.Test;

public class FilterForwardChainRuleTest {

    @Test
    public void toNftStatementsForPortForwardingTest() {
        FilterForwardChainRule rule = new FilterForwardChainRule().inputInterface("ppp0").outputInterface("eth0")
                .dstNetwork("10.0.0.5").dstMask((short) 32).protocol("tcp").dstPort(8080)
                .type(RuleType.PORT_FORWARDING);

        List<String> statements = rule.toNftStatements();

        assertEquals(2, statements.size());
        assertEquals(
                "forward_kura_pf iifname \"ppp0\" oifname \"eth0\" ip daddr 10.0.0.5/32 tcp dport 8080 counter accept",
                statements.get(0));
        assertEquals(
                "forward_kura_pf iifname \"eth0\" oifname \"ppp0\" meta l4proto tcp ip saddr 10.0.0.5/32 ct state established,related counter accept",
                statements.get(1));
    }

    @Test
    public void chainNameVariesByRuleTypeTest() {
        FilterForwardChainRule generic = new FilterForwardChainRule().inputInterface("eth0").outputInterface("ppp0")
                .type(RuleType.GENERIC);
        FilterForwardChainRule ipForwarding = new FilterForwardChainRule().inputInterface("eth0")
                .outputInterface("ppp0").type(RuleType.IP_FORWARDING);

        assertEquals("forward_kura iifname \"eth0\" oifname \"ppp0\" counter accept",
                generic.toNftStatements().get(0));
        assertEquals("forward_kura_ipf iifname \"eth0\" oifname \"ppp0\" counter accept",
                ipForwarding.toNftStatements().get(0));
    }

    @Test
    public void protocolWithoutPortsUsesMetaL4protoOnBothDirectionsTest() {
        // Mirrors NATRule.getFilterForwardChainRule(): protocol set, no ports at all.
        FilterForwardChainRule rule = new FilterForwardChainRule().inputInterface("eth0").outputInterface("ppp0")
                .srcNetwork("192.168.1.0").srcMask((short) 24).protocol("tcp").srcPortFirst(0).srcPortLast(0)
                .type(RuleType.IP_FORWARDING);

        List<String> statements = rule.toNftStatements();

        assertEquals(
                "forward_kura_ipf iifname \"eth0\" oifname \"ppp0\" ip saddr 192.168.1.0/24 meta l4proto tcp counter accept",
                statements.get(0));
        assertEquals(
                "forward_kura_ipf iifname \"ppp0\" oifname \"eth0\" meta l4proto tcp ct state established,related counter accept",
                statements.get(1));
    }
}
