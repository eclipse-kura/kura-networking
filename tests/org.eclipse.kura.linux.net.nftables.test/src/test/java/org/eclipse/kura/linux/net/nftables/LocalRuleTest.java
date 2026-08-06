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

import java.net.UnknownHostException;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.junit.Test;

public class LocalRuleTest {

    @Test
    public void toNftStatementWithNetworkInterfaceMacAndPortRangeTest() throws UnknownHostException {
        LocalRule localRule = new LocalRule("1400:1401", "udp",
                new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("192.168.0.0"), Short.parseShort("24")),
                "eth2", null, "00:11:22:33:44:55:66", "10100:10200");

        assertEquals(
                "input_kura ip saddr 192.168.0.0/24 iifname \"eth2\" ether saddr 00:11:22:33:44:55:66 udp sport 10100-10200 udp dport 1400-1401 accept",
                localRule.toString());
    }

    @Test
    public void toNftStatementWithUnpermittedInterfaceTest() {
        LocalRule localRule = new LocalRule(8080, "tcp", null, null, "eth1", null, null);

        assertEquals("input_kura iifname != \"eth1\" tcp dport 8080 accept", localRule.toString());
    }

    @Test
    public void toNftStatementWithSinglePortNoRestrictionsTest() {
        LocalRule localRule = new LocalRule(22, "tcp", null, null, null, null, null);

        assertEquals("input_kura tcp dport 22 accept", localRule.toString());
    }

    @Test
    public void isCompleteTest() {
        LocalRule complete = new LocalRule(22, "tcp", null, null, null, null, null);
        assertTrue(complete.isComplete());

        LocalRule incomplete = new LocalRule();
        assertFalse(incomplete.isComplete());
    }

    @Test
    public void equalsAndHashCodeTest() {
        LocalRule a = new LocalRule(22, "tcp", null, "eth0", null, null, null);
        LocalRule b = new LocalRule(22, "tcp", null, "eth0", null, null, null);
        LocalRule c = new LocalRule(23, "tcp", null, "eth0", null, null, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertFalse(a.equals(c));
    }
}
