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

import java.net.UnknownHostException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.nftables.jna.NftExecutor;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * nftables-based IPv6 Linux firewall implementation, owning the independent
 * {@code ip6 kura} table. See {@link LinuxFirewall} for the IPv4 counterpart.
 * Fully independent of it: separate rule sets, separate persistence file,
 * separate enable/disable/ICMP-toggle behavior.
 */
public final class LinuxFirewallIPv6 extends AbstractLinuxFirewall {

    private static final Logger logger = LoggerFactory.getLogger(LinuxFirewallIPv6.class);

    private static LinuxFirewallIPv6 instance;

    public static synchronized LinuxFirewallIPv6 getInstance(NftExecutor nftExecutor) {
        if (instance == null) {
            instance = new LinuxFirewallIPv6(nftExecutor);
        } else {
            instance.setNftExecutor(nftExecutor);
        }
        return instance;
    }

    private LinuxFirewallIPv6(NftExecutor nftExecutor) {
        super(nftExecutor);
        try {
            initialize();
        } catch (KuraException e) {
            logger.error("failed to initialize LinuxFirewallIPv6", e);
        }
    }

    @Override
    protected String getFamily() {
        return NftablesConfigConstants.FAMILY_V6;
    }

    @Override
    protected String getPersistenceFileName() {
        return NftablesConfigConstants.FIREWALL_CONFIG_FILE_NAME_V6;
    }

    @Override
    protected String getPersistenceTmpFileName() {
        return NftablesConfigConstants.FIREWALL_TMP_CONFIG_FILE_NAME_V6;
    }

    @Override
    protected String getIpForwardFileName() {
        return NftablesConfigConstants.IP_FORWARD_FILE_NAME_V6;
    }

    @Override
    protected IPAddress getDefaultAddress() throws UnknownHostException {
        return IP6Address.getDefaultAddress();
    }
}
