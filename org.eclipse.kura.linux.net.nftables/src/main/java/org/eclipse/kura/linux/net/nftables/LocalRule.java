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
import java.util.Optional;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;

/**
 * Creates an nft "input_kura" accept rule allowing an incoming port
 * connection.
 */
public class LocalRule {

    private int port;
    private Optional<String> portRange = Optional.empty();
    private String protocol;
    private Optional<String> permittedNetworkString = Optional.empty();
    private Optional<String> permittedInterfaceName = Optional.empty();
    private Optional<String> unpermittedInterfaceName = Optional.empty();
    private Optional<String> permittedMAC = Optional.empty();
    private Optional<String> sourcePortRange = Optional.empty();

    public LocalRule(int port, String protocol, NetworkPair<? extends IPAddress> permittedNetwork,
            String permittedInterfaceName, String unpermittedInterfaceName, String permittedMAC,
            String sourcePortRange) {
        this.port = port;
        this.portRange = Optional.empty();
        this.protocol = protocol;
        setPermittedNetwork(permittedNetwork);
        setPermittedInterfaceName(permittedInterfaceName);
        setUnpermittedInterfaceName(unpermittedInterfaceName);
        setPermittedMAC(permittedMAC);
        setSourcePortRange(sourcePortRange);
    }

    public LocalRule(String portRange, String protocol, NetworkPair<? extends IPAddress> permittedNetwork,
            String permittedInterfaceName, String unpermittedInterfaceName, String permittedMAC,
            String sourcePortRange) {
        this.port = -1;
        setPortRange(portRange);
        this.protocol = protocol;
        setPermittedNetwork(permittedNetwork);
        setPermittedInterfaceName(permittedInterfaceName);
        setUnpermittedInterfaceName(unpermittedInterfaceName);
        setPermittedMAC(permittedMAC);
        setSourcePortRange(sourcePortRange);
    }

    public LocalRule() {
        this.port = -1;
        this.protocol = null;
    }

    public boolean isComplete() {
        if (this.protocol != null && this.port != -1) {
            return true;
        } else if (this.protocol != null && this.portRange.isPresent()) {
            return isPortRangeValid(this.portRange.get());
        } else {
            return false;
        }
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Sets the permitted network directly from its serialized "address/prefix"
     * form, used when reconstructing a rule from {@link NftPersistenceStore}.
     */
    public void setPermittedNetworkString(String permittedNetworkString) {
        if (permittedNetworkString != null && !permittedNetworkString.trim().isEmpty()) {
            this.permittedNetworkString = Optional.of(permittedNetworkString);
        }
    }

    public void setPermittedNetwork(NetworkPair<? extends IPAddress> permittedNetwork) {
        if (permittedNetwork != null) {
            this.permittedNetworkString = Optional
                    .of(permittedNetwork.getIpAddress().getHostAddress() + "/" + permittedNetwork.getPrefix());
        }
    }

    public void setPermittedInterfaceName(String permittedInterfaceName) {
        if (permittedInterfaceName != null && !permittedInterfaceName.trim().isEmpty()) {
            this.permittedInterfaceName = Optional.of(permittedInterfaceName);
        }
    }

    public void setUnpermittedInterfaceName(String unpermittedInterfaceName) {
        if (unpermittedInterfaceName != null && !unpermittedInterfaceName.trim().isEmpty()) {
            this.unpermittedInterfaceName = Optional.of(unpermittedInterfaceName);
        }
    }

    public void setPermittedMAC(String permittedMAC) {
        if (permittedMAC != null && !permittedMAC.trim().isEmpty()) {
            this.permittedMAC = Optional.of(permittedMAC);
        }
    }

    public void setSourcePortRange(String sourcePortRange) {
        if (sourcePortRange != null && !sourcePortRange.trim().isEmpty()) {
            this.sourcePortRange = Optional.of(sourcePortRange);
        }
    }

    public void setPort(int port) {
        this.port = port;
        this.portRange = Optional.empty();
    }

    public void setPortRange(String portRange) {
        this.port = -1;
        if (portRange != null && !portRange.trim().isEmpty()) {
            this.portRange = Optional.of(portRange);
        }
    }

    public String getSourcePortRange() {
        return this.sourcePortRange.orElse(null);
    }

    public String getPermittedInterfaceName() {
        return this.permittedInterfaceName.orElse(null);
    }

    public String getUnpermittedInterfaceName() {
        return this.unpermittedInterfaceName.orElse(null);
    }

    public int getPort() {
        return this.port;
    }

    public String getPortRange() {
        return this.portRange.orElse(null);
    }

    public String getProtocol() {
        return this.protocol;
    }

    public NetworkPair<? extends IPAddress> getPermittedNetwork() throws KuraException {
        NetworkPair<? extends IPAddress> permittedNetwork = null;
        try {
            if (this.permittedNetworkString.isPresent()) {
                String[] split = this.permittedNetworkString.get().split("/");
                permittedNetwork = new NetworkPair<>(IPAddress.parseHostAddress(split[0]), Short.parseShort(split[1]));
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        return permittedNetwork;
    }

    public String getPermittedNetworkString() {
        return this.permittedNetworkString.orElse(null);
    }

    public String getPermittedMAC() {
        return this.permittedMAC.orElse(null);
    }

    /**
     * Builds the nft statement for this rule, e.g.:
     *
     * <pre>
     * add rule inet kura input_kura ip saddr 192.168.1.0/24 iifname "eth0" ether saddr AA:BB:CC:DD:EE:FF tcp sport 1024-2048 tcp dport 8080 accept
     * </pre>
     */
    public String toNftStatement() {
        StringBuilder sb = new StringBuilder();

        this.permittedNetworkString.ifPresent(
                net -> sb.append(NftSyntaxUtil.family(net)).append(" saddr ").append(net).append(' '));

        if (this.permittedInterfaceName.isPresent()) {
            sb.append("iifname \"").append(this.permittedInterfaceName.get()).append("\" ");
        } else if (this.unpermittedInterfaceName.isPresent()) {
            sb.append("iifname != \"").append(this.unpermittedInterfaceName.get()).append("\" ");
        }

        this.permittedMAC.ifPresent(mac -> sb.append("ether saddr ").append(mac).append(' '));

        this.sourcePortRange.ifPresent(range -> sb.append(this.protocol).append(" sport ")
                .append(NftSyntaxUtil.toNftPortRange(range)).append(' '));

        sb.append(this.protocol).append(" dport ");
        if (this.port != -1) {
            sb.append(this.port);
        } else {
            sb.append(NftSyntaxUtil.toNftPortRange(this.portRange.get()));
        }
        sb.append(" accept");

        return NftablesConfigConstants.fragment(NftablesConfigConstants.INPUT_KURA_CHAIN, sb.toString());
    }

    @Override
    public String toString() {
        return toNftStatement();
    }

    private boolean isPortRangeValid(String range) {
        try {
            String[] rangeParts = range.split(":");
            if (rangeParts.length == 2) {
                int portStart = Integer.parseInt(rangeParts[0]);
                int portEnd = Integer.parseInt(rangeParts[1]);
                return portStart > 0 && portStart < 65535 && portEnd > 0 && portEnd < 65535 && portStart < portEnd;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LocalRule other = (LocalRule) obj;
        return this.port == other.port && Objects.equals(this.portRange, other.portRange)
                && Objects.equals(this.protocol, other.protocol)
                && Objects.equals(this.permittedNetworkString, other.permittedNetworkString)
                && Objects.equals(this.permittedInterfaceName, other.permittedInterfaceName)
                && Objects.equals(this.unpermittedInterfaceName, other.unpermittedInterfaceName)
                && Objects.equals(this.permittedMAC, other.permittedMAC)
                && Objects.equals(this.sourcePortRange, other.sourcePortRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.port, this.portRange, this.protocol, this.permittedNetworkString,
                this.permittedInterfaceName, this.unpermittedInterfaceName, this.permittedMAC, this.sourcePortRange);
    }
}
