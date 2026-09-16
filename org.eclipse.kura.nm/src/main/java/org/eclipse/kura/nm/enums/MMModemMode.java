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
package org.eclipse.kura.nm.enums;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.net.status.modem.ModemMode;
import org.freedesktop.dbus.types.UInt32;

public enum MMModemMode {

    MM_MODEM_MODE_NONE(0x00000000),
    MM_MODEM_MODE_CS(0x00000001),
    MM_MODEM_MODE_2G(0x00000002),
    MM_MODEM_MODE_3G(0x00000004),
    MM_MODEM_MODE_4G(0x00000008),
    MM_MODEM_MODE_5G(0x00000010),
    MM_MODEM_MODE_ANY(0xFFFFFFFF);

    private int value;

    private MMModemMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public UInt32 toUInt32() {
        return new UInt32(Integer.toUnsignedString(this.value));
    }

    public static MMModemMode toMMModemMode(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return MMModemMode.MM_MODEM_MODE_NONE;
        case 0x00000001:
            return MMModemMode.MM_MODEM_MODE_CS;
        case 0x00000002:
            return MMModemMode.MM_MODEM_MODE_2G;
        case 0x00000004:
            return MMModemMode.MM_MODEM_MODE_3G;
        case 0x00000008:
            return MMModemMode.MM_MODEM_MODE_4G;
        case 0x00000010:
            return MMModemMode.MM_MODEM_MODE_5G;
        case 0xFFFFFFFF:
            return MMModemMode.MM_MODEM_MODE_ANY;
        default:
            return MMModemMode.MM_MODEM_MODE_NONE;
        }
    }

    public static MMModemMode fromString(String type) {
        switch (type) {
        case "NONE":
            return MMModemMode.MM_MODEM_MODE_NONE;
        case "CS":
            return MMModemMode.MM_MODEM_MODE_CS;
        case "2G":
            return MMModemMode.MM_MODEM_MODE_2G;
        case "3G":
            return MMModemMode.MM_MODEM_MODE_3G;
        case "4G":
            return MMModemMode.MM_MODEM_MODE_4G;
        case "5G":
            return MMModemMode.MM_MODEM_MODE_5G;
        case "ANY":
            return MMModemMode.MM_MODEM_MODE_ANY;
        default:
            throw new IllegalArgumentException(String.format("Unrecognized MMModemMode: %s", type));
        }
    }

    public static ModemMode toModemMode(UInt32 type) {
        switch (type.intValue()) {
        case 0x00000000:
            return ModemMode.NONE;
        case 0x00000001:
            return ModemMode.CS;
        case 0x00000002:
            return ModemMode.MODE_2G;
        case 0x00000004:
            return ModemMode.MODE_3G;
        case 0x00000008:
            return ModemMode.MODE_4G;
        case 0x00000010:
            return ModemMode.MODE_5G;
        case 0xFFFFFFFF:
            return ModemMode.ANY;
        default:
            return ModemMode.NONE;
        }
    }

    public static Set<ModemMode> toModemModeFromBitMask(UInt32 bitMask) {
        long bitMaskValue = bitMask.longValue();
        if (bitMaskValue == 0x00000000L) {
            return EnumSet.of(ModemMode.NONE);
        }
        if (bitMaskValue == 0xFFFFFFFFL) {
            return EnumSet.of(ModemMode.ANY);
        }

        EnumSet<ModemMode> modemModes = EnumSet.noneOf(ModemMode.class);
        for (MMModemMode mode : MMModemMode.values()) {
            if (mode == MM_MODEM_MODE_NONE || mode == MM_MODEM_MODE_ANY) {
                continue;
            }
            if ((bitMaskValue & mode.getValue()) == mode.getValue()) {
                modemModes.add(toModemMode(mode.toUInt32()));
            }
        }
        return modemModes;
    }

    public static Set<MMModemMode> fromStringList(List<String> modes) {
        if (modes.isEmpty()) {
            return EnumSet.of(MMModemMode.MM_MODEM_MODE_NONE);
        }

        EnumSet<MMModemMode> result = EnumSet.noneOf(MMModemMode.class);
        for (String mode : modes) {
            result.add(MMModemMode.fromString(mode));
        }

        if (result.size() > 1 && (result.contains(MM_MODEM_MODE_ANY) || result.contains(MM_MODEM_MODE_NONE))) {
            throw new IllegalArgumentException(
                    "Too many modes passed. When MM_MODEM_MODE_ANY and MM_MODEM_MODE_NONE are used, the set should contain only one mode.");
        }

        return result;
    }

    public static UInt32 toBitMask(Set<MMModemMode> modes) {
        if (modes.isEmpty()) {
            return MM_MODEM_MODE_NONE.toUInt32();
        }

        if (modes.size() > 1 && (modes.contains(MM_MODEM_MODE_ANY) || modes.contains(MM_MODEM_MODE_NONE))) {
            throw new IllegalArgumentException(
                    "Too many modes passed. When MM_MODEM_MODE_ANY and MM_MODEM_MODE_NONE are used, the set should contain only one mode.");
        }

        long result = 0x00000000L;
        for (MMModemMode mode : modes) {
            if (mode == MM_MODEM_MODE_ANY) {
                return MM_MODEM_MODE_ANY.toUInt32();
            }
            if (mode == MM_MODEM_MODE_NONE) {
                return MM_MODEM_MODE_NONE.toUInt32();
            }

            result |= mode.toUInt32().longValue();
        }

        return new UInt32(result);
    }

    public static Set<MMModemMode> fromBitMask(UInt32 bitMask) {
        long bitMaskValue = bitMask.longValue();
        if (bitMaskValue == 0x00000000L) {
            return EnumSet.of(MMModemMode.MM_MODEM_MODE_NONE);
        }
        if (bitMaskValue == 0xFFFFFFFFL) {
            return EnumSet.of(MM_MODEM_MODE_ANY);
        }

        EnumSet<MMModemMode> modemModes = EnumSet.noneOf(MMModemMode.class);
        for (MMModemMode mode : MMModemMode.values()) {
            if (mode == MM_MODEM_MODE_NONE || mode == MM_MODEM_MODE_ANY) {
                continue;
            }
            if ((bitMaskValue & mode.getValue()) == mode.getValue()) {
                modemModes.add(toMMModemMode(mode.toUInt32()));
            }
        }
        return modemModes;
    }
}
