/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IP6Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetConfigIP4;
import org.eclipse.kura.net.NetConfigIP6;
import org.eclipse.kura.net.NetInterfaceStatus;
import org.eclipse.kura.net.dhcp.DhcpServerCfg;
import org.eclipse.kura.net.dhcp.DhcpServerCfgIP4;
import org.eclipse.kura.net.dhcp.DhcpServerConfigIP4;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.usb.UsbDevice;
import org.eclipse.kura.usb.UsbNetDevice;
import org.junit.Test;

public class IpConfigurationInterpreterTest {

    private static IP4Address ip4(String address) throws Exception {
        return (IP4Address) IPAddress.parseHostAddress(address);
    }

    private static IP6Address ip6(String address) throws Exception {
        return (IP6Address) IPAddress.parseHostAddress(address);
    }

    @Test
    public void shouldReturnEmptyListWhenPropertiesAreNull() throws Exception {
        List<NetConfig> netConfigs = IpConfigurationInterpreter.populateConfiguration(null, "eth0", ip4("192.168.1.1"),
                false);

        assertNotNull(netConfigs);
        assertTrue(netConfigs.isEmpty());
    }

    @Test
    public void shouldPopulateStaticIp4AndIp6Configuration() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interface.eth0.config.ip4.status", "netIPv4StatusEnabledLAN");
        props.put("net.interface.eth0.config.dhcpClient4.enabled", false);
        props.put("net.interface.eth0.config.ip4.address", "192.168.1.10");
        props.put("net.interface.eth0.config.ip4.prefix", "24");
        props.put("net.interface.eth0.config.ip4.gateway", "192.168.1.1");
        props.put("net.interface.eth0.config.ip4.dnsServers", "8.8.8.8, 8.8.4.4");
        props.put("net.interface.eth0.config.ip4.winsServers", "10.0.0.1");
        props.put("net.interface.eth0.config.ip4.domains", "example.com,test.com");

        props.put("net.interface.eth0.config.ip6.status", "netIPv6StatusEnabledLAN");
        props.put("net.interface.eth0.config.dhcpClient6.enabled", false);
        props.put("net.interface.eth0.config.ip6.address", "fe80::1");
        props.put("net.interface.eth0.config.ip6.dnsServers", "fe80::2,fe80::3");
        props.put("net.interface.eth0.config.ip6.domains", "example6.com");

        List<NetConfig> netConfigs = IpConfigurationInterpreter.populateConfiguration(props, "eth0",
                ip4("192.168.1.10"), false);

        assertNotNull(netConfigs);
        assertEquals(2, netConfigs.size());

        NetConfigIP4 expectedIp4 = new NetConfigIP4(NetInterfaceStatus.netIPv4StatusEnabledLAN, true);
        expectedIp4.setAddress(ip4("192.168.1.10"));
        expectedIp4.setNetworkPrefixLength((short) 24);
        expectedIp4.setGateway(ip4("192.168.1.1"));
        expectedIp4.setDnsServers(Arrays.asList(ip4("8.8.8.8"), ip4("8.8.4.4")));
        expectedIp4.setWinsServers(Arrays.asList(ip4("10.0.0.1")));
        expectedIp4.setDomains(Arrays.asList("example.com", "test.com"));

        assertEquals(expectedIp4, netConfigs.get(0));

        NetConfigIP6 expectedIp6 = new NetConfigIP6(NetInterfaceStatus.netIPv6StatusEnabledLAN, true, false);
        expectedIp6.setAddress(ip6("fe80::1"));
        expectedIp6.setDnsServers(Arrays.asList(ip6("fe80::2"), ip6("fe80::3")));
        expectedIp6.setDomains(Arrays.asList("example6.com"));

        assertEquals(expectedIp6, netConfigs.get(1));
    }

    @Test
    public void shouldUseDefaultStatusWhenIp4StatusMissingAndNotVirtual() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interface.eth0.config.dhcpClient4.enabled", false);

        List<NetConfig> netConfigs = IpConfigurationInterpreter.populateConfiguration(props, "eth0",
                ip4("192.168.1.10"), false);

        NetConfigIP4 ip4Config = (NetConfigIP4) netConfigs.get(0);

        assertEquals(NetInterfaceStatus.netIPv4StatusDisabled, ip4Config.getStatus());
        assertFalse(ip4Config.isAutoConnect());
    }

    @Test
    public void shouldSetDhcpAndIgnoreStaticFieldsWhenDhcpClient4Enabled() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interface.eth0.config.dhcpClient4.enabled", true);
        props.put("net.interface.eth0.config.ip4.address", "192.168.1.10");
        props.put("net.interface.eth0.config.ip4.gateway", "192.168.1.1");
        props.put("net.interface.eth0.config.ip4.prefix", "24");

        List<NetConfig> netConfigs = IpConfigurationInterpreter.populateConfiguration(props, "eth0",
                ip4("192.168.1.10"), false);

        NetConfigIP4 ip4Config = (NetConfigIP4) netConfigs.get(0);

        assertTrue(ip4Config.isDhcp());
        assertNull(ip4Config.getAddress());
        assertNull(ip4Config.getGateway());
    }

    @Test
    public void shouldAddFirewallAutoNatConfigWhenNatEnabled() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interface.eth0.config.nat.enabled", true);
        props.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        List<NetConfig> netConfigs = IpConfigurationInterpreter.populateConfiguration(props, "eth0",
                ip4("192.168.1.10"), false);

        assertEquals(2, netConfigs.size());
        assertEquals(new FirewallAutoNatConfig("eth0", "unknown", true), netConfigs.get(1));
    }

    @Test
    public void shouldNotAddFirewallAutoNatConfigWhenNatDisabled() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interface.eth0.config.nat.enabled", false);
        props.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        List<NetConfig> netConfigs = IpConfigurationInterpreter.populateConfiguration(props, "eth0",
                ip4("192.168.1.10"), false);

        assertEquals(1, netConfigs.size());
    }

    @Test
    public void shouldAddDhcpServerConfigWhenValidRangeProvided() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interface.eth0.config.dhcpClient4.enabled", false);
        props.put("net.interface.eth0.config.ip4.address", "192.168.1.1");
        props.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        props.put("net.interface.eth0.config.dhcpServer4.prefix", (short) 24);
        props.put("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.1.100");
        props.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.1.200");
        props.put("net.interface.eth0.config.dhcpServer4.defaultLeaseTime", 7200);
        props.put("net.interface.eth0.config.dhcpServer4.maxLeaseTime", 14400);
        props.put("net.interface.eth0.config.dhcpServer4.passDns", true);
        props.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        List<NetConfig> netConfigs = IpConfigurationInterpreter.populateConfiguration(props, "eth0",
                ip4("192.168.1.1"), false);

        assertEquals(2, netConfigs.size());

        IP4Address routerAddress = ip4("192.168.1.1");
        DhcpServerCfg dhcpServerCfg = new DhcpServerCfg("eth0", true, 7200, 14400, true);
        DhcpServerCfgIP4 dhcpServerCfgIP4 = new DhcpServerCfgIP4(ip4("192.168.1.0"), ip4("255.255.255.0"), (short) 24,
                routerAddress, ip4("192.168.1.100"), ip4("192.168.1.200"), Arrays.asList(routerAddress));
        DhcpServerConfigIP4 expected = new DhcpServerConfigIP4(dhcpServerCfg, dhcpServerCfgIP4);

        assertEquals(expected, netConfigs.get(1));
    }

    @Test
    public void shouldNotAddDhcpServerConfigWhenRangeIsMissing() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interface.eth0.config.dhcpServer4.enabled", true);
        props.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        List<NetConfig> netConfigs = IpConfigurationInterpreter.populateConfiguration(props, "eth0",
                ip4("192.168.1.1"), false);

        assertEquals(1, netConfigs.size());
    }

    @Test
    public void shouldSilentlyIgnoreInvalidDhcpServerConfigWhenDisabled() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interface.eth0.config.dhcpClient4.enabled", false);
        props.put("net.interface.eth0.config.ip4.address", "192.168.1.1");
        props.put("net.interface.eth0.config.dhcpServer4.enabled", false);
        props.put("net.interface.eth0.config.dhcpServer4.rangeStart", "192.168.1.100");
        props.put("net.interface.eth0.config.dhcpServer4.rangeEnd", "192.168.1.200");
        props.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        List<NetConfig> netConfigs = IpConfigurationInterpreter.populateConfiguration(props, "eth0",
                ip4("192.168.1.1"), false);

        assertEquals(1, netConfigs.size());
    }

    @Test
    public void shouldNotAddIp6ConfigWhenDhcp6Enabled() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interface.eth0.config.dhcpClient6.enabled", true);

        List<NetConfig> netConfigs = IpConfigurationInterpreter.populateConfiguration(props, "eth0",
                ip4("192.168.1.1"), false);

        assertEquals(1, netConfigs.size());
    }

    @Test
    public void shouldReturnUsbNetDeviceWhenVendorAndProductIdPresent() {
        Map<String, Object> props = new HashMap<>();
        props.put("net.interface.eth0.usb.vendor.id", "1234");
        props.put("net.interface.eth0.usb.vendor.name", "VendorName");
        props.put("net.interface.eth0.usb.product.id", "5678");
        props.put("net.interface.eth0.usb.product.name", "ProductName");
        props.put("net.interface.eth0.usb.busNumber", "1");
        props.put("net.interface.eth0.usb.devicePath", "1.2");

        UsbDevice usbDevice = IpConfigurationInterpreter.getUsbDeviceInfo(props, "eth0");

        UsbNetDevice expected = new UsbNetDevice("1234", "5678", "VendorName", "ProductName", "1", "1.2", "eth0");
        assertEquals(expected, usbDevice);
    }

    @Test
    public void shouldReturnNullUsbDeviceWhenVendorOrProductIdMissing() {
        Map<String, Object> props = new HashMap<>();

        assertNull(IpConfigurationInterpreter.getUsbDeviceInfo(props, "eth0"));
    }

    @Test
    public void shouldReturnNullUsbDeviceWhenPropertiesAreNull() {
        assertNull(IpConfigurationInterpreter.getUsbDeviceInfo(null, "eth0"));
    }
}
