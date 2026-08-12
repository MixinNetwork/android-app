package one.mixin.android.ui.wallet

import org.junit.Assert.assertEquals
import org.junit.Test

class WalletHomePreferencesTest {
    @Test
    fun dynamicBannerClosedKeyIsGlobal() {
        assertEquals(
            PREF_WALLET_HOME_DYNAMIC_BANNER_CLOSED,
            walletHomeDynamicBannerClosedKey(),
        )
    }
}
