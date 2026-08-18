package one.mixin.android.crypto

import blockchain.Blockchain
import one.mixin.android.extension.hexString
import one.mixin.android.tip.bip44.Bip44Path
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.base.Network
import org.bitcoinj.base.SegwitAddress
import org.bitcoinj.crypto.ECKey
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.Bip32ECKeyPair.HARDENED_BIT
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object PearlKeyGenerator {
    private const val MIN_SEED_SIZE = 16
    private const val MAX_SEED_SIZE = 64
    private const val PEARL_HRP = "prl"
    private const val TAP_TWEAK_TAG = "TapTweak"
    private val curve = ECKey.ecDomainParameters()
    private val curveOrder = curve.n

    private data class ExtendedPrivateKey(
        val key: ByteArray,
        val chainCode: ByteArray,
    )

    private object PearlNetwork : Network by BitcoinNetwork.MAINNET {
        override fun id(): String = "org.pearl.production"

        override fun segwitAddressHrp(): String = PEARL_HRP

        override fun uriScheme(): String = "pearl"
    }

    fun getPrivateKeyFromMnemonic(mnemonic: String, passphrase: String = "", index: Int = 0): ByteArray {
        return getPrivateKeyFromSeed(mnemonicToSeed(mnemonic, passphrase), index)
    }

    fun getPrivateKeyFromSeed(seed: ByteArray, index: Int = 0): ByteArray {
        var key = masterKey(seed)
        val path = Bip44Path.pearl(index)
        path.dropLast(1).forEach { childIndex ->
            key = deriveNextValidChild(key, childIndex)
        }

        val privateKey = deriveNextValidChild(key, path.last()).key.toPositiveBigInteger()
        return Numeric.toBytesPadded(privateKey, 32)
    }

    fun privateKeyToAddress(privateKey: ByteArray): String {
        return SegwitAddress.fromProgram(PearlNetwork, 1, taprootOutputKey(privateKey)).toString()
    }

    fun seedToAddress(seed: ByteArray, index: Int = 0): String {
        val address = privateKeyToAddress(getPrivateKeyFromSeed(seed, index))
        val aarAddress = Blockchain.generatePearlAddressAtIndex(seed.hexString(), index.toLong())
        check(address == aarAddress) { "Pearl address mismatch: $aarAddress != $address" }
        return address
    }

    fun mnemonicToAddress(mnemonic: String, passphrase: String = "", index: Int = 0): String {
        return seedToAddress(mnemonicToSeed(mnemonic, passphrase), index)
    }

    fun isAddressValid(address: String): Boolean {
        return runCatching { parseAddress(address) }.isSuccess
    }

    internal fun parseAddress(address: String): SegwitAddress {
        val parsedAddress = SegwitAddress.fromBech32(address, PearlNetwork)
        require(
            parsedAddress.witnessVersion == 1 &&
                parsedAddress.witnessProgram.size == SegwitAddress.WITNESS_PROGRAM_LENGTH_TR,
        ) { "Invalid Pearl address" }
        return parsedAddress
    }

    internal fun taprootOutputKey(privateKey: ByteArray): ByteArray {
        val privateKeyValue = validatePrivateKey(privateKey)
        val internalKey = evenYPublicKey(privateKeyValue)
        val xOnlyInternalKey = Numeric.toBytesPadded(internalKey.affineXCoord.toBigInteger(), 32)
        val outputKey = internalKey.add(curve.g.multiply(tapTweak(xOnlyInternalKey))).normalize()
        require(!outputKey.isInfinity) { "Invalid Pearl Taproot output key" }
        return Numeric.toBytesPadded(outputKey.affineXCoord.toBigInteger(), 32)
    }

    internal fun taprootTweakedPrivateKey(privateKey: ByteArray): ByteArray {
        val privateKeyValue = validatePrivateKey(privateKey)
        val publicKey = ECKey.fromPrivate(privateKeyValue, true).pubKeyPoint.normalize()
        val internalPrivateKey = if (publicKey.affineYCoord.toBigInteger().testBit(0)) {
            curveOrder.subtract(privateKeyValue)
        } else {
            privateKeyValue
        }
        val xOnlyInternalKey = Numeric.toBytesPadded(publicKey.affineXCoord.toBigInteger(), 32)
        val tweakedPrivateKey = internalPrivateKey.add(tapTweak(xOnlyInternalKey)).mod(curveOrder)
        require(tweakedPrivateKey.signum() > 0) { "Invalid Pearl Taproot private key" }
        return Numeric.toBytesPadded(tweakedPrivateKey, 32)
    }

    private fun validatePrivateKey(privateKey: ByteArray): BigInteger {
        val privateKeyValue = privateKey.toPositiveBigInteger()
        require(privateKeyValue.signum() > 0 && privateKeyValue < curveOrder) {
            "Invalid Pearl private key"
        }
        return privateKeyValue
    }

    private fun evenYPublicKey(privateKeyValue: BigInteger) =
        ECKey.fromPrivate(privateKeyValue, true).pubKeyPoint.normalize().let { publicKey ->
            if (publicKey.affineYCoord.toBigInteger().testBit(0)) publicKey.negate().normalize() else publicKey
        }

    private fun tapTweak(xOnlyInternalKey: ByteArray): BigInteger {
        val tagHash = sha256(TAP_TWEAK_TAG.toByteArray(StandardCharsets.UTF_8))
        val tweak = BigInteger(1, sha256(tagHash + tagHash + xOnlyInternalKey))
        require(tweak < curveOrder) { "Invalid Pearl Taproot tweak" }
        return tweak
    }

    private fun mnemonicToSeed(mnemonic: String, passphrase: String): ByteArray {
        val mnemonicWords = mnemonic.split(" ")
        MnemonicCode.INSTANCE.check(mnemonicWords)
        return MnemonicCode.toSeed(mnemonicWords, passphrase)
    }

    private fun masterKey(seed: ByteArray): ExtendedPrivateKey {
        require(seed.size in MIN_SEED_SIZE..MAX_SEED_SIZE) { "Invalid Pearl seed length" }
        val keyMaterial = hmacSha512("Bitcoin seed".toByteArray(StandardCharsets.UTF_8), seed)
        val key = keyMaterial.copyOfRange(0, 32)
        val keyValue = key.toPositiveBigInteger()
        require(keyValue.signum() > 0 && keyValue < curveOrder) { "Invalid Pearl seed" }
        return ExtendedPrivateKey(key, keyMaterial.copyOfRange(32, 64))
    }

    private fun deriveNonStandard(parent: ExtendedPrivateKey, childIndex: Int): ExtendedPrivateKey? {
        val data = ByteArray(37)
        if (childIndex and HARDENED_BIT != 0) {
            // Pearl preserves btcsuite's legacy left-aligned hardened derivation behavior.
            parent.key.copyInto(data, destinationOffset = 1)
        } else {
            val publicKey = ECKey.fromPrivate(parent.key.toPositiveBigInteger(), true).pubKey
            publicKey.copyInto(data)
        }
        data[33] = (childIndex ushr 24).toByte()
        data[34] = (childIndex ushr 16).toByte()
        data[35] = (childIndex ushr 8).toByte()
        data[36] = childIndex.toByte()

        val keyMaterial = hmacSha512(parent.chainCode, data)
        val leftValue = keyMaterial.copyOfRange(0, 32).toPositiveBigInteger()
        val childValue = deriveChildPrivateKeyValue(parent.key.toPositiveBigInteger(), leftValue)
            ?: return null
        return ExtendedPrivateKey(
            key = childValue.toMinimalUnsignedByteArray(),
            chainCode = keyMaterial.copyOfRange(32, 64),
        )
    }

    internal fun deriveChildPrivateKeyValue(parentValue: BigInteger, leftValue: BigInteger): BigInteger? {
        if (leftValue.signum() == 0 || leftValue >= curveOrder) {
            return null
        }
        return leftValue.add(parentValue).mod(curveOrder).takeIf { it.signum() > 0 }
    }

    private fun deriveNextValidChild(parent: ExtendedPrivateKey, initialChildIndex: Int): ExtendedPrivateKey {
        var childIndex = initialChildIndex
        while (true) {
            deriveNonStandard(parent, childIndex)?.let { return it }
            require(childIndex != -1) { "Invalid Pearl child index" }
            childIndex++
        }
    }

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        return Mac.getInstance("HmacSHA512").run {
            init(SecretKeySpec(key, "HmacSHA512"))
            doFinal(data)
        }
    }

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    private fun ByteArray.toPositiveBigInteger(): BigInteger {
        return if (isEmpty()) BigInteger.ZERO else BigInteger(1, this)
    }

    private fun BigInteger.toMinimalUnsignedByteArray(): ByteArray {
        if (signum() == 0) return ByteArray(0)
        val bytes = toByteArray()
        return if (bytes.first() == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
    }
}
