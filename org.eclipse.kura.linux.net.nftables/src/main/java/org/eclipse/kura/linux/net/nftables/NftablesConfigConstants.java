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

import java.util.Arrays;
import java.util.List;

/**
 * Family-independent building blocks for the Kura nftables ruleset. IPv4 and
 * IPv6 are kept in fully independent nft tables/families ({@code ip}/
 * {@code ip6}, mirroring the independence the iptables-era
 * {@code iptables}/{@code ip6tables} tables had), and within each family the
 * ruleset is split into three separate tables - {@code filter}, {@code nat},
 * {@code mangle} - mirroring the iptables-era table layout 1:1 (rather than
 * folding everything into one nft table), so every chain/rule fragment
 * defined here is written once and assembled by a family-aware
 * {@link NftablesConfig} instance, which is the only place that knows
 * whether it is talking to the "ip" or "ip6" family.
 */
public class NftablesConfigConstants {

    protected static final String FAMILY_V4 = "ip";
    protected static final String FAMILY_V6 = "ip6";

    protected static final String TABLE_FILTER = "filter";
    protected static final String TABLE_NAT = "nat";
    protected static final String TABLE_MANGLE = "mangle";

    protected static final String INPUT = "input";
    protected static final String OUTPUT = "output";
    protected static final String FORWARD = "forward";
    protected static final String PREROUTING = "prerouting";
    protected static final String POSTROUTING = "postrouting";

    // nft identifiers (table/chain/set names) do not allow hyphens - use
    // underscores instead, unlike the iptables-era chain names this mirrors.
    // Chain names only need to be unique within their own table, so the mangle
    // table's chains (below) safely reuse these same bare names in their own
    // separate table namespace.
    protected static final String INPUT_KURA_CHAIN = "input_kura";
    protected static final String OUTPUT_KURA_CHAIN = "output_kura";
    protected static final String FORWARD_KURA_CHAIN = "forward_kura";
    protected static final String FORWARD_KURA_PF_CHAIN = "forward_kura_pf";
    protected static final String FORWARD_KURA_IPF_CHAIN = "forward_kura_ipf";
    protected static final String PREROUTING_KURA_CHAIN = "prerouting_kura";
    protected static final String PREROUTING_KURA_PF_CHAIN = "prerouting_kura_pf";
    protected static final String POSTROUTING_KURA_CHAIN = "postrouting_kura";
    protected static final String POSTROUTING_KURA_PF_CHAIN = "postrouting_kura_pf";
    protected static final String POSTROUTING_KURA_IPF_CHAIN = "postrouting_kura_ipf";

    protected static final String FILTER_PRIORITY = "filter";
    protected static final String DSTNAT_PRIORITY = "dstnat";
    protected static final String SRCNAT_PRIORITY = "srcnat";
    protected static final String MANGLE_PRIORITY = "mangle";

    protected static final String FIREWALL_CONFIG_FILE_NAME_V4 = "/etc/sysconfig/nftables-kura-v4.conf";
    protected static final String FIREWALL_TMP_CONFIG_FILE_NAME_V4 = "/tmp/nftables-kura-v4.conf";
    protected static final String FIREWALL_CONFIG_FILE_NAME_V6 = "/etc/sysconfig/nftables-kura-v6.conf";
    protected static final String FIREWALL_TMP_CONFIG_FILE_NAME_V6 = "/tmp/nftables-kura-v6.conf";

    protected static final String IP_FORWARD_FILE_NAME_V4 = "/proc/sys/net/ipv4/ip_forward";
    protected static final String IP_FORWARD_FILE_NAME_V6 = "/proc/sys/net/ipv6/conf/all/forwarding";

    // ---- filter table ----

    protected static final List<String> FILTER_BASE_CHAIN_FRAGMENTS = Arrays.asList(
            INPUT + " { type filter hook input priority " + FILTER_PRIORITY + "; policy drop; }",
            OUTPUT + " { type filter hook output priority " + FILTER_PRIORITY + "; policy accept; }",
            FORWARD + " { type filter hook forward priority " + FILTER_PRIORITY + "; policy drop; }");

    protected static final List<String> FILTER_KURA_CHAIN_NAMES = Arrays.asList(INPUT_KURA_CHAIN, OUTPUT_KURA_CHAIN,
            FORWARD_KURA_CHAIN, FORWARD_KURA_PF_CHAIN, FORWARD_KURA_IPF_CHAIN);

    protected static final List<String> FILTER_JUMP_RULE_FRAGMENTS = Arrays.asList(
            jumpFragment(INPUT, INPUT_KURA_CHAIN), jumpFragment(OUTPUT, OUTPUT_KURA_CHAIN),
            jumpFragment(FORWARD, FORWARD_KURA_CHAIN));

    // ---- nat table ----

    protected static final List<String> NAT_BASE_CHAIN_FRAGMENTS = Arrays.asList(
            PREROUTING + " { type nat hook prerouting priority " + DSTNAT_PRIORITY + "; policy accept; }",
            POSTROUTING + " { type nat hook postrouting priority " + SRCNAT_PRIORITY + "; policy accept; }");

    protected static final List<String> NAT_KURA_CHAIN_NAMES = Arrays.asList(PREROUTING_KURA_CHAIN,
            PREROUTING_KURA_PF_CHAIN, POSTROUTING_KURA_CHAIN, POSTROUTING_KURA_PF_CHAIN, POSTROUTING_KURA_IPF_CHAIN);

    protected static final List<String> NAT_JUMP_RULE_FRAGMENTS = Arrays.asList(
            jumpFragment(PREROUTING, PREROUTING_KURA_CHAIN), jumpFragment(PREROUTING, PREROUTING_KURA_PF_CHAIN),
            jumpFragment(POSTROUTING, POSTROUTING_KURA_CHAIN), jumpFragment(POSTROUTING, POSTROUTING_KURA_PF_CHAIN),
            jumpFragment(POSTROUTING, POSTROUTING_KURA_IPF_CHAIN));

    // ---- mangle table ----
    // No pf/ipf sub-chains here, matching the iptables-era mangle layout - it
    // only ever carries caller-supplied additionalMangleRules passthrough, no
    // structured local/port-forward/NAT rule types.

    protected static final List<String> MANGLE_BASE_CHAIN_FRAGMENTS = Arrays.asList(
            INPUT + " { type filter hook input priority " + MANGLE_PRIORITY + "; policy accept; }",
            OUTPUT + " { type filter hook output priority " + MANGLE_PRIORITY + "; policy accept; }",
            FORWARD + " { type filter hook forward priority " + MANGLE_PRIORITY + "; policy accept; }",
            PREROUTING + " { type filter hook prerouting priority " + MANGLE_PRIORITY + "; policy accept; }",
            POSTROUTING + " { type filter hook postrouting priority " + MANGLE_PRIORITY + "; policy accept; }");

    protected static final List<String> MANGLE_KURA_CHAIN_NAMES = Arrays.asList(INPUT_KURA_CHAIN, OUTPUT_KURA_CHAIN,
            FORWARD_KURA_CHAIN, PREROUTING_KURA_CHAIN, POSTROUTING_KURA_CHAIN);

    protected static final List<String> MANGLE_JUMP_RULE_FRAGMENTS = Arrays.asList(
            jumpFragment(INPUT, INPUT_KURA_CHAIN), jumpFragment(OUTPUT, OUTPUT_KURA_CHAIN),
            jumpFragment(FORWARD, FORWARD_KURA_CHAIN), jumpFragment(PREROUTING, PREROUTING_KURA_CHAIN),
            jumpFragment(POSTROUTING, POSTROUTING_KURA_CHAIN));

    // ---- filter-table baseline rules ----

    protected static final String LOOPBACK_ACCEPT_FRAGMENT = fragment(INPUT_KURA_CHAIN, "iifname \"lo\" accept");
    protected static final String ESTABLISHED_RELATED_ACCEPT_FRAGMENT = fragment(INPUT_KURA_CHAIN,
            "ct state established,related accept");

    protected static final List<String> ALLOW_ICMP_V4 = Arrays
            .asList(fragment(INPUT_KURA_CHAIN, "icmp type { echo-request, echo-reply } accept"));

    // Faithful nft port of the iptables-era IptablesConfigIPv6.ALLOW_ICMP_IPV6
    // array - numeric type/code values throughout (verified against that
    // ground-truth list, not nft's symbolic icmpv6 type keywords, which for the
    // less common types were an unverified guess in an earlier version of this
    // list). "icmpv6 type X icmpv6 code Y" mirrors the iptables "--icmpv6-type
    // X/Y" subtype syntax. Types 141/142/148/149 (Inverse Neighbor Discovery,
    // SEND Certification Path) keep the original's "-s fe80::/10" link-local
    // source restriction.
    protected static final List<String> ALLOW_ICMP_V6 = Arrays.asList(
            // Destination Unreachable / Packet Too Big / Time Exceeded / Parameter Problem
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 1 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 2 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 3 icmpv6 code 0 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 3 icmpv6 code 1 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 4 icmpv6 code 0 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 4 icmpv6 code 1 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 4 icmpv6 code 2 accept"),
            // Echo Request / Echo Reply
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 128 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 129 accept"),
            // Mobile IPv6 Home Agent / Mobile Prefix Discovery
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 144 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 145 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 146 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 147 accept"),
            // Multicast Listener Discovery - essential for IPv6 multicast (mDNS, DHCPv6, etc.)
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 130 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 131 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 132 accept"),
            // Critical Neighbor/Router Discovery - no source restriction for IPv6 connectivity
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 133 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 134 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 135 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 136 accept"),
            // Inverse Neighbor Discovery / SEND Certification Path - link-local source only
            fragment(INPUT_KURA_CHAIN, "ip6 saddr fe80::/10 icmpv6 type 141 accept"),
            fragment(INPUT_KURA_CHAIN, "ip6 saddr fe80::/10 icmpv6 type 142 accept"),
            fragment(INPUT_KURA_CHAIN, "ip6 saddr fe80::/10 icmpv6 type 148 accept"),
            fragment(INPUT_KURA_CHAIN, "ip6 saddr fe80::/10 icmpv6 type 149 accept"),
            // Multicast Router Discovery - essential for IPv6 routing protocols
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 151 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 152 accept"),
            fragment(INPUT_KURA_CHAIN, "icmpv6 type 153 accept"),
            // Forwarded traffic: only error messages + mobility, no ND/MLD/link-local subset
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 1 accept"),
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 2 accept"),
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 3 icmpv6 code 0 accept"),
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 3 icmpv6 code 1 accept"),
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 4 icmpv6 code 0 accept"),
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 4 icmpv6 code 1 accept"),
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 4 icmpv6 code 2 accept"),
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 144 accept"),
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 145 accept"),
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 146 accept"),
            fragment(FORWARD_KURA_CHAIN, "icmpv6 type 147 accept"));

    protected static final List<String> DO_NOT_ALLOW_ICMP = Arrays.asList();

    protected static String jumpFragment(String fromChain, String toChain) {
        return fragment(fromChain, "jump " + toChain);
    }

    /**
     * Builds a bare "&lt;chain&gt; &lt;statement&gt;" fragment - deliberately
     * without the "add rule &lt;family&gt; &lt;table&gt;" prefix, since only a
     * family-aware {@link NftablesConfig} instance knows whether it is
     * assembling the "ip" or "ip6" ruleset, and which of the three tables
     * (filter/nat/mangle) a given fragment belongs to.
     */
    protected static String fragment(String chain, String statement) {
        return chain + " " + statement;
    }

    protected NftablesConfigConstants() {
        // Empty constructor
    }
}
