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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.net.firewall.RuleType;

/**
 * Saves/loads an {@link NftRulesetModel} as the source of truth for
 * restart-restore, using a simple internal line-based format this class
 * fully owns (one rule per tagged, "|"-delimited line) - deliberately not
 * nft-syntax text, so restore never needs to reverse-parse rule fragments the
 * way the iptables-era engine did.
 */
public class NftPersistenceStore {

    private static final String DELIM = "\\|";
    private static final String JOIN_DELIM = "|";

    private static final String TAG_LOCAL = "LOCAL";
    private static final String TAG_PORTFWD = "PORTFWD";
    private static final String TAG_AUTONAT = "AUTONAT";
    private static final String TAG_NAT = "NAT";
    private static final String TAG_ALLOW_ICMP = "ALLOW_ICMP";
    private static final String TAG_ADD_FILTER = "ADDFILTER";
    private static final String TAG_ADD_NAT = "ADDNAT";
    private static final String TAG_ADD_MANGLE = "ADDMANGLE";

    private final String fileName;
    private final String tmpFileName;

    public NftPersistenceStore(String fileName, String tmpFileName) {
        this.fileName = fileName;
        this.tmpFileName = tmpFileName;
    }

    public void save(NftRulesetModel model) throws KuraException {
        List<String> lines = new ArrayList<>();

        lines.add(TAG_ALLOW_ICMP + JOIN_DELIM + model.isAllowIcmp());

        for (LocalRule rule : model.getLocalRules()) {
            lines.add(String.join(JOIN_DELIM, TAG_LOCAL, str(rule.getPort()), nz(rule.getPortRange()),
                    nz(rule.getProtocol()), nz(rule.getPermittedNetworkString()), nz(rule.getPermittedInterfaceName()),
                    nz(rule.getUnpermittedInterfaceName()), nz(rule.getPermittedMAC()),
                    nz(rule.getSourcePortRange())));
        }

        for (PortForwardRule rule : model.getPortForwardRules()) {
            lines.add(String.join(JOIN_DELIM, TAG_PORTFWD, nz(rule.getInboundIface()), nz(rule.getOutboundIface()),
                    nz(rule.getAddress()), str(rule.getAddressMask()), nz(rule.getProtocol()), str(rule.getInPort()),
                    str(rule.getOutPort()), str(rule.isMasquerade()), nz(rule.getPermittedNetwork()),
                    str(rule.getPermittedNetworkMask()), nz(rule.getPermittedMAC()), str(rule.getSourcePortStart()),
                    str(rule.getSourcePortEnd())));
        }

        for (NATRule rule : model.getAutoNatRules()) {
            lines.add(natRuleLine(TAG_AUTONAT, rule));
        }
        for (NATRule rule : model.getNatRules()) {
            lines.add(natRuleLine(TAG_NAT, rule));
        }

        for (String rule : model.getAdditionalFilterRules()) {
            lines.add(TAG_ADD_FILTER + JOIN_DELIM + rule);
        }
        for (String rule : model.getAdditionalNatRules()) {
            lines.add(TAG_ADD_NAT + JOIN_DELIM + rule);
        }
        for (String rule : model.getAdditionalMangleRules()) {
            lines.add(TAG_ADD_MANGLE + JOIN_DELIM + rule);
        }

        writeAtomically(lines);
    }

    public Optional<NftRulesetModel> load() throws KuraException {
        Path path = Paths.get(this.fileName);
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        NftRulesetModel model = new NftRulesetModel();
        Set<LocalRule> localRules = new LinkedHashSet<>();
        Set<PortForwardRule> portForwardRules = new LinkedHashSet<>();
        Set<NATRule> autoNatRules = new LinkedHashSet<>();
        Set<NATRule> natRules = new LinkedHashSet<>();
        Set<String> additionalFilterRules = new LinkedHashSet<>();
        Set<String> additionalNatRules = new LinkedHashSet<>();
        Set<String> additionalMangleRules = new LinkedHashSet<>();

        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                int firstDelim = line.indexOf('|');
                String tag = firstDelim < 0 ? line : line.substring(0, firstDelim);
                String[] fields = line.split(DELIM, -1);

                switch (tag) {
                case TAG_ALLOW_ICMP:
                    model.setAllowIcmp(Boolean.parseBoolean(fields[1]));
                    break;
                case TAG_LOCAL:
                    localRules.add(parseLocalRule(fields));
                    break;
                case TAG_PORTFWD:
                    portForwardRules.add(parsePortForwardRule(fields));
                    break;
                case TAG_AUTONAT:
                    autoNatRules.add(parseNatRule(fields));
                    break;
                case TAG_NAT:
                    natRules.add(parseNatRule(fields));
                    break;
                case TAG_ADD_FILTER:
                    additionalFilterRules.add(line.substring(firstDelim + 1));
                    break;
                case TAG_ADD_NAT:
                    additionalNatRules.add(line.substring(firstDelim + 1));
                    break;
                case TAG_ADD_MANGLE:
                    additionalMangleRules.add(line.substring(firstDelim + 1));
                    break;
                default:
                    break;
                }
            }
        } catch (IOException e) {
            throw new KuraIOException(e, "Failed to read " + this.fileName);
        }

        model.setLocalRules(localRules);
        model.setPortForwardRules(portForwardRules);
        model.setAutoNatRules(autoNatRules);
        model.setNatRules(natRules);
        model.setAdditionalFilterRules(additionalFilterRules);
        model.setAdditionalNatRules(additionalNatRules);
        model.setAdditionalMangleRules(additionalMangleRules);

        return Optional.of(model);
    }

    private String natRuleLine(String tag, NATRule rule) {
        return String.join(JOIN_DELIM, tag, nz(rule.getSourceInterface()), nz(rule.getDestinationInterface()),
                nz(rule.getProtocol()), nz(rule.getSource()), nz(rule.getDestination()), str(rule.isMasquerade()),
                rule.getRuleType() == null ? "" : rule.getRuleType().name());
    }

    private LocalRule parseLocalRule(String[] f) {
        // LOCAL|port|portRange|protocol|permittedNetworkString|permittedInterfaceName|unpermittedInterfaceName|permittedMAC|sourcePortRange
        LocalRule rule = new LocalRule();
        int port = Integer.parseInt(f[1]);
        if (port != -1) {
            rule.setPort(port);
        } else {
            rule.setPortRange(emptyToNull(f[2]));
        }
        rule.setProtocol(emptyToNull(f[3]));
        rule.setPermittedNetworkString(emptyToNull(f[4]));
        rule.setPermittedInterfaceName(emptyToNull(f[5]));
        rule.setUnpermittedInterfaceName(emptyToNull(f[6]));
        rule.setPermittedMAC(emptyToNull(f[7]));
        rule.setSourcePortRange(emptyToNull(f[8]));
        return rule;
    }

    private PortForwardRule parsePortForwardRule(String[] f) {
        // PORTFWD|inboundIface|outboundIface|address|addressMask|protocol|inPort|outPort|masquerade|
        // permittedNetwork|permittedNetworkMask|permittedMAC|sourcePortStart|sourcePortEnd
        PortForwardRule rule = new PortForwardRule().inboundIface(emptyToNull(f[1])).outboundIface(emptyToNull(f[2]))
                .address(emptyToNull(f[3])).addressMask(Short.parseShort(f[4])).protocol(emptyToNull(f[5]))
                .inPort(Integer.parseInt(f[6])).outPort(Integer.parseInt(f[7]))
                .masquerade(Boolean.parseBoolean(f[8])).permittedNetwork(emptyToNull(f[9]))
                .permittedNetworkMask(Short.parseShort(f[10])).permittedMAC(emptyToNull(f[11]));
        int sourcePortStart = Integer.parseInt(f[12]);
        int sourcePortEnd = Integer.parseInt(f[13]);
        if (sourcePortStart > 0 && sourcePortEnd > 0) {
            rule.sourcePortRange(sourcePortStart + ":" + sourcePortEnd);
        }
        return rule;
    }

    private NATRule parseNatRule(String[] f) {
        // (AUTO)NAT|sourceInterface|destinationInterface|protocol|source|destination|masquerade|type
        String sourceInterface = emptyToNull(f[1]);
        String destinationInterface = emptyToNull(f[2]);
        String protocol = emptyToNull(f[3]);
        String source = emptyToNull(f[4]);
        String destination = emptyToNull(f[5]);
        boolean masquerade = Boolean.parseBoolean(f[6]);
        RuleType type = f.length > 7 && !f[7].isEmpty() ? RuleType.valueOf(f[7]) : RuleType.GENERIC;

        if (protocol == null) {
            return new NATRule(sourceInterface, destinationInterface, masquerade, type);
        }
        return new NATRule(sourceInterface, destinationInterface, protocol, source, destination, masquerade, type);
    }

    private void writeAtomically(List<String> lines) throws KuraException {
        try {
            Path tmpPath = Paths.get(this.tmpFileName);
            Files.write(tmpPath, lines, StandardCharsets.UTF_8);
            Files.move(tmpPath, Paths.get(this.fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new KuraIOException(e, "Failed to persist " + this.fileName);
        }
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static String str(int value) {
        return Integer.toString(value);
    }

    private static String str(boolean value) {
        return Boolean.toString(value);
    }
}
