package one.mixin.android.web3

import kotlinx.coroutines.runBlocking
import one.mixin.android.api.DataErrorException
import one.mixin.android.api.MixinResponse
import one.mixin.android.api.ResponseError
import one.mixin.android.api.request.web3.EstimateFeeResponse
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.web3.js.JsSignMessage
import org.junit.Test
import org.sol4k.PublicKey
import org.sol4k.Transaction
import org.sol4k.instruction.TransferInstruction
import org.sol4kt.addPlaceholderSignature
import org.sol4kt.VersionedTransactionCompat
import java.math.BigDecimal
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DappTransactionPreflightTest {
    @Test
    fun checksValueAndFeeWithoutAnExplicitTransferToken() = runBlocking<Unit> {
        val result = preflight(ethereumMessage(value = "0xde0b6b3a7640000"), "1.00001")

        assertTrue(result.insufficientBalance)
        assertEquals(0, result.balance.amount.compareTo(BigDecimal.ONE))
        assertEquals(0, result.balance.fee.compareTo(BigDecimal("0.000021")))
    }

    @Test
    fun rejectsPositiveBalanceBelowFee() = runBlocking<Unit> {
        assertTrue(preflight(ethereumMessage(), "0.000001").insufficientBalance)
    }

    @Test
    fun rejectsZeroBalance() = runBlocking<Unit> {
        assertTrue(preflight(ethereumMessage(), "0").insufficientBalance)
    }

    @Test
    fun allowsExactValueAndFeeBalance() = runBlocking<Unit> {
        assertFalse(preflight(ethereumMessage(value = "0xde0b6b3a7640000"), "1.000021").insufficientBalance)
    }

    @Test
    fun includesHigherRequestedMaxFee() = runBlocking<Unit> {
        val result = preflight(ethereumMessage(maxFee = "0x77359400"), "0.00003")

        assertTrue(result.insufficientBalance)
        assertEquals(0, result.balance.fee.compareTo(BigDecimal("0.000042")))
    }

    @Test
    fun messageSigningDoesNotRequireBalanceOrEstimateFee() = runBlocking<Unit> {
        for (type in listOf(JsSignMessage.TYPE_MESSAGE, JsSignMessage.TYPE_PERSONAL_MESSAGE, JsSignMessage.TYPE_TYPED_MESSAGE, JsSignMessage.TYPE_SIGN_IN)) {
            assertNull(
                preflightDappTransaction(
                    JsSignMessage(1, type, data = "message"), Chain.Ethereum, "account",
                    findToken = { error("Message signing must not load a balance") },
                    estimateFee = { error("Message signing must not estimate a fee") },
                ),
            )
        }
    }

    @Test
    fun failedFeeEstimateCannotProceedToSigning() = runBlocking<Unit> {
        val exception = assertFailsWith<Web3Exception> {
            preflightDappTransaction(
                ethereumMessage(), Chain.Ethereum, "account",
                findToken = { token("1") },
                estimateFee = { MixinResponse(ResponseError(400, 10001, "Estimation failed")) },
            )
        }
        assertEquals(10001, exception.code)
    }

    @Test
    fun missingBalanceCannotProceedToSigning() = runBlocking<Unit> {
        assertFailsWith<DataErrorException> {
            preflightDappTransaction(
                ethereumMessage(), Chain.Ethereum, "account",
                findToken = { null },
                estimateFee = { feeResponse() },
            )
        }
    }

    @Test
    fun rejectsSolanaBalanceBelowActualFee() = runBlocking<Unit> {
        val payer = PublicKey("5TDMKU3basuWC9sb9xAJgvn17KYFTLk9srPifmjZqJH9")
        val recipient = PublicKey("9B5XszUGdMaxCZ7uSQhPzdks5ZQSmWxrmzCSvtJ6Ns6g")
        val transaction = Transaction(payer.toBase58(), TransferInstruction(payer, recipient, 100L), payer)
        transaction.addPlaceholderSignature()
        val result = preflightDappTransaction(
            JsSignMessage(1, JsSignMessage.TYPE_RAW_TRANSACTION, data = Base64.getEncoder().encodeToString(transaction.serialize())),
            Chain.Solana,
            payer.toBase58(),
            findToken = { token("0.000004", Chain.Solana) },
            estimateFee = { error("Solana fees are calculated from the transaction") },
        )

        assertNotNull(result)
        assertTrue(result.insufficientBalance)
        assertEquals(0, result.balance.fee.compareTo(BigDecimal("0.000005")))
    }

    @Test
    fun solanaCosignerDoesNotNeedFeePayerBalance() = runBlocking<Unit> {
        val payer = PublicKey("5TDMKU3basuWC9sb9xAJgvn17KYFTLk9srPifmjZqJH9")
        val cosigner = PublicKey("9B5XszUGdMaxCZ7uSQhPzdks5ZQSmWxrmzCSvtJ6Ns6g")
        val transaction = Transaction(payer.toBase58(), TransferInstruction(payer, cosigner, 100L), payer)
        transaction.addPlaceholderSignature()
        val parsed = VersionedTransactionCompat.from(Base64.getEncoder().encodeToString(transaction.serialize()))
        val sponsored = VersionedTransactionCompat(
            parsed.message.copy(header = parsed.message.header.copy(numRequireSignatures = 2)),
            mutableListOf(parsed.signatures.first(), parsed.signatures.first()),
        )

        assertNull(
            preflightDappTransaction(
                JsSignMessage(1, JsSignMessage.TYPE_RAW_TRANSACTION, data = Base64.getEncoder().encodeToString(sponsored.serialize())),
                Chain.Solana,
                cosigner.toBase58(),
                findToken = { error("A cosigner does not pay the transaction fee") },
                estimateFee = { error("Solana fees are calculated from the transaction") },
            ),
        )
    }

    @Test
    fun cachedFeeStillChecksTheLatestBalance() = runBlocking<Unit> {
        val original = preflight(ethereumMessage(), "1")
        val refreshed = preflightDappTransaction(
            ethereumMessage(), Chain.Ethereum, "account",
            findToken = { token("0.000001") },
            estimateFee = { error("The existing estimate should be reused") },
            cachedTipGas = original.tipGas,
        )

        assertNotNull(refreshed)
        assertTrue(refreshed.insufficientBalance)
    }

    private suspend fun preflight(message: JsSignMessage, balance: String) = requireNotNull(
        preflightDappTransaction(
            message, Chain.Ethereum, "account",
            findToken = { token(balance) },
            estimateFee = { feeResponse() },
        ),
    )

    private fun ethereumMessage(value: String? = null, maxFee: String? = null) = JsSignMessage(
        1,
        JsSignMessage.TYPE_TRANSACTION,
        wcEthereumTransaction = WCEthereumTransaction("account", "recipient", null, null, maxFee, null, null, null, value, null),
    )

    private fun feeResponse() = MixinResponse<EstimateFeeResponse>().apply {
        data = EstimateFeeResponse(Chain.Ethereum.assetId, "21000", "1000000000", "1000000000", null, null, null, null)
    }

    private fun token(balance: String, chain: Chain = Chain.Ethereum) = Web3TokenItem(
        walletId = "wallet",
        assetId = chain.assetId,
        chainId = chain.assetId,
        name = chain.name,
        assetKey = "",
        symbol = chain.symbol,
        iconUrl = "",
        precision = 18,
        balance = balance,
        priceUsd = "0",
        changeUsd = "0",
        chainIcon = null,
        chainName = null,
        chainSymbol = null,
        hidden = false,
        level = 0,
    )
}
