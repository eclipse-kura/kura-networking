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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.eclipse.kura.executor.CommandExecutorService;
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
    public void shouldReturnConfigConverterForDnsmasq() {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetConfigConverter();

        thenNoExceptionsOccurred();
        thenReturnedConfigConvertedIsPresent();
        thenReturnedConfigConverterIs(DnsmasqConfigConverter.class);
    }

    @Test
    public void shouldReturnLeaseReaderForDnsmasq() {
        givenDhcpServerManager(DhcpServerTool.DNSMASQ);

        whenGetLeaseReader();

        thenNoExceptionsOccurred();
        thenReturnedLeaseReaderIsPresent();
        thenReturnedLeaseReaderIs(DnsmasqLeaseReader.class);
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

        this.dhcpServerManager = new DhcpServerManager(dhcpServerTool, this.executorMock) {

            @Override
            protected boolean verifyToolAndServiceExists() {
                return true;
            }
        };
    }

    /*
     * When
     */

    private void whenGetConfigFilename(String interfaceName) {
        try {
            this.returnedFilename = this.dhcpServerManager.getConfigFilename(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetConfigConverter() {
        try {
            this.returnedConfigConverter = this.dhcpServerManager.getConfigConverter();
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void whenGetLeaseReader() {
        try {
            this.returnedLeaseReader = this.dhcpServerManager.getLeaseReader();
        } catch (Exception e) {
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

    private void thenReturnedConfigConverterIs(Class<?> dhcpServerConfigConverter) {
        assertEquals(dhcpServerConfigConverter, this.returnedConfigConverter.get().getClass());
    }

    private void thenReturnedLeaseReaderIsPresent() {
        assertTrue(this.returnedLeaseReader.isPresent());
    }

    private void thenReturnedLeaseReaderIs(Class<?> dhcpServerLeaseReader) {
        assertEquals(dhcpServerLeaseReader, this.returnedLeaseReader.get().getClass());
    }
}
