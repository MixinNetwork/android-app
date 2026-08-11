package one.mixin.android.web3.send

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.crypto.PearlKeyGenerator
import one.mixin.android.extension.hexStringToByteArray
import org.bitcoinj.core.Transaction
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptPattern
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.nio.ByteBuffer

class UtxoTransactionBuilderTest {
    @Test
    fun pearlTaprootAddressesBuildUnsignedTransaction() {
        val privateKey = "465752911a76faccd24460a76c69ac7fb6edc603295cd27afc1a6f7a948b8a40"
        val sender = PearlKeyGenerator.privateKeyToAddress(privateKey.hexStringToByteArray())
        val receiver = "prl1pf7fr22zh8f49j9neucnrwvrk5vz47zj8p2sqr2j0g2w5sylmm2tsg7x98l"
        val output = WalletOutput(
            outputId = "output",
            assetId = Constants.ChainId.PEARL_CHAIN_ID,
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
        assertTrue(transaction.outputs.all { ScriptPattern.isP2TR(Script.parse(it.scriptBytes)) })
    }
}
