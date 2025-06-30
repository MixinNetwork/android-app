package one.mixin.android.crypto

import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric

object CryptoWalletHelper {

    private const val ETH_DERIVATION_PATH = "m/44'/60'/0'/0/0"

    fun mnemonicToSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        val mnemonicWords = mnemonic.split(" ")

        MnemonicCode.INSTANCE.check(mnemonicWords)

        return MnemonicCode.toSeed(mnemonicWords, passphrase)
    }

    fun seedToMasterKey(seed: ByteArray): DeterministicKey {
        return HDKeyDerivation.createMasterPrivateKey(seed)
    }

    fun deriveEthereumPrivateKey(masterKey: DeterministicKey): ByteArray {
        // BIP44 path: m/44'/60'/0'/0/0
        val derivationPath = listOf(
            ChildNumber(44, true),   // m/44'
            ChildNumber(60, true),   // m/44'/60'
            ChildNumber(0, true),    // m/44'/60'/0'
            ChildNumber(0, false),   // m/44'/60'/0'/0
            ChildNumber(0, false)    // m/44'/60'/0'/0/0
        )

        var key = masterKey
        for (childNumber in derivationPath) {
            key = HDKeyDerivation.deriveChildKey(key, childNumber)
        }

        return key.privKeyBytes
    }

    fun privateKeyToAddress(privateKey: ByteArray): String {
        val ecKeyPair = ECKeyPair.create(privateKey)
        return Keys.getAddress(ecKeyPair)
    }

    // Complete process: mnemonic -> private key -> address
    fun mnemonicToEthereumWallet(mnemonic: String, passphrase: String = ""): EthereumWallet {
        try {
            val seed = mnemonicToSeed(mnemonic, passphrase)
            val masterKey = seedToMasterKey(seed)
            val privateKey = deriveEthereumPrivateKey(masterKey)
            val address = privateKeyToAddress(privateKey)

            return EthereumWallet(
                mnemonic = mnemonic,
                privateKey = Numeric.toHexString(privateKey),
                address = "0x$address"
            )

        } catch (e: Exception) {
            throw RuntimeException("Wallet generation failed: ${e.message}", e)
        }
    }

    fun isValidEthereumAddress(address: String): Boolean {
        return try {
            Keys.toChecksumAddress(address.removePrefix("0x"))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun privateKeyToWIF(privateKey: ByteArray): String {
        return Numeric.toHexString(privateKey)
    }
}

data class EthereumWallet(
    val mnemonic: String,
    val privateKey: String,
    val address: String
)
