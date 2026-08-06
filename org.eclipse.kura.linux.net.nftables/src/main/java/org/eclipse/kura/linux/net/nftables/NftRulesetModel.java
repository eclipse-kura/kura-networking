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
package org.eclipse.kura.linux.net.nftables;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Structured source-of-truth for restart-restore. Unlike the iptables-era
 * design, this is persisted directly (see {@link NftPersistenceStore}) rather
 * than reconstructed by reverse-parsing serialized rule text - the live
 * nftables ruleset is never read back for restore.
 */
public class NftRulesetModel {

    private Set<LocalRule> localRules = new LinkedHashSet<>();
    private Set<PortForwardRule> portForwardRules = new LinkedHashSet<>();
    private Set<NATRule> autoNatRules = new LinkedHashSet<>();
    private Set<NATRule> natRules = new LinkedHashSet<>();
    private boolean allowIcmp = true;
    private Set<String> additionalFilterRules = new LinkedHashSet<>();
    private Set<String> additionalNatRules = new LinkedHashSet<>();
    private Set<String> additionalMangleRules = new LinkedHashSet<>();

    public Set<LocalRule> getLocalRules() {
        return this.localRules;
    }

    public void setLocalRules(Set<LocalRule> localRules) {
        this.localRules = localRules;
    }

    public Set<PortForwardRule> getPortForwardRules() {
        return this.portForwardRules;
    }

    public void setPortForwardRules(Set<PortForwardRule> portForwardRules) {
        this.portForwardRules = portForwardRules;
    }

    public Set<NATRule> getAutoNatRules() {
        return this.autoNatRules;
    }

    public void setAutoNatRules(Set<NATRule> autoNatRules) {
        this.autoNatRules = autoNatRules;
    }

    public Set<NATRule> getNatRules() {
        return this.natRules;
    }

    public void setNatRules(Set<NATRule> natRules) {
        this.natRules = natRules;
    }

    public boolean isAllowIcmp() {
        return this.allowIcmp;
    }

    public void setAllowIcmp(boolean allowIcmp) {
        this.allowIcmp = allowIcmp;
    }

    public Set<String> getAdditionalFilterRules() {
        return this.additionalFilterRules;
    }

    public void setAdditionalFilterRules(Set<String> additionalFilterRules) {
        this.additionalFilterRules = additionalFilterRules;
    }

    public Set<String> getAdditionalNatRules() {
        return this.additionalNatRules;
    }

    public void setAdditionalNatRules(Set<String> additionalNatRules) {
        this.additionalNatRules = additionalNatRules;
    }

    public Set<String> getAdditionalMangleRules() {
        return this.additionalMangleRules;
    }

    public void setAdditionalMangleRules(Set<String> additionalMangleRules) {
        this.additionalMangleRules = additionalMangleRules;
    }
}
