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

/**
 * Small shared helpers for building nft-language statement fragments, used by
 * every rule class in this package.
 */
final class NftSyntaxUtil {

    private NftSyntaxUtil() {
        // Empty constructor
    }

    static boolean isIPv6(String addressOrCidr) {
        return addressOrCidr != null && addressOrCidr.contains(":");
    }

    static String family(String addressOrCidr) {
        return isIPv6(addressOrCidr) ? "ip6" : "ip";
    }

    /**
     * Converts a Kura-style "start:end" port range into nft's "start-end" syntax.
     */
    static String toNftPortRange(String range) {
        return range == null ? null : range.replace(':', '-');
    }

    /**
     * Wraps an IPv6 literal in brackets for "address:port" style syntax (e.g.
     * nft's "dnat to [addr]:port"), leaving IPv4 literals untouched.
     */
    static String bracketIfIPv6(String address) {
        return isIPv6(address) ? "[" + address + "]" : address;
    }
}
