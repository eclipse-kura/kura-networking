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

import org.eclipse.kura.linux.net.nftables.jna.NftExecutor;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;

/**
 * Minimal concrete {@link AbstractLinuxFirewall} for exercising the base
 * class directly in tests, without going through the singleton
 * {@link LinuxFirewall}/{@link LinuxFirewallIPv6}. The persistence file paths
 * returned here are never actually used - callers immediately overwrite
 * {@code persistenceStore} with a scratch-file-backed instance (see
 * {@link FirewallTestUtils#newTempPersistenceStore()}) before running any
 * test, since they're only read once by the superclass constructor to build
 * a throwaway initial value.
 */
class TestableLinuxFirewall extends AbstractLinuxFirewall {

    TestableLinuxFirewall(NftExecutor nftExecutor) {
        super(nftExecutor);
    }

    @Override
    protected String getFamily() {
        return NftablesConfigConstants.FAMILY_V4;
    }

    @Override
    protected String getPersistenceFileName() {
        return "/tmp/unused-testable-linux-firewall.conf";
    }

    @Override
    protected String getPersistenceTmpFileName() {
        return "/tmp/unused-testable-linux-firewall.tmp";
    }

    @Override
    protected String getIpForwardFileName() {
        return "/tmp/unused-testable-linux-firewall-ip-forward";
    }

    @Override
    protected IPAddress getDefaultAddress() throws UnknownHostException {
        return IP4Address.getDefaultAddress();
    }
}
