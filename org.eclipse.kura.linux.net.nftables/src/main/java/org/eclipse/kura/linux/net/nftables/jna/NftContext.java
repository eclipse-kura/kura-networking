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

import com.sun.jna.Pointer;

/**
 * Wraps a single native {@code nft_ctx} handle. Output and error buffering
 * are always enabled, since libnftables only makes command error text
 * available via {@code nft_ctx_get_error_buffer()} when
 * {@code nft_ctx_buffer_error()} was called first - otherwise it goes
 * straight to the process's real stderr and is lost to the caller.
 */
public final class NftContext implements NftExecutor, AutoCloseable {

    private final NftLibrary lib;
    private final Pointer ctxPtr;
    private volatile boolean closed = false;

    public NftContext() {
        this(NftLibrary.INSTANCE);
    }

    NftContext(NftLibrary lib) {
        this.lib = lib;
        this.ctxPtr = lib.nft_ctx_new(NftLibrary.NFT_CTX_DEFAULT);
        if (this.ctxPtr == null) {
            throw new IllegalStateException("nft_ctx_new returned NULL");
        }
        lib.nft_ctx_buffer_error(this.ctxPtr);
        lib.nft_ctx_buffer_output(this.ctxPtr);
        lib.nft_ctx_input_set_flags(this.ctxPtr, NftLibrary.NFT_CTX_INPUT_NO_DNS);
    }

    @Override
    public synchronized NftCommandResult run(String nftStatements) {
        checkOpen();
        int rc = this.lib.nft_run_cmd_from_buffer(this.ctxPtr, nftStatements);
        return new NftCommandResult(rc == 0, rc, this.lib.nft_ctx_get_output_buffer(this.ctxPtr),
                this.lib.nft_ctx_get_error_buffer(this.ctxPtr));
    }

    @Override
    public synchronized NftCommandResult dumpRuleset(boolean json) {
        checkOpen();
        int flags = this.lib.nft_ctx_output_get_flags(this.ctxPtr);
        if (json) {
            this.lib.nft_ctx_output_set_flags(this.ctxPtr, flags | NftLibrary.NFT_CTX_OUTPUT_JSON);
        }
        try {
            return run("list ruleset");
        } finally {
            this.lib.nft_ctx_output_set_flags(this.ctxPtr, flags);
        }
    }

    public synchronized void setDryRun(boolean dryRun) {
        checkOpen();
        this.lib.nft_ctx_set_dry_run(this.ctxPtr, dryRun);
    }

    private void checkOpen() {
        if (this.closed) {
            throw new IllegalStateException("NftContext already closed");
        }
    }

    @Override
    public synchronized void close() {
        if (!this.closed) {
            this.lib.nft_ctx_free(this.ctxPtr);
            this.closed = true;
        }
    }
}
