/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.kura.nm.enums.MMModemMode;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.modemmanager1.Modem;
import org.freedesktop.modemmanager1.SetCurrentModesStruct;
import org.junit.Test;

public class ModemManagerDbusWrapperTest {

    private static final String MM_BUS_NAME = "org.freedesktop.ModemManager1";
    private static final String MM_MODEM_NAME = "org.freedesktop.ModemManager1.Modem";
    private static final String MODEM_PATH = "/org/freedesktop/ModemManager1/Modem/0";

    private final DBusConnection mockedDbusConnection = mock(DBusConnection.class);
    private final Modem mockedModem = mock(Modem.class);
    private final Properties mockedModemProperties = mock(Properties.class);

    private ModemManagerDbusWrapper modemManagerDbusWrapper;
    private Exception occurredException;

    @Test
    public void setModemModesShouldDoNothingIfEnabledModesAreMissing() throws DBusException {
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.empty(), Optional.of("4G"));

        thenExceptionDidNotOccur();
        thenNoDbusInteractionOccurred();
    }

    @Test
    public void setModemModesShouldDoNothingIfModemPathIsMissing() throws DBusException {
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.empty(), Optional.of(Arrays.asList("3G", "4G")), Optional.of("4G"));

        thenExceptionDidNotOccur();
        thenNoDbusInteractionOccurred();
    }

    @Test
    public void setModemModesShouldSetEnabledAndPreferredModes() throws DBusException {
        givenMockedModem();
        givenMockedModemProperties();
        givenMockedCurrentModes(EnumSet.of(MMModemMode.MM_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_3G),
                MMModemMode.MM_MODEM_MODE_2G);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Arrays.asList("3G", "4G")),
                Optional.of("4G"));

        thenExceptionDidNotOccur();
        thenModesWereSetTo(0x0000000CL, 0x00000008L);
    }

    @Test
    public void setModemModesShouldSetSingleEnabledMode() throws DBusException {
        givenMockedModem();
        givenMockedModemProperties();
        givenMockedCurrentModes(EnumSet.of(MMModemMode.MM_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_3G),
                MMModemMode.MM_MODEM_MODE_2G);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Collections.singletonList("5G")),
                Optional.of("5G"));

        thenExceptionDidNotOccur();
        thenModesWereSetTo(0x00000010L, 0x00000010L);
    }

    @Test
    public void setModemModesShouldUseNonePreferredModeIfPreferredModeIsMissing() throws DBusException {
        givenMockedModem();
        givenMockedModemProperties();
        givenMockedCurrentModes(EnumSet.of(MMModemMode.MM_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_3G),
                MMModemMode.MM_MODEM_MODE_2G);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Arrays.asList("2G", "3G")),
                Optional.empty());

        thenExceptionDidNotOccur();
        thenModesWereSetTo(0x00000006L, 0x00000000L);
    }

    @Test
    public void setModemModesShouldSetNoneIfEnabledModesAreEmpty() throws DBusException {
        givenMockedModem();
        givenMockedModemProperties();
        givenMockedCurrentModes(EnumSet.of(MMModemMode.MM_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_3G),
                MMModemMode.MM_MODEM_MODE_2G);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Collections.emptyList()),
                Optional.empty());

        thenExceptionDidNotOccur();
        thenModesWereSetTo(0x00000000L, 0x00000000L);
    }

    @Test
    public void setModemModesShouldSetAnyMode() throws DBusException {
        givenMockedModem();
        givenMockedModemProperties();
        givenMockedCurrentModes(EnumSet.of(MMModemMode.MM_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_3G),
                MMModemMode.MM_MODEM_MODE_2G);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Collections.singletonList("ANY")),
                Optional.empty());

        thenExceptionDidNotOccur();
        thenModesWereSetTo(0xFFFFFFFFL, 0x00000000L);
    }

    @Test
    public void setModemModesShouldThrowWithUnrecognizedEnabledMode() throws DBusException {
        givenMockedModem();
        givenMockedModemProperties();
        givenMockedCurrentModes(EnumSet.of(MMModemMode.MM_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_3G),
                MMModemMode.MM_MODEM_MODE_2G);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Collections.singletonList("LTE")),
                Optional.empty());

        thenExceptionOccurred(IllegalArgumentException.class);
        thenModesWereNotSet();
    }

    @Test
    public void setModemModesShouldThrowWithUnrecognizedPreferredMode() throws DBusException {
        givenMockedModem();
        givenMockedModemProperties();
        givenMockedCurrentModes(EnumSet.of(MMModemMode.MM_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_3G),
                MMModemMode.MM_MODEM_MODE_2G);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Collections.singletonList("4G")),
                Optional.of("LTE"));

        thenExceptionOccurred(IllegalArgumentException.class);
        thenModesWereNotSet();
    }

    @Test
    public void setModemModesShouldThrowWhenAnyIsMixedWithOtherModes() throws DBusException {
        givenMockedModem();
        givenMockedModemProperties();
        givenMockedCurrentModes(EnumSet.of(MMModemMode.MM_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_3G),
                MMModemMode.MM_MODEM_MODE_2G);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Arrays.asList("ANY", "4G")),
                Optional.empty());

        thenExceptionOccurred(IllegalArgumentException.class);
        thenModesWereNotSet();
    }

    @Test
    public void setModemModesShouldThrowWhenNoneIsMixedWithOtherModes() throws DBusException {
        givenMockedModem();
        givenMockedModemProperties();
        givenMockedCurrentModes(EnumSet.of(MMModemMode.MM_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_3G),
                MMModemMode.MM_MODEM_MODE_2G);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Arrays.asList("NONE", "4G")),
                Optional.empty());

        thenExceptionOccurred(IllegalArgumentException.class);
        thenModesWereNotSet();
    }

    @Test
    public void setModemModesShouldNotThrowIfSetCurrentModesFails() throws DBusException {
        givenMockedModem();
        givenMockedModemProperties();
        givenMockedCurrentModes(EnumSet.of(MMModemMode.MM_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_3G),
                MMModemMode.MM_MODEM_MODE_2G);
        givenMockedModemWillThrowOnSetCurrentModes();
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Arrays.asList("3G", "4G")),
                Optional.of("4G"));

        thenExceptionDidNotOccur();
        thenModesWereSetTo(0x0000000CL, 0x00000008L);
    }

    @Test
    public void setModemModesShouldThrowIfModemCannotBeRetrieved() throws DBusException {
        givenMockedDbusConnectionWillThrowWhenRetrieving(Modem.class);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Arrays.asList("3G", "4G")),
                Optional.of("4G"));

        thenExceptionOccurred(DBusException.class);
        thenModesWereNotSet();
    }

    @Test
    public void setModemModesShouldThrowIfModemPropertiesCannotBeRetrieved() throws DBusException {
        givenMockedModem();
        givenMockedDbusConnectionWillThrowWhenRetrieving(Properties.class);
        givenModemManagerDbusWrapper();

        whenSetModemModesIsCalledWith(Optional.of(MODEM_PATH), Optional.of(Arrays.asList("3G", "4G")),
                Optional.of("4G"));

        thenExceptionOccurred(DBusException.class);
        thenModesWereNotSet();
    }

    /*
     * Given
     */

    private void givenMockedModem() throws DBusException {
        when(this.mockedDbusConnection.getRemoteObject(MM_BUS_NAME, MODEM_PATH, Modem.class))
                .thenReturn(this.mockedModem);
    }

    private void givenMockedModemProperties() throws DBusException {
        when(this.mockedDbusConnection.getRemoteObject(MM_BUS_NAME, MODEM_PATH, Properties.class))
                .thenReturn(this.mockedModemProperties);
    }

    private void givenMockedCurrentModes(Set<MMModemMode> enabledModes, MMModemMode preferredMode) {
        when(this.mockedModemProperties.Get(MM_MODEM_NAME, "CurrentModes"))
                .thenReturn(new Object[] { MMModemMode.toBitMask(enabledModes), preferredMode.toUInt32() });
    }

    private void givenMockedModemWillThrowOnSetCurrentModes() {
        doThrow(new DBusExecutionException("Operation not supported")).when(this.mockedModem)
                .SetCurrentModes(any(SetCurrentModesStruct.class));
    }

    private <I extends DBusInterface> void givenMockedDbusConnectionWillThrowWhenRetrieving(Class<I> remoteObjectClass)
            throws DBusException {
        when(this.mockedDbusConnection.getRemoteObject(MM_BUS_NAME, MODEM_PATH, remoteObjectClass))
                .thenThrow(new DBusException("Remote object not found"));
    }

    private void givenModemManagerDbusWrapper() {
        this.modemManagerDbusWrapper = new ModemManagerDbusWrapper(this.mockedDbusConnection);
    }

    /*
     * When
     */

    private void whenSetModemModesIsCalledWith(Optional<String> mmDbusPath, Optional<List<String>> enabledModes,
            Optional<String> preferredMode) {
        try {
            this.modemManagerDbusWrapper.setModemModes(mmDbusPath, enabledModes, preferredMode);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    /*
     * Then
     */

    private void thenExceptionDidNotOccur() {
        assertNull(this.occurredException);
    }

    private void thenExceptionOccurred(Class<? extends Exception> expectedExceptionClass) {
        assertNotNull(this.occurredException);
        assertEquals(expectedExceptionClass, this.occurredException.getClass());
    }

    private void thenNoDbusInteractionOccurred() {
        verifyNoInteractions(this.mockedDbusConnection);
        verifyNoInteractions(this.mockedModem);
    }

    private void thenModesWereSetTo(long expectedEnabledModesBitMask, long expectedPreferredMode) {
        verify(this.mockedModem, times(1)).SetCurrentModes(new SetCurrentModesStruct(
                new UInt32(expectedEnabledModesBitMask), new UInt32(expectedPreferredMode)));
    }

    private void thenModesWereNotSet() {
        verify(this.mockedModem, never()).SetCurrentModes(any(SetCurrentModesStruct.class));
    }
}
