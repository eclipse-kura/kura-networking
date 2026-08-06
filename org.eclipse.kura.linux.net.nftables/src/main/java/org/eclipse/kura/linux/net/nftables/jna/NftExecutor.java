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
package org.eclipse.kura.linux.net.nftables.jna;

/**
 * Seam between the nftables rule engine and the native libnftables binding.
 * Kept free of any JNA type so callers (and their unit tests) never need a
 * real libnftables installed.
 */
public interface NftExecutor {

    /**
     * Submits an nft-language statements buffer (one or more statements,
     * newline separated) as a single transaction.
     */
    NftCommandResult run(String nftStatements);

    /**
     * Dumps the live ruleset owned by this context, for diagnostics only -
     * never used as a restore mechanism.
     */
    NftCommandResult dumpRuleset(boolean json);
}
