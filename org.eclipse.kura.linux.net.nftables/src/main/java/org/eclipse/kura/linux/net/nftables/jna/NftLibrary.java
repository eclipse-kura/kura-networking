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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA binding for the high-level libnftables "nft_ctx" command API (the same
 * API the {@code nft} CLI binary itself uses to execute nftables-language
 * command buffers). {@code nft_ctx} is an opaque handle on the native side,
 * represented here as a plain {@link Pointer}.
 */
public interface NftLibrary extends Library {

    NftLibrary INSTANCE = Native.load("nftables", NftLibrary.class);

    int NFT_CTX_DEFAULT = 0;

    int NFT_CTX_INPUT_NO_DNS = 1;

    // Bit position not verified against a real /usr/include/nftables/libnftables.h; only used
    // by the non-critical ruleset dump/diagnostics path, never by rule application.
    int NFT_CTX_OUTPUT_JSON = 1 << 5;

    Pointer nft_ctx_new(int flags);

    void nft_ctx_free(Pointer ctx);

    void nft_ctx_output_set_flags(Pointer ctx, int flags);

    int nft_ctx_output_get_flags(Pointer ctx);

    void nft_ctx_input_set_flags(Pointer ctx, int flags);

    int nft_ctx_buffer_output(Pointer ctx);

    void nft_ctx_unbuffer_output(Pointer ctx);

    String nft_ctx_get_output_buffer(Pointer ctx);

    int nft_ctx_buffer_error(Pointer ctx);

    void nft_ctx_unbuffer_error(Pointer ctx);

    String nft_ctx_get_error_buffer(Pointer ctx);

    int nft_run_cmd_from_buffer(Pointer ctx, String buf);

    void nft_ctx_set_dry_run(Pointer ctx, boolean dry);
}
