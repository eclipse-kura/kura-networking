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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class PortForwardRuleTest {

    @Test
    public void filterForwardChainRuleDoesNotMatchOnExternalPortTest() {
        // externalPort (3040) is only valid before DNAT rewrites it in prerouting;
        // by the time a packet reaches the forward chain the DNAT has already
        // happened, so the forward-accept rule must never reference inPort - the
        // dstNetwork restriction (the specific internal host) is narrow enough.
        PortForwardRule rule = new PortForwardRule().inboundIface("eth0").outboundIface("eth1")
                .address("172.16.0.110").addressMask((short) 32).protocol("tcp").inPort(3040).outPort(4050);

        List<String> statements = rule.getFilterForwardChainRule().toNftStatements();

        assertFalse(statements.get(0).contains("3040"));
        assertEquals(
                "forward_kura_pf iifname \"eth0\" oifname \"eth1\" ip daddr 172.16.0.110/32 meta l4proto tcp counter accept",
                statements.get(0));
        assertEquals(
                "forward_kura_pf iifname \"eth1\" oifname \"eth0\" meta l4proto tcp ip saddr 172.16.0.110/32 ct state established,related counter accept",
                statements.get(1));
    }

    @Test
    public void filterForwardChainRuleIncludesPermittedNetworkRestrictionTest() {
        PortForwardRule rule = new PortForwardRule().inboundIface("eth0").outboundIface("eth1")
                .address("172.16.0.110").addressMask((short) 32).protocol("tcp").inPort(3040).outPort(4050)
                .permittedNetwork("192.168.1.0").permittedNetworkMask((short) 24);

        String forwardStatement = rule.getFilterForwardChainRule().toNftStatements().get(0);

        assertTrue(forwardStatement.contains("ip saddr 192.168.1.0/24"));
    }

    @Test
    public void natPreroutingChainRuleDnatsToInternalPortTest() {
        PortForwardRule rule = new PortForwardRule().inboundIface("eth0").outboundIface("eth1")
                .address("172.16.0.110").addressMask((short) 32).protocol("tcp").inPort(3040).outPort(4050);

        String dnatStatement = rule.getNatPreroutingChainRule().toString();

        assertTrue(dnatStatement.contains("dport 3040"));
        assertTrue(dnatStatement.contains("dnat to 172.16.0.110:4050"));
    }

    @Test
    public void natPostroutingChainRuleScopedToDestinationAndProtocolTest() {
        // Must not be the bare interface-only auto-nat form - that would masquerade
        // *all* traffic leaving outboundIface, not just this forwarded connection.
        PortForwardRule rule = new PortForwardRule().inboundIface("eth0").outboundIface("eth1")
                .address("172.16.0.110").addressMask((short) 32).protocol("tcp").inPort(3040).outPort(4050)
                .masquerade(true);

        String masqueradeStatement = rule.getNatPostroutingChainRule().toNftStatement().get();

        assertEquals(
                "postrouting_kura_pf oifname \"eth1\" meta l4proto tcp ip daddr 172.16.0.110/32 counter masquerade",
                masqueradeStatement);
    }
}
