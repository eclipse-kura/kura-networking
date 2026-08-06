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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.kura.KuraException;
import org.junit.Test;

public class LinuxFirewallTest extends FirewallTestUtils {

    private LinuxFirewall getFreshInstance() throws IOException {
        setUpMock();
        LinuxFirewall firewall = LinuxFirewall.getInstance(this.executor);
        firewall.persistenceStore = newTempPersistenceStore();
        return firewall;
    }

    @Test
    public void getInstanceReturnsSameSingletonTest() throws IOException {
        LinuxFirewall first = getFreshInstance();
        LinuxFirewall second = LinuxFirewall.getInstance(this.executor);

        assertSame(first, second);
    }

    @Test
    public void addLocalRuleTest() throws KuraException, IOException {
        LinuxFirewall firewall = getFreshInstance();

        LocalRule localRule = new LocalRule(5400, "tcp", null, "eth0", null, "00:11:22:33:44:55:66", "10100:10200");
        firewall.addLocalRules(Arrays.asList(localRule));

        assertTrue(firewall.getLocalRules().stream()
                .anyMatch(rule -> rule.getPort() == 5400 && "tcp".equals(rule.getProtocol())
                        && "eth0".equals(rule.getPermittedInterfaceName())
                        && "00:11:22:33:44:55:66".equals(rule.getPermittedMAC())
                        && "10100:10200".equals(rule.getSourcePortRange())));
        assertTrue(this.executor.lastScript().contains("add rule ip filter input_kura"));
    }

    @Test
    public void addPortForwardRuleTest() throws KuraException, IOException {
        LinuxFirewall firewall = getFreshInstance();

        PortForwardRule rule = new PortForwardRule().inboundIface("ppp0").outboundIface("eth0").address("10.0.0.5")
                .protocol("tcp").inPort(8080).outPort(80).masquerade(true);
        firewall.addPortForwardRules(Arrays.asList(rule));

        assertTrue(firewall.getPortForwardRules().stream().anyMatch(r -> r.getInPort() == 8080));
        assertTrue(this.executor.lastScript().contains("dnat to 10.0.0.5:80"));
    }

    @Test
    public void disableRemovesKuraTablesTest() throws KuraException, IOException {
        LinuxFirewall firewall = getFreshInstance();

        firewall.disable();

        assertTrue(this.executor.submittedScripts.contains("delete table ip filter"));
        assertTrue(this.executor.submittedScripts.contains("delete table ip nat"));
        assertTrue(this.executor.submittedScripts.contains("delete table ip mangle"));
    }
}
