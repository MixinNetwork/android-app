package one.mixin.android.web3.details

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toHex
import one.mixin.android.web3.send.BtcTransactionBuilder
import one.mixin.android.web3.send.UtxoTransactionSigner
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.base.ScriptType
import org.bitcoinj.core.Transaction
import org.bitcoinj.crypto.ECKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer

class PendingTransactionActionRouteTest {
    @Test
    fun pearlUsesUtxoPendingActions() {
        assertEquals(
            PendingTransactionActionRoute.Utxo,
            pendingTransactionActionRoute(Constants.ChainId.PEARL_CHAIN_ID),
        )
    }

    @Test
    fun bitcoinUsesUtxoPendingActions() {
        assertEquals(
            PendingTransactionActionRoute.Utxo,
            pendingTransactionActionRoute(Constants.ChainId.BITCOIN_CHAIN_ID),
        )
    }

    @Test
    fun nonRbfBitcoinPendingTransactionDoesNotExposeReplacementActions() {
        val raw = nonRbfBitcoinTransaction()

        assertFalse(BtcTransactionBuilder.isReplaceable(raw))
        assertTrue(
            shouldHideUtxoPendingActions(
                chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
                rawTransactionHex = raw,
                hasSignedChange = false,
            )
        )
    }

    @Test
    fun ethereumUsesEvmPendingActions() {
        assertEquals(
            PendingTransactionActionRoute.Evm,
            pendingTransactionActionRoute(Constants.ChainId.ETHEREUM_CHAIN_ID),
        )
    }

    private fun nonRbfBitcoinTransaction(): String {
        val privateKey = "465752911a76faccd24460a76c69ac7fb6edc603295cd27afc1a6f7a948b8a40"
        val sender = UtxoTransactionSigner.address(
            privateKey.hexStringToByteArray(),
            Constants.ChainId.BITCOIN_CHAIN_ID,
        )
        val receiver = ECKey.fromPrivate(BigInteger.TWO, true)
            .toAddress(ScriptType.P2WPKH, BitcoinNetwork.MAINNET)
            .toString()
        val output = WalletOutput(
            outputId = "output",
            assetId = Constants.ChainId.BITCOIN_CHAIN_ID,
            transactionHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            outputIndex = 2,
            amount = "0.004",
            address = sender,
            pubkeyHex = "",
            pubkeyType = "",
            status = "unspent",
            createdAt = "",
            updatedAt = "",
        )
        val unsigned = BtcTransactionBuilder.buildSendTransaction(
            chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
            fromAddress = sender,
            toAddress = receiver,
            amountBtc = "0.001",
            localUtxos = listOf(output),
            feeRate = BigDecimal.ONE,
        )
        val transaction = Transaction.read(ByteBuffer.wrap(unsigned.rawHex.hexStringToByteArray()))
        transaction.replaceInput(0, transaction.inputs.single().withSequence(0xffffffffL))
        return transaction.serialize().toHex()
    }
}
