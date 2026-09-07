#!/usr/sbin/nft -f
#
# Kura firewall ruleset - translated from the live iptables-save / ip6tables-save
# dump captured 2026-08-05 on reliagate-15a-14.
#
# DESIGN DECISIONS (read before editing):
#
# 1. Separate `ip kura` / `ip6 kura` tables, not a unified `inet` table.
#    Chosen for lowest translation risk on a first cutover: family-specific
#    content here (ICMPv6 allow-list, IPv6 extension-header checks, differing
#    connlimit masks) is extensive enough that keeping the two stacks visually
#    separate, mirroring the current iptables/ip6tables split, is easier to
#    audit line-by-line against the source dump. Unifying into `inet kura`
#    is a reasonable follow-up once this is validated in production.
#
# 2. Table is named `kura`, deliberately NOT `filter`/`nat`. Docker, running
#    on this host via its default iptables/iptables-nft backend, owns real
#    tables literally named `ip filter` and `ip nat` (containing DOCKER,
#    DOCKER-USER, DOCKER-ISOLATION-STAGE-1/2). This ruleset does not touch,
#    recreate, or assume anything about those tables - Docker keeps managing
#    them exactly as it does today, independently.
#
# 3. Empty/no-op chains from the source dump are pruned. In the original
#    dump, mangle's input-kura/output-kura/forward-kura/postrouting-kura,
#    nat's input-kura/output-kura, and filter's output-kura all contain
#    nothing but a jump and a RETURN - zero behavioural effect. Omitted here
#    rather than translated literally. This is also why chains that shared
#    a name across mangle/nat/filter in the original (e.g. "prerouting-kura")
#    don't collide below - only one real chain per name survives pruning,
#    except prerouting-kura itself, which has real content in BOTH mangle
#    and nat and is explicitly split into prerouting-kura-mangle /
#    prerouting-kura-nat.
#
# 4. FORWARD policy on the IPv4 side is `accept`, not `drop` - the single
#    most important deviation from a literal transliteration, and it is
#    load-bearing for Docker coexistence, not an oversight. In the current
#    single-table iptables model, forward-kura/-pf/-ipf NEVER themselves
#    issue a DROP - they only ACCEPT specific known traffic and otherwise
#    fall through via RETURN. The actual default-deny for FORWARD is
#    enforced later in that SAME iptables FORWARD chain by Docker's own
#    rules and, ultimately, by the chain's own policy DROP - reached only
#    after Docker's DOCKER-USER/DOCKER-ISOLATION chains have also had a
#    chance to run. Once Kura's forward logic lives in its own nftables
#    base chain, hooked independently at the same `forward` hook as
#    Docker's still-separate `ip filter` FORWARD chain, giving Kura's chain
#    `policy drop` would risk it terminating packets before Docker's own
#    chain is ever evaluated, silently breaking container networking
#    depending on hook-priority tie-breaking. Kura's chain stays
#    accept-only/fall-through, exactly matching current behaviour, and lets
#    Docker's own untouched FORWARD chain keep providing the real
#    default-deny, exactly as today.
#    IPv6 FORWARD keeps `policy drop`, matching the original exactly - the
#    ip6tables-save dump shows no Docker involvement in IPv6 on this host
#    (Docker's IPv6 support is off by default), so there is no equivalent
#    coexistence risk there. Revisit if IPv6 is enabled for Docker networks.
#
# 5. Kura's own `forward` base chain is hooked at `priority filter - 5`
#    rather than the default `filter` (0) Docker's iptables-nft FORWARD
#    chain uses. Not required for correctness (#4 - Kura's chain never
#    issues a terminal verdict that could pre-empt Docker's), but it makes
#    evaluation order explicit rather than leaving two independently
#    registered tables at an identical priority to an unspecified tie-break.
#
# 6. Two duplicate rules present verbatim in BOTH source dumps
#    (`--tcp-flags FIN,ACK FIN` appears twice in each prerouting-kura mangle
#    chain) are de-duplicated here - the second occurrence is a no-op since
#    the first already drops any matching packet.
#
# 7. FLAGGED FOR YOUR REVIEW - forward-kura-ipf (IPv4) allows *unrestricted*
#    new TCP connections FROM eth1 TO eth0 (no destination restriction, no
#    ct state requirement), while the reverse direction is restricted to
#    established/related only. Combined with the DNAT/masquerade rules
#    elsewhere, eth1 is this device's externally-facing interface and eth0
#    faces the internal 172.16.0.110 host. Taken literally, this currently
#    permits ANY host reachable via eth1 to open ANY new TCP connection to
#    ANY host reachable via eth0, unrestricted by destination or port.
#    Translated here EXACTLY as found (no behavioural change) - but it's
#    unusually broad for what "automatic NAT for IP forwarding" is normally
#    used for (typically LAN-initiates-outbound), and worth confirming this
#    matches your intended topology rather than a pre-existing
#    misconfiguration this migration would otherwise just carry forward.
#
# 8. AH/ESP/"no next header" matching (IPv6) uses `ip6 nexthdr` rather than
#    `exthdr`, because nftables' exthdr existence keywords only cover
#    {hbh, frag, rt, dst, mh} - AH (51) and ESP (50) aren't extension
#    headers in that sense. Caveat: `ip6 nexthdr ah` only matches if AH is
#    the IMMEDIATE next header after the fixed IPv6 header - unlike
#    iptables' `ipv6header --soft`, it does not recursively check further
#    down an extension-header chain. Narrower than the original if AH/ESP
#    could ever appear behind another extension header on this network -
#    rare in practice, but not presented here as a perfect equivalent.
#
# 9. The `-f` (drop non-first IPv4 fragments) rule is translated literally,
#    but is very likely already a no-op in the CURRENT iptables setup, for
#    the same reason it stays a no-op here: connection tracking reassembles
#    fragments before packets reach the mangle PREROUTING hook in the
#    normal case. No behavioural change either way - just not load-bearing.
#
# VALIDATION STATUS: passed `nft -c -f` AND a real `nft -f` load against
# nftables 1.0.9 in a test environment (target device runs v1.0.2, kernel
# 5.15.71 aarch64 - a materially older version; syntax here predates 1.0.2
# but a final `nft -c -f` ON THE ACTUAL DEVICE before deploying is still
# strongly recommended given the version gap). The live load surfaced one
# real fix (explicit `meta l4proto tcp` on both connlimit rules, applied
# below) that the syntax-only check had not flagged - I did not get to
# finish the equivalent live-load review of the IPv6 table before my test
# environment became unresponsive; treat the IPv6 table as -c-verified but
# not yet live-load-verified, and re-check it once you have a moment.

define TCP_ALL_FLAGS = fin | syn | rst | psh | ack | urg

##############################################################################
# IPv4
##############################################################################

table ip kura {

	set connlimit_v4 {
		type ipv4_addr
		size 65535
		flags dynamic
	}

	chain mangle_prerouting {
		type filter hook prerouting priority mangle; policy accept;
		jump prerouting-kura-mangle
	}

	chain prerouting-kura-mangle {
		ct state invalid drop
		tcp flags != syn / fin,syn,rst,ack ct state new drop
		ip protocol tcp ct state new tcp option maxseg size != 536-65535 drop
		tcp flags fin,syn / fin,syn drop
		tcp flags syn,rst / syn,rst drop
		tcp flags fin,rst / fin,rst drop
		tcp flags fin / fin,ack drop
		tcp flags urg / ack,urg drop
		tcp flags fin / fin,ack drop
		tcp flags psh / psh,ack drop
		tcp flags fin,syn,rst,psh,ack,urg / fin,syn,rst,psh,ack,urg drop
		tcp flags 0x0 / fin,syn,rst,psh,ack,urg drop
		tcp flags fin,psh,urg / fin,syn,rst,psh,ack,urg drop
		tcp flags fin,syn,psh,urg / fin,syn,rst,psh,ack,urg drop
		tcp flags fin,syn,rst,ack,urg / fin,syn,rst,psh,ack,urg drop
		icmp type echo-request ct state new,related,established drop
		icmp type echo-reply ct state new,related,established limit rate 60/second burst 20 packets accept
		icmp type echo-reply drop
		ip frag-off & 0x1fff != 0 drop
		tcp flags & rst == rst limit rate 2/second burst 2 packets accept
		tcp flags & rst == rst drop
		ip protocol tcp ct state new limit rate 60/second burst 20 packets accept
		ip protocol tcp ct state new drop
	}

	chain nat_prerouting {
		type nat hook prerouting priority dstnat; policy accept;
		jump prerouting-kura-nat
	}

	chain prerouting-kura-nat {
		jump prerouting-kura-pf
	}

	chain prerouting-kura-pf {
		iifname "eth1" tcp dport 3040 dnat to 172.16.0.110:4050
	}

	chain nat_postrouting {
		type nat hook postrouting priority srcnat; policy accept;
		jump postrouting-kura
	}

	chain postrouting-kura {
		jump postrouting-kura-ipf
		jump postrouting-kura-pf
	}

	# "ipf" = automatic NAT for IP forwarding.
	chain postrouting-kura-ipf {
		oifname "eth0" ip protocol tcp masquerade
	}

	# "pf" = masquerade tied to the specific port-forward above, scoped to
	# the internal host so it replies to this gateway rather than trying to
	# route directly back to the original external client.
	chain postrouting-kura-pf {
		ip daddr 172.16.0.110 ct status dnat oifname "eth0" ip protocol tcp masquerade
	}

	chain filter_input {
		type filter hook input priority filter; policy drop;
		jump input-kura
	}

	chain input-kura {
		# Per-source concurrent-connection cap (iptables --connlimit-above 111
		# --connlimit-mask 32). meta l4proto tcp made explicit - see the fix
		# note above.
		meta l4proto tcp ct state new add @connlimit_v4 { ip saddr ct count over 111 } reject with tcp reset

		iifname "lo" accept
		ct state established,related accept

		iifname "eth0" tcp dport 22 accept
		ip saddr 10.234.0.0/16 iifname "tun0" tcp dport 22 accept
		iifname "eth1" tcp dport 22 accept

		tcp dport 34 accept

		iifname "eth0" udp dport 53 accept
		iifname "eth0" udp dport 67 accept

		iifname "eth0" tcp dport 443 accept
		ip saddr 10.234.0.0/16 iifname "tun0" tcp dport 443 accept
		iifname "eth1" tcp dport 443 accept

		tcp dport 1234 accept

		ip saddr 127.0.0.1 iifname "lo" tcp dport 2947 accept

		iifname "eth0" tcp dport 4443 accept
		ip saddr 10.234.0.0/16 iifname "tun0" tcp dport 4443 accept

		tcp dport 5400 accept
	}

	# See design notes #4/#5 - policy accept is deliberate, load-bearing for
	# Docker coexistence, not an oversight.
	chain filter_forward {
		type filter hook forward priority filter - 5; policy accept;
		jump forward-kura
	}

	chain filter_forward_backstop {
		type filter hook forward priority filter + 5; policy drop;
	}

	chain forward-kura {
		jump forward-kura-ipf
		jump forward-kura-pf
	}

	# See design note #7 - FLAGGED for your review.
	chain forward-kura-ipf {
		iifname "eth1" oifname "eth0" meta l4proto tcp accept
		iifname "eth0" oifname "eth1" meta l4proto tcp ct state established,related accept
	}

	chain forward-kura-pf {
		ip daddr 172.16.0.110 iifname "eth1" oifname "eth0" meta l4proto tcp accept
		ip saddr 172.16.0.110 iifname "eth0" oifname "eth1" meta l4proto tcp ct state established,related accept
	}
}

##############################################################################
# IPv6
##############################################################################

table ip6 kura {

	set connlimit_v6 {
		type ipv6_addr
		size 65535
		flags dynamic
	}

	chain mangle_prerouting {
		type filter hook prerouting priority mangle; policy accept;
		jump prerouting-kura-mangle
	}

	chain prerouting-kura-mangle {
		ct state invalid counter drop
		tcp flags != syn / fin,syn,rst,ack ct state new counter drop
		meta l4proto tcp ct state new tcp option maxseg size != 536-65535 counter drop
		tcp flags fin,syn / fin,syn counter drop
		tcp flags syn,rst / syn,rst counter drop
		tcp flags fin,rst / fin,rst counter drop
		tcp flags fin / fin,ack counter drop
		tcp flags urg / ack,urg counter drop
		tcp flags fin / fin,ack counter drop
		tcp flags psh / psh,ack counter drop
		tcp flags fin,syn,rst,psh,ack,urg / fin,syn,rst,psh,ack,urg counter drop
		tcp flags 0x0 / fin,syn,rst,psh,ack,urg counter drop
		tcp flags fin,psh,urg / fin,syn,rst,psh,ack,urg counter drop
		tcp flags fin,syn,psh,urg / fin,syn,rst,psh,ack,urg counter drop
		tcp flags fin,syn,rst,ack,urg / fin,syn,rst,psh,ack,urg counter drop
		meta l4proto ipv6-icmp icmpv6 type echo-request counter drop

		# See design note #8 re: exthdr coverage and the ip6 nexthdr AH/ESP caveat.
		exthdr dst exists drop
		exthdr hbh exists drop
		exthdr rt exists drop
		exthdr frag exists drop
		ip6 nexthdr 51 drop
		ip6 nexthdr 50 drop
		ip6 nexthdr 59 drop
		#
		# -t mangle -A prerouting-kura -m ipv6header --header ipv6-opts --soft -j DROP
		# -t mangle -A prerouting-kura -m ipv6header --header hop-by-hop --soft -j DROP
		# -t mangle -A prerouting-kura -m ipv6header --header ipv6-route --soft -j DROP
		# -t mangle -A prerouting-kura -m ipv6header --header ipv6-frag --soft -j DROP
		# -t mangle -A prerouting-kura -m ipv6header --header ah --soft -j DROP
		# -t mangle -A prerouting-kura -m ipv6header --header esp --soft -j DROP
		# -t mangle -A prerouting-kura -m ipv6header --header ipv6-nonxt --soft -j DROP
		tcp flags & rst == rst limit rate 2/second burst 2 packets counter accept
		tcp flags & rst == rst counter drop
		meta l4proto tcp ct state new limit rate 1/second burst 15 packets counter accept
		meta l4proto tcp ct state new counter drop
		meta l4proto ipv6-icmp limit rate 60/second burst 20 packets counter accept
		meta l4proto ipv6-icmp counter drop
	}

	chain filter_input {
		type filter hook input priority filter; policy drop;
		jump input-kura
	}

	chain input-kura {
		meta l4proto tcp ct state new add @connlimit_v6 { ip6 saddr ct count over 111 } counter reject with tcp reset
		iifname "lo" counter accept
		ct state related,established counter accept
		tcp dport 5401 counter accept
		tcp dport 22 counter accept
		meta l4proto ipv6-icmp icmpv6 type destination-unreachable counter accept
		meta l4proto ipv6-icmp icmpv6 type packet-too-big counter accept
		meta l4proto ipv6-icmp icmpv6 type time-exceeded icmpv6 code 0 counter accept
		meta l4proto ipv6-icmp icmpv6 type time-exceeded icmpv6 code 1 counter accept
		meta l4proto ipv6-icmp icmpv6 type parameter-problem icmpv6 code 0 counter accept
		meta l4proto ipv6-icmp icmpv6 type parameter-problem icmpv6 code 1 counter accept
		meta l4proto ipv6-icmp icmpv6 type parameter-problem icmpv6 code 2 counter accept
		meta l4proto ipv6-icmp icmpv6 type echo-request counter accept
		meta l4proto ipv6-icmp icmpv6 type echo-reply counter accept
		meta l4proto ipv6-icmp icmpv6 type 144 counter accept
		meta l4proto ipv6-icmp icmpv6 type 145 counter accept
		meta l4proto ipv6-icmp icmpv6 type 146 counter accept
		meta l4proto ipv6-icmp icmpv6 type 147 counter accept
		meta l4proto ipv6-icmp icmpv6 type mld-listener-query counter accept
		meta l4proto ipv6-icmp icmpv6 type mld-listener-report counter accept
		meta l4proto ipv6-icmp icmpv6 type mld-listener-done counter accept
		meta l4proto ipv6-icmp icmpv6 type nd-router-solicit counter accept
		meta l4proto ipv6-icmp icmpv6 type nd-router-advert counter accept
		meta l4proto ipv6-icmp icmpv6 type nd-neighbor-solicit counter accept
		meta l4proto ipv6-icmp icmpv6 type nd-neighbor-advert counter accept
		ip6 saddr fe80::/10 icmpv6 type ind-neighbor-solicit counter accept
		ip6 saddr fe80::/10 icmpv6 type ind-neighbor-advert counter accept
		ip6 saddr fe80::/10 icmpv6 type 148 counter accept
		ip6 saddr fe80::/10 icmpv6 type 149 counter accept
		meta l4proto ipv6-icmp icmpv6 type 151 counter accept
		meta l4proto ipv6-icmp icmpv6 type 152 counter accept
		meta l4proto ipv6-icmp icmpv6 type 153 counter accept
	}

	chain filter_forward {
		type filter hook forward priority filter; policy drop;
		jump forward-kura
	}

	chain forward-kura {
		meta l4proto ipv6-icmp icmpv6 type destination-unreachable counter accept
		meta l4proto ipv6-icmp icmpv6 type packet-too-big counter accept
		meta l4proto ipv6-icmp icmpv6 type time-exceeded icmpv6 code 0 counter accept
		meta l4proto ipv6-icmp icmpv6 type time-exceeded icmpv6 code 1 counter accept
		meta l4proto ipv6-icmp icmpv6 type parameter-problem icmpv6 code 0 counter accept
		meta l4proto ipv6-icmp icmpv6 type parameter-problem icmpv6 code 1 counter accept
		meta l4proto ipv6-icmp icmpv6 type parameter-problem icmpv6 code 2 counter accept
		meta l4proto ipv6-icmp icmpv6 type 144 counter accept
		meta l4proto ipv6-icmp icmpv6 type 145 counter accept
		meta l4proto ipv6-icmp icmpv6 type 146 counter accept
		meta l4proto ipv6-icmp icmpv6 type 147 counter accept
	}

}