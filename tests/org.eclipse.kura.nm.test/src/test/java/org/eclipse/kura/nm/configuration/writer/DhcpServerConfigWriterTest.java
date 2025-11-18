/*******************************************************************************
 * Copyright (c) 2023, 2025 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.nm.configuration.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.dhcp.DhcpServerManager;
import org.eclipse.kura.linux.net.dhcp.DhcpServerTool;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqConfigConverter;
import org.eclipse.kura.nm.NetworkProperties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DhcpServerConfigWriterTest {

    private static final String DNSMASQ_CONFIG_FILENAME = "dnsmasq-%s.conf";

    @Rule
    public TemporaryFolder mockFiles = new TemporaryFolder();

    private final DhcpServerManager dhcpServerMock = mock(DhcpServerManager.class);
    private final Map<String, Object> networkProperties = new HashMap<>();
    private DhcpServerTool selectedTool;
    private String configFilename;
    private DhcpServerConfigWriter writer;
    private boolean isUnknownHostException = false;
    private boolean isKuraException = false;

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnDnsmasqConfigFileName() throws Exception {
        givenDhcpTool(DhcpServerTool.DNSMASQ);
        givenDhcpConfigWriter("eth0");

        whenGetConfigFilename();

        thenDhcpServerFileNameIs(String.format(DNSMASQ_CONFIG_FILENAME, "eth0"));
    }

    @Test
    public void shouldWriteCorrectDnsmasqConfigurationFile() throws Exception {
        givenDhcpTool(DhcpServerTool.DNSMASQ);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.nat.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");

        whenWriteConfiguration();

        thenUnknownHostExceptionIsNotCaught();
        thenKuraExceptionIsNotCaught();
        thenConfigFileContains(new StringBuilder().append("interface=eth0\n")
                .append("dhcp-range=eth0,192.168.0.111,192.168.0.120,900s\n")
                .append("dhcp-option=eth0,1,255.255.255.0\n").append("dhcp-option=eth0,3,192.168.0.11\n")
                .append("dhcp-option=eth0,6,0.0.0.0\n").append("dhcp-option=eth0,27,1\n").toString());
    }

    @Test
    public void shouldWriteCorrectDnsmasqConfigurationFileWithoutNat() throws Exception {
        givenDhcpTool(DhcpServerTool.DNSMASQ);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.nat.enabled", false);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");

        whenWriteConfiguration();

        thenUnknownHostExceptionIsNotCaught();
        thenKuraExceptionIsNotCaught();
        thenConfigFileContains(new StringBuilder().append("interface=eth0\n")
                .append("dhcp-range=eth0,192.168.0.111,192.168.0.120,900s\n")
                .append("dhcp-option=eth0,1,255.255.255.0\n").append("dhcp-option=eth0,3\n")
                .append("dhcp-option=eth0,6\n").append("dhcp-ignore-names=eth0\n").append("dhcp-option=eth0,27,1\n")
                .toString());
    }

    @Test
    public void shouldWriteCorrectDnsmasqConfigurationFileWithoutPassDNS() throws Exception {
        givenDhcpTool(DhcpServerTool.DNSMASQ);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.nat.enabled", true);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 900);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 1000);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.passDns", false);
        givenNetworkPropertiesWith("net.interface.eth0.config.ip4.address", "192.168.0.11");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.0.111");
        givenNetworkPropertiesWith("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.0.120");
        givenDhcpConfigWriter("eth0");

        whenWriteConfiguration();

        thenUnknownHostExceptionIsNotCaught();
        thenKuraExceptionIsNotCaught();
        thenConfigFileContains(new StringBuilder().append("interface=eth0\n")
                .append("dhcp-range=eth0,192.168.0.111,192.168.0.120,900s\n")
                .append("dhcp-option=eth0,1,255.255.255.0\n").append("dhcp-option=eth0,3,192.168.0.11\n")
                .append("dhcp-option=eth0,6\n").append("dhcp-ignore-names=eth0\n").append("dhcp-option=eth0,27,1\n")
                .toString());
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenDhcpTool(DhcpServerTool tool) {

        switch (tool) {
        case DNSMASQ:
            when(this.dhcpServerMock.getConfigConverter()).thenReturn(Optional.of(new DnsmasqConfigConverter()));
            break;
        }

        this.selectedTool = tool;
    }

    private void givenNetworkPropertiesWith(String key, Object value) {
        this.networkProperties.put(key, value);
    }

    private void givenDhcpConfigWriter(String interfaceName) throws IOException {
        String filename;

        switch (DhcpServerConfigWriterTest.this.selectedTool) {
        case DNSMASQ:
            filename = String.format(DNSMASQ_CONFIG_FILENAME, interfaceName);
            break;
        default:
            filename = "etc";
        }

        this.configFilename = this.mockFiles.newFile(filename).getAbsolutePath();

        this.writer = new DhcpServerConfigWriter(this.dhcpServerMock, interfaceName,
                new NetworkProperties(this.networkProperties)) {

            @Override
            protected String getConfigFilename() {
                return DhcpServerConfigWriterTest.this.configFilename;
            }
        };
    }

    /*
     * When
     */

    private void whenGetConfigFilename() {
        this.configFilename = this.writer.getConfigFilename();
    }

    private void whenWriteConfiguration() {
        try {
            this.writer.writeConfiguration();
        } catch (UnknownHostException e) {
            this.isUnknownHostException = true;
        } catch (KuraException e) {
            this.isKuraException = true;
        }
    }

    /*
     * Then
     */

    private void thenDhcpServerFileNameIs(String expectedFilename) {
        assertEquals(this.mockFiles.getRoot() + "/" + expectedFilename, this.configFilename);
    }

    private void thenConfigFileContains(String expectedContent) throws IOException {
        File dhcpConfigFile = new File(this.configFilename);
        assertTrue(dhcpConfigFile.exists());

        String content = FileUtils.readFileToString(dhcpConfigFile, "UTF-8");
        assertEquals(expectedContent, content);
    }

    private void thenUnknownHostExceptionIsNotCaught() {
        assertFalse(this.isUnknownHostException);
    }

    private void thenKuraExceptionIsNotCaught() {
        assertFalse(this.isKuraException);
    }

}
