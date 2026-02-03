package org.eclipse.kura.nm.configuration;

import java.util.Map;
import java.util.Objects;

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
        
        if(Objects.isNull(newConnectionSettings)) {
            throw new IllegalArgumentException("New connection settings cannot be null");
        }
        
        if(Objects.isNull(oldConnectionSettings)) {
            return false;
        }
        
        for (String settingKey : newConnectionSettings.keySet()) {
            Map<String, Variant<?>> newSetting = newConnectionSettings.get(settingKey);
            Map<String, Variant<?>> oldSetting = oldConnectionSettings.get(settingKey);

            if (oldSetting == null) {
                return false;
            }

            for (String propertyKey : newSetting.keySet()) {
                Variant<?> newValue = newSetting.get(propertyKey);
                Variant<?> oldValue = oldSetting.get(propertyKey);

                if (!Objects.equals(newValue, oldValue)) {
                    return false;
                }
            }
        }

        return true;
    }

}
