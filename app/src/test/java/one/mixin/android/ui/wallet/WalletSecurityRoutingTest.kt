package one.mixin.android.ui.wallet

import kotlin.test.Test
import kotlin.test.assertEquals

class WalletSecurityRoutingTest {
    @Test
    fun `login import mnemonic verifies pin before fetch when pin is missing`() {
        assertEquals(
            WalletSecurityStartRoute.VerifyPin,
            walletSecurityStartRoute(WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC),
        )
    }

    @Test
    fun `login import mnemonic fetches without another pin prompt after pin verification`() {
        assertEquals(
            WalletSecurityStartRoute.FetchPendingMnemonic,
            walletSecurityStartRoute(WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC, hasVerifiedPin = true),
        )
    }

    @Test
    fun `registration import mnemonic fetches without another pin prompt`() {
        assertEquals(
            WalletSecurityStartRoute.FetchPendingMnemonic,
            walletSecurityStartRoute(WalletSecurityActivity.Mode.REGISTER_IMPORT_MNEMONIC),
        )
    }

    @Test
    fun `pending mnemonic import blocks back only during import steps`() {
        assertEquals(true, shouldBlockWalletSecurityBack(WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC, isImportStep = true))
        assertEquals(true, shouldBlockWalletSecurityBack(WalletSecurityActivity.Mode.REGISTER_IMPORT_MNEMONIC, isImportStep = true))
        assertEquals(false, shouldBlockWalletSecurityBack(WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC, isImportStep = false))
        assertEquals(false, shouldBlockWalletSecurityBack(WalletSecurityActivity.Mode.IMPORT_MNEMONIC, isImportStep = true))
    }

    @Test
    fun `pending mnemonic import hides the pin page close button`() {
        assertEquals(true, shouldHideWalletSecurityClose(WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC))
        assertEquals(true, shouldHideWalletSecurityClose(WalletSecurityActivity.Mode.REGISTER_IMPORT_MNEMONIC))
        assertEquals(false, shouldHideWalletSecurityClose(WalletSecurityActivity.Mode.IMPORT_MNEMONIC))
    }
}
