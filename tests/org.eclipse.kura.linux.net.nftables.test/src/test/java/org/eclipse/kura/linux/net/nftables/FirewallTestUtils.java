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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Shared test fixtures: a {@link RecordingNftExecutor} standing in for a real
 * libnftables binding, and a scratch-file-backed {@link NftPersistenceStore}
 * so tests never touch the real {@code /etc/sysconfig} paths.
 */
public abstract class FirewallTestUtils {

    protected RecordingNftExecutor executor;

    protected void setUpMock() {
        this.executor = new RecordingNftExecutor();
    }

    protected NftPersistenceStore newTempPersistenceStore() throws IOException {
        File cfg = File.createTempFile("nftables_kura-test", ".conf");
        File tmp = File.createTempFile("nftables_kura-test-tmp", ".conf");
        cfg.deleteOnExit();
        tmp.deleteOnExit();
        Files.deleteIfExists(cfg.toPath());
        return new NftPersistenceStore(cfg.getAbsolutePath(), tmp.getAbsolutePath());
    }
}
