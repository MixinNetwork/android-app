package one.mixin.android.ui.wallet

import one.mixin.android.ui.wallet.home.WalletHomeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WalletHomePreferencesTest {
    @Test
    fun dynamicBannerClosedKeyIsScopedByWalletTypeAndWalletId() {
        val classicWalletA = walletHomeDynamicBannerClosedKey(WalletHomeType.CLASSIC, "wallet-a")
        val classicWalletB = walletHomeDynamicBannerClosedKey(WalletHomeType.CLASSIC, "wallet-b")
        val privacyWalletA = walletHomeDynamicBannerClosedKey(WalletHomeType.PRIVACY, "wallet-a")

        assertEquals("pref_wallet_home_dynamic_banner_closed:CLASSIC:wallet-a", classicWalletA)
        assertNotEquals(classicWalletA, classicWalletB)
        assertNotEquals(classicWalletA, privacyWalletA)
    }
}
