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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.firewall.RuleType;

/**
 * Builds the nft MASQUERADE statement for a NAT or port-forward rule.
 */
public class NatPostroutingChainRule {

    private String dstNetwork;
    private short dstMask;
    private String srcNetwork;
    private short srcMask;
    private String dstInterface;
    private String protocol;
    private boolean masquerade;
    private RuleType type = RuleType.GENERIC;

    public NatPostroutingChainRule() {
        // fluent builder, use setters
    }

    public NatPostroutingChainRule(String dstInterface, boolean masquerade, RuleType type) {
        this.dstInterface = dstInterface;
        this.masquerade = masquerade;
        this.type = type;
    }

    public NatPostroutingChainRule(String dstInterface, String protocol, String dstNetwork, String srcNetwork,
            boolean masquerade, RuleType type) throws KuraException {
        this.dstInterface = dstInterface;
        this.protocol = protocol;
        if (dstNetwork != null) {
            String[] split = dstNetwork.split("/");
            this.dstNetwork = split[0];
            this.dstMask = Short.parseShort(split[1]);
        }
        if (srcNetwork != null) {
            String[] split = srcNetwork.split("/");
            this.srcNetwork = split[0];
            this.srcMask = Short.parseShort(split[1]);
        }
        this.masquerade = masquerade;
        this.type = type;
    }

    public NatPostroutingChainRule dstNetwork(String dstNetwork) {
        this.dstNetwork = dstNetwork;
        return this;
    }

    public NatPostroutingChainRule dstMask(short dstMask) {
        this.dstMask = dstMask;
        return this;
    }

    public NatPostroutingChainRule srcNetwork(String srcNetwork) {
        this.srcNetwork = srcNetwork;
        return this;
    }

    public NatPostroutingChainRule srcMask(short srcMask) {
        this.srcMask = srcMask;
        return this;
    }

    public NatPostroutingChainRule dstInterface(String dstInterface) {
        this.dstInterface = dstInterface;
        return this;
    }

    public NatPostroutingChainRule protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public NatPostroutingChainRule masquerade(boolean masquerade) {
        this.masquerade = masquerade;
        return this;
    }

    public NatPostroutingChainRule type(RuleType type) {
        this.type = type;
        return this;
    }

    public boolean isMasquerade() {
        return this.masquerade;
    }

    public RuleType getType() {
        return this.type;
    }

    private String chainName() {
        if (this.type == RuleType.IP_FORWARDING) {
            return NftablesConfigConstants.POSTROUTING_KURA_IPF_CHAIN;
        } else if (this.type == RuleType.PORT_FORWARDING) {
            return NftablesConfigConstants.POSTROUTING_KURA_PF_CHAIN;
        }
        return NftablesConfigConstants.POSTROUTING_KURA_CHAIN;
    }

    /**
     * Builds the MASQUERADE statement. Only emits a statement when
     * {@code masquerade == true}, matching the pre-existing iptables-era
     * behavior where non-masquerade NAT entries produce nothing here.
     */
    public Optional<String> toNftStatement() {
        if (!this.masquerade) {
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("oifname \"").append(this.dstInterface).append("\" ");
        if (this.protocol != null) {
            sb.append("meta l4proto ").append(this.protocol).append(' ');
        }
        if (this.dstNetwork != null) {
            sb.append(NftSyntaxUtil.family(this.dstNetwork)).append(" daddr ").append(this.dstNetwork).append('/')
                    .append(this.dstMask).append(' ');
        }
        if (this.srcNetwork != null) {
            sb.append(NftSyntaxUtil.family(this.srcNetwork)).append(" saddr ").append(this.srcNetwork).append('/')
                    .append(this.srcMask).append(' ');
        }
        sb.append("counter masquerade");

        return Optional.of(NftablesConfigConstants.fragment(chainName(), sb.toString()));
    }

    /**
     * Correlation helper kept for in-memory bookkeeping (e.g. matching a
     * postrouting entry back to its forward-accept counterpart when deleting a
     * rule); no longer used for restore, since the structured model is
     * persisted directly.
     */
    public boolean isMatchingForwardChainRule(FilterForwardChainRule forwardRule) {
        if (forwardRule == null) {
            return false;
        }
        return Objects.equals(this.dstInterface, forwardRule.getOutputInterface())
                && Objects.equals(this.protocol, forwardRule.getProtocol()) && this.type == forwardRule.getType();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NatPostroutingChainRule)) {
            return false;
        }
        NatPostroutingChainRule other = (NatPostroutingChainRule) obj;
        return Objects.equals(this.dstNetwork, other.dstNetwork) && this.dstMask == other.dstMask
                && Objects.equals(this.srcNetwork, other.srcNetwork) && this.srcMask == other.srcMask
                && Objects.equals(this.dstInterface, other.dstInterface)
                && Objects.equals(this.protocol, other.protocol) && this.masquerade == other.masquerade
                && this.type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.dstNetwork, this.dstMask, this.srcNetwork, this.srcMask, this.dstInterface,
                this.protocol, this.masquerade, this.type);
    }
}
