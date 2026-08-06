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

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.firewall.RuleType;
import org.junit.Test;

public class NftPersistenceStoreTest {

    private NftPersistenceStore newStore() throws Exception {
        File cfg = File.createTempFile("nft-persist-test", ".conf");
        File tmp = File.createTempFile("nft-persist-test-tmp", ".conf");
        cfg.deleteOnExit();
        tmp.deleteOnExit();
        java.nio.file.Files.deleteIfExists(cfg.toPath());
        return new NftPersistenceStore(cfg.getAbsolutePath(), tmp.getAbsolutePath());
    }

    @Test
    public void loadReturnsEmptyWhenFileMissingTest() throws Exception {
        NftPersistenceStore store = newStore();

        Optional<NftRulesetModel> model = store.load();

        assertFalse(model.isPresent());
    }

    @Test
    public void roundTripsAllRuleTypesTest() throws Exception {
        NftPersistenceStore store = newStore();

        NftRulesetModel model = new NftRulesetModel();
        model.setAllowIcmp(false);

        Set<LocalRule> localRules = new LinkedHashSet<>();
        localRules.add(new LocalRule(5400, "tcp", null, "eth0", null, "00:11:22:33:44:55:66", "10100:10200"));
        model.setLocalRules(localRules);

        Set<PortForwardRule> portForwardRules = new LinkedHashSet<>();
        portForwardRules.add(new PortForwardRule().inboundIface("ppp0").outboundIface("eth0").address("10.0.0.5")
                .addressMask((short) 32).protocol("tcp").inPort(8080).outPort(80).masquerade(true)
                .permittedNetwork("192.168.1.0").permittedNetworkMask((short) 24).permittedMAC("AA:BB:CC:DD:EE:FF")
                .sourcePortRange("1024:2048"));
        model.setPortForwardRules(portForwardRules);

        Set<NATRule> autoNatRules = new LinkedHashSet<>();
        autoNatRules.add(new NATRule("eth0", "ppp0", true, RuleType.GENERIC));
        model.setAutoNatRules(autoNatRules);

        Set<NATRule> natRules = new LinkedHashSet<>();
        natRules.add(new NATRule("eth1", "ppp1", "tcp", "192.168.1.0/24", "0.0.0.0/0", true, RuleType.IP_FORWARDING));
        model.setNatRules(natRules);

        model.setAdditionalFilterRules(Collections.singleton("add rule inet kura input_kura tcp dport 9 accept"));

        store.save(model);
        Optional<NftRulesetModel> loaded = store.load();

        assertTrue(loaded.isPresent());
        NftRulesetModel result = loaded.get();
        assertFalse(result.isAllowIcmp());
        assertEquals(localRules, result.getLocalRules());
        assertEquals(portForwardRules, result.getPortForwardRules());
        assertEquals(autoNatRules, result.getAutoNatRules());
        assertEquals(natRules, result.getNatRules());
        assertEquals(Collections.singleton("add rule inet kura input_kura tcp dport 9 accept"),
                result.getAdditionalFilterRules());
    }

    @Test(expected = KuraException.class)
    public void saveFailsWhenTargetDirectoryDoesNotExistTest() throws Exception {
        NftPersistenceStore store = new NftPersistenceStore("/nonexistent-dir-xyz/nftables_kura.conf",
                "/nonexistent-dir-xyz/nftables_kura.tmp");

        store.save(new NftRulesetModel());
    }
}
