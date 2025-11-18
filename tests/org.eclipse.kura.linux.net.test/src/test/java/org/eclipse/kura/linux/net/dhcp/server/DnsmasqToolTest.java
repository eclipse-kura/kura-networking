/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.dhcp.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.eclipse.kura.KuraProcessExecutionErrorException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.ExitStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DnsmasqToolTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private File tmpConfigFile;
    private CommandExecutorService mockExecutor;
    private DnsmasqTool tool;
    private boolean isRunning;
    private CommandStatus startInterfaceStatus;
    private boolean interfaceDisabled;
    private Exception occurredException;

    /*
     * Scenarios
     */

    @Test
    public void isRunningShouldReturnTrueIfAlreadyStarted() throws Exception {
        givenExecutorReturnsExitStatus(0, true);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDnsmasqTool();
        givenStartInterface("eth0");

        whenIsRunning("eth0");

        thenIsRunningReturned(true);
    }

    @Test
    public void isRunningShouldReturnFalseIfNeverStarted() throws Exception {
        givenExecutorReturnsExitStatus(0, true);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDnsmasqTool();

        whenIsRunning("eth0");

        thenIsRunningReturned(false);
    }

    @Test
    public void isRunningShouldReturnFalseIfServiceIsNotActive() throws Exception {
        givenExecutorReturnsExitStatus(1, false);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDnsmasqTool();

        whenIsRunning("eth0");

        thenIsRunningReturned(false);
    }

    @Test
    public void shouldRemoveInterfaceConfigIfStartFails() throws Exception {
        givenExecutorReturnsExitStatus(1, false);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDnsmasqTool();

        whenStartInterface("eth0");

        thenInterfaceNotStarted();
        thenConfigFileNotPresent();
    }

    @Test
    public void shouldRemoveInterfaceConfigIfInterfaceDisabled() throws Exception {
        givenExecutorReturnsExitStatus(0, true);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDnsmasqTool();
        givenStartInterface("eth0");

        whenDisableInterface("eth0");

        thenConfigFileNotPresent();
        thenDisableWasSuccessful();
    }

    @Test
    public void shouldReturnExceptionWhenFailToDisableInterface() throws Exception {
        givenExecutorReturnsExitStatus(1, false);
        givenConfigFile("etc/dnsmasq.d/dnsmasq-eth0.conf");
        givenDnsmasqTool();

        whenDisableInterface("eth0");

        thenConfigFileNotPresent();
        thenKuraProcessExecutionErrorExceptionOccurred();
    }

    /*
     * Steps
     */

    /*
     * Given
     */

    private void givenExecutorReturnsExitStatus(int exitCode, boolean isSuccessful) {
        this.mockExecutor = mock(CommandExecutorService.class);

        ExitStatus returnedExitStatus = new ExitStatus() {

            @Override
            public int getExitCode() {
                return exitCode;
            }

            @Override
            public boolean isSuccessful() {
                return isSuccessful;
            }

        };
        CommandStatus returnedStatus = new CommandStatus(new Command(DnsmasqTool.IS_ACTIVE_COMMANDLINE),
                returnedExitStatus);

        when(this.mockExecutor.execute(any())).thenReturn(returnedStatus);
    }

    private void givenConfigFile(String filename) throws IOException {
        try {
            this.tmpFolder.newFolder("etc", "dnsmasq.d");
        } catch (Exception e) {
            // ignore
        }
        this.tmpConfigFile = this.tmpFolder.newFile(filename);
    }

    private void givenDnsmasqTool() {
        this.tool = new DnsmasqTool(this.mockExecutor) {

            @Override
            public String getConfigFilename(String interfaceName) {
                return DnsmasqToolTest.this.tmpConfigFile.getAbsolutePath();
            }

        };
        this.tool
                .setDnsmasqGlobalConfigFile(this.tmpConfigFile.getAbsoluteFile().getParent() + "/dnsmasq-globals.conf");
    }

    private void givenStartInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        this.tool.startInterface(interfaceName);
    }

    /*
     * When
     */

    private void whenIsRunning(String interfaceName) throws KuraProcessExecutionErrorException {
        this.isRunning = this.tool.isRunning(interfaceName);
    }

    private void whenStartInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        this.startInterfaceStatus = this.tool.startInterface(interfaceName);
    }

    private void whenDisableInterface(String interfaceName) throws KuraProcessExecutionErrorException {
        try {
            this.interfaceDisabled = this.tool.disableInterface(interfaceName);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenIsRunningReturned(boolean expectedResult) {
        assertEquals(expectedResult, this.isRunning);
    }

    private void thenInterfaceNotStarted() {
        assertFalse(this.startInterfaceStatus.getExitStatus().isSuccessful());
    }

    private void thenConfigFileNotPresent() {
        assertFalse(this.tmpConfigFile.exists());
    }

    private void thenDisableWasSuccessful() {
        assertTrue(this.interfaceDisabled);
    }

    private void thenKuraProcessExecutionErrorExceptionOccurred() {
        assertTrue(this.occurredException instanceof KuraProcessExecutionErrorException);
    }

    /*
     * Utilities
     */

}
