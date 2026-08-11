package one.mixin.android.web3.send

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.crypto.PearlKeyGenerator
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toHex
import org.bitcoinj.base.Coin
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.base.ScriptType
import org.bitcoinj.core.Transaction
import org.bitcoinj.crypto.ECKey
import org.bitcoinj.script.ScriptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer

class UtxoTransactionSignerTest {
    @Test
    fun bip340SigningMatchesOfficialVectorZero() {
        val privateKey = "0000000000000000000000000000000000000000000000000000000000000003".hexStringToByteArray()
        val message = "0000000000000000000000000000000000000000000000000000000000000000".hexStringToByteArray()
        val auxiliaryRandom = ByteArray(32)
        val expected = "e907831f80848d1069a5371b402410364bdf1c5f8307b0084c55f1ce2dca821525f66a4a85ea8b71e482a74f382d2ce5ebeee8fdb2172f477df4900d310536c0"

        val signature = Bip340Signer.sign(privateKey, message, auxiliaryRandom)

        assertEquals(expected, signature.toHex())
        assertTrue(
            Bip340Signer.verify(
                "f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9".hexStringToByteArray(),
                message,
                signature,
            ),
        )
    }

    @Test
    fun taprootDefaultSighashMatchesOfficialBip341Vector() {
        val rawUnsignedTx = "02000000097de20cbff686da83a54981d2b9bab3586f4ca7e48f57f5b55963115f3b334e9c010000000000000000d7b7cab57b1393ace2d064f4d4a2cb8af6def61273e127517d44759b6dafdd990000000000fffffffff8e1f583384333689228c5d28eac13366be082dc57441760d957275419a418420000000000fffffffff0689180aa63b30cb162a73c6d2a38b7eeda2a83ece74310fda0843ad604853b0100000000feffffffaa5202bdf6d8ccd2ee0f0202afbbb7461d9264a25e5bfd3c5a52ee1239e0ba6c0000000000feffffff956149bdc66faa968eb2be2d2faa29718acbfe3941215893a2a3446d32acd050000000000000000000e664b9773b88c09c32cb70a2a3e4da0ced63b7ba3b22f848531bbb1d5d5f4c94010000000000000000e9aa6b8e6c9de67619e6a3924ae25696bb7b694bb677a632a74ef7eadfd4eabf0000000000ffffffffa778eb6a263dc090464cd125c466b5a99667720b1c110468831d058aa1b82af10100000000ffffffff0200ca9a3b000000001976a91406afd46bcdfd22ef94ac122aa11f241244a37ecc88ac807840cb0000000020ac9a87f5594be208f8532db38cff670c450ed2fea8fcdefcc9a663f78bab962b0065cd1d"
        val prevouts = listOf(
            TaprootPrevout(420000000, "512053a1f6e454df1aa2776a2814a721372d6258050de330b3c6d10ee8f4e0dda343".hexStringToByteArray()),
            TaprootPrevout(462000000, "5120147c9c57132f6e7ecddba9800bb0c4449251c92a1e60371ee77557b6620f3ea3".hexStringToByteArray()),
            TaprootPrevout(294000000, "76a914751e76e8199196d454941c45d1b3a323f1433bd688ac".hexStringToByteArray()),
            TaprootPrevout(504000000, "5120e4d810fd50586274face62b8a807eb9719cef49c04177cc6b76a9a4251d5450e".hexStringToByteArray()),
            TaprootPrevout(630000000, "512091b64d5324723a985170e4dc5a0f84c041804f2cd12660fa5dec09fc21783605".hexStringToByteArray()),
            TaprootPrevout(378000000, "00147dd65592d0ab2fe0d0257d571abf032cd9db93dc".hexStringToByteArray()),
            TaprootPrevout(672000000, "512075169f4001aa68f15bbed28b218df1d0a62cbbcf1188c6665110c293c907b831".hexStringToByteArray()),
            TaprootPrevout(546000000, "5120712447206d7a5238acc7ff53fbe94a3b64539ad291c7cdbc490b7577e4b17df5".hexStringToByteArray()),
            TaprootPrevout(588000000, "512077e30a5522dd9f894c3f8b8bd4c4b2cf82ca7da8a3ea6a239655c39c050ab220".hexStringToByteArray()),
        )
        val transaction = Transaction.read(ByteBuffer.wrap(rawUnsignedTx.hexStringToByteArray()))

        val signatureHash = TaprootSignatureHash.hash(transaction, prevouts, inputIndex = 4)

        assertEquals("4f900a0bae3f1446fd48490c2958b5a023228f01661cda3496a11da502a7f7ef", signatureHash.toHex())
    }

    @Test
    fun pearlTransactionUsesSingleSchnorrWitness() {
        val privateKey = "465752911a76faccd24460a76c69ac7fb6edc603295cd27afc1a6f7a948b8a40".hexStringToByteArray()
        val sender = PearlKeyGenerator.privateKeyToAddress(privateKey)
        val receiver = "prl1pf7fr22zh8f49j9neucnrwvrk5vz47zj8p2sqr2j0g2w5sylmm2tsg7x98l"
        val output = utxoOutput(sender, Constants.ChainId.PEARL_CHAIN_ID)
        val unsigned = BtcTransactionBuilder.buildSendTransaction(
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
            fromAddress = sender,
            toAddress = receiver,
            amountBtc = "0.001",
            localUtxos = listOf(output),
            feeRate = BigDecimal.ONE,
            minimumChangeSatoshis = 100_000L,
        )

        val signed = UtxoTransactionSigner.sign(
            unsignedRawHex = unsigned.rawHex,
            privateKey = privateKey,
            chainId = Constants.ChainId.PEARL_CHAIN_ID,
            localUtxos = listOf(output),
        )

        val transaction = Transaction.read(ByteBuffer.wrap(signed.signedHex.hexStringToByteArray()))
        val signature = transaction.inputs.single().witness.getPush(0)
        val prevout = TaprootPrevout(
            amountSatoshis = Coin.parseCoin(output.amount).value,
            scriptPubKey = ScriptBuilder.createOutputScript(PearlKeyGenerator.parseAddress(sender)).program(),
        )
        val signatureHash = TaprootSignatureHash.hash(transaction, listOf(prevout), inputIndex = 0)
        assertEquals(1, transaction.inputs.single().witness.pushCount)
        assertEquals(64, signature.size)
        assertEquals(listOf(output.outputId), signed.consumedOutputIds)
        assertEquals(sender, signed.fromAddress)
        assertTrue(Bip340Signer.verify(PearlKeyGenerator.taprootOutputKey(privateKey), signatureHash, signature))
    }

    @Test
    fun bitcoinTransactionStillUsesP2wpkhWitness() {
        val privateKey = "465752911a76faccd24460a76c69ac7fb6edc603295cd27afc1a6f7a948b8a40".hexStringToByteArray()
        val sender = UtxoTransactionSigner.address(privateKey, Constants.ChainId.BITCOIN_CHAIN_ID)
        val receiver = ECKey.fromPrivate(BigInteger.TWO, true).toAddress(ScriptType.P2WPKH, BitcoinNetwork.MAINNET).toString()
        val output = utxoOutput(sender, Constants.ChainId.BITCOIN_CHAIN_ID)
        val unsigned = BtcTransactionBuilder.buildSendTransaction(
            fromAddress = sender,
            toAddress = receiver,
            amountBtc = "0.001",
            localUtxos = listOf(output),
            feeRate = BigDecimal.ONE,
            minimumChangeSatoshis = 100_000L,
        )

        val signed = UtxoTransactionSigner.sign(
            unsignedRawHex = unsigned.rawHex,
            privateKey = privateKey,
            chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
            localUtxos = listOf(output),
        )

        val transaction = Transaction.read(ByteBuffer.wrap(signed.signedHex.hexStringToByteArray()))
        assertEquals(2, transaction.inputs.single().witness.pushCount)
        assertEquals(listOf(output.outputId), signed.consumedOutputIds)
        assertEquals(sender, signed.fromAddress)
    }

    private fun utxoOutput(address: String, assetId: String) = WalletOutput(
        outputId = "output",
        assetId = assetId,
        transactionHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        outputIndex = 2,
        amount = "0.004",
        address = address,
        pubkeyHex = "",
        pubkeyType = "",
        status = "unspent",
        createdAt = "",
        updatedAt = "",
    )
}
