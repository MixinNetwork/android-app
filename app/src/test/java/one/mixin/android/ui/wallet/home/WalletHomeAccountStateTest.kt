package one.mixin.android.ui.wallet.home

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletHomeAccountStateTest {
    private val baseState = WalletHomeState(
        walletType = WalletHomeType.PRIVACY,
        cards = listOf(
            WalletHomeCardType.BALANCE,
            WalletHomeCardType.BANNER,
            WalletHomeCardType.POSITIONS,
        ),
    )
    private val cashAccount = WalletHomeCashAccount(
        balanceUsd = BigDecimal("5.8"),
        rewardApy = "3.5",
    )
    private val earnAccount = WalletHomeEarnAccount(
        assetId = "asset-1",
        assetSymbol = "USDT",
        iconUrl = "https://example.com/usdt.png",
        balanceUsd = BigDecimal("1000"),
        earningsUsd = BigDecimal("1.25"),
        apyText = "5.00%",
    )
    private val zeroBalanceEarnAccount = earnAccount.copy(
        balanceUsd = BigDecimal.ZERO,
        earningsUsd = BigDecimal.ZERO,
    )

    @Test
    fun noAccountsDoesNotAddAnAccountCard() {
        val state = baseState.withEarnAccounts(emptyList())

        assertEquals(baseState.cards, state.cards)
    }

    @Test
    fun fiatOnlyKeepsTheOriginalCashCard() {
        val state = baseState.copy(cashAccount = cashAccount).withEarnAccounts(emptyList())

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.CASH,
                WalletHomeCardType.POSITIONS,
            ),
            state.cards,
        )
    }

    @Test
    fun earnOnlyUsesTheStandaloneAccountCard() {
        val state = baseState.withEarnAccounts(listOf(earnAccount))

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.ACCOUNTS,
                WalletHomeCardType.POSITIONS,
            ),
            state.cards,
        )
    }

    @Test
    fun zeroBalanceEarnOnlyUsesTheStandaloneAccountCard() {
        val state = baseState.withEarnAccounts(listOf(zeroBalanceEarnAccount))

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.ACCOUNTS,
                WalletHomeCardType.POSITIONS,
            ),
            state.cards,
        )
    }

    @Test
    fun fiatAndZeroBalanceEarnStillShowsBothAccountCards() {
        val earnThenCash = baseState
            .withEarnAccounts(listOf(zeroBalanceEarnAccount))
            .withCashAccount(cashAccount)
        val cashThenEarn = baseState
            .withCashAccount(cashAccount)
            .withEarnAccounts(listOf(zeroBalanceEarnAccount))

        val expectedCards = listOf(
            WalletHomeCardType.BALANCE,
            WalletHomeCardType.BANNER,
            WalletHomeCardType.ACCOUNTS,
            WalletHomeCardType.POSITIONS,
        )
        assertEquals(expectedCards, earnThenCash.cards)
        assertEquals(expectedCards, cashThenEarn.cards)
    }

    @Test
    fun fiatAndEarnUseOneStandaloneAccountCard() {
        val state = baseState
            .copy(cashAccount = cashAccount)
            .withEarnAccounts(listOf(earnAccount))

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.ACCOUNTS,
                WalletHomeCardType.POSITIONS,
            ),
            state.cards,
        )
    }
}
