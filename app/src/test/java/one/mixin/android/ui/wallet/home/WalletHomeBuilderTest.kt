package one.mixin.android.ui.wallet.home

import org.junit.Assert.assertEquals
import org.junit.Test

class WalletHomeBuilderTest {
    @Test
    fun privacyWalletWithPositionsDoesNotShowTopMovers() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = true,
            showBanner = false,
            showReferral = false,
            hasPositions = true,
            hasTopMovers = true,
            hasTransactions = true,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.POSITIONS,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.TRANSACTIONS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun privacyWalletWithoutPositionsShowsTopMoversBeforeTokens() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = true,
            showBanner = false,
            showReferral = false,
            hasPositions = false,
            hasTopMovers = true,
            hasTransactions = true,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.TOP_MOVERS,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.TRANSACTIONS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun classicWalletDoesNotShowPerpetualCards() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.CLASSIC,
            hasAssetValue = true,
            showBanner = false,
            showReferral = false,
            hasPositions = true,
            hasTopMovers = true,
            hasTransactions = true,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.TRANSACTIONS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }
}
