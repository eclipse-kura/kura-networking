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
 * Builds the pair of nft statements (forward direction + established/related
 * return direction) that accept forwarded traffic for a NAT or port-forward
 * rule.
 */
public class FilterForwardChainRule {

    private String inputInterface;
    private String outputInterface;
    private String srcNetwork;
    private short srcMask;
    private String dstNetwork;
    private short dstMask;
    private String protocol;
    private String permittedMacAddress;
    private int srcPortFirst;
    private int srcPortLast;
    private int dstPort;
    private RuleType type = RuleType.GENERIC;

    public FilterForwardChainRule() {
        // fluent builder, use setters
    }

    public FilterForwardChainRule inputInterface(String inputInterface) {
        this.inputInterface = inputInterface;
        return this;
    }

    public FilterForwardChainRule outputInterface(String outputInterface) {
        this.outputInterface = outputInterface;
        return this;
    }

    public FilterForwardChainRule srcNetwork(String srcNetwork) {
        this.srcNetwork = srcNetwork;
        return this;
    }

    public FilterForwardChainRule srcMask(short srcMask) {
        this.srcMask = srcMask;
        return this;
    }

    public FilterForwardChainRule dstNetwork(String dstNetwork) {
        this.dstNetwork = dstNetwork;
        return this;
    }

    public FilterForwardChainRule dstMask(short dstMask) {
        this.dstMask = dstMask;
        return this;
    }

    public FilterForwardChainRule protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public FilterForwardChainRule permittedMacAddress(String permittedMacAddress) {
        this.permittedMacAddress = permittedMacAddress;
        return this;
    }

    public FilterForwardChainRule srcPortFirst(int srcPortFirst) {
        this.srcPortFirst = srcPortFirst;
        return this;
    }

    public FilterForwardChainRule srcPortLast(int srcPortLast) {
        this.srcPortLast = srcPortLast;
        return this;
    }

    public FilterForwardChainRule dstPort(int dstPort) {
        this.dstPort = dstPort;
        return this;
    }

    public FilterForwardChainRule type(RuleType type) {
        this.type = type;
        return this;
    }

    public String getInputInterface() {
        return this.inputInterface;
    }

    public String getOutputInterface() {
        return this.outputInterface;
    }

    public String getSrcNetwork() {
        return this.srcNetwork;
    }

    public String getDstNetwork() {
        return this.dstNetwork;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public RuleType getType() {
        return this.type;
    }

    private String chainName() {
        if (this.type == RuleType.IP_FORWARDING) {
            return NftablesConfigConstants.FORWARD_KURA_IPF_CHAIN;
        } else if (this.type == RuleType.PORT_FORWARDING) {
            return NftablesConfigConstants.FORWARD_KURA_PF_CHAIN;
        }
        return NftablesConfigConstants.FORWARD_KURA_CHAIN;
    }

    /**
     * Returns the forward-direction accept statement followed by the reverse
     * (established/related) accept statement, e.g.:
     *
     * <pre>
     * add rule inet kura forward_kura_pf iifname "eth0" oifname "eth1" ip daddr 10.0.0.5/32 tcp dport 80 accept
     * add rule inet kura forward_kura_pf iifname "eth1" oifname "eth0" ip saddr 10.0.0.5/32 tcp sport 80 ct state established,related accept
     * </pre>
     */
    public List<String> toNftStatements() {
        String chain = chainName();

        StringBuilder forward = new StringBuilder();
        if (this.inputInterface != null) {
            forward.append("iifname \"").append(this.inputInterface).append("\" ");
        }
        if (this.outputInterface != null) {
            forward.append("oifname \"").append(this.outputInterface).append("\" ");
        }
        if (this.srcNetwork != null) {
            forward.append(NftSyntaxUtil.family(this.srcNetwork)).append(" saddr ").append(this.srcNetwork)
                    .append('/').append(this.srcMask).append(' ');
        }
        if (this.dstNetwork != null) {
            forward.append(NftSyntaxUtil.family(this.dstNetwork)).append(" daddr ").append(this.dstNetwork)
                    .append('/').append(this.dstMask).append(' ');
        }
        if (this.permittedMacAddress != null) {
            forward.append("ether saddr ").append(this.permittedMacAddress).append(' ');
        }
        if (this.protocol != null && this.srcPortFirst > 0) {
            forward.append(this.protocol).append(" sport ").append(this.srcPortFirst).append('-')
                    .append(this.srcPortLast).append(' ');
        }
        if (this.protocol != null && this.dstPort > 0) {
            forward.append(this.protocol).append(" dport ").append(this.dstPort).append(' ');
        }
        if (this.protocol != null && this.srcPortFirst <= 0 && this.dstPort <= 0) {
            // Protocol restricted but no port to hang it off of (e.g. a NAT/port-forward
            // rule with a protocol but no port restriction) - "meta l4proto" matches the
            // transport protocol directly, family-neutral (works the same in the "ip" and
            // "ip6" tables).
            forward.append("meta l4proto ").append(this.protocol).append(' ');
        }
        forward.append("counter accept");

        StringBuilder reverse = new StringBuilder();
        if (this.outputInterface != null) {
            reverse.append("iifname \"").append(this.outputInterface).append("\" ");
        }
        if (this.inputInterface != null) {
            reverse.append("oifname \"").append(this.inputInterface).append("\" ");
        }
        if (this.protocol != null) {
            reverse.append("meta l4proto ").append(this.protocol).append(' ');
        }
        if (this.dstNetwork != null) {
            reverse.append(NftSyntaxUtil.family(this.dstNetwork)).append(" saddr ").append(this.dstNetwork)
                    .append('/').append(this.dstMask).append(' ');
        }
        reverse.append("ct state established,related counter accept");

        return Arrays.asList(NftablesConfigConstants.fragment(chain, forward.toString()),
                NftablesConfigConstants.fragment(chain, reverse.toString()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FilterForwardChainRule)) {
            return false;
        }
        FilterForwardChainRule other = (FilterForwardChainRule) obj;
        return Objects.equals(this.inputInterface, other.inputInterface)
                && Objects.equals(this.outputInterface, other.outputInterface)
                && Objects.equals(this.srcNetwork, other.srcNetwork) && this.srcMask == other.srcMask
                && Objects.equals(this.dstNetwork, other.dstNetwork) && this.dstMask == other.dstMask
                && Objects.equals(this.protocol, other.protocol)
                && Objects.equals(this.permittedMacAddress, other.permittedMacAddress)
                && this.srcPortFirst == other.srcPortFirst && this.srcPortLast == other.srcPortLast
                && this.dstPort == other.dstPort && this.type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.inputInterface, this.outputInterface, this.srcNetwork, this.srcMask, this.dstNetwork,
                this.dstMask, this.protocol, this.permittedMacAddress, this.srcPortFirst, this.srcPortLast,
                this.dstPort, this.type);
    }
}
