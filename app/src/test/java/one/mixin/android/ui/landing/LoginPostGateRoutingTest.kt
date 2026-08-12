package one.mixin.android.ui.landing

import kotlin.test.Test
import kotlin.test.assertEquals

class LoginPostGateRoutingTest {
    @Test
    fun `continues normal startup without pending mnemonic`() {
        assertEquals(
            PendingMnemonicStartupRoute.Continue,
            routePendingMnemonicStartup(hasPendingImport = false, hasSafe = false),
        )
        assertEquals(
            PendingMnemonicStartupRoute.Continue,
            routePendingMnemonicStartup(hasPendingImport = false, hasSafe = true),
        )
    }

    @Test
    fun `resumes account setup when pending mnemonic has no Safe`() {
        assertEquals(
            PendingMnemonicStartupRoute.ResumeAccountSetup,
            routePendingMnemonicStartup(hasPendingImport = true, hasSafe = false),
        )
    }

    @Test
    fun `continues pending mnemonic in MainActivity after Safe is ready`() {
        assertEquals(
            PendingMnemonicStartupRoute.Continue,
            routePendingMnemonicStartup(hasPendingImport = true, hasSafe = true),
        )
    }
}
