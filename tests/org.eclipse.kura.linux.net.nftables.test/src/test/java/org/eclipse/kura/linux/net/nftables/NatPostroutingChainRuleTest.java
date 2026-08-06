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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.firewall.RuleType;
import org.junit.Test;

public class NatPostroutingChainRuleTest {

    @Test
    public void autoNatMasqueradeStatementTest() {
        NatPostroutingChainRule rule = new NatPostroutingChainRule("eth1", true, RuleType.GENERIC);

        assertTrue(rule.toNftStatement().isPresent());
        assertEquals("postrouting_kura oifname \"eth1\" counter masquerade",
                rule.toNftStatement().get());
    }

    @Test
    public void autoNatWithoutMasqueradeProducesNothingTest() {
        NatPostroutingChainRule rule = new NatPostroutingChainRule("eth1", false, RuleType.GENERIC);

        assertFalse(rule.toNftStatement().isPresent());
    }

    @Test
    public void fullNatMasqueradeStatementTest() throws KuraException {
        NatPostroutingChainRule rule = new NatPostroutingChainRule("eth1", "tcp", null, "192.168.1.0/24", true,
                RuleType.IP_FORWARDING);

        assertEquals("postrouting_kura_ipf oifname \"eth1\" meta l4proto tcp ip saddr 192.168.1.0/24 counter masquerade",
                rule.toNftStatement().get());
    }

    @Test
    public void portForwardingChainNameTest() {
        NatPostroutingChainRule rule = new NatPostroutingChainRule("eth1", true, RuleType.PORT_FORWARDING);

        assertEquals("postrouting_kura_pf oifname \"eth1\" counter masquerade",
                rule.toNftStatement().get());
    }
}
