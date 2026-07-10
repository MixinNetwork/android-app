package one.mixin.android.ui.wallet.home

import com.google.gson.Gson
import one.mixin.android.api.response.CashAccount
import one.mixin.android.api.response.WalletHomeBanner
import one.mixin.android.util.GsonHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class WalletHomeCashAccountTest {
    private data class CashAccountEnvelope(
        val data: CashAccount,
    )

    @Test
    fun cashAccountParsesRewardApyFromApiResponse() {
        val json = """
            {"data":{"balance":"5.8","chain_id":"64692c23-8971-4cf4-84a7-4dd1271dd887","destination":"8ZftmiYXTGdwqXzus1tcDewMWUeD4AFMfFwdKyvqn3ef","min_amount":"0.01","reward_apy":"3.5"}}
        """.trimIndent()

        val response = Gson().fromJson(json, CashAccountEnvelope::class.java)

        assertEquals("3.5", response.data.rewardApy)
    }

    @Test
    fun walletHomeCashAccountFormatsRewardApyWithPercentSuffix() {
        val account = CashAccount(
            balance = "5.8",
            minAmount = "0.01",
            rewardApy = "3.5",
        ).toWalletHomeCashAccount()

        assertEquals("3.5", account?.rewardApy)
        assertEquals("3.5%", account?.apyText)
    }

    @Test
    fun cashAccountApyTextDoesNotDuplicatePercentSuffix() {
        assertEquals("3.5%", cashAccountApyText("3.5%"))
        assertEquals("3.5%", cashAccountApyText(" 3.5 % "))
    }

    @Test
    fun walletHomeStateShowsCachedCashAccountWhenStillLoading() {
        val cashAccount = WalletHomeCashAccount(
            balanceUsd = BigDecimal("5.8"),
            rewardApy = "3.5",
        )

        val state = WalletHomeState(
            walletType = WalletHomeType.PRIVACY,
            isLoading = true,
        ).withCashAccount(cashAccount)

        assertEquals(cashAccount, state.cashAccount)
        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.CASH,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.SUPPORT,
            ),
            state.cards,
        )
        assertEquals(false, state.isLoading)
    }

    @Test
    fun walletHomeStateInsertsCachedCashCardAfterBalance() {
        val cashAccount = WalletHomeCashAccount(
            balanceUsd = BigDecimal("5.8"),
            rewardApy = "3.5",
        )

        val state = WalletHomeState(
            walletType = WalletHomeType.PRIVACY,
            cards = listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.TOKENS,
            ),
        ).withCashAccount(cashAccount)

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.CASH,
                WalletHomeCardType.TOKENS,
            ),
            state.cards,
        )
    }

    @Test
    fun cashAccountReadsLegacyBalanceUsd() {
        val directAccount = GsonHelper.customGson.fromJson("""{"balanceUsd":5.8,"rewardApy":"3.5"}""", WalletHomeCashAccount::class.java)

        assertEquals(0, directAccount.balanceUsd.compareTo(BigDecimal("5.8")))
    }

    @Test
    fun cashAccountReadsLegacyWalletHomeCache() {
        val json = """
            {
              "walletType":"PRIVACY",
              "fiatTotal":"0.00",
              "btcTotal":"0.00",
              "fiatSymbol":"USD",
              "totalTokenCount":0,
              "totalTransactionCount":0,
              "cashAccount":{"balanceUsd":5.8,"rewardApy":"3.5"}
            }
        """.trimIndent()

        val cashAccount = json.legacyWalletHomeCashAccountCache()

        assertEquals(0, cashAccount?.balanceUsd?.compareTo(BigDecimal("5.8")))
        assertEquals("3.5", cashAccount?.rewardApy)
    }

    @Test
    fun cashAccountReadsLegacyBalanceKey() {
        val cashAccount = """
            {"cashAccount":{"balance":"6.2","rewardApy":"4.1"}}
        """.trimIndent().legacyWalletHomeCashAccountCache()

        assertEquals(0, cashAccount?.balanceUsd?.compareTo(BigDecimal("6.2")))
        assertEquals("4.1", cashAccount?.rewardApy)
    }

    @Test
    fun walletHomeCacheDoesNotRestoreCashAccount() {
        val json = """
            {
              "walletType":"PRIVACY",
              "fiatTotal":"0.00",
              "btcTotal":"0.00",
              "fiatSymbol":"USD",
              "totalTokenCount":0,
              "totalTransactionCount":0,
              "cashAccount":{"balanceUsd":5.8,"rewardApy":"3.5"}
            }
        """.trimIndent()

        val cache = GsonHelper.customGson.fromJson(json, WalletHomeCache::class.java)
        assertNotNull(cache)
        assertNotNull(cache.cashAccount)

        val state = cache.toState()

        assertNull(state.cashAccount)
        assertFalse(state.cards.contains(WalletHomeCardType.CASH))
    }

    @Test
    fun walletHomeStateShowsDynamicBannerWhenStillLoading() {
        val banner = WalletHomeBanner(
            bannerId = "ad-1",
            title = "Ad",
            actionUrl = "mixin://ad",
        )

        val state = WalletHomeState(
            walletType = WalletHomeType.PRIVACY,
            isLoading = true,
        ).withDynamicBanners(
            dynamicBanners = listOf(banner),
            showAddWalletBanner = false,
        )

        assertEquals(listOf(banner), state.dynamicBanners)
        assertEquals(
            listOf(
                WalletHomeCardType.EMPTY_GUIDE,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.TOKENS,
                WalletHomeCardType.SUPPORT,
            ),
            state.cards,
        )
        assertFalse(state.isLoading)
    }
}
