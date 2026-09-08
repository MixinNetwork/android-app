package one.mixin.android.db.web3.vo

import one.mixin.android.Constants
import one.mixin.android.crypto.CryptoWalletHelper
import one.mixin.android.tip.wc.internal.Chain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RobinhoodTokenItemTest {
    @Test
    fun robinhoodTokenUsesExpectedDisplayAndNativeAssetMetadata() {
        val nativeToken = token("0x0000000000000000000000000000000000000000")
        val erc20Token = token("0x1111111111111111111111111111111111111111")

        assertEquals("Robinhood", nativeToken.getChainDisplayName())
        assertEquals("ETH", nativeToken.getChainSymbolFromName())
        assertEquals(Chain.Robinhood, nativeToken.getChainFromName())
        assertTrue(nativeToken.isNativeEvmAsset())
        assertFalse(erc20Token.isNativeEvmAsset())
    }

    @Test
    fun robinhoodReusesEthereumAddressDerivation() {
        val privateKey = "0000000000000000000000000000000000000000000000000000000000000001"

        assertEquals(
            CryptoWalletHelper.privateKeyToAddress(privateKey, Constants.ChainId.ETHEREUM_CHAIN_ID),
            CryptoWalletHelper.privateKeyToAddress(privateKey, Constants.ChainId.Robinhood),
        )
    }

    private fun token(assetKey: String) = Web3TokenItem(
        walletId = "wallet",
        assetId = Constants.ChainId.Robinhood,
        chainId = Constants.ChainId.Robinhood,
        name = "ETH",
        assetKey = assetKey,
        symbol = "ETH",
        iconUrl = "",
        precision = 18,
        balance = "0",
        priceUsd = "0",
        changeUsd = "0",
        chainIcon = null,
        chainName = null,
        chainSymbol = null,
        hidden = false,
        level = 1,
    )
}
