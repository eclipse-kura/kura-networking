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

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

/**
 * Outcome of a single {@link NftExecutor} invocation.
 */
public class NftCommandResult {

    private final boolean success;
    private final int returnCode;
    private final String outputBuffer;
    private final String errorBuffer;

    public NftCommandResult(boolean success, int returnCode, String outputBuffer, String errorBuffer) {
        this.success = success;
        this.returnCode = returnCode;
        this.outputBuffer = outputBuffer == null ? "" : outputBuffer;
        this.errorBuffer = errorBuffer == null ? "" : errorBuffer;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public int getReturnCode() {
        return this.returnCode;
    }

    public String getOutputBuffer() {
        return this.outputBuffer;
    }

    public String getErrorBuffer() {
        return this.errorBuffer;
    }

    public void throwIfError() throws KuraException {
        if (!this.success) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR,
                    "nft command failed (rc=" + this.returnCode + "): " + this.errorBuffer);
        }
    }
}
