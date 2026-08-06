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

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.firewall.RuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fluent description of a NAT rule (either a plain interface-to-interface
 * "auto NAT" or a full NAT with protocol/source/destination restrictions);
 * factory for the nft statement builders that implement it (MASQUERADE +
 * forward-accept pair).
 */
public class NATRule {

    private static final Logger logger = LoggerFactory.getLogger(NATRule.class);

    private String sourceInterface;
    private String destinationInterface;
    private String protocol;
    private String source;
    private String destination;
    private boolean masquerade;
    private RuleType type;

    public NATRule(String sourceInterface, String destinationInterface, boolean masquerade, RuleType type) {
        this.sourceInterface = sourceInterface;
        this.destinationInterface = destinationInterface;
        this.masquerade = masquerade;
        this.type = type;
    }

    public NATRule(String sourceInterface, String destinationInterface, String protocol, String source,
            String destination, boolean masquerade, RuleType type) {
        this(sourceInterface, destinationInterface, masquerade, type);
        this.source = source;
        this.destination = destination;
        this.protocol = protocol;
    }

    public NATRule() {
        this.sourceInterface = null;
        this.destinationInterface = null;
    }

    public boolean isComplete() {
        return this.sourceInterface != null && this.destinationInterface != null;
    }

    public void setSourceInterface(String sourceInterface) {
        this.sourceInterface = sourceInterface;
    }

    public void setDestinationInterface(String destinationInterface) {
        this.destinationInterface = destinationInterface;
    }

    public void setMasquerade(boolean masquerade) {
        this.masquerade = masquerade;
    }

    public String getSource() {
        return this.source;
    }

    public String getDestination() {
        return this.destination;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public String getSourceInterface() {
        return this.sourceInterface;
    }

    public String getDestinationInterface() {
        return this.destinationInterface;
    }

    public boolean isMasquerade() {
        return this.masquerade;
    }

    public RuleType getRuleType() {
        return this.type;
    }

    public NatPostroutingChainRule getNatPostroutingChainRule() {
        NatPostroutingChainRule ret;
        if (this.protocol == null) {
            ret = new NatPostroutingChainRule(this.destinationInterface, this.masquerade, this.type);
        } else {
            try {
                ret = new NatPostroutingChainRule(this.destinationInterface, this.protocol, this.destination,
                        this.source, this.masquerade, this.type);
            } catch (KuraException e) {
                ret = null;
                logger.error("failed to obtain NatPostroutingChainRule", e);
            }
        }
        return ret;
    }

    public FilterForwardChainRule getFilterForwardChainRule() {
        String srcNetwork = null;
        String dstNetwork = null;
        short srcMask = 0;
        short dstMask = 0;
        if (this.source != null) {
            srcNetwork = this.source.split("/")[0];
            srcMask = Short.parseShort(this.source.split("/")[1]);
        }
        if (this.destination != null) {
            dstNetwork = this.destination.split("/")[0];
            dstMask = Short.parseShort(this.destination.split("/")[1]);
        }
        return new FilterForwardChainRule().inputInterface(this.sourceInterface)
                .outputInterface(this.destinationInterface).srcNetwork(srcNetwork).srcMask(srcMask)
                .dstNetwork(dstNetwork).dstMask(dstMask).protocol(this.protocol).srcPortFirst(0).srcPortLast(0)
                .type(this.type);
    }

    @Override
    public int hashCode() {
        final int prime = 71;
        int result = 1;
        result = prime * result + (this.sourceInterface == null ? 0 : this.sourceInterface.hashCode());
        result = prime * result + (this.destinationInterface == null ? 0 : this.destinationInterface.hashCode());
        result = prime * result + (this.source == null ? 0 : this.source.hashCode());
        result = prime * result + (this.destination == null ? 0 : this.destination.hashCode());
        result = prime * result + (this.protocol == null ? 0 : this.protocol.hashCode());
        result = prime * result + (this.masquerade ? 1277 : 1279);

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NATRule)) {
            return false;
        }
        NATRule other = (NATRule) o;

        return compareObjects(this.sourceInterface, other.sourceInterface)
                && compareObjects(this.destinationInterface, other.destinationInterface)
                && this.masquerade == other.masquerade && compareObjects(this.protocol, other.protocol)
                && compareObjects(this.source, other.source) && compareObjects(this.destination, other.destination);
    }

    private boolean compareObjects(Object obj1, Object obj2) {
        if (obj1 != null) {
            return obj1.equals(obj2);
        } else if (obj2 != null) {
            return false;
        }
        return true;
    }
}
