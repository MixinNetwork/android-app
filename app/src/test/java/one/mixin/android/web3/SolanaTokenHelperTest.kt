package one.mixin.android.web3

import one.mixin.android.Constants
import one.mixin.android.db.web3.vo.Web3TokenItem
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SolanaTokenHelperTest {
    @Test
    fun nativeSolSpendableBalanceLeavesRentForNativeFlow() {
        val spendable = nativeSolSpendableBalance(
            balance = BigDecimal("1"),
            solFee = BigDecimal("0.01"),
        )

        assertEquals(BigDecimal("0.98910912"), spendable)
    }

    @Test
    fun nativeSolSpendableBalanceAllowsZeroRemainderForGaslessFlow() {
        val spendable = nativeSolSpendableBalance(
            balance = BigDecimal("1"),
            solFee = BigDecimal("0.01"),
            allowZeroBalance = true,
        )

        assertEquals(BigDecimal("0.99"), spendable)
    }

    @Test
    fun hasSolBalanceAfterFeeAndRentRequiresRentByDefault() {
        assertFalse(
            hasSolBalanceAfterFeeAndRent(
                balance = BigDecimal("1"),
                solFee = BigDecimal("0.9992"),
            )
        )
    }

    @Test
    fun hasSolBalanceAfterFeeAndRentAllowsZeroWhenEnabled() {
        assertTrue(
            hasSolBalanceAfterFeeAndRent(
                balance = BigDecimal("1"),
                solFee = BigDecimal("1"),
                allowZeroBalance = true,
            )
        )
    }

    @Test
    fun requiredSolBalanceAddsRentForTokenAccountCreationReserve() {
        val required = requiredSolBalance(
            transferAmount = BigDecimal.ZERO,
            solFee = SOLANA_TOKEN_ACCOUNT_RENT_EXEMPTION,
            sendingNativeSol = false,
        )

        assertEquals(BigDecimal("0.00293016"), required)
    }

    @Test
    fun solanaTransferAmountRangeAddsTokenAccountReserveForNativeFee() {
        val range = solanaTransferAmountRange(
            token = solanaSplToken(balance = "10"),
            feeToken = solanaNativeToken(balance = "0.003"),
            feeAmount = BigDecimal("0.001"),
            recipientAccountState = SolanaRecipientAccountState.NEEDS_TOKEN_ACCOUNT,
            includeAtaCreationReserve = true,
        )

        assertEquals(BigDecimal.ZERO, range.maxAmount)
    }

    @Test
    fun solanaTransferAmountRangeAllowsGaslessSolSendToZero() {
        val range = solanaTransferAmountRange(
            token = solanaNativeToken(balance = "1"),
            feeToken = solanaNativeToken(balance = "1"),
            feeAmount = BigDecimal("0.01"),
            recipientAccountState = SolanaRecipientAccountState.EXISTS,
            allowZeroBalance = true,
        )

        assertEquals(BigDecimal.ZERO, range.minAmount)
        assertEquals(BigDecimal("0.99"), range.maxAmount)
    }

    @Test
    fun solanaTransferAmountRangeRequiresRentForNewSolRecipient() {
        val range = solanaTransferAmountRange(
            token = solanaNativeToken(balance = "1"),
            feeToken = solanaNativeToken(balance = "1"),
            feeAmount = BigDecimal("0.01"),
            recipientAccountState = SolanaRecipientAccountState.NEEDS_SYSTEM_ACCOUNT,
        )

        assertEquals(SOLANA_RENT_EXEMPTION, range.minAmount)
        assertEquals(BigDecimal("0.98910912"), range.maxAmount)
    }

    private fun solanaNativeToken(balance: String) = Web3TokenItem(
        walletId = "wallet",
        assetId = Constants.ChainId.SOLANA_CHAIN_ID,
        chainId = Constants.ChainId.SOLANA_CHAIN_ID,
        name = "Solana",
        assetKey = Constants.ChainId.SOLANA_CHAIN_ID,
        symbol = "SOL",
        iconUrl = "",
        precision = 9,
        balance = balance,
        priceUsd = "0",
        changeUsd = "0",
        chainIcon = "",
        chainName = "Solana",
        chainSymbol = "SOL",
        hidden = false,
        level = 0,
    )

    private fun solanaSplToken(balance: String) = Web3TokenItem(
        walletId = "wallet",
        assetId = "usdc",
        chainId = Constants.ChainId.SOLANA_CHAIN_ID,
        name = "USDC",
        assetKey = "mint",
        symbol = "USDC",
        iconUrl = "",
        precision = 6,
        balance = balance,
        priceUsd = "0",
        changeUsd = "0",
        chainIcon = "",
        chainName = "Solana",
        chainSymbol = "SOL",
        hidden = false,
        level = 0,
    )
}
