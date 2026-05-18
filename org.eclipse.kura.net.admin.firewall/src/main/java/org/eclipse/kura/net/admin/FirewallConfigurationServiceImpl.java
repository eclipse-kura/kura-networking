/*******************************************************************************
 * Copyright (c) 2023, 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.net.admin;

import static org.eclipse.kura.configuration.ConfigurationService.KURA_SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.net.UnknownHostException;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.SelfConfiguringComponent;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.core.configuration.metatype.ObjectFactory;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Tocd;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.core.net.FirewallConfiguration;
import org.eclipse.kura.executor.PrivilegedExecutorService;
import org.eclipse.kura.linux.net.iptables.AbstractLinuxFirewall;
import org.eclipse.kura.linux.net.iptables.LinuxFirewall;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.configuration.NetworkConfigurationMessages;
import org.eclipse.kura.net.configuration.NetworkConfigurationPropertyNames;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4;
import org.eclipse.kura.net.firewall.FirewallOpenPortConfigIP4.FirewallOpenPortConfigIP4Builder;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4;
import org.eclipse.kura.net.firewall.FirewallPortForwardConfigIP4.FirewallPortForwardConfigIP4Builder;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "org.eclipse.kura.net.admin.FirewallConfigurationService", //
    immediate = true, //
    configurationPolicy = ConfigurationPolicy.OPTIONAL, //
    property = { //
        "kura.service.pid=org.eclipse.kura.net.admin.FirewallConfigurationService", //
        "kura.ui.service.hide=true" //
    }
)
public class FirewallConfigurationServiceImpl extends
        AbstractFirewallConfigurationServiceImpl<IP4Address, FirewallOpenPortConfigIP4Builder, FirewallPortForwardConfigIP4Builder>
        implements FirewallConfigurationService, SelfConfiguringComponent {

    private static final Logger logger = LoggerFactory.getLogger(FirewallConfigurationServiceImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    public void setEventAdmin(EventAdmin eventAdmin) {
        super.setEventAdmin(eventAdmin);
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    public void setExecutorService(PrivilegedExecutorService executorService) {
        super.setExecutorService(executorService);
    }

    @Activate
    public void activate(ComponentContext context, Map<String, Object> properties) {
        super.activate(context, properties);
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    @Modified
    public void updated(Map<String, Object> properties) {
        super.updated(properties);
    }

    @Override
    protected FirewallConfiguration buildFirewallConfigurationFromProperties(Map<String, Object> properties) {
        return new FirewallConfiguration(properties);
    }

    @Override
    protected FirewallConfiguration buildFirewallConfiguration() {
        return new FirewallConfiguration();
    }

    @Override
    protected FirewallOpenPortConfigIP4Builder getOpenPortConfigIPBuilder() {
        return FirewallOpenPortConfigIP4.builder();
    }

    @Override
    protected FirewallPortForwardConfigIP4Builder getPortForwardConfigIPBuilder() {
        return FirewallPortForwardConfigIP4.builder();
    }

    @Override
    protected IP4Address getDefaultAddress() throws UnknownHostException {
        return IP4Address.getDefaultAddress();
    }

    @Override
    protected IP4Address getIPAddress(String address) throws UnknownHostException {
        return (IP4Address) IPAddress.parseHostAddress(address);
    }

    @Override
    public ComponentConfiguration getConfiguration() throws KuraException {
        logger.debug("getConfiguration()");
        try {
            Map<String, Object> firewallConfigurationProperties = getFirewallConfiguration()
                    .getConfigurationProperties();
            firewallConfigurationProperties.put(KURA_SERVICE_PID, PID);
            firewallConfigurationProperties.put(SERVICE_PID, PID);
            return new ComponentConfigurationImpl(PID, getDefinition(), firewallConfigurationProperties);
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    protected Tocd getDefinition() {

        ObjectFactory objectFactory = new ObjectFactory();
        Tocd tocd = objectFactory.createTocd();

        tocd.setName("FirewallConfigurationService");
        tocd.setId("org.eclipse.kura.net.admin.FirewallConfigurationService");
        tocd.setDescription("Firewall Configuration Service");

        Tad tad = objectFactory.createTad();
        tad.setId(FirewallConfiguration.OPEN_PORTS_PROP_NAME);
        tad.setName(FirewallConfiguration.OPEN_PORTS_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_OPEN_PORTS_VALUE);
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.FIREWALL_OPEN_PORTS));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(FirewallConfiguration.PORT_FORWARDING_PROP_NAME);
        tad.setName(FirewallConfiguration.PORT_FORWARDING_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_PORT_FORWARDING_VALUE);
        tad.setDescription(
                NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.FIREWALL_PORT_FORWARDING));
        tocd.addAD(tad);

        tad = objectFactory.createTad();
        tad.setId(FirewallConfiguration.NAT_PROP_NAME);
        tad.setName(FirewallConfiguration.NAT_PROP_NAME);
        tad.setType(Tscalar.STRING);
        tad.setCardinality(0);
        tad.setRequired(true);
        tad.setDefault(FirewallConfiguration.DFLT_NAT_VALUE);
        tad.setDescription(NetworkConfigurationMessages.getMessage(NetworkConfigurationPropertyNames.FIREWALL_NAT));
        tocd.addAD(tad);

        return tocd;
    }

    @Override
    protected AbstractLinuxFirewall getLinuxFirewall() {
        if (this.firewall == null) {
            this.firewall = LinuxFirewall.getInstance(this.executorService);
        }
        return this.firewall;
    }

}
