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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.kura.net.firewall.RuleType;

/**
 * Builds the nft DNAT statement for a port-forward rule.
 */
public class NatPreroutingChainRule {

    private static final List<String> NO_RESTRICTION_SENTINELS = Arrays.asList("0.0.0.0", "::",
            "0:0:0:0:0:0:0:0:0");

    private String inputInterface;
    private String protocol;
    private int externalPort;
    private int internalPort;
    private int srcPortFirst;
    private int srcPortLast;
    private String dstIpAddress;
    private String permittedNetwork;
    private short permittedNetworkMask;
    private String permittedMacAddress;
    private RuleType type = RuleType.GENERIC;

    public NatPreroutingChainRule() {
        // fluent builder, use setters
    }

    public NatPreroutingChainRule inputInterface(String inputInterface) {
        this.inputInterface = inputInterface;
        return this;
    }

    public NatPreroutingChainRule protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public NatPreroutingChainRule externalPort(int externalPort) {
        this.externalPort = externalPort;
        return this;
    }

    public NatPreroutingChainRule internalPort(int internalPort) {
        this.internalPort = internalPort;
        return this;
    }

    public NatPreroutingChainRule srcPortFirst(int srcPortFirst) {
        this.srcPortFirst = srcPortFirst;
        return this;
    }

    public NatPreroutingChainRule srcPortLast(int srcPortLast) {
        this.srcPortLast = srcPortLast;
        return this;
    }

    public NatPreroutingChainRule dstIpAddress(String dstIpAddress) {
        this.dstIpAddress = dstIpAddress;
        return this;
    }

    public NatPreroutingChainRule permittedNetwork(String permittedNetwork) {
        this.permittedNetwork = permittedNetwork;
        return this;
    }

    public NatPreroutingChainRule permittedNetworkMask(short permittedNetworkMask) {
        this.permittedNetworkMask = permittedNetworkMask;
        return this;
    }

    public NatPreroutingChainRule permittedMacAddress(String permittedMacAddress) {
        this.permittedMacAddress = permittedMacAddress;
        return this;
    }

    public NatPreroutingChainRule type(RuleType type) {
        this.type = type;
        return this;
    }

    private String chainName() {
        // Prerouting/DNAT only ever applies to port-forward rules - plain NAT
        // (auto or IP_FORWARDING) rules have no DNAT component, only a
        // postrouting MASQUERADE and a forward accept pair.
        return NftablesConfigConstants.PREROUTING_KURA_PF_CHAIN;
    }

    /**
     * Builds the DNAT statement, e.g.:
     *
     * <pre>
     * add rule inet kura prerouting_kura_pf ip saddr 192.168.1.0/24 iifname "eth0" ether saddr AA:BB:CC:DD:EE:FF tcp sport 1024-2048 tcp dport 8080 dnat to 10.0.0.5:80
     * </pre>
     */
    public String toNftStatement() {
        StringBuilder sb = new StringBuilder();

        if (this.permittedNetwork != null && !NO_RESTRICTION_SENTINELS.contains(this.permittedNetwork)) {
            sb.append(NftSyntaxUtil.family(this.permittedNetwork)).append(" saddr ").append(this.permittedNetwork)
                    .append('/').append(this.permittedNetworkMask).append(' ');
        }
        if (this.inputInterface != null) {
            sb.append("iifname \"").append(this.inputInterface).append("\" ");
        }
        if (this.permittedMacAddress != null) {
            sb.append("ether saddr ").append(this.permittedMacAddress).append(' ');
        }
        if (this.srcPortFirst > 0) {
            sb.append(this.protocol).append(" sport ").append(this.srcPortFirst).append('-').append(this.srcPortLast)
                    .append(' ');
        }
        sb.append(this.protocol).append(" dport ").append(this.externalPort).append(" counter dnat to ")
                .append(NftSyntaxUtil.bracketIfIPv6(this.dstIpAddress)).append(':').append(this.internalPort);

        return NftablesConfigConstants.fragment(chainName(), sb.toString());
    }

    @Override
    public String toString() {
        return toNftStatement();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NatPreroutingChainRule)) {
            return false;
        }
        NatPreroutingChainRule other = (NatPreroutingChainRule) obj;
        return Objects.equals(this.inputInterface, other.inputInterface)
                && Objects.equals(this.protocol, other.protocol) && this.externalPort == other.externalPort
                && this.internalPort == other.internalPort && this.srcPortFirst == other.srcPortFirst
                && this.srcPortLast == other.srcPortLast && Objects.equals(this.dstIpAddress, other.dstIpAddress)
                && Objects.equals(this.permittedNetwork, other.permittedNetwork)
                && this.permittedNetworkMask == other.permittedNetworkMask
                && Objects.equals(this.permittedMacAddress, other.permittedMacAddress) && this.type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.inputInterface, this.protocol, this.externalPort, this.internalPort,
                this.srcPortFirst, this.srcPortLast, this.dstIpAddress, this.permittedNetwork,
                this.permittedNetworkMask, this.permittedMacAddress, this.type);
    }
}
