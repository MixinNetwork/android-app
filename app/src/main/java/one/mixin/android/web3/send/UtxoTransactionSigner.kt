package one.mixin.android.web3.send

import one.mixin.android.Constants
import one.mixin.android.api.response.web3.WalletOutput
import one.mixin.android.crypto.UtxoKeyGenerator
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.toHex
import org.bitcoinj.base.Coin
import org.bitcoinj.base.VarInt
import org.bitcoinj.base.internal.ByteUtils
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionWitness
import org.bitcoinj.crypto.ECKey
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import org.bouncycastle.math.ec.ECPoint
import org.web3j.utils.Numeric
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

object UtxoTransactionSigner {
    data class SignedUtxoTransaction(
        val signedHex: String,
        val consumedOutputIds: List<String>,
        val fromAddress: String,
    )

    fun sign(
        unsignedRawHex: String,
        privateKey: ByteArray,
        chainId: String,
        localUtxos: List<WalletOutput>,
    ): SignedUtxoTransaction =
        when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> signBitcoin(unsignedRawHex, privateKey, localUtxos)
            Constants.ChainId.PEARL_CHAIN_ID -> signPearl(unsignedRawHex, privateKey, localUtxos)
            else -> throw IllegalArgumentException("Unsupported UTXO chain: $chainId")
        }

    fun address(privateKey: ByteArray, chainId: String): String =
        UtxoKeyGenerator.privateKeyToAddress(privateKey, chainId)

    private fun signBitcoin(
        unsignedRawHex: String,
        privateKey: ByteArray,
        localUtxos: List<WalletOutput>,
    ): SignedUtxoTransaction {
        val signingKey = ECKey.fromPrivate(privateKey, true)
        val fromAddress = address(privateKey, Constants.ChainId.BITCOIN_CHAIN_ID)
        val transaction = readTransaction(unsignedRawHex)
        val matchedUtxos = matchInputs(transaction, localUtxos, Constants.ChainId.BITCOIN_CHAIN_ID, fromAddress)
        val scriptCode: Script = ScriptBuilder.createP2PKHOutputScript(signingKey)
        transaction.inputs.toList().forEachIndexed { inputIndex, input ->
            val utxoAmount = Coin.parseCoin(matchedUtxos[inputIndex].amount)
            val signature = transaction.calculateWitnessSignature(
                inputIndex,
                signingKey,
                scriptCode,
                utxoAmount,
                Transaction.SigHash.ALL,
                false,
            )
            val witness = TransactionWitness.of(signature.encodeToBitcoin(), signingKey.pubKey)
            transaction.replaceInput(inputIndex, input.withScriptBytes(byteArrayOf()).withWitness(witness))
        }
        return transaction.signedResult(matchedUtxos, fromAddress)
    }

    private fun signPearl(
        unsignedRawHex: String,
        privateKey: ByteArray,
        localUtxos: List<WalletOutput>,
    ): SignedUtxoTransaction {
        val fromAddress = address(privateKey, Constants.ChainId.PEARL_CHAIN_ID)
        val transaction = readTransaction(unsignedRawHex)
        val matchedUtxos = matchInputs(transaction, localUtxos, Constants.ChainId.PEARL_CHAIN_ID, fromAddress)
        val prevouts = matchedUtxos.map { utxo ->
            TaprootPrevout(
                amountSatoshis = Coin.parseCoin(utxo.amount).value,
                scriptPubKey = ScriptBuilder.createOutputScript(
                    UtxoKeyGenerator.parseAddress(utxo.address, Constants.ChainId.PEARL_CHAIN_ID),
                ).program(),
            )
        }
        val tweakedPrivateKey = UtxoKeyGenerator.taprootTweakedPrivateKey(privateKey)
        try {
            transaction.inputs.toList().forEachIndexed { inputIndex, input ->
                val signatureHash = TaprootSignatureHash.hash(transaction, prevouts, inputIndex)
                val signature = Bip340Signer.sign(tweakedPrivateKey, signatureHash)
                transaction.replaceInput(
                    inputIndex,
                    input.withScriptBytes(byteArrayOf()).withWitness(TransactionWitness.of(signature)),
                )
            }
            return transaction.signedResult(matchedUtxos, fromAddress)
        } finally {
            tweakedPrivateKey.fill(0)
        }
    }

    private fun readTransaction(rawHex: String): Transaction {
        val bytes = rawHex.removePrefix("0x").trim().hexStringToByteArray()
        return Transaction.read(ByteBuffer.wrap(bytes))
    }

    private fun matchInputs(
        transaction: Transaction,
        localUtxos: List<WalletOutput>,
        chainId: String,
        fromAddress: String,
    ): List<WalletOutput> {
        require(localUtxos.all { it.assetId == chainId && it.address == fromAddress }) {
            "UTXOs do not belong to the signing chain and address"
        }
        val utxoMap = localUtxos.associateBy { utxo -> "${utxo.transactionHash}:${utxo.outputIndex}" }
        return transaction.inputs.mapIndexed { inputIndex, input ->
            val utxoKey = "${input.outpoint.hash()}:${input.outpoint.index()}"
            val utxo = utxoMap[utxoKey]
                ?: throw IllegalArgumentException("Missing utxo for input[$inputIndex] $utxoKey")
            require(Coin.parseCoin(utxo.amount).isPositive) { "Invalid utxo amount on input[$inputIndex]" }
            utxo
        }
    }

    private fun Transaction.signedResult(
        matchedUtxos: List<WalletOutput>,
        fromAddress: String,
    ) = SignedUtxoTransaction(
        signedHex = serialize().toHex(),
        consumedOutputIds = matchedUtxos.map(WalletOutput::outputId),
        fromAddress = fromAddress,
    )
}

internal data class TaprootPrevout(
    val amountSatoshis: Long,
    val scriptPubKey: ByteArray,
)

internal object TaprootSignatureHash {
    fun hash(
        transaction: Transaction,
        prevouts: List<TaprootPrevout>,
        inputIndex: Int,
    ): ByteArray {
        require(prevouts.size == transaction.inputs.size) { "Missing Taproot prevout data" }
        require(inputIndex in transaction.inputs.indices) { "Invalid Taproot input index" }

        val signatureMessage = ByteArrayOutputStream().apply {
            write(0)
            ByteUtils.writeInt32LE(transaction.version, this)
            ByteUtils.writeInt32LE(transaction.lockTime().rawValue(), this)
            write(sha256(transaction.inputs.flatMapBytes { it.outpoint.serialize() }))
            write(sha256(prevouts.flatMapBytes { longToLittleEndian(it.amountSatoshis) }))
            write(sha256(prevouts.flatMapBytes { VarInt.of(it.scriptPubKey.size.toLong()).serialize() + it.scriptPubKey }))
            write(sha256(transaction.inputs.flatMapBytes { intToLittleEndian(it.sequenceNumber) }))
            write(sha256(transaction.outputs.flatMapBytes { it.serialize() }))
            write(0)
            ByteUtils.writeInt32LE(inputIndex, this)
        }.toByteArray()
        return Bip340Signer.taggedHash("TapSighash", byteArrayOf(0) + signatureMessage)
    }

    private fun intToLittleEndian(value: Long): ByteArray =
        ByteArray(4).also { ByteUtils.writeInt32LE(value, it, 0) }

    private fun longToLittleEndian(value: Long): ByteArray =
        ByteArray(8).also { ByteUtils.writeInt64LE(value, it, 0) }

    private inline fun <T> Iterable<T>.flatMapBytes(transform: (T) -> ByteArray): ByteArray =
        ByteArrayOutputStream().apply {
            for (item in this@flatMapBytes) write(transform(item))
        }.toByteArray()

    private fun sha256(value: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(value)
}

internal object Bip340Signer {
    private val curve = ECKey.ecDomainParameters()
    private val curveOrder = curve.n
    private val fieldPrime = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16)
    private val secureRandom = SecureRandom()

    fun sign(privateKey: ByteArray, message: ByteArray, auxiliaryRandom: ByteArray = randomBytes()): ByteArray {
        require(message.size == 32) { "BIP340 message must be 32 bytes" }
        require(auxiliaryRandom.size == 32) { "BIP340 auxiliary randomness must be 32 bytes" }
        val secretValue = BigInteger(1, privateKey)
        require(secretValue.signum() > 0 && secretValue < curveOrder) { "Invalid BIP340 private key" }
        val publicPoint = curve.g.multiply(secretValue).normalize()
        val signingValue = if (hasEvenY(publicPoint)) secretValue else curveOrder.subtract(secretValue)
        val publicKey = xOnly(publicPoint)
        val signingKeyBytes = Numeric.toBytesPadded(signingValue, 32)
        val maskedKey = signingKeyBytes.xor(taggedHash("BIP0340/aux", auxiliaryRandom))
        try {
            val nonceValue = BigInteger(
                1,
                taggedHash("BIP0340/nonce", maskedKey + publicKey + message),
            ).mod(curveOrder)
            require(nonceValue.signum() > 0) { "Invalid BIP340 nonce" }
            val noncePoint = curve.g.multiply(nonceValue).normalize()
            val adjustedNonce = if (hasEvenY(noncePoint)) nonceValue else curveOrder.subtract(nonceValue)
            val challenge = BigInteger(
                1,
                taggedHash("BIP0340/challenge", xOnly(noncePoint) + publicKey + message),
            ).mod(curveOrder)
            val signature = xOnly(noncePoint) + Numeric.toBytesPadded(
                adjustedNonce.add(challenge.multiply(signingValue)).mod(curveOrder),
                32,
            )
            check(verify(publicKey, message, signature)) { "Generated invalid BIP340 signature" }
            return signature
        } finally {
            signingKeyBytes.fill(0)
            maskedKey.fill(0)
        }
    }

    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean =
        runCatching {
            require(publicKey.size == 32 && message.size == 32 && signature.size == 64)
            val publicPoint = liftX(BigInteger(1, publicKey))
            val r = BigInteger(1, signature.copyOfRange(0, 32))
            val s = BigInteger(1, signature.copyOfRange(32, 64))
            require(r < fieldPrime && s < curveOrder)
            val challenge = BigInteger(
                1,
                taggedHash("BIP0340/challenge", signature.copyOfRange(0, 32) + publicKey + message),
            ).mod(curveOrder)
            val noncePoint = curve.g.multiply(s).add(publicPoint.multiply(curveOrder.subtract(challenge))).normalize()
            !noncePoint.isInfinity && hasEvenY(noncePoint) && noncePoint.affineXCoord.toBigInteger() == r
        }.getOrDefault(false)

    fun taggedHash(tag: String, value: ByteArray): ByteArray {
        val tagHash = MessageDigest.getInstance("SHA-256").digest(tag.toByteArray(StandardCharsets.UTF_8))
        return MessageDigest.getInstance("SHA-256").digest(tagHash + tagHash + value)
    }

    private fun liftX(x: BigInteger): ECPoint {
        require(x < fieldPrime)
        return curve.curve.decodePoint(byteArrayOf(2) + Numeric.toBytesPadded(x, 32)).normalize()
    }

    private fun xOnly(point: ECPoint): ByteArray =
        Numeric.toBytesPadded(point.normalize().affineXCoord.toBigInteger(), 32)

    private fun hasEvenY(point: ECPoint): Boolean =
        !point.normalize().affineYCoord.toBigInteger().testBit(0)

    private fun ByteArray.xor(other: ByteArray): ByteArray =
        ByteArray(size) { index -> this[index].toInt().xor(other[index].toInt()).toByte() }

    private fun randomBytes(): ByteArray = ByteArray(32).also(secureRandom::nextBytes)
}
