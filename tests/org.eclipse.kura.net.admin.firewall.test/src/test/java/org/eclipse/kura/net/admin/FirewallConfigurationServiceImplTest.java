/*******************************************************************************
 * Copyright (c) 2017, 2026 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.net.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.net.FirewallConfiguration;
import org.eclipse.kura.linux.net.iptables.AbstractLinuxFirewall;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.linux.net.iptables.LocalRule;
import org.eclipse.kura.linux.net.iptables.NATRule;
import org.eclipse.kura.linux.net.iptables.PortForwardRule;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetProtocol;
import org.eclipse.kura.net.NetworkPair;
import org.eclipse.kura.net.firewall.FirewallAutoNatConfig;
import org.eclipse.kura.net.firewall.FirewallNatConfig;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4.FirewallOpenPortConfigIP4Builder;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4.FirewallPortForwardConfigIP4Builder;
import org.eclipse.kura.net.firewall.RuleType;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;

public class FirewallConfigurationServiceImplTest {

    @Test(expected = NullPointerException.class)
    public void testActivateEmptyProps() throws KuraException {
        LinuxFirewall lfMock = mock(LinuxFirewall.class);
        EventAdmin eaMock = mock(EventAdmin.class);
        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected LinuxFirewall getLinuxFirewall() {
                return lfMock;
            }
        };
        svc.setEventAdmin(eaMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        BundleContext bundleContext = mock(BundleContext.class);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);

        Map<String, Object> properties = null;
        svc.activate(componentContext, properties);
    }

    @Test
    public void testActivate() throws KuraException, NumberFormatException, UnknownHostException {
        LinuxFirewall lfMock = mock(LinuxFirewall.class);
        EventAdmin eaMock = mock(EventAdmin.class);
        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected LinuxFirewall getLinuxFirewall() {
                return lfMock;
            }
        };
        svc.setEventAdmin(eaMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        BundleContext bundleContext = mock(BundleContext.class);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);

        Dictionary<String, Object> ccProperties = new Hashtable<>();
        ccProperties.put("kura.service.pid", "myFirewall");
        when(componentContext.getProperties()).thenReturn(ccProperties);

        Map<String, Object> properties = new HashMap<>();
        properties.put("firewall.open.ports", "22,tcp,1.2.3.4/32,eth1,,,,#");
        properties.put("firewall.nat", "eth0,eth1,tcp,0.0.0.0/0,0.0.0.0/0,true,#");
        properties.put("firewall.port.forwarding", "eth0,eth1,1.2.3.4,tcp,4050,3040,true,0.0.0.0/0,,,#");
        List<LocalRule> localRules = new ArrayList<>();
        localRules.add(new LocalRule(
                22,
                "tcp",
                new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("1.2.3.4"), Short.parseShort("32")),
                "eth1",
                null,
                null,
                null));
        List<PortForwardRule> portForwardRules = new ArrayList<>();
        portForwardRules.add(new PortForwardRule()
                .inboundIface("eth0")
                .outboundIface("eth1")
                .address("1.2.3.4")
                .protocol("tcp")
                .inPort(4050)
                .outPort(3040)
                .masquerade(true)
                .permittedNetwork("0.0.0.0")
                .permittedNetworkMask(0));
        List<NATRule> natRules = new ArrayList<>();
        natRules.add(new NATRule("eth0", "eth1", "tcp", "0.0.0.0/0", "0.0.0.0/0", true, RuleType.IP_FORWARDING));

        svc.activate(componentContext, properties);

        verify(lfMock).replace(localRules, portForwardRules, natRules);
    }

    @Test
    public void testGetConfiguration() throws KuraException {
        // test rules conversion into configuration properties

        LinuxFirewall linuxFirewall = mock(LinuxFirewall.class);
        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected Set<LocalRule> getLocalRules() throws KuraException {
                Set<LocalRule> result = new HashSet<>();

                IP4Address ipAddress = null;
                try {
                    ipAddress = (IP4Address) IP4Address.parseHostAddress("10.10.1.0");
                } catch (UnknownHostException e) {
                }
                NetworkPair<IP4Address> permittedNetwork = new NetworkPair<IP4Address>(ipAddress, (short) 24);
                String permittedIface = "eth0";
                String unpermittedIface = "wlan0";
                String permittedMac = null;
                String srcPorts = null;
                LocalRule rule = new LocalRule(
                        "1100:1200", "tcp", permittedNetwork, permittedIface, unpermittedIface, permittedMac, srcPorts);
                result.add(rule);
                rule = new LocalRule(
                        1300, "tcp", permittedNetwork, permittedIface, unpermittedIface, permittedMac, srcPorts);
                result.add(rule);

                return result;
            }

            @Override
            protected Set<PortForwardRule> getPortForwardRules() throws KuraException {
                Set<PortForwardRule> result = new HashSet<>();

                PortForwardRule rule = new PortForwardRule()
                        .inboundIface("wlan0")
                        .outboundIface("eth0")
                        .address("10.10.1.15")
                        .protocol("tcp")
                        .inPort(1234)
                        .outPort(2345)
                        .masquerade(true)
                        .permittedNetwork("10.10.1.0")
                        .permittedNetworkMask(24);
                result.add(rule);

                return result;
            }

            @Override
            protected Set<NATRule> getAutoNatRules() throws KuraException {
                Set<NATRule> result = new HashSet<>();
                return result;
            }

            @Override
            protected Set<NATRule> getNatRules() throws KuraException {
                Set<NATRule> result = new HashSet<>();

                NATRule rule = new NATRule("wlan0", "eth0", "tcp", null, "10.10.1.0/24", true, RuleType.IP_FORWARDING);
                result.add(rule);

                return result;
            }

            @Override
            protected AbstractLinuxFirewall getLinuxFirewall() {
                return linuxFirewall;
            }
        };

        ComponentConfiguration configuration = svc.getConfiguration();

        Map<String, Object> properties = configuration.getConfigurationProperties();

        assertNotNull(properties);
        assertEquals(5, properties.size());

        assertTrue(properties.containsKey("firewall.open.ports"));
        String ports = (String) properties.get("firewall.open.ports");
        assertNotNull(ports);
        assertTrue(ports.contains("1300,tcp,10.10.1.0/24,eth0,wlan0,,,#"));
        assertTrue(ports.contains("1100:1200,tcp,10.10.1.0/24,eth0,wlan0,,,#"));

        assertTrue(properties.containsKey("firewall.nat"));
        String nat = (String) properties.get("firewall.nat");
        assertNotNull(nat);
        assertTrue(nat.contains("wlan0,eth0,tcp,,10.10.1.0/24,true,#"));

        assertTrue(properties.containsKey("firewall.port.forwarding"));
        String fwd = (String) properties.get("firewall.port.forwarding");
        assertNotNull(fwd);
        assertTrue(fwd.contains("wlan0,eth0,10.10.1.15,tcp,1234,2345,true,10.10.1.0/24,,,#"));
    }

    @Test
    public void testGetFirewallConfiguration() throws KuraException {
        // test 'raw' configuration retrieval

        LinuxFirewall linuxFirewall = mock(LinuxFirewall.class);
        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected Set<LocalRule> getLocalRules() throws KuraException {
                Set<LocalRule> result = new HashSet<>();

                IP4Address ipAddress = null;
                try {
                    ipAddress = (IP4Address) IP4Address.parseHostAddress("10.10.1.0");
                } catch (UnknownHostException e) {
                }
                NetworkPair<IP4Address> permittedNetwork = new NetworkPair<IP4Address>(ipAddress, (short) 24);
                String permittedIface = "eth0";
                String unpermittedIface = "wlan0";
                String permittedMac = null;
                String srcPorts = null;
                LocalRule rule = new LocalRule(
                        "1100:1200", "tcp", permittedNetwork, permittedIface, unpermittedIface, permittedMac, srcPorts);
                result.add(rule);
                rule = new LocalRule(
                        1300, "tcp", permittedNetwork, permittedIface, unpermittedIface, permittedMac, srcPorts);
                result.add(rule);

                return result;
            }

            @Override
            protected Set<PortForwardRule> getPortForwardRules() throws KuraException {
                Set<PortForwardRule> result = new HashSet<>();

                PortForwardRule rule = new PortForwardRule()
                        .inboundIface("wlan0")
                        .outboundIface("eth0")
                        .address("10.10.1.15")
                        .protocol("tcp")
                        .inPort(1234)
                        .outPort(2345)
                        .masquerade(true)
                        .permittedNetwork("10.10.1.0")
                        .permittedNetworkMask(24);
                result.add(rule);

                return result;
            }

            @Override
            protected Set<NATRule> getAutoNatRules() throws KuraException {
                Set<NATRule> result = new HashSet<>();

                NATRule rule = new NATRule("wlan0", "eth0", "tcp", null, "10.10.1.0/24", true, RuleType.GENERIC);
                result.add(rule);

                return result;
            }

            @Override
            protected Set<NATRule> getNatRules() throws KuraException {
                Set<NATRule> result = new HashSet<>();

                NATRule rule = new NATRule("wlan0", "eth0", "tcp", null, "10.10.1.0/24", true, RuleType.IP_FORWARDING);
                result.add(rule);

                return result;
            }

            @Override
            protected AbstractLinuxFirewall getLinuxFirewall() {
                return linuxFirewall;
            }
        };

        FirewallConfiguration configuration = svc.getFirewallConfiguration();

        List<FirewallAutoNatConfig> autoNatConfigs = configuration.getAutoNatConfigs();
        assertEquals(1, autoNatConfigs.size());
        FirewallAutoNatConfig autoNat = autoNatConfigs.get(0);
        assertEquals("wlan0", autoNat.getSourceInterface());
        assertEquals("eth0", autoNat.getDestinationInterface());
        assertTrue(autoNat.isMasquerade());

        List<FirewallNatConfig> natConfigs = configuration.getNatConfigs();
        assertEquals(1, natConfigs.size());
        FirewallNatConfig nat = natConfigs.get(0);
        assertEquals("10.10.1.0/24", nat.getDestination());
        assertEquals("eth0", nat.getDestinationInterface());
        assertEquals("tcp", nat.getProtocol());
        assertNull(nat.getSource());
        assertEquals("wlan0", nat.getSourceInterface());

        List<FirewallOpenPortConfigIP<? extends IPAddress>> portConfigs = configuration.getOpenPortConfigs();
        assertEquals(2, portConfigs.size());

        FirewallOpenPortConfigIP<? extends IPAddress> port = portConfigs.get(1);
        assertEquals("eth0", port.getPermittedInterfaceName());
        assertNull(port.getPermittedMac());
        assertEquals("10.10.1.0", port.getPermittedNetwork().getIpAddress().getHostAddress());
        assertEquals(24, port.getPermittedNetwork().getPrefix());
        assertEquals(1300, port.getPort());
        assertNull(port.getPortRange());
        assertEquals(NetProtocol.tcp, port.getProtocol());
        assertNull(port.getSourcePortRange());
        assertEquals("wlan0", port.getUnpermittedInterfaceName());

        port = portConfigs.get(0);
        assertEquals("eth0", port.getPermittedInterfaceName());
        assertNull(port.getPermittedMac());
        assertEquals("10.10.1.0", port.getPermittedNetwork().getIpAddress().getHostAddress());
        assertEquals(24, port.getPermittedNetwork().getPrefix());
        assertEquals(-1, port.getPort());
        assertEquals("1100:1200", port.getPortRange());
        assertEquals(NetProtocol.tcp, port.getProtocol());
        assertNull(port.getSourcePortRange());
        assertEquals("wlan0", port.getUnpermittedInterfaceName());

        List<FirewallPortForwardConfigIP<? extends IPAddress>> forwardConfigs = configuration.getPortForwardConfigs();
        assertEquals(1, forwardConfigs.size());

        FirewallPortForwardConfigIP<? extends IPAddress> fwd = forwardConfigs.get(0);
        assertEquals("10.10.1.15", fwd.getAddress().getHostAddress());
        assertEquals("wlan0", fwd.getInboundInterface());
        assertEquals(1234, fwd.getInPort());
        assertEquals("eth0", fwd.getOutboundInterface());
        assertEquals(2345, fwd.getOutPort());
        assertNull(fwd.getPermittedMac());
        assertEquals("10.10.1.0", fwd.getPermittedNetwork().getIpAddress().getHostAddress());
        assertEquals(24, fwd.getPermittedNetwork().getPrefix());
        assertEquals(NetProtocol.tcp, fwd.getProtocol());
        assertNull(fwd.getSourcePortRange());
    }

    @Test
    public void testUpdateFailureHandler() throws KuraException {
        // test updated() that fails while applying the firewall configuration

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                "firewall.open.ports",
                "1300,tcp,10.10.1.0/24,eth0,wlan0,,,#" + ";1100:1200,tcp,10.10.1.0/24,eth0,wlan0,,,#");

        properties.put("firewall.nat", "wlan0,eth0,tcp,,10.10.1.0/24,true,#");

        properties.put("firewall.port.forwarding", "wlan0,eth0,10.10.1.15,tcp,1234,2345,true,10.10.1.0/24,,,#");

        LinuxFirewall linuxFirewall = mock(LinuxFirewall.class);
        doThrow(new KuraException(KuraErrorCode.CONFIGURATION_ERROR))
                .when(linuxFirewall)
                .replace(anyList(), anyList(), anyList());
        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected AbstractLinuxFirewall getLinuxFirewall() {
                return linuxFirewall;
            }
        };

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        Dictionary<String, Object> ccProperties = new Hashtable<>();
        ccProperties.put("kura.service.pid", "myFirewall");
        when(componentContext.getProperties()).thenReturn(ccProperties);
        svc.activate(componentContext, properties);
        svc.updated(properties);

        // updated() is invoked once by activate() and once explicitly: both times
        // replace() fails
        // and the exception is caught and logged, so the event is still posted both
        // times.
        verify(linuxFirewall, times(2)).replace(anyList(), anyList(), anyList());
        verify(eventAdminMock, times(2)).postEvent(any());
    }

    @Test
    public void testUpdateHandler() throws KuraException, UnknownHostException {
        // test updated() that actually applies new configuration from properties

        Map<String, Object> properties = new HashMap<>();

        properties.put(
                "firewall.open.ports",
                "1300,tcp,10.10.1.0/24,eth0,wlan0,,,#" + ";1100:1200,tcp,10.10.1.0/24,eth0,wlan0,,,#");

        properties.put("firewall.nat", "wlan0,eth0,tcp,,10.10.1.0/24,true,#");

        properties.put("firewall.port.forwarding", "wlan0,eth0,10.10.1.15,tcp,1234,2345,true,10.10.1.0/24,,,#");

        LinuxFirewall linuxFirewall = mock(LinuxFirewall.class);
        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected AbstractLinuxFirewall getLinuxFirewall() {
                return linuxFirewall;
            }
        };

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        Dictionary<String, Object> ccProperties = new Hashtable<>();
        ccProperties.put("kura.service.pid", "myFirewall");
        when(componentContext.getProperties()).thenReturn(ccProperties);
        svc.activate(componentContext, properties);
        svc.updated(properties);

        NetworkPair<IP4Address> permittedNetwork =
                new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("10.10.1.0"), (short) 24);

        List<LocalRule> localRules = new ArrayList<>();
        localRules.add(new LocalRule(1300, "tcp", permittedNetwork, "eth0", "wlan0", null, null));
        localRules.add(new LocalRule("1100:1200", "tcp", permittedNetwork, "eth0", "wlan0", null, null));

        List<PortForwardRule> portForwardRules = new ArrayList<>();
        portForwardRules.add(new PortForwardRule()
                .inboundIface("wlan0")
                .outboundIface("eth0")
                .address("10.10.1.15")
                .protocol("tcp")
                .inPort(1234)
                .outPort(2345)
                .masquerade(true)
                .permittedNetwork("10.10.1.0")
                .permittedNetworkMask(24));

        List<NATRule> natRules = new ArrayList<>();
        natRules.add(new NATRule("wlan0", "eth0", "tcp", null, "10.10.1.0/24", true, RuleType.IP_FORWARDING));

        verify(linuxFirewall, times(2)).replace(localRules, portForwardRules, natRules);
        verify(eventAdminMock, times(2)).postEvent(any());
    }

    @Test
    public void testSetFirewallOpenPortConfiguration() throws KuraException, UnknownHostException {
        LinuxFirewall linuxFirewall = mock(LinuxFirewall.class);

        List<PortForwardRule> existingPortForwardRules = new ArrayList<>();
        existingPortForwardRules.add(new PortForwardRule()
                .inboundIface("eth0")
                .outboundIface("eth1")
                .address("10.10.1.15")
                .protocol("tcp")
                .inPort(1234)
                .outPort(2345)
                .masquerade(true)
                .permittedNetwork("10.10.1.0")
                .permittedNetworkMask(24));
        List<NATRule> existingNatRules = new ArrayList<>();
        existingNatRules.add(new NATRule("eth0", "eth1", "tcp", null, "10.10.1.0/24", true, RuleType.IP_FORWARDING));

        when(linuxFirewall.getPortForwardRules()).thenReturn(new HashSet<>(existingPortForwardRules));
        when(linuxFirewall.getNatRules()).thenReturn(new HashSet<>(existingNatRules));

        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected AbstractLinuxFirewall getLinuxFirewall() {
                return linuxFirewall;
            }
        };

        List<FirewallOpenPortConfigIP<? extends IPAddress>> firewallConfiguration = new ArrayList<>();
        FirewallOpenPortConfigIP4Builder builder = FirewallOpenPortConfigIP4.builder();
        builder.withPort(1234).withProtocol(NetProtocol.tcp);
        firewallConfiguration.add(builder.build());

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        Dictionary<String, Object> ccProperties = new Hashtable<>();
        ccProperties.put("kura.service.pid", "myFirewall");
        when(componentContext.getProperties()).thenReturn(ccProperties);
        svc.activate(componentContext, new HashMap<>());
        svc.setFirewallOpenPortConfiguration(firewallConfiguration);

        List<LocalRule> expectedLocalRules = new ArrayList<>();
        expectedLocalRules.add(new LocalRule(
                1234,
                "tcp",
                new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("0.0.0.0"), (short) 0),
                null,
                null,
                null,
                null));

        verify(linuxFirewall).replace(expectedLocalRules, existingPortForwardRules, existingNatRules);
    }

    @Test
    public void testSetFirewallPortForwardingConfiguration() throws KuraException, UnknownHostException {
        LinuxFirewall linuxFirewall = mock(LinuxFirewall.class);

        List<LocalRule> existingLocalRules = new ArrayList<>();
        existingLocalRules.add(new LocalRule(
                22,
                "tcp",
                new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("0.0.0.0"), (short) 0),
                "eth0",
                null,
                null,
                null));
        List<NATRule> existingNatRules = new ArrayList<>();
        existingNatRules.add(new NATRule("eth0", "eth1", "tcp", null, "10.10.1.0/24", true, RuleType.IP_FORWARDING));

        when(linuxFirewall.getLocalRules()).thenReturn(new HashSet<>(existingLocalRules));
        when(linuxFirewall.getNatRules()).thenReturn(new HashSet<>(existingNatRules));

        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected AbstractLinuxFirewall getLinuxFirewall() {
                return linuxFirewall;
            }
        };

        FirewallPortForwardConfigIP4Builder pfBuilder = FirewallPortForwardConfigIP4.builder();
        pfBuilder
                .withInboundIface("eth0")
                .withOutboundIface("eth1")
                .withAddress((IP4Address) IPAddress.parseHostAddress("172.16.0.1"))
                .withProtocol(NetProtocol.tcp)
                .withInPort(3040)
                .withOutPort(4050)
                .withMasquerade(true)
                .withPermittedNetwork(
                        new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("172.16.0.100"), (short) 32));
        List<FirewallPortForwardConfigIP<? extends IPAddress>> firewallConfiguration = new ArrayList<>();
        firewallConfiguration.add(pfBuilder.build());

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        Dictionary<String, Object> ccProperties = new Hashtable<>();
        ccProperties.put("kura.service.pid", "myFirewall");
        when(componentContext.getProperties()).thenReturn(ccProperties);
        svc.activate(componentContext, new HashMap<>());
        svc.setFirewallPortForwardingConfiguration(firewallConfiguration);

        List<PortForwardRule> expectedPortForwardRules = new ArrayList<>();
        expectedPortForwardRules.add(new PortForwardRule()
                .inboundIface("eth0")
                .outboundIface("eth1")
                .address("172.16.0.1")
                .protocol("tcp")
                .inPort(3040)
                .outPort(4050)
                .masquerade(true)
                .permittedNetwork("172.16.0.100")
                .permittedNetworkMask(32));

        verify(linuxFirewall).replace(existingLocalRules, expectedPortForwardRules, existingNatRules);
    }

    @Test
    public void testSetFirewallNatConfiguration() throws KuraException, UnknownHostException {
        LinuxFirewall linuxFirewall = mock(LinuxFirewall.class);

        List<LocalRule> existingLocalRules = new ArrayList<>();
        existingLocalRules.add(new LocalRule(
                22,
                "tcp",
                new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("0.0.0.0"), (short) 0),
                "eth0",
                null,
                null,
                null));
        List<PortForwardRule> existingPortForwardRules = new ArrayList<>();
        existingPortForwardRules.add(new PortForwardRule()
                .inboundIface("eth0")
                .outboundIface("eth1")
                .address("10.10.1.15")
                .protocol("tcp")
                .inPort(1234)
                .outPort(2345)
                .masquerade(true)
                .permittedNetwork("10.10.1.0")
                .permittedNetworkMask(24));

        when(linuxFirewall.getLocalRules()).thenReturn(new HashSet<>(existingLocalRules));
        when(linuxFirewall.getPortForwardRules()).thenReturn(new HashSet<>(existingPortForwardRules));

        FirewallConfigurationServiceImpl svc = new FirewallConfigurationServiceImpl() {

            @Override
            protected AbstractLinuxFirewall getLinuxFirewall() {
                return linuxFirewall;
            }
        };

        List<FirewallNatConfig> natConfigs = new ArrayList<>();
        natConfigs.add(new FirewallNatConfig(
                "eth0", "eth1", "tcp", "172.16.0.1/32", "172.16.0.2/32", true, RuleType.IP_FORWARDING));

        EventAdmin eventAdminMock = mock(EventAdmin.class);
        svc.setEventAdmin(eventAdminMock);

        ComponentContext componentContext = mock(ComponentContext.class);
        Dictionary<String, Object> ccProperties = new Hashtable<>();
        ccProperties.put("kura.service.pid", "myFirewall");
        when(componentContext.getProperties()).thenReturn(ccProperties);
        svc.activate(componentContext, new HashMap<>());
        svc.setFirewallNatConfiguration(natConfigs);

        List<NATRule> expectedNatRules = new ArrayList<>();
        expectedNatRules.add(
                new NATRule("eth0", "eth1", "tcp", "172.16.0.1/32", "172.16.0.2/32", true, RuleType.IP_FORWARDING));

        verify(linuxFirewall).replace(existingLocalRules, existingPortForwardRules, expectedNatRules);
    }
}
