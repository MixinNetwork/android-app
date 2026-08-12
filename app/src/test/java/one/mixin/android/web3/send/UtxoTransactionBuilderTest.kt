package one.mixin.android.web3.send

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.crypto.PearlKeyGenerator
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toHex
import org.bitcoinj.core.Transaction
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.nio.ByteBuffer

class UtxoTransactionBuilderTest {
    private val privateKey = "465752911a76faccd24460a76c69ac7fb6edc603295cd27afc1a6f7a948b8a40"
    private val sender = PearlKeyGenerator.privateKeyToAddress(privateKey.hexStringToByteArray())
    private val receiver = "prl1pf7fr22zh8f49j9neucnrwvrk5vz47zj8p2sqr2j0g2w5sylmm2tsg7x98l"

    @Test
    fun pearlTaprootAddressesBuildUnsignedTransaction() {
        val output = pearlOutput()

        val built = BtcTransactionBuilder.buildSendTransaction(
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
            fromAddress = sender,
            toAddress = receiver,
            amountBtc = "0.001",
            localUtxos = listOf(output),
            feeRate = BigDecimal.ONE,
            minimumChangeSatoshis = 100_000L,
        )

        val transaction = Transaction.read(ByteBuffer.wrap(built.rawHex.hexStringToByteArray()))
        assertTrue(transaction.inputs.all { it.sequenceNumber <= 0xfffffffdL })
        assertTrue(transaction.outputs.all { ScriptPattern.isP2TR(Script.parse(it.scriptBytes)) })
    }

    @Test
    fun pearlSpeedUpBuildsHigherFeeTaprootReplacement() {
        val output = pearlOutput()
        val original = buildPearlTransaction(output)
        val replacementHex = BtcTransactionBuilder.buildSpeedUpReplacement(
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
            rawTransactionHex = original.rawHex,
            fromAddress = sender,
            localUtxos = listOf(output),
            feeRate = BigDecimal.TEN,
        )

        val replacement = Transaction.read(ByteBuffer.wrap(replacementHex.hexStringToByteArray()))

        assertTrue(BtcTransactionBuilder.isReplaceable(replacementHex))
        assertTrue(replacement.outputs.all { ScriptPattern.isP2TR(Script.parse(it.scriptBytes)) })
        assertTrue(BtcTransactionBuilder.fee(replacementHex, listOf(output)) > original.feeBtc)
    }

    @Test
    fun pearlCancelBuildsHigherFeeReplacementToSelf() {
        val output = pearlOutput()
        val original = buildPearlTransaction(output)
        assertTrue(BtcTransactionBuilder.canCancel(Constants.ChainId.PEARL_CHAIN_ID, original.rawHex, sender))
        val replacementHex = BtcTransactionBuilder.buildCancelReplacement(
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
            rawTransactionHex = original.rawHex,
            fromAddress = sender,
            localUtxos = listOf(output),
            feeRate = BigDecimal.TEN,
        )

        val replacement = Transaction.read(ByteBuffer.wrap(replacementHex.hexStringToByteArray()))
        val selfScript = org.bitcoinj.script.ScriptBuilder.createOutputScript(PearlKeyGenerator.parseAddress(sender)).program()

        assertEquals(1, replacement.outputs.size)
        assertTrue(replacement.outputs.single().scriptBytes.contentEquals(selfScript))
        assertTrue(BtcTransactionBuilder.isReplaceable(replacementHex))
        assertTrue(BtcTransactionBuilder.fee(replacementHex, listOf(output)) > original.feeBtc)
        assertFalse(BtcTransactionBuilder.canCancel(Constants.ChainId.PEARL_CHAIN_ID, replacementHex, sender))
    }

    @Test
    fun pearlLegacyTransactionWithoutRbfIsNotReplaceable() {
        val legacy = Transaction.read(ByteBuffer.wrap(buildPearlTransaction(pearlOutput()).rawHex.hexStringToByteArray()))
        legacy.replaceInput(0, legacy.inputs.single().withSequence(0xffffffffL))
        val legacyHex = legacy.serialize().toHex()

        assertFalse(BtcTransactionBuilder.isReplaceable(legacyHex))
        assertThrows(IllegalArgumentException::class.java) {
            BtcTransactionBuilder.buildCancelReplacement(
                chainId = Constants.ChainId.PEARL_CHAIN_ID,
                rawTransactionHex = legacyHex,
                fromAddress = sender,
                localUtxos = listOf(pearlOutput()),
                feeRate = BigDecimal.TEN,
            )
        }
    }

    @Test
    fun pearlSpeedUpOnlyAddsUnspentUtxos() {
        val originalOutput = pearlOutput(status = "signed")
        val original = BtcTransactionBuilder.buildSendTransaction(
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
            fromAddress = sender,
            toAddress = receiver,
            amountBtc = "0.003998",
            localUtxos = listOf(originalOutput.copy(status = "unspent")),
            feeRate = BigDecimal.ONE,
        )
        val signedExtra = pearlOutput(
            outputId = "signed-extra",
            transactionHash = "1123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            status = "signed",
        )
        val unspentExtra = pearlOutput(
            outputId = "unspent-extra",
            transactionHash = "2123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        )

        val replacementHex = BtcTransactionBuilder.buildSpeedUpReplacement(
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
            rawTransactionHex = original.rawHex,
            fromAddress = sender,
            localUtxos = listOf(originalOutput, signedExtra, unspentExtra),
            feeRate = BigDecimal.TEN,
        )
        val replacement = Transaction.read(ByteBuffer.wrap(replacementHex.hexStringToByteArray()))
        val replacementHashes = replacement.inputs.map { it.outpoint.hash().toString() }

        assertTrue(unspentExtra.transactionHash in replacementHashes)
        assertFalse(signedExtra.transactionHash in replacementHashes)
    }

    private fun buildPearlTransaction(output: WalletOutput) = BtcTransactionBuilder.buildSendTransaction(
        chainId = Constants.ChainId.PEARL_CHAIN_ID,
        fromAddress = sender,
        toAddress = receiver,
        amountBtc = "0.001",
        localUtxos = listOf(output),
        feeRate = BigDecimal.ONE,
    )

    private fun pearlOutput(
        outputId: String = "output",
        transactionHash: String = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        status: String = "unspent",
    ) = WalletOutput(
        outputId = outputId,
        assetId = Constants.ChainId.PEARL_CHAIN_ID,
        transactionHash = transactionHash,
        outputIndex = 2,
        amount = "0.004",
        address = sender,
        pubkeyHex = "",
        pubkeyType = "",
        status = status,
        createdAt = "",
        updatedAt = "",
    )
}
