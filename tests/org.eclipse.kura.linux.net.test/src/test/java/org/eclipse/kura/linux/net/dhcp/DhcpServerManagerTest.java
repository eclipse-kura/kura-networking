/*******************************************************************************
 * Copyright (c) 2017, 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.dhcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqConfigConverter;
import org.eclipse.kura.linux.net.dhcp.server.DnsmasqLeaseReader;
import org.eclipse.kura.linux.net.util.LinuxNetworkUtil;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DhcpServerManagerTest {

    private static final String EXAMPLE_INTERFACE = "eth0";

    private static final String EXPECTED_DNSMASQ_CONF_FILENAME = "/etc/dnsmasq.d/%s-%s.conf";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private DhcpServerManager dhcpServerManager;
    private CommandExecutorService executorMock;
    private String returnedFilename;
    private Optional<DhcpServerConfigConverter> returnedConfigConverter;
    private Optional<DhcpServerLeaseReader> returnedLeaseReader;
    private Exception occurredException;
    private MockedStatic<LinuxNetworkUtil> mockedLinuxNetworkUtil;
    private DhcpServerTool tool;

    private boolean serverManagerStatus = false;

    /*
     * Scenarios
     */

    @Test
    public void shouldReturnConfigFilenameForDnsmasq() {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetConfigFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs(
                String.format(EXPECTED_DNSMASQ_CONF_FILENAME, DhcpServerTool.DNSMASQ.getValue(), EXAMPLE_INTERFACE));
    }

    @Test
    public void shouldReturnConfigFilenameForNone() {
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetConfigFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs("/etc/");
    }

    @Test
    public void shouldReturnLeasesFilenameForDnsmasq() {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetLeasesFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs("/var/lib/dhcp/dnsmasq.leases");
    }

    @Test
    public void shouldReturnLeasesFilenameForNone() {
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetLeasesFilename(EXAMPLE_INTERFACE);

        thenNoExceptionsOccurred();
        thenReturnedFilenameIs("/var/lib/dhcp/");
    }

    @Test
    public void shouldReturnConfigConverterForDnsmasq() {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetConfigConverter();

        thenNoExceptionsOccurred();
        thenReturnedConfigConvertedIsPresent();
        thenReturnedConfigConverterIs(DnsmasqConfigConverter.class);
    }

    @Test
    public void shouldReturnConfigConverterForNone() {
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetConfigConverter();

        thenNoExceptionsOccurred();
        thenReturnedConfigConvertedIsEmpty();
    }

    @Test
    public void shouldReturnLeaseReaderForDnsmasq() {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetLeaseReader();

        thenNoExceptionsOccurred();
        thenReturnedLeaseReaderIsPresent();
        thenReturnedLeaseReaderIs(DnsmasqLeaseReader.class);
    }

    @Test
    public void shouldReturnLeaseReaderForNone() {
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetLeaseReader();

        thenNoExceptionsOccurred();
        thenReturnedLeaseReaderIsEmpty();
    }

    @Test
    public void shouldGetDnsmasqToolIfBinaryAndUnitArePresent() {
        givenLinuxNetworkUtil("dnsmasq", Optional.of("dnsmasq.service"));
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetTool();

        thenToolIs(DhcpServerTool.DNSMASQ);
    }

    @Test
    public void shouldNotGetDnsmasqToolIfOnlyBinaryIsPresent() {
        givenLinuxNetworkUtil("dnsmasq", Optional.empty());
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetTool();

        thenToolIs(DhcpServerTool.NONE);
    }

    @Test
    public void shouldNotGetAnyTool() {
        givenLinuxNetworkUtil("", Optional.empty());
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenGetTool();

        thenToolIs(DhcpServerTool.NONE);
    }

    @Test
    public void shouldNotBeRunningWithNone() {
        givenLinuxNetworkUtil("", Optional.empty());
        givenDhcpServerManager(DhcpServerTool.NONE);

        whenCheckServiceStatus("eth0");

        thenNoExceptionsOccurred();
        thenDhcpServerManagerRunningStatusIs(false);
    }

    @After
    public void deregisterStaticMocks() {
        if (this.mockedLinuxNetworkUtil != null) {
            this.mockedLinuxNetworkUtil.close();
        }
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenDhcpServerManager(DhcpServerTool dhcpServerTool) {
        this.executorMock = Mockito.mock(CommandExecutorService.class);
        when(this.executorMock.execute(any())).thenReturn(new CommandStatus(null, new LinuxExitStatus(0)));

        try {
            TestUtil.setFieldValue(new DhcpServerManager(null), "dhcpServerTool", dhcpServerTool);
            this.dhcpServerManager = new DhcpServerManager(this.executorMock);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void givenLinuxNetworkUtil(String toolName, Optional<String> unitName) {
        this.mockedLinuxNetworkUtil = Mockito.mockStatic(LinuxNetworkUtil.class);
        this.mockedLinuxNetworkUtil.when(() -> LinuxNetworkUtil.toolExists(toolName)).thenReturn(true);
        if (unitName.isPresent()) {
            this.mockedLinuxNetworkUtil.when(() -> LinuxNetworkUtil.systemdSystemUnitExists(unitName.get()))
                    .thenReturn(true);
        }
    }

    /*
     * When
     */

    private void whenGetConfigFilename(String interfaceName) {
        try {
            this.returnedFilename = DhcpServerManager.getConfigFilename(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetLeasesFilename(String interfaceName) {
        try {
            this.returnedFilename = DhcpServerManager.getLeasesFilename(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetConfigConverter() {
        try {
            this.returnedConfigConverter = DhcpServerManager.getConfigConverter();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetLeaseReader() {
        try {
            this.returnedLeaseReader = DhcpServerManager.getLeaseReader();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetTool() {
        try {
            this.tool = DhcpServerManager.getTool();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenCheckServiceStatus(String ienterfaceName) {
        try {
            this.serverManagerStatus = this.dhcpServerManager.isRunning(ienterfaceName);
        } catch (KuraException e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenNoExceptionsOccurred() {
        assertNull(this.occurredException);
    }

    private void thenReturnedFilenameIs(String expectedFilename) {
        assertEquals(expectedFilename, this.returnedFilename);
    }

    private void thenReturnedConfigConvertedIsPresent() {
        assertTrue(this.returnedConfigConverter.isPresent());
    }

    private void thenReturnedConfigConvertedIsEmpty() {
        assertFalse(this.returnedConfigConverter.isPresent());
    }

    private void thenReturnedConfigConverterIs(Class<?> dhcpServerConfigConverter) {
        assertEquals(dhcpServerConfigConverter, this.returnedConfigConverter.get().getClass());
    }

    private void thenReturnedLeaseReaderIsPresent() {
        assertTrue(this.returnedLeaseReader.isPresent());
    }

    private void thenReturnedLeaseReaderIsEmpty() {
        assertFalse(this.returnedLeaseReader.isPresent());
    }

    private void thenReturnedLeaseReaderIs(Class<?> dhcpServerLeaseReader) {
        assertEquals(dhcpServerLeaseReader, this.returnedLeaseReader.get().getClass());
    }

    private void thenToolIs(DhcpServerTool expectedTool) {
        assertEquals(expectedTool, this.tool);
    }

    private void thenDhcpServerManagerRunningStatusIs(boolean expectedStatus) {
        assertEquals(expectedStatus, this.serverManagerStatus);
    }

}
