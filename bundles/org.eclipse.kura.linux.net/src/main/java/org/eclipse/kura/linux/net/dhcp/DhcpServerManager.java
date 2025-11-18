/*******************************************************************************
 * Copyright (c) 2011, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.dhcp;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraIOException;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.dhcp.server.DhcpLinuxTool;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqConfigConverter;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqLeaseReader;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqTool;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DhcpServerManager {

    private static final Logger logger = LoggerFactory.getLogger(DhcpServerManager.class);

    private final DhcpLinuxTool linuxTool;

    private final DhcpServerTool dhcpServerTool;

    public DhcpServerManager(DhcpServerTool dhcpServerTool, CommandExecutorService service) {
        this.dhcpServerTool = dhcpServerTool;
        if (dhcpServerTool == DhcpServerTool.DNSMASQ) {

            if (verifyToolAndServiceExists()) {
                this.linuxTool = new DnsmasqTool(service);

                logger.info("Using {} as DHCP server.", dhcpServerTool.getValue());
            } else {
                throw new IllegalArgumentException(dhcpServerTool.name() + " not available on the system.");
            }
        } else {
            throw new IllegalArgumentException(dhcpServerTool.name() + " not supported.");
        }
    }

    // For testing purpose
    protected boolean verifyToolAndServiceExists() {
        return LinuxNetworkUtil.toolExists(DhcpServerTool.DNSMASQ.getValue()) && //
                LinuxNetworkUtil.systemdSystemUnitExists(DhcpServerTool.DNSMASQ.getValue() + ".service");
    }

    public boolean isRunning(String interfaceName) throws KuraException {
        return this.linuxTool.isRunning(interfaceName);
    }

    public boolean enable(String interfaceName) throws KuraException {
        if (isRunning(interfaceName)) {
            logger.error("DHCP server is already running for {}, bringing it down...", interfaceName);
            disable(interfaceName);
        }

        File configFile = new File(this.linuxTool.getConfigFilename(interfaceName));
        if (configFile.exists()) {

            createLeasesFile(interfaceName);
            CommandStatus status = this.linuxTool.startInterface(interfaceName);

            if (status.getExitStatus().isSuccessful()) {
                logger.debug("DHCP server started.");
                return true;
            } else {
                logger.debug("Can't start DHCP server, config file does not exist: {}", configFile.getAbsolutePath());
            }
        }

        return false;
    }

    private void createLeasesFile(String interfaceName) throws KuraIOException {
        try {
            FileUtils.touch(new File(this.linuxTool.getLeasesFilename(Optional.of(interfaceName))));
        } catch (IOException e) {
            throw new KuraIOException(e, "Cannot create DHCP server leases file");
        }
    }

    public boolean disable(String interfaceName) throws KuraException {
        logger.debug("Disable DHCP server for {}", interfaceName);

        return this.linuxTool.disableInterface(interfaceName);
    }

    public Optional<DhcpServerConfigConverter> getConfigConverter() {
        if (this.dhcpServerTool == DhcpServerTool.DNSMASQ) {
            return Optional.of(new DnsmasqConfigConverter());
        }
        return Optional.empty();
    }

    public Optional<DhcpServerLeaseReader> getLeaseReader() {
        if (this.dhcpServerTool == DhcpServerTool.DNSMASQ) {
            return Optional.of(new DnsmasqLeaseReader());
        }
        return Optional.empty();
    }

    public String getConfigFilename(String interfaceName) {
        return this.linuxTool.getConfigFilename(interfaceName);
    }
}
