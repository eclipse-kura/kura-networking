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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.nm.enums.MMModemMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

public class KuraModemModeTest {

    @RunWith(Parameterized.class)
    public static class KuraModemModeFromStringTest {

        @Parameters
        public static Collection<Object[]> ModemModeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new String("NONE"), KuraModemMode.KURA_MODEM_MODE_NONE, null });
            params.add(new Object[] { new String("CS"), KuraModemMode.KURA_MODEM_MODE_CS, null });
            params.add(new Object[] { new String("2G"), KuraModemMode.KURA_MODEM_MODE_2G, null });
            params.add(new Object[] { new String("3G"), KuraModemMode.KURA_MODEM_MODE_3G, null });
            params.add(new Object[] { new String("4G"), KuraModemMode.KURA_MODEM_MODE_4G, null });
            params.add(new Object[] { new String("5G"), KuraModemMode.KURA_MODEM_MODE_5G, null });
            params.add(new Object[] { new String("ANY"), KuraModemMode.KURA_MODEM_MODE_ANY, null });
            params.add(new Object[] { new String(""), null, IllegalArgumentException.class });
            params.add(new Object[] { new String(" "), null, IllegalArgumentException.class });
            params.add(new Object[] { new String("6G"), null, IllegalArgumentException.class });
            params.add(new Object[] { new String("none"), null, IllegalArgumentException.class });
            params.add(new Object[] { new String("Any"), null, IllegalArgumentException.class });
            params.add(new Object[] { new String(" 2G "), null, IllegalArgumentException.class });
            params.add(new Object[] { new String("MM_MODEM_MODE_2G"), null, IllegalArgumentException.class });
            params.add(new Object[] { new String("NONE,CS"), null, IllegalArgumentException.class });
            return params;
        }

        private final String inputValue;
        private final KuraModemMode expectedModemMode;
        private final Class<? extends Exception> expectedExceptionClass;
        private KuraModemMode calculatedModemMode;
        private Exception occurredException;

        public KuraModemModeFromStringTest(String stringValue, KuraModemMode modemMode,
                Class<? extends Exception> expectedExceptionClass) {
            this.inputValue = stringValue;
            this.expectedModemMode = modemMode;
            this.expectedExceptionClass = expectedExceptionClass;
        }

        @Test
        public void shouldReturnCorrectModemModeOrThrowException() {
            whenCalculatedModemMode();
            if (this.expectedExceptionClass != null) {
                thenExceptionOccurred(this.expectedExceptionClass);
            } else {
                thenNoExceptionOccurred();
                thenCalculatedModemModeIsCorrect();
            }
        }

        private void whenCalculatedModemMode() {
            try {
                this.calculatedModemMode = KuraModemMode.fromString(this.inputValue);
            } catch (Exception e) {
                this.occurredException = e;
            }
        }

        private void thenCalculatedModemModeIsCorrect() {
            assertEquals(this.expectedModemMode, this.calculatedModemMode);
        }

        private void thenNoExceptionOccurred() {
            assertNull(this.occurredException);
        }

        private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
            assertNotNull(this.occurredException);
            assertEquals(expectedException, this.occurredException.getClass());
        }
    }

    @RunWith(Parameterized.class)
    public static class KuraModemModeFromStringListTest {

        @Parameters
        public static Collection<Object[]> ModemModeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { //
                    Arrays.asList("NONE"), //
                    EnumSet.of(KuraModemMode.KURA_MODEM_MODE_NONE), //
                    null //
            });
            params.add(new Object[] { //
                    Arrays.asList("CS", "2G"), //
                    EnumSet.of(KuraModemMode.KURA_MODEM_MODE_CS, KuraModemMode.KURA_MODEM_MODE_2G), //
                    null //
            });
            params.add(new Object[] { //
                    Arrays.asList("2G", "3G"), //
                    EnumSet.of(KuraModemMode.KURA_MODEM_MODE_2G, KuraModemMode.KURA_MODEM_MODE_3G), //
                    null //
            });
            params.add(new Object[] { //
                    Arrays.asList("3G"), //
                    EnumSet.of(KuraModemMode.KURA_MODEM_MODE_3G), //
                    null //
            });
            params.add(new Object[] { //
                    Arrays.asList("CS", "2G", "3G", "4G"), //
                    EnumSet.of(KuraModemMode.KURA_MODEM_MODE_CS, KuraModemMode.KURA_MODEM_MODE_2G,
                            KuraModemMode.KURA_MODEM_MODE_3G, KuraModemMode.KURA_MODEM_MODE_4G), //
                    null //
            });
            params.add(new Object[] { //
                    Arrays.asList("ANY"), //
                    EnumSet.of(KuraModemMode.KURA_MODEM_MODE_ANY), //
                    null //
            });
            params.add(new Object[] { //
                    Arrays.asList(), //
                    EnumSet.of(KuraModemMode.KURA_MODEM_MODE_NONE), //
                    null //
            });
            params.add(new Object[] { //
                    Arrays.asList("ANY", "NONE"), //
                    null, //
                    IllegalArgumentException.class //
            });
            params.add(new Object[] { //
                    Arrays.asList("ANY", "2G"), //
                    null, //
                    IllegalArgumentException.class //
            });
            params.add(new Object[] { //
                    Arrays.asList("NONE", "2G"), //
                    null, //
                    IllegalArgumentException.class //
            });
            params.add(new Object[] { //
                    Arrays.asList("NONE", "CS", "2G", "3G", "4G"), //
                    null, //
                    IllegalArgumentException.class //
            });
            params.add(new Object[] { //
                    Arrays.asList("ANY", "CS", "2G", "3G", "4G"), //
                    null, //
                    IllegalArgumentException.class //
            });
            params.add(new Object[] { //
                    Arrays.asList("NONE", "CS", "2G", "3G", "4G", "5G", "ANY"), //
                    null, //
                    IllegalArgumentException.class //
            });
            params.add(new Object[] { //
                    Arrays.asList("6G"), //
                    null, //
                    IllegalArgumentException.class //
            });
            params.add(new Object[] { //
                    Arrays.asList("2G", "6G"), //
                    null, //
                    IllegalArgumentException.class //
            });
            params.add(new Object[] { //
                    Arrays.asList(""), //
                    null, //
                    IllegalArgumentException.class //
            });
            params.add(new Object[] { //
                    Arrays.asList("none", "cs"), //
                    null, //
                    IllegalArgumentException.class //
            });
            return params;
        }

        private final List<String> inputValue;
        private final Set<KuraModemMode> expectedModemModes;
        private final Class<? extends Exception> expectedExceptionClass;
        private Set<KuraModemMode> calculatedModemModes;
        private Exception occurredException;

        public KuraModemModeFromStringListTest(List<String> inputValue, Set<KuraModemMode> modemModes,
                Class<? extends Exception> expectedExceptionClass) {
            this.inputValue = inputValue;
            this.expectedModemModes = modemModes;
            this.expectedExceptionClass = expectedExceptionClass;
        }

        @Test
        public void shouldReturnCorrectModemModesOrThrowException() {
            whenCalculatedModemModes();
            if (this.expectedExceptionClass != null) {
                thenExceptionOccurred(this.expectedExceptionClass);
            } else {
                thenNoExceptionOccurred();
                thenCalculatedModemModesIsCorrect();
            }
        }

        private void whenCalculatedModemModes() {
            try {
                this.calculatedModemModes = KuraModemMode.fromStringList(this.inputValue);
            } catch (Exception e) {
                this.occurredException = e;
            }
        }

        private void thenCalculatedModemModesIsCorrect() {
            assertEquals(this.expectedModemModes, this.calculatedModemModes);
        }

        private void thenNoExceptionOccurred() {
            assertNull(this.occurredException);
        }

        private <E extends Exception> void thenExceptionOccurred(Class<E> expectedException) {
            assertNotNull(this.occurredException);
            assertEquals(expectedException, this.occurredException.getClass());
        }

    }

    @RunWith(Parameterized.class)
    public static class KuraModemModeToMMModemModeTest {

        @Parameters
        public static Collection<Object[]> ModemModeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { KuraModemMode.KURA_MODEM_MODE_NONE, MMModemMode.MM_MODEM_MODE_NONE });
            params.add(new Object[] { KuraModemMode.KURA_MODEM_MODE_CS, MMModemMode.MM_MODEM_MODE_CS });
            params.add(new Object[] { KuraModemMode.KURA_MODEM_MODE_2G, MMModemMode.MM_MODEM_MODE_2G });
            params.add(new Object[] { KuraModemMode.KURA_MODEM_MODE_3G, MMModemMode.MM_MODEM_MODE_3G });
            params.add(new Object[] { KuraModemMode.KURA_MODEM_MODE_4G, MMModemMode.MM_MODEM_MODE_4G });
            params.add(new Object[] { KuraModemMode.KURA_MODEM_MODE_5G, MMModemMode.MM_MODEM_MODE_5G });
            params.add(new Object[] { KuraModemMode.KURA_MODEM_MODE_ANY, MMModemMode.MM_MODEM_MODE_ANY });
            return params;
        }

        private final KuraModemMode inputValue;
        private final MMModemMode expectedModemMode;
        private MMModemMode calculatedModemMode;
        private Exception occurredException;

        public KuraModemModeToMMModemModeTest(KuraModemMode inputValue, MMModemMode expectedModemMode) {
            this.inputValue = inputValue;
            this.expectedModemMode = expectedModemMode;
        }

        @Test
        public void shouldReturnCorrectMMModemMode() {
            whenCalculatedMMModemMode();
            thenNoExceptionOccurred();
            thenCalculatedMMModemModeIsCorrect();
        }

        private void whenCalculatedMMModemMode() {
            try {
                this.calculatedModemMode = this.inputValue.toMMModemMode();
            } catch (Exception e) {
                this.occurredException = e;
            }
        }

        private void thenCalculatedMMModemModeIsCorrect() {
            assertEquals(this.expectedModemMode, this.calculatedModemMode);
        }

        private void thenNoExceptionOccurred() {
            assertNull(this.occurredException);
        }
    }

    @Test
    public void everyKuraModemModeShouldMapToAnMMModemMode() {
        for (KuraModemMode mode : KuraModemMode.values()) {
            assertNotNull(mode.toMMModemMode());
        }
    }
}
