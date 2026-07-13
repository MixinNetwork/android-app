package one.mixin.android.ui.wallet

import one.mixin.android.vo.WalletCategory
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
    fun `pending mnemonic login imports an imported mnemonic wallet`() {
        assertEquals(
            WalletCategory.IMPORTED_MNEMONIC.value,
            importWalletCategoryForMode(WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC),
        )
    }

    @Test
    fun `pending mnemonic registration imports an imported mnemonic wallet`() {
        assertEquals(
            WalletCategory.IMPORTED_MNEMONIC.value,
            importWalletCategoryForMode(WalletSecurityActivity.Mode.REGISTER_IMPORT_MNEMONIC),
        )
    }

    @Test
    fun `manual mnemonic import remains imported mnemonic`() {
        assertEquals(
            WalletCategory.IMPORTED_MNEMONIC.value,
            importWalletCategoryForMode(WalletSecurityActivity.Mode.IMPORT_MNEMONIC),
        )
    }

    @Test
    fun `pending mnemonic import blocks back only during import steps`() {
        assertEquals(true, shouldBlockWalletSecurityBack(WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC, isImportStep = true))
        assertEquals(true, shouldBlockWalletSecurityBack(WalletSecurityActivity.Mode.REGISTER_IMPORT_MNEMONIC, isImportStep = true))
        assertEquals(false, shouldBlockWalletSecurityBack(WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC, isImportStep = false))
        assertEquals(false, shouldBlockWalletSecurityBack(WalletSecurityActivity.Mode.IMPORT_MNEMONIC, isImportStep = true))
    }
}
