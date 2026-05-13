package one.mixin.android.ui.wallet

import one.mixin.android.Constants
import one.mixin.android.vo.WithdrawalMemoPossibility
import one.mixin.android.vo.safe.TokenItem
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test

class Web3FeeSelectionTest {
    @Test
    fun sameAssetWithEnoughBalanceIsPreferredFirst() {
        val selected = feeOption(assetId = "usdc", balance = "1", fee = "2")
        val preferred = feeOption(assetId = "eth", balance = "3", fee = "1")

        val result = selectPreferredGaslessFeeOption(
            options = listOf(selected, preferred),
            preferredAssetId = "eth",
            selectedKey = selected.selectionKey,
        )

        assertEquals(preferred, result)
    }

    @Test
    fun previouslySelectedOptionWinsWhenPreferredAssetIsNotAffordable() {
        val selected = feeOption(assetId = "usdc", balance = "3", fee = "1")
        val preferred = feeOption(assetId = "eth", balance = "0.5", fee = "1")

        val result = selectPreferredGaslessFeeOption(
            options = listOf(selected, preferred),
            preferredAssetId = "eth",
            selectedKey = selected.selectionKey,
        )

        assertEquals(selected, result)
    }

    @Test
    fun firstAffordableOptionIsUsedAsFallback() {
        val selected = feeOption(assetId = "usdc", balance = "1", fee = "2")
        val preferred = feeOption(assetId = "eth", balance = "0.5", fee = "1")
        val fallback = feeOption(assetId = "btc", balance = "5", fee = "1")

        val result = selectPreferredGaslessFeeOption(
            options = listOf(selected, preferred, fallback),
            preferredAssetId = "eth",
            selectedKey = selected.selectionKey,
        )

        assertEquals(fallback, result)
    }

    @Test
    fun firstApiOptionIsUsedWhenNoFeeOptionIsAffordable() {
        val first = feeOption(assetId = "usdc", balance = "1", fee = "2")
        val second = feeOption(assetId = "eth", balance = "0.5", fee = "1")

        val result = selectPreferredGaslessFeeOption(
            options = listOf(first, second),
            preferredAssetId = "eth",
            selectedKey = second.selectionKey,
        )

        assertEquals(first, result)
    }

    @Test
    fun gaslessNativeSolOptionStillRequiresRentOrExactZeroRemainder() {
        val solFee = feeOption(
            assetId = Constants.ChainId.SOLANA_CHAIN_ID,
            chainId = Constants.ChainId.SOLANA_CHAIN_ID,
            symbol = "SOL",
            balance = "1",
            fee = "0.9992",
            source = NetworkFee.Source.GASLESS,
        )

        assertFalse(solFee.canCoverSelectionFee())
    }

    private fun feeOption(
        assetId: String,
        chainId: String = "chain",
        symbol: String = assetId.uppercase(),
        balance: String,
        fee: String,
        source: NetworkFee.Source = NetworkFee.Source.GASLESS,
    ) = NetworkFee(
        token = tokenItem(
            assetId = assetId,
            chainId = chainId,
            symbol = symbol,
            balance = balance,
        ),
        fee = fee,
        source = source,
    )

    private fun tokenItem(
        assetId: String,
        chainId: String,
        symbol: String,
        balance: String,
    ) = TokenItem(
        assetId = assetId,
        symbol = symbol,
        name = symbol,
        iconUrl = "",
        balance = balance,
        priceBtc = "0",
        priceUsd = "0",
        chainId = chainId,
        changeUsd = "0",
        changeBtc = "0",
        hidden = false,
        confirmations = 0,
        chainIconUrl = "",
        chainSymbol = symbol,
        chainName = symbol,
        assetKey = assetId,
        dust = null,
        withdrawalMemoPossibility = WithdrawalMemoPossibility.POSSIBLE,
        collectionHash = null,
        level = 0,
        precision = 8,
    )
}
