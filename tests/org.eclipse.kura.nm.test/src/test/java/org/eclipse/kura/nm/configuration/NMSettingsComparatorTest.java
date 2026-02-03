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
 *******************************************************************************/
package org.eclipse.kura.nm.configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import org.freedesktop.dbus.types.Variant;
import org.junit.Test;

public class NMSettingsComparatorTest {

    private Map<String, Map<String, Variant<?>>> newSettings = new HashMap<>();
    private Map<String, Map<String, Variant<?>>> oldSettings = new HashMap<>();
    
    @Test(expected = IllegalArgumentException.class)
    public void throwsWhenNewSettingsAreNull() {
        NMSettingsComparator.areSettingsEqual(null, oldSettings);
    }

    @Test
    public void returnsTrueWhenSettingsAreEqual() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));
        
        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        assertTrue(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsTrueWhenSettingsAreEqualButSubset() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));
        
        oldSettings.put("ipv4", new HashMap<>());
        oldSettings.get("ipv4").put("method", new Variant<String>("auto"));

        assertTrue(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }
    
    @Test
    public void returnsTrueWhenBothSettingsAreEmpty() {
        assertTrue(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }
    
    @Test
    public void returnsTrueWhenNewSettingsAreEmpty() {
        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        assertTrue(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenOldSettingsAreNull() {
        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, null));
    }

    @Test
    public void returnsFalseWhenSettingsAreNotEqual() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));
        
        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My Ethernet"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(2));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenSettingsAreNotEqualButSubset() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));
        
        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My Ethernet"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(2));

        oldSettings.put("ipv4", new HashMap<>());
        oldSettings.get("ipv4").put("method", new Variant<String>("auto"));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }

    @Test
    public void returnsFalseWhenSettingsAreEqualButSuperset() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        newSettings.get("connection").put("autoconnect-retries", new Variant<>(1));

        newSettings.put("ipv4", new HashMap<>());
        newSettings.get("ipv4").put("method", new Variant<String>("auto"));
        
        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My Ethernet"));
        oldSettings.get("connection").put("autoconnect-retries", new Variant<>(2));

        assertFalse(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }
}
