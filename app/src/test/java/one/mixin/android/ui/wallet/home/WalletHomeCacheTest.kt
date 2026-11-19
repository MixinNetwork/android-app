package one.mixin.android.ui.wallet.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletHomeCacheTest {
    @Test
    fun restoresPendingIndicatorWithMissingOrNullIconUrls() {
        val expected = WalletHomePendingIndicator(
            kind = WalletHomePendingKind.SINGLE_DEPOSIT,
            value = "1 BTC",
            iconUrls = emptyList(),
            singleAssetId = "asset-1",
        )

        for (iconField in listOf("", """, "iconUrls":null""")) {
            val state = restoreState(
                """{"kind":"SINGLE_DEPOSIT","value":"1 BTC","singleAssetId":"asset-1"$iconField}""",
            )

            assertEquals(iconField, expected, state.pendingIndicator)
        }
    }

    @Test
    fun preservesPendingIndicatorIconUrls() {
        val state = restoreState(
            """{"kind":"MULTIPLE_DEPOSITS","value":"2","iconUrls":["https://example.com/icon.png",null]}""",
        )

        assertEquals(
            WalletHomePendingIndicator(
                kind = WalletHomePendingKind.MULTIPLE_DEPOSITS,
                value = "2",
                iconUrls = listOf("https://example.com/icon.png", null),
            ),
            state.pendingIndicator,
        )
    }

    @Test
    fun ignoresPendingIndicatorWithoutRequiredFields() {
        for (indicator in listOf("""{"value":"1 BTC"}""", """{"kind":"SINGLE_DEPOSIT"}""")) {
            val state = restoreState(indicator)

            assertNull(indicator, state.pendingIndicator)
            assertEquals("12.34", state.fiatTotal)
        }
    }

    private fun restoreState(indicator: String): WalletHomeState =
        checkNotNull(
            """
            {
              "walletType":"PRIVACY",
              "fiatTotal":"12.34",
              "btcTotal":"0.00",
              "fiatSymbol":"USD",
              "totalTokenCount":0,
              "totalTransactionCount":0,
              "pendingIndicator":$indicator
            }
            """.trimIndent().parseWalletHomeCache(),
        ).toState()
}
