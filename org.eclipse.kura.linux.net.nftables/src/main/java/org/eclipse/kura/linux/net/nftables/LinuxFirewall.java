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
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * nftables-based IPv4 Linux firewall implementation, owning the independent
 * {@code ip kura} table. See {@link LinuxFirewallIPv6} for the IPv6
 * counterpart.
 */
public final class LinuxFirewall extends AbstractLinuxFirewall {

    private static final Logger logger = LoggerFactory.getLogger(LinuxFirewall.class);

    private static LinuxFirewall instance;

    // Singleton, since at creation it loads the persisted ruleset model from the
    // filesystem once instead of on every call. Same caveat as the iptables-era
    // implementation: dangerous in a multi-thread system since the executor is
    // swapped in on every getInstance() call.
    public static synchronized LinuxFirewall getInstance(NftExecutor nftExecutor) {
        if (instance == null) {
            instance = new LinuxFirewall(nftExecutor);
        } else {
            instance.setNftExecutor(nftExecutor);
        }
        return instance;
    }

    private LinuxFirewall(NftExecutor nftExecutor) {
        super(nftExecutor);
        try {
            initialize();
        } catch (KuraException e) {
            logger.error("failed to initialize LinuxFirewall", e);
        }
    }

    @Override
    protected String getFamily() {
        return NftablesConfigConstants.FAMILY_V4;
    }

    @Override
    protected String getPersistenceFileName() {
        return NftablesConfigConstants.FIREWALL_CONFIG_FILE_NAME_V4;
    }

    @Override
    protected String getPersistenceTmpFileName() {
        return NftablesConfigConstants.FIREWALL_TMP_CONFIG_FILE_NAME_V4;
    }

    @Override
    protected String getIpForwardFileName() {
        return NftablesConfigConstants.IP_FORWARD_FILE_NAME_V4;
    }

    @Override
    protected IPAddress getDefaultAddress() throws UnknownHostException {
        return IP4Address.getDefaultAddress();
    }
}
