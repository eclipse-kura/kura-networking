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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.nm.NetworkProperties;
import org.eclipse.kura.nm.SemanticVersion;
import org.eclipse.kura.nm.enums.NMDeviceType;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.networkmanager.settings.Connection;
import org.junit.Test;
import org.mockito.Mockito;

public class NMSettingsComparatorTest {

    private Map<String, Map<String, Variant<?>>> newSettings = new HashMap<>();
    private Map<String, Map<String, Variant<?>>> oldSettings = new HashMap<>();

    @Test
    public void basicTest() {
        newSettings.put("connection", new HashMap<>());
        newSettings.get("connection").put("id", new Variant<String>("My WiFi"));
        
        oldSettings.put("connection", new HashMap<>());
        oldSettings.get("connection").put("id", new Variant<String>("My WiFi"));

        assertTrue(NMSettingsComparator.areSettingsEqual(newSettings, oldSettings));
    }
}
