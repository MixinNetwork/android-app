package one.mixin.android.ui.wallet.home

import org.junit.Assert.assertEquals
import org.junit.Test

class WalletHomeBuilderTest {
    @Test
    fun `privacy wallet with assets and positions shows positions before tokens`() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = true,
            showBanner = true,
            showReferral = true,
            hasPositions = true,
            hasTopMovers = true,
            hasTransactions = true,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.POSITIONS,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.TRANSACTIONS,
                WalletHomeCardType.REFERRAL,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun `privacy wallet without positions shows top movers after tokens`() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = true,
            showBanner = false,
            showReferral = false,
            hasPositions = false,
            hasTopMovers = true,
            hasTransactions = false,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.TOP_MOVERS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun `classic wallet never shows positions or top movers`() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.CLASSIC,
            hasAssetValue = true,
            showBanner = true,
            showReferral = true,
            hasPositions = true,
            hasTopMovers = true,
            hasTransactions = true,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.TRANSACTIONS,
                WalletHomeCardType.REFERRAL,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun `wallet without asset value starts with empty guide`() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.CLASSIC,
            hasAssetValue = false,
            showBanner = false,
            showReferral = false,
            hasPositions = false,
            hasTopMovers = false,
            hasTransactions = false,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.EMPTY_GUIDE,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun `wallet without asset value but missing key starts with balance`() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.CLASSIC,
            hasAssetValue = false,
            showBanner = false,
            showReferral = false,
            hasPositions = false,
            hasTopMovers = false,
            hasTransactions = false,
            hasImportKeyAction = true,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun `referral shows above support at the bottom`() {
        val cards = WalletHomeBuilder.build(
            walletType = WalletHomeType.PRIVACY,
            hasAssetValue = true,
            showBanner = false,
            showReferral = true,
            hasPositions = false,
            hasTopMovers = false,
            hasTransactions = false,
        )

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.REFERRAL,
                WalletHomeCardType.SUPPORT,
            ),
            cards,
        )
    }

    @Test
    fun `wallet home token preview uses capped token list`() {
        assertEquals(3, WalletHomeSection.previewCount(10))
        assertEquals(2, WalletHomeSection.previewCount(2))
    }

    @Test
    fun `section preview count is capped at three and more is shown after three`() {
        assertEquals(3, WalletHomeSection.previewCount(4))
        assertEquals(true, WalletHomeSection.hasMore(4))
        assertEquals(3, WalletHomeSection.previewCount(3))
        assertEquals(false, WalletHomeSection.hasMore(3))
    }
}
