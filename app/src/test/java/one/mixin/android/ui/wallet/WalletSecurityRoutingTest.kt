package one.mixin.android.ui.wallet

import one.mixin.android.vo.WalletCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletSecurityRoutingTest {
    @Test
    fun `login import mnemonic verifies pin before fetch`() {
        assertEquals(
            WalletSecurityStartRoute.VerifyPin,
            walletSecurityStartRoute(WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC),
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
    fun `pending mnemonic login imports a classic wallet`() {
        assertEquals(
            WalletCategory.CLASSIC.value,
            importWalletCategoryForMode(WalletSecurityActivity.Mode.LOGIN_IMPORT_MNEMONIC),
        )
    }

    @Test
    fun `pending mnemonic registration imports a classic wallet`() {
        assertEquals(
            WalletCategory.CLASSIC.value,
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
}
