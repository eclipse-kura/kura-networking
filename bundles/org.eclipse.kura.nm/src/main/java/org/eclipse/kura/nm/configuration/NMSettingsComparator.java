package org.eclipse.kura.nm.configuration;

import java.util.Map;

import org.freedesktop.dbus.types.Variant;

public class NMSettingsComparator {

    /* This method compares two NM connection settings maps and determines if they are equal. This comparison
     * is asymmetric, meaning that if newConnectionSettings contains all the settings in oldConnectionSettings with the same
     * values, it is considered equal, even if oldConnectionSettings has additional settings.
     *
     * @param newConnectionSettings The new connection settings to compare.
     * @param oldConnectionSettings The old connection settings to compare against. Can be a superset of newConnectionSettings.
     * @return true if the settings are considered equal, false otherwise.
     */
    public static boolean areSettingsEqual(Map<String,Map<String,Variant<?>>> newConnectionSettings,
            Map<String,Map<String,Variant<?>>> oldConnectionSettings) {
        // TODO Auto-generated method stub
        return false;
    }

}
