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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.linux.net.nftables.jna.NftCommandResult;
import org.eclipse.kura.linux.net.nftables.jna.NftExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pure builder/serializer/executor of the Kura nftables ruleset for ONE
 * address family (either {@code ip} or {@code ip6}, see
 * {@link NftablesConfigConstants#FAMILY_V4}/{@link NftablesConfigConstants#FAMILY_V6}).
 * IPv4 and IPv6 are kept in fully independent nft tables, mirroring the
 * independence the iptables-era {@code iptables}/{@code ip6tables} tables
 * had - so each instance of this class only ever touches its own family's
 * tables, never the other's. Within a family, the ruleset is further split
 * into three separate tables - {@code filter}, {@code nat}, {@code mangle} -
 * mirroring the iptables-era table layout 1:1.
 *
 * <p>
 * Every {@link #applyRules()} call rebuilds all three of this family's
 * tables as one script and submits it as a single nft transaction, which
 * removes the need for a separate live-flush-then-rebuild step: the script
 * always starts by ensuring each table exists, flushing it, then
 * re-declaring the (idempotent) chains before appending the actual rules.
 */
public class NftablesConfig extends NftablesConfigConstants {

    private static final Logger logger = LoggerFactory.getLogger(NftablesConfig.class);

    private final String family;
    private final NftExecutor nftExecutor;

    private Set<LocalRule> localRules = new LinkedHashSet<>();
    private Set<PortForwardRule> portForwardRules = new LinkedHashSet<>();
    private Set<NATRule> autoNatRules = new LinkedHashSet<>();
    private Set<NATRule> natRules = new LinkedHashSet<>();
    private boolean allowIcmp = true;
    private Set<String> additionalFilterRules = new LinkedHashSet<>();
    private Set<String> additionalNatRules = new LinkedHashSet<>();
    private Set<String> additionalMangleRules = new LinkedHashSet<>();

    /**
     * @param family
     *               either {@link NftablesConfigConstants#FAMILY_V4} ("ip") or
     *               {@link NftablesConfigConstants#FAMILY_V6} ("ip6")
     */
    public NftablesConfig(NftExecutor nftExecutor, String family) {
        this.nftExecutor = nftExecutor;
        this.family = family;
    }

    public void setLocalRules(Set<LocalRule> localRules) {
        this.localRules = localRules;
    }

    public Set<LocalRule> getLocalRules() {
        return this.localRules;
    }

    public void setPortForwardRules(Set<PortForwardRule> portForwardRules) {
        this.portForwardRules = portForwardRules;
    }

    public Set<PortForwardRule> getPortForwardRules() {
        return this.portForwardRules;
    }

    public void setAutoNatRules(Set<NATRule> autoNatRules) {
        this.autoNatRules = autoNatRules;
    }

    public Set<NATRule> getAutoNatRules() {
        return this.autoNatRules;
    }

    public void setNatRules(Set<NATRule> natRules) {
        this.natRules = natRules;
    }

    public Set<NATRule> getNatRules() {
        return this.natRules;
    }

    public void setAllowIcmp(boolean allowIcmp) {
        this.allowIcmp = allowIcmp;
    }

    public boolean isAllowIcmp() {
        return this.allowIcmp;
    }

    public void setAdditionalFilterRules(Set<String> additionalFilterRules) {
        this.additionalFilterRules = additionalFilterRules;
    }

    public void setAdditionalNatRules(Set<String> additionalNatRules) {
        this.additionalNatRules = additionalNatRules;
    }

    public void setAdditionalMangleRules(Set<String> additionalMangleRules) {
        this.additionalMangleRules = additionalMangleRules;
    }

    /**
     * Rebuilds and applies this family's full Kura ruleset (all three tables) as
     * a single nft transaction.
     */
    public void applyRules() throws KuraException {
        String script = String.join("\n", buildScript());
        NftCommandResult result = this.nftExecutor.run(script);
        logOnFailure(script, result);
        result.throwIfError();
    }

    /**
     * Recreates the table/chain skeleton with only the loopback and
     * established/related accept rules - a safe minimal fallback that still
     * drops everything else via the filter table's base-chain policy.
     */
    public void applyBlockPolicy() throws KuraException {
        List<String> scriptLines = new ArrayList<>();
        appendBootstrap(scriptLines);
        scriptLines.add(ruleLine(TABLE_FILTER, LOOPBACK_ACCEPT_FRAGMENT));
        scriptLines.add(ruleLine(TABLE_FILTER, ESTABLISHED_RELATED_ACCEPT_FRAGMENT));
        String script = String.join("\n", scriptLines);
        NftCommandResult result = this.nftExecutor.run(script);
        logOnFailure(script, result);
        result.throwIfError();
    }

    /**
     * Removes this family's three Kura tables (filter/nat/mangle) entirely,
     * reverting to no Kura-owned firewall enforcement for this family (the nft
     * equivalent of resetting policies back to default-accept-everywhere). Each
     * table is deleted independently so one already being absent doesn't abort
     * the removal of the others (a single combined transaction would roll back
     * entirely if any one delete failed). Never touches the other family's
     * tables.
     */
    public void clearAllChains() throws KuraException {
        deleteTableTolerant(TABLE_FILTER);
        deleteTableTolerant(TABLE_NAT);
        deleteTableTolerant(TABLE_MANGLE);
    }

    public String dumpRuleset(boolean json) {
        return this.nftExecutor.dumpRuleset(json).getOutputBuffer();
    }

    private void deleteTableTolerant(String table) throws KuraException {
        NftCommandResult result = this.nftExecutor.run("delete table " + this.family + " " + table);
        if (!result.isSuccess() && !result.getErrorBuffer().toLowerCase().contains("no such")) {
            result.throwIfError();
        }
    }

    private List<String> buildScript() {
        List<String> script = new ArrayList<>();
        appendBootstrap(script);

        script.add(ruleLine(TABLE_FILTER, LOOPBACK_ACCEPT_FRAGMENT));
        script.add(ruleLine(TABLE_FILTER, ESTABLISHED_RELATED_ACCEPT_FRAGMENT));
        List<String> icmpFragments;
        if (!this.allowIcmp) {
            icmpFragments = DO_NOT_ALLOW_ICMP;
        } else if (FAMILY_V6.equals(this.family)) {
            icmpFragments = ALLOW_ICMP_V6;
        } else {
            icmpFragments = ALLOW_ICMP_V4;
        }
        for (String fragment : icmpFragments) {
            script.add(ruleLine(TABLE_FILTER, fragment));
        }

        for (LocalRule rule : this.localRules) {
            script.add(ruleLine(TABLE_FILTER, rule.toNftStatement()));
        }

        for (PortForwardRule rule : this.portForwardRules) {
            script.add(ruleLine(TABLE_NAT, rule.getNatPreroutingChainRule().toNftStatement()));
            rule.getNatPostroutingChainRule().toNftStatement().map(f -> ruleLine(TABLE_NAT, f))
                    .ifPresent(script::add);
            for (String fragment : rule.getFilterForwardChainRule().toNftStatements()) {
                script.add(ruleLine(TABLE_FILTER, fragment));
            }
        }

        for (NATRule rule : this.autoNatRules) {
            appendNatRule(script, rule);
        }
        for (NATRule rule : this.natRules) {
            appendNatRule(script, rule);
        }

        script.addAll(this.additionalFilterRules);
        script.addAll(this.additionalNatRules);
        script.addAll(this.additionalMangleRules);

        return script;
    }

    private void appendNatRule(List<String> script, NATRule rule) {
        NatPostroutingChainRule postrouting = rule.getNatPostroutingChainRule();
        if (postrouting != null) {
            postrouting.toNftStatement().map(f -> ruleLine(TABLE_NAT, f)).ifPresent(script::add);
        }
        for (String fragment : rule.getFilterForwardChainRule().toNftStatements()) {
            script.add(ruleLine(TABLE_FILTER, fragment));
        }
    }

    private void logOnFailure(String script, NftCommandResult result) {
        if (!result.isSuccess()) {
            logger.error("nft command failed (rc={}): {}\nSubmitted script:\n{}", result.getReturnCode(),
                    result.getErrorBuffer(), script);
        }
    }

    private void appendBootstrap(List<String> script) {
        appendTableBootstrap(script, TABLE_FILTER, FILTER_BASE_CHAIN_FRAGMENTS, FILTER_KURA_CHAIN_NAMES,
                FILTER_JUMP_RULE_FRAGMENTS);
        appendTableBootstrap(script, TABLE_NAT, NAT_BASE_CHAIN_FRAGMENTS, NAT_KURA_CHAIN_NAMES,
                NAT_JUMP_RULE_FRAGMENTS);
        appendTableBootstrap(script, TABLE_MANGLE, MANGLE_BASE_CHAIN_FRAGMENTS, MANGLE_KURA_CHAIN_NAMES,
                MANGLE_JUMP_RULE_FRAGMENTS);
    }

    private void appendTableBootstrap(List<String> script, String table, List<String> baseChainFragments,
            List<String> kuraChainNames, List<String> jumpRuleFragments) {
        script.add("add table " + this.family + " " + table);
        script.add("flush table " + this.family + " " + table);
        for (String fragment : baseChainFragments) {
            script.add(chainLine(table, fragment));
        }
        for (String chainName : kuraChainNames) {
            script.add(chainLine(table, chainName));
        }
        for (String fragment : jumpRuleFragments) {
            script.add(ruleLine(table, fragment));
        }
    }

    private String chainLine(String table, String chainNameOrFragment) {
        return "add chain " + this.family + " " + table + " " + chainNameOrFragment;
    }

    private String ruleLine(String table, String fragment) {
        return "add rule " + this.family + " " + table + " " + fragment;
    }
}
