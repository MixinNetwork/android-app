package one.mixin.android.ui.home.web3.trade

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TradeInputTest {
    @Test
    fun tradeAmountInputUsesWalletSpecificDecimalPlaces() {
        assertEquals(8, tradeInputMaxDecimalPlaces(isCommonWallet = false, precision = 18))
        assertEquals(6, tradeInputMaxDecimalPlaces(isCommonWallet = true, precision = 6))
        assertEquals(0, tradeInputMaxDecimalPlaces(isCommonWallet = true, precision = 0))
        assertEquals(8, tradeInputMaxDecimalPlaces(isCommonWallet = true, precision = -1))
    }

    @Test
    fun web3TradeAmountInputUsesSharedDecimalLimitEvenWithWalletId() {
        val token = token(
            walletId = "web3-wallet",
            decimals = 18,
            isWeb3 = true,
        )

        assertEquals(8, token.tradeInputMaxDecimalPlaces())
    }

    @Test
    fun commonWalletTradeAmountInputUsesTokenDecimalPlaces() {
        val token = token(
            walletId = "common-wallet",
            decimals = 6,
            isWeb3 = false,
        )

        assertEquals(6, token.tradeInputMaxDecimalPlaces())
    }

    @Test
    fun tradePriceInputAllowsAtMostEightDecimalPlaces() {
        assertEquals(8, tradePriceInputMaxDecimalPlaces())
        assertTrue(isTradeInputDecimalAllowed("1.12345678", tradePriceInputMaxDecimalPlaces()))
        assertFalse(isTradeInputDecimalAllowed("1.123456789", tradePriceInputMaxDecimalPlaces()))
    }

    @Test
    fun tradeAmountInputAllowsConfiguredDecimalPlaces() {
        assertTrue(isTradeInputDecimalAllowed(""))
        assertTrue(isTradeInputDecimalAllowed("12"))
        assertTrue(isTradeInputDecimalAllowed("12."))
        assertTrue(isTradeInputDecimalAllowed("12.12345678"))
        assertTrue(isTradeInputDecimalAllowed("0.00000000"))

        assertFalse(isTradeInputDecimalAllowed("12.123456789"))
        assertFalse(isTradeInputDecimalAllowed("0.000000001"))
        assertFalse(isTradeInputDecimalAllowed("1.234", maxDecimalPlaces = 2))
        assertFalse(isTradeInputDecimalAllowed("5.", maxDecimalPlaces = 0))
        assertFalse(isTradeInputDecimalAllowed("5.0", maxDecimalPlaces = 0))

        assertTrue(isTradeInputDecimalAllowed("12.123456789", maxDecimalPlaces = null))
    }

    @Test
    fun tradeAmountInputLimitsProgrammaticValuesToConfiguredDecimalPlaces() {
        assertEquals("", limitTradeInputDecimalPlaces(""))
        assertEquals("12", limitTradeInputDecimalPlaces("12"))
        assertEquals("12.", limitTradeInputDecimalPlaces("12."))
        assertEquals("12.12345678", limitTradeInputDecimalPlaces("12.12345678"))
        assertEquals("12.12345678", limitTradeInputDecimalPlaces("12.123456789"))
        assertEquals("0.00000000", limitTradeInputDecimalPlaces("0.000000001"))
        assertEquals("1.23", limitTradeInputDecimalPlaces("1.234", maxDecimalPlaces = 2))
        assertEquals("5", limitTradeInputDecimalPlaces("5.", maxDecimalPlaces = 0))
        assertEquals("5", limitTradeInputDecimalPlaces("5.123", maxDecimalPlaces = 0))
        assertEquals(
            "12.123456789",
            limitTradeInputDecimalPlaces("12.123456789", maxDecimalPlaces = null)
        )
    }

    @Test
    fun tradeInputTextFieldValueLimitsPastedDecimals() {
        val value = limitTradeInputTextFieldValue(
            value = TextFieldValue(
                text = "12.123456789",
                selection = TextRange(12),
            ),
            maxDecimalPlaces = 8,
        )

        assertEquals("12.12345678", value.text)
        assertEquals(TextRange(11), value.selection)
    }

    private fun token(
        walletId: String?,
        decimals: Int,
        isWeb3: Boolean,
    ) = SwapToken(
        walletId = walletId,
        address = "",
        assetId = "asset-id",
        decimals = decimals,
        name = "Token",
        symbol = "TKN",
        icon = "",
        chain = SwapChain("", "", "", ""),
        isWeb3 = isWeb3,
    )
}
