package org.eclipse.kura.nm;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.nm.enums.MMModemMode;

public enum KuraModemMode {

    KURA_MODEM_MODE_NONE("NONE"),
    KURA_MODEM_MODE_CS("CS"),
    KURA_MODEM_MODE_2G("2G"),
    KURA_MODEM_MODE_3G("3G"),
    KURA_MODEM_MODE_4G("4G"),
    KURA_MODEM_MODE_5G("5G"),
    KURA_MODEM_MODE_ANY("ANY");

    private final String value;

    private KuraModemMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static KuraModemMode fromString(String name) {
        for (KuraModemMode mode : KuraModemMode.values()) {
            if (mode.getValue().equals(name)) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Invalid modem mode in snapshot: " + name);
    }

    public static Set<KuraModemMode> fromStringList(List<String> modes) {
        if (modes.isEmpty()) {
            return EnumSet.of(KURA_MODEM_MODE_NONE);
        }

        EnumSet<KuraModemMode> result = EnumSet.noneOf(KuraModemMode.class);
        for (String mode : modes) {
            result.add(KuraModemMode.fromString(mode));
        }

        if (result.size() > 1 && (result.contains(KURA_MODEM_MODE_ANY) || result.contains(KURA_MODEM_MODE_NONE))) {
            throw new IllegalArgumentException(
                    "Too many modes passed. When \"NONE\" and \"ANY\" are used, the set should contain only one mode.");
        }

        return result;
    }

    public MMModemMode toMMModemMode() {
        switch (this) {
        case KURA_MODEM_MODE_NONE:
            return MMModemMode.MM_MODEM_MODE_NONE;
        case KURA_MODEM_MODE_CS:
            return MMModemMode.MM_MODEM_MODE_CS;
        case KURA_MODEM_MODE_2G:
            return MMModemMode.MM_MODEM_MODE_2G;
        case KURA_MODEM_MODE_3G:
            return MMModemMode.MM_MODEM_MODE_3G;
        case KURA_MODEM_MODE_4G:
            return MMModemMode.MM_MODEM_MODE_4G;
        case KURA_MODEM_MODE_5G:
            return MMModemMode.MM_MODEM_MODE_5G;
        case KURA_MODEM_MODE_ANY:
            return MMModemMode.MM_MODEM_MODE_ANY;
        default:
            throw new IllegalArgumentException(String.format("Unrecognized KuraModemMode: %s", value));
        }

    }
}
