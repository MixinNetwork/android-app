package one.mixin.android.ui.web

import one.mixin.android.Constants
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebFragmentRedirectTest {
    @Test
    fun closesWebContainerForPageOpeningMixinRedirects() {
        listOf(
            "${Constants.Scheme.MIXIN_MARKET}/btc",
            "${Constants.Scheme.HTTPS_MARKET}/btc",
            "${Constants.Scheme.MIXIN_TRADE}?input=btc",
            "${Constants.Scheme.HTTPS_TRADE}?input=btc",
            "${Constants.Scheme.MIXIN_SWAP}?input=btc",
            "${Constants.Scheme.HTTPS_SWAP}?input=btc",
            "${Constants.Scheme.CONVERSATIONS}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46",
            "${Constants.Scheme.MIXIN_BUY}",
            "${Constants.Scheme.HTTPS_BUY}",
            "${Constants.Scheme.HTTPS_INSCRIPTION}/hash",
            "${Constants.Scheme.APPS}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46?action=open",
            "${Constants.Scheme.HTTPS_APPS}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46?action=open",
        ).forEach { url ->
            assertTrue(url, url.shouldCloseWebContainerForMixinRedirect())
        }
    }

    @Test
    fun keepsWebContainerForBottomSheetMixinRedirects() {
        listOf(
            "${Constants.Scheme.USERS}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46",
            "${Constants.Scheme.HTTPS_USERS}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46",
            "${Constants.Scheme.PAY}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46",
            "${Constants.Scheme.MIXIN_PAY}a465ffdb-4441-4cb9-8b45-00cf79dfbc46",
            "${Constants.Scheme.HTTPS_PAY}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46",
            "${Constants.Scheme.ADDRESS}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46",
            "${Constants.Scheme.HTTPS_ADDRESS}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46",
            "${Constants.Scheme.MIXIN_REFERRALS}/abc",
            "${Constants.Scheme.HTTPS_REFERRALS}/abc",
            Constants.Scheme.MIXIN_WC,
            "${Constants.Scheme.WALLET_CONNECT_PREFIX}topic@2",
        ).forEach { url ->
            assertFalse(url, url.shouldCloseWebContainerForMixinRedirect())
        }
    }

    @Test
    fun keepsWebContainerForAppLinksWithoutOpenAction() {
        listOf(
            "${Constants.Scheme.APPS}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46",
            "${Constants.Scheme.HTTPS_APPS}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46",
            "${Constants.Scheme.APPS}/a465ffdb-4441-4cb9-8b45-00cf79dfbc46?action=auth",
        ).forEach { url ->
            assertFalse(url, url.shouldCloseWebContainerForMixinRedirect())
        }
    }
}
