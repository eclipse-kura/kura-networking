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

import java.util.Objects;

import org.eclipse.kura.net.firewall.RuleType;

/**
 * Fluent description of a port-forward rule; factory for the three nft
 * statement builders that together implement it (DNAT, optional MASQUERADE,
 * forward-accept pair).
 */
public class PortForwardRule {

    private String inboundIface;
    private String outboundIface;
    private String address;
    private short addressMask = 32;
    private String protocol;
    private int inPort;
    private int outPort;
    private boolean masquerade;
    private String permittedNetwork;
    private short permittedNetworkMask = -1;
    private String permittedMAC;
    private int sourcePortStart;
    private int sourcePortEnd;

    public PortForwardRule() {
        // fluent builder, use setters
    }

    public PortForwardRule inboundIface(String inboundIface) {
        this.inboundIface = inboundIface;
        return this;
    }

    public PortForwardRule outboundIface(String outboundIface) {
        this.outboundIface = outboundIface;
        return this;
    }

    public PortForwardRule address(String address) {
        this.address = address;
        return this;
    }

    public PortForwardRule addressMask(short addressMask) {
        this.addressMask = addressMask;
        return this;
    }

    public PortForwardRule protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public PortForwardRule inPort(int inPort) {
        this.inPort = inPort;
        return this;
    }

    public PortForwardRule outPort(int outPort) {
        this.outPort = outPort;
        return this;
    }

    public PortForwardRule masquerade(boolean masquerade) {
        this.masquerade = masquerade;
        return this;
    }

    public PortForwardRule permittedNetwork(String permittedNetwork) {
        this.permittedNetwork = permittedNetwork;
        return this;
    }

    public PortForwardRule permittedNetworkMask(short permittedNetworkMask) {
        this.permittedNetworkMask = permittedNetworkMask;
        return this;
    }

    public PortForwardRule permittedMAC(String permittedMAC) {
        this.permittedMAC = permittedMAC;
        return this;
    }

    public PortForwardRule sourcePortRange(String sourcePortRange) {
        if (sourcePortRange != null && !sourcePortRange.trim().isEmpty()) {
            String[] parts = sourcePortRange.split(":");
            this.sourcePortStart = Integer.parseInt(parts[0]);
            this.sourcePortEnd = Integer.parseInt(parts[1]);
        }
        return this;
    }

    public boolean isComplete() {
        return this.protocol != null && this.inboundIface != null && this.outboundIface != null
                && this.address != null && this.inPort != 0 && this.outPort != 0;
    }

    public String getInboundIface() {
        return this.inboundIface;
    }

    public String getOutboundIface() {
        return this.outboundIface;
    }

    public String getAddress() {
        return this.address;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public int getInPort() {
        return this.inPort;
    }

    public int getOutPort() {
        return this.outPort;
    }

    public boolean isMasquerade() {
        return this.masquerade;
    }

    public short getAddressMask() {
        return this.addressMask;
    }

    public String getPermittedNetwork() {
        return this.permittedNetwork;
    }

    public short getPermittedNetworkMask() {
        return this.permittedNetworkMask;
    }

    public String getPermittedMAC() {
        return this.permittedMAC;
    }

    public int getSourcePortStart() {
        return this.sourcePortStart;
    }

    public int getSourcePortEnd() {
        return this.sourcePortEnd;
    }

    public String getSourcePortRange() {
        if (this.sourcePortStart == this.sourcePortEnd && this.sourcePortStart == 0) {
            return null;
        }
        return this.sourcePortStart + ":" + this.sourcePortEnd;
    }

    public NatPreroutingChainRule getNatPreroutingChainRule() {
        return new NatPreroutingChainRule().inputInterface(this.inboundIface).protocol(this.protocol)
                .externalPort(this.inPort).internalPort(this.outPort).srcPortFirst(this.sourcePortStart)
                .srcPortLast(this.sourcePortEnd).dstIpAddress(this.address)
                .permittedNetwork(this.permittedNetwork == null ? "0.0.0.0" : this.permittedNetwork)
                .permittedNetworkMask(this.permittedNetworkMask < 0 ? 0 : this.permittedNetworkMask)
                .permittedMacAddress(this.permittedMAC).type(RuleType.PORT_FORWARDING);
    }

    public NatPostroutingChainRule getNatPostroutingChainRule() {
        // Scoped to this port-forward's own destination host/protocol (and permitted
        // source network, if any) - not the bare interface-only auto-nat form, which
        // would masquerade *all* traffic leaving outboundIface instead of just this
        // forwarded connection.
        return new NatPostroutingChainRule().dstNetwork(this.address).dstMask(this.addressMask)
                .srcNetwork(this.permittedNetwork).srcMask((short) Math.max(this.permittedNetworkMask, 0))
                .dstInterface(this.outboundIface).protocol(this.protocol).masquerade(this.masquerade)
                .type(RuleType.PORT_FORWARDING);
    }

    public FilterForwardChainRule getFilterForwardChainRule() {
        // No destination-port match: the dstNetwork restriction (the specific
        // internal host) is narrow enough on its own, and matching on the internal
        // port here as well is redundant, so this is left as a pure protocol match.
        return new FilterForwardChainRule().inputInterface(this.inboundIface).outputInterface(this.outboundIface)
                .srcNetwork(this.permittedNetwork).srcMask((short) Math.max(this.permittedNetworkMask, 0))
                .dstNetwork(this.address).dstMask(this.addressMask).protocol(this.protocol)
                .permittedMacAddress(this.permittedMAC).srcPortFirst(this.sourcePortStart)
                .srcPortLast(this.sourcePortEnd).type(RuleType.PORT_FORWARDING);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PortForwardRule)) {
            return false;
        }
        PortForwardRule other = (PortForwardRule) obj;
        return Objects.equals(this.inboundIface, other.inboundIface)
                && Objects.equals(this.outboundIface, other.outboundIface)
                && Objects.equals(this.address, other.address) && this.addressMask == other.addressMask
                && Objects.equals(this.protocol, other.protocol) && this.inPort == other.inPort
                && this.outPort == other.outPort && this.masquerade == other.masquerade
                && Objects.equals(this.permittedNetwork, other.permittedNetwork)
                && this.permittedNetworkMask == other.permittedNetworkMask
                && Objects.equals(this.permittedMAC, other.permittedMAC)
                && this.sourcePortStart == other.sourcePortStart && this.sourcePortEnd == other.sourcePortEnd;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.inboundIface, this.outboundIface, this.address, this.addressMask, this.protocol,
                this.inPort, this.outPort, this.masquerade, this.permittedNetwork, this.permittedNetworkMask,
                this.permittedMAC, this.sourcePortStart, this.sourcePortEnd);
    }
}
