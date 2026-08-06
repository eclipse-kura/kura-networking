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
 * Pure-Java {@link NftLibrary} test double - no native code, no
 * {@code Native.load}, so tests using this never need a real libnftables
 * installed.
 */
class FakeNftLibrary implements NftLibrary {

    static final Pointer DUMMY_CTX = new Pointer(1);

    boolean returnNullCtx = false;
    boolean errorBufferingEnabled = false;
    boolean outputBufferingEnabled = false;
    int inputFlags;
    int outputFlags;
    boolean dryRun;
    boolean freed = false;

    int nextReturnCode = 0;
    String nextOutputBuffer = "";
    String nextErrorBuffer = "";
    String lastCommandBuffer;
    int outputFlagsDuringLastRun;

    @Override
    public Pointer nft_ctx_new(int flags) {
        return this.returnNullCtx ? null : DUMMY_CTX;
    }

    @Override
    public void nft_ctx_free(Pointer ctx) {
        this.freed = true;
    }

    @Override
    public void nft_ctx_output_set_flags(Pointer ctx, int flags) {
        this.outputFlags = flags;
    }

    @Override
    public int nft_ctx_output_get_flags(Pointer ctx) {
        return this.outputFlags;
    }

    @Override
    public void nft_ctx_input_set_flags(Pointer ctx, int flags) {
        this.inputFlags = flags;
    }

    @Override
    public int nft_ctx_buffer_output(Pointer ctx) {
        this.outputBufferingEnabled = true;
        return 0;
    }

    @Override
    public void nft_ctx_unbuffer_output(Pointer ctx) {
        this.outputBufferingEnabled = false;
    }

    @Override
    public String nft_ctx_get_output_buffer(Pointer ctx) {
        return this.nextOutputBuffer;
    }

    @Override
    public int nft_ctx_buffer_error(Pointer ctx) {
        this.errorBufferingEnabled = true;
        return 0;
    }

    @Override
    public void nft_ctx_unbuffer_error(Pointer ctx) {
        this.errorBufferingEnabled = false;
    }

    @Override
    public String nft_ctx_get_error_buffer(Pointer ctx) {
        return this.nextErrorBuffer;
    }

    @Override
    public int nft_run_cmd_from_buffer(Pointer ctx, String buf) {
        this.lastCommandBuffer = buf;
        this.outputFlagsDuringLastRun = this.outputFlags;
        return this.nextReturnCode;
    }

    @Override
    public void nft_ctx_set_dry_run(Pointer ctx, boolean dry) {
        this.dryRun = dry;
    }
}
