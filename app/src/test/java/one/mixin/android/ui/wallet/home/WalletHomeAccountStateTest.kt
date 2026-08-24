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
    private val wealthAccount = WalletHomeWealthAccount(
        assetId = "asset-1",
        assetSymbol = "USDT",
        iconUrl = "https://example.com/usdt.png",
        balanceUsd = BigDecimal("1000"),
        earningsUsd = BigDecimal("1.25"),
        apyText = "5.00%",
    )
    private val zeroBalanceWealthAccount = wealthAccount.copy(
        balanceUsd = BigDecimal.ZERO,
        earningsUsd = BigDecimal.ZERO,
    )

    @Test
    fun noAccountsDoesNotAddAnAccountCard() {
        val state = baseState.withWealthAccounts(emptyList())

        assertEquals(baseState.cards, state.cards)
    }

    @Test
    fun fiatOnlyKeepsTheOriginalCashCard() {
        val state = baseState.copy(cashAccount = cashAccount).withWealthAccounts(emptyList())

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.CASH,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.POSITIONS,
            ),
            state.cards,
        )
    }

    @Test
    fun wealthOnlyUsesTheStandaloneAccountCard() {
        val state = baseState.withWealthAccounts(listOf(wealthAccount))

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.ACCOUNTS,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.POSITIONS,
            ),
            state.cards,
        )
    }

    @Test
    fun zeroBalanceWealthOnlyUsesTheStandaloneAccountCard() {
        val state = baseState.withWealthAccounts(listOf(zeroBalanceWealthAccount))

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.ACCOUNTS,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.POSITIONS,
            ),
            state.cards,
        )
    }

    @Test
    fun fiatAndZeroBalanceWealthShowOnlyTheCashCardRegardlessOfLoadOrder() {
        val wealthThenCash = baseState
            .withWealthAccounts(listOf(zeroBalanceWealthAccount))
            .withCashAccount(cashAccount)
        val cashThenWealth = baseState
            .withCashAccount(cashAccount)
            .withWealthAccounts(listOf(zeroBalanceWealthAccount))

        val expectedCards = listOf(
            WalletHomeCardType.BALANCE,
            WalletHomeCardType.CASH,
            WalletHomeCardType.BANNER,
            WalletHomeCardType.POSITIONS,
        )
        assertEquals(expectedCards, wealthThenCash.cards)
        assertEquals(expectedCards, cashThenWealth.cards)
    }

    @Test
    fun fiatAndWealthUseOneStandaloneAccountCard() {
        val state = baseState
            .copy(cashAccount = cashAccount)
            .withWealthAccounts(listOf(wealthAccount))

        assertEquals(
            listOf(
                WalletHomeCardType.BALANCE,
                WalletHomeCardType.ACCOUNTS,
                WalletHomeCardType.BANNER,
                WalletHomeCardType.POSITIONS,
            ),
            state.cards,
        )
    }
}
