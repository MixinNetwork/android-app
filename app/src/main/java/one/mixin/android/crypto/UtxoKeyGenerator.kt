package one.mixin.android.crypto

import blockchain.Blockchain
import one.mixin.android.Constants
import one.mixin.android.extension.hexString
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.bip44.generateBip44Key
import org.bitcoinj.base.Address
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.base.ScriptType
import org.bitcoinj.base.SegwitAddress
import org.bitcoinj.crypto.DumpedPrivateKey
import org.bitcoinj.crypto.ECKey
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object UtxoKeyGenerator {
    private const val MIN_PEARL_SEED_SIZE = 16
    private const val MAX_PEARL_SEED_SIZE = 64
    private const val TAP_TWEAK_TAG = "TapTweak"
    private val curve = ECKey.ecDomainParameters()
    private val curveOrder = curve.n

    fun getPrivateKeyFromMnemonic(
        mnemonic: String,
        chainId: String,
        passphrase: String = "",
        index: Int = 0,
    ): ByteArray {
        return getPrivateKeyFromSeed(mnemonicToSeed(mnemonic, passphrase), chainId, index)
    }

    fun getPrivateKeyFromSeed(
        seed: ByteArray,
        chainId: String,
        index: Int = 0,
    ): ByteArray =
        when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> deriveBitcoinPrivateKey(seed, index)
            Constants.ChainId.PEARL_CHAIN_ID -> derivePearlPrivateKey(seed, index)
            Constants.ChainId.Litecoin -> deriveLitecoinPrivateKey(seed, index)
            else -> unsupportedChain(chainId)
        }

    fun privateKeyToAddress(
        privateKey: ByteArray,
        chainId: String,
    ): String =
        when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> {
                val ecKey = ECKey.fromPrivate(BigInteger(1, privateKey), true)
                ecKey.toAddress(ScriptType.P2WPKH, BitcoinNetwork.MAINNET).toString()
            }
            Constants.ChainId.PEARL_CHAIN_ID -> {
                SegwitAddress.fromProgram(PearlNetwork, 1, taprootOutputKey(privateKey)).toString()
            }
            Constants.ChainId.Litecoin -> {
                validateSecp256k1PrivateKey(privateKey)
                val ecKey = ECKey.fromPrivate(BigInteger(1, privateKey), true)
                ecKey.toAddress(ScriptType.P2WPKH, LitecoinNetwork).toString()
            }
            else -> unsupportedChain(chainId)
        }

    fun privateKeyToAddress(
        privateKey: String,
        chainId: String,
    ): String {
        return when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> {
                val dumpedPrivateKey = runCatching {
                    DumpedPrivateKey.fromBase58(BitcoinNetwork.MAINNET, privateKey)
                }.getOrNull()
                dumpedPrivateKey?.key
                    ?.toAddress(ScriptType.P2WPKH, BitcoinNetwork.MAINNET)
                    ?.toString()
                    ?: privateKeyToAddress(Numeric.hexStringToByteArray(privateKey), chainId)
            }
            Constants.ChainId.PEARL_CHAIN_ID -> {
                val dumpedPrivateKey = runCatching {
                    DumpedPrivateKey.fromBase58(BitcoinNetwork.MAINNET, privateKey)
                }.getOrNull()
                val privateKeyBytes = dumpedPrivateKey?.key?.privKeyBytes
                    ?: Numeric.hexStringToByteArray(privateKey)
                privateKeyToAddress(privateKeyBytes, chainId)
            }
            Constants.ChainId.Litecoin -> {
                val privateKeyBytes = parseLitecoinPrivateKey(privateKey)
                privateKeyToAddress(privateKeyBytes, chainId)
            }
            else -> unsupportedChain(chainId)
        }
    }

    fun parseLitecoinPrivateKey(privateKey: String): ByteArray {
        val bytes = if (litecoinHexPrivateKey.matches(privateKey)) {
            Numeric.hexStringToByteArray(privateKey.removePrefix("0x").removePrefix("0X"))
        } else {
            UtxoChainConfig.forChain(Constants.ChainId.Litecoin).wif.decode(privateKey)
        }
        validateSecp256k1PrivateKey(bytes)
        return bytes
    }

    fun mnemonicToAddress(
        mnemonic: String,
        chainId: String,
        passphrase: String = "",
        index: Int = 0,
    ): String {
        val seed = mnemonicToSeed(mnemonic, passphrase)
        return when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> {
                privateKeyToAddress(deriveBitcoinPrivateKey(seed, index), chainId)
            }
            Constants.ChainId.PEARL_CHAIN_ID -> derivePearlFromSeed(seed, index).address
            Constants.ChainId.Litecoin -> privateKeyToAddress(deriveLitecoinPrivateKey(seed, index), chainId)
            else -> unsupportedChain(chainId)
        }
    }

    fun isAddressValid(
        address: String,
        chainId: String,
    ): Boolean = runCatching { parseAddress(address, chainId) }.isSuccess

    internal fun deriveFromTipSeed(
        seed: ByteArray,
        chainId: String,
        index: Int,
    ): TipDerivedKey =
        when (chainId) {
            Constants.ChainId.BITCOIN_CHAIN_ID -> deriveBitcoinFromTipSeed(seed, index)
            Constants.ChainId.PEARL_CHAIN_ID -> derivePearlFromSeed(seed, index)
            Constants.ChainId.Litecoin -> deriveLitecoinFromTipSeed(seed, index)
            else -> unsupportedChain(chainId)
        }

    internal fun parseAddress(
        address: String,
        chainId: String,
    ): Address =
        UtxoChainConfig.forChain(chainId).parseAddress(address)

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

    private fun deriveBitcoinFromTipSeed(seed: ByteArray, index: Int): TipDerivedKey {
        val privateKey = deriveBitcoinPrivateKey(seed, index)
        val address = privateKeyToAddress(privateKey, Constants.ChainId.BITCOIN_CHAIN_ID)
        val expectedAddress = Blockchain.generateBitcoinSegwitAddress(
            seed.hexString(),
            Bip44Path.bitcoinSegwitPathString(index),
        )
        if (expectedAddress != address) {
            throw IllegalArgumentException("Generate illegal Bitcoin SegWit Address")
        }
        return TipDerivedKey(privateKey, address)
    }

    private fun derivePearlFromSeed(seed: ByteArray, index: Int): TipDerivedKey {
        val privateKey = derivePearlPrivateKey(seed, index)
        val address = privateKeyToAddress(privateKey, Constants.ChainId.PEARL_CHAIN_ID)
        val privateKeyAddressFromGo = Blockchain.generatePearlAddressFromPrivateKey(privateKey.hexString())
        check(address == privateKeyAddressFromGo) {
            "Pearl private key address mismatch: $privateKeyAddressFromGo != $address"
        }
        val seedAddressFromGo = Blockchain.generatePearlAddress(
            seed.hexString(),
            Bip44Path.pearlPathString(index),
        )
        check(address == seedAddressFromGo) { "Pearl seed address mismatch: $seedAddressFromGo != $address" }
        return TipDerivedKey(privateKey, address)
    }

    private fun deriveLitecoinFromTipSeed(seed: ByteArray, index: Int): TipDerivedKey {
        val privateKey = deriveLitecoinPrivateKey(seed, index)
        val address = privateKeyToAddress(privateKey, Constants.ChainId.Litecoin)
        val privateKeyAddressFromGo = Blockchain.generateLitecoinAddressFromPrivateKey(privateKey.hexString())
        check(address == privateKeyAddressFromGo) { "Litecoin private key address mismatch" }
        val seedAddressFromGo = Blockchain.generateLitecoinAddress(seed.hexString(), Bip44Path.litecoinSegwitPathString(index))
        check(address == seedAddressFromGo) { "Litecoin seed address mismatch" }
        return TipDerivedKey(privateKey, address)
    }

    private fun deriveBitcoinPrivateKey(seed: ByteArray, index: Int): ByteArray {
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        val bip84KeyPair = generateBip44Key(masterKeyPair, Bip44Path.bitcoinSegwit(index))
        return Numeric.toBytesPadded(bip84KeyPair.privateKey, 32)
    }

    private fun derivePearlPrivateKey(seed: ByteArray, index: Int): ByteArray {
        require(seed.size in MIN_PEARL_SEED_SIZE..MAX_PEARL_SEED_SIZE) { "Invalid Pearl seed length" }
        require(index >= 0) { "Invalid Pearl address index" }
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        val keyPair = generateBip44Key(masterKeyPair, Bip44Path.pearl(index))
        return Numeric.toBytesPadded(keyPair.privateKey, 32)
    }

    private fun deriveLitecoinPrivateKey(seed: ByteArray, index: Int): ByteArray {
        require(index >= 0) { "Invalid Litecoin address index" }
        val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
        val keyPair = generateBip44Key(masterKeyPair, Bip44Path.litecoinSegwit(index))
        return Numeric.toBytesPadded(keyPair.privateKey, 32)
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

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    private fun unsupportedChain(chainId: String): Nothing {
        throw IllegalArgumentException("Unsupported UTXO chain: $chainId")
    }

    private fun ByteArray.toPositiveBigInteger(): BigInteger {
        return if (isEmpty()) BigInteger.ZERO else BigInteger(1, this)
    }

    private val litecoinHexPrivateKey = Regex("^(?:0[xX])?[0-9a-fA-F]{64}$")
}
