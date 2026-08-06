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
import java.util.List;

import org.eclipse.kura.linux.net.nftables.jna.NftCommandResult;
import org.eclipse.kura.linux.net.nftables.jna.NftExecutor;

/**
 * Records every script submitted via {@link #run(String)}, so tests can
 * assert on the generated nft statements without a real libnftables.
 */
class RecordingNftExecutor implements NftExecutor {

    final List<String> submittedScripts = new ArrayList<>();
    boolean nextResultSuccess = true;
    String nextErrorBuffer = "";

    @Override
    public NftCommandResult run(String nftStatements) {
        this.submittedScripts.add(nftStatements);
        return new NftCommandResult(this.nextResultSuccess, this.nextResultSuccess ? 0 : 1, "", this.nextErrorBuffer);
    }

    @Override
    public NftCommandResult dumpRuleset(boolean json) {
        return new NftCommandResult(true, 0, "", "");
    }

    String lastScript() {
        return this.submittedScripts.get(this.submittedScripts.size() - 1);
    }
}
