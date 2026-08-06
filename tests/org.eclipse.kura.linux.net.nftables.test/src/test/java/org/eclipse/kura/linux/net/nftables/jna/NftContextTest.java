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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class NftContextTest {

    @Test
    public void constructorEnablesOutputAndErrorBufferingAndNoDnsTest() {
        FakeNftLibrary fake = new FakeNftLibrary();

        try (NftContext ctx = new NftContext(fake)) {
            assertTrue(fake.outputBufferingEnabled);
            assertTrue(fake.errorBufferingEnabled);
            assertEquals(NftLibrary.NFT_CTX_INPUT_NO_DNS, fake.inputFlags);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void constructorThrowsWhenNativeContextIsNullTest() {
        FakeNftLibrary fake = new FakeNftLibrary();
        fake.returnNullCtx = true;

        new NftContext(fake);
    }

    @Test
    public void runReturnsSuccessResultTest() {
        FakeNftLibrary fake = new FakeNftLibrary();
        fake.nextReturnCode = 0;
        fake.nextOutputBuffer = "ok";

        try (NftContext ctx = new NftContext(fake)) {
            NftCommandResult result = ctx.run("add table inet kura");

            assertTrue(result.isSuccess());
            assertEquals(0, result.getReturnCode());
            assertEquals("ok", result.getOutputBuffer());
            assertEquals("add table inet kura", fake.lastCommandBuffer);
        }
    }

    @Test
    public void runReturnsFailureResultTest() {
        FakeNftLibrary fake = new FakeNftLibrary();
        fake.nextReturnCode = 1;
        fake.nextErrorBuffer = "syntax error";

        try (NftContext ctx = new NftContext(fake)) {
            NftCommandResult result = ctx.run("bogus");

            assertFalse(result.isSuccess());
            assertEquals("syntax error", result.getErrorBuffer());
        }
    }

    @Test
    public void dumpRulesetRestoresPreviousOutputFlagsTest() {
        FakeNftLibrary fake = new FakeNftLibrary();

        try (NftContext ctx = new NftContext(fake)) {
            fake.outputFlags = 0;
            ctx.dumpRuleset(true);

            assertEquals(NftLibrary.NFT_CTX_OUTPUT_JSON,
                    fake.outputFlagsDuringLastRun & NftLibrary.NFT_CTX_OUTPUT_JSON);
            assertEquals(0, fake.outputFlags);
        }
    }

    @Test
    public void closeFreesContextAndBlocksFurtherUseTest() {
        FakeNftLibrary fake = new FakeNftLibrary();
        NftContext ctx = new NftContext(fake);

        ctx.close();
        assertTrue(fake.freed);

        try {
            ctx.run("add table inet kura");
            fail("expected IllegalStateException after close()");
        } catch (IllegalStateException expected) {
            // expected
        }

        // closing twice must not double-free
        fake.freed = false;
        ctx.close();
        assertFalse(fake.freed);
    }
}
