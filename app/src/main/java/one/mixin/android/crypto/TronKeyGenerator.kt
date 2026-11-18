package one.mixin.android.crypto

import blockchain.Blockchain
import one.mixin.android.extension.hexString
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.bip44.generateBip44Key
import org.bitcoinj.base.Base58
import org.bitcoinj.base.exceptions.AddressFormatException
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric

object TronKeyGenerator {
    private const val ADDRESS_PREFIX = 0x41
    private val hexPrivateKey = Regex("^(?:0[xX])?[0-9a-fA-F]{64}$")

    fun getPrivateKeyFromMnemonic(mnemonic: String, passphrase: String = "", index: Int = 0): ByteArray {
        val words = mnemonic.split(" ")
        MnemonicCode.INSTANCE.check(words)
        return getPrivateKeyFromSeed(MnemonicCode.toSeed(words, passphrase), index)
    }

    fun getPrivateKeyFromSeed(seed: ByteArray, index: Int = 0): ByteArray {
        require(seed.size in 16..64) { "Tron seed must be between 16 and 64 bytes" }
        val keyPair = generateBip44Key(Bip32ECKeyPair.generateKeyPair(seed), Bip44Path.tron(index))
        return Numeric.toBytesPadded(keyPair.privateKey, 32)
    }

    fun parsePrivateKey(privateKey: String): ByteArray {
        require(hexPrivateKey.matches(privateKey)) { "Invalid Tron private key encoding" }
        return Numeric.hexStringToByteArray(privateKey.removePrefix("0x").removePrefix("0X"))
            .also(::validateSecp256k1PrivateKey)
    }

    fun privateKeyToAddress(privateKey: ByteArray): String {
        validateSecp256k1PrivateKey(privateKey)
        val address = Keys.getAddress(ECKeyPair.create(privateKey).publicKey)
        return Base58.encodeChecked(ADDRESS_PREFIX, Numeric.hexStringToByteArray(address))
    }

    fun isAddressValid(address: String): Boolean {
        return try {
            val decoded = Base58.decodeChecked(address)
            decoded.size == 21 && decoded[0].toInt() and 0xff == ADDRESS_PREFIX
        } catch (e: AddressFormatException) {
            false
        }
    }

    internal fun deriveFromTipSeed(seed: ByteArray, index: Int): TipDerivedKey {
        val privateKey = getPrivateKeyFromSeed(seed, index)
        val address = privateKeyToAddress(privateKey)
        val expectedAddress = Blockchain.generateTronAddress(seed.hexString(), Bip44Path.tronPathString(index))
        check(address == expectedAddress) { "Tron address mismatch" }
        return TipDerivedKey(privateKey, address)
    }
}
