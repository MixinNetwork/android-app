package one.mixin.android.crypto

import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode
import org.sol4k.Keypair
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric

object CryptoWalletHelper {

    private const val ETH_DERIVATION_PATH = "m/44'/60'/0'/0/0"
    private const val SOL_DERIVATION_PATH = "m/44'/501'/0'/0'"

    private fun mnemonicToSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        val mnemonicWords = mnemonic.split(" ")

        MnemonicCode.INSTANCE.check(mnemonicWords)

        return MnemonicCode.toSeed(mnemonicWords, passphrase)
    }

    private fun deriveEthereumPrivateKeyAtIndex(masterKey: DeterministicKey, index: Int): ByteArray {
        val derivationPath = listOf(
            ChildNumber(44, true),   // m/44'
            ChildNumber(60, true),   // m/44'/60'
            ChildNumber(0, true),    // m/44'/60'/0'
            ChildNumber(0, false),   // m/44'/60'/0'/0
            ChildNumber(index, false)    // m/44'/60'/0'/0/0
        )

        var key = masterKey
        for (childNumber in derivationPath) {
            key = HDKeyDerivation.deriveChildKey(key, childNumber)
        }

        return key.privKeyBytes
    }

    private fun deriveSolPrivateKeyAtIndex(masterKey: DeterministicKey, index: Int): ByteArray {
        val derivationPath = listOf(
            ChildNumber(44, true),   // m/44'
            ChildNumber(501, true),  // m/44'/501'
            ChildNumber(index, true),    // m/44'/501'/0'
            ChildNumber(0, true) // m/44'/501'/0'/index'
        )

        var key = masterKey
        for (childNumber in derivationPath) {
            key = HDKeyDerivation.deriveChildKey(key, childNumber)
        }

        return key.privKeyBytes
    }

    private fun privateKeyToAddress(privateKey: ByteArray): String {
        val ecKeyPair = ECKeyPair.create(privateKey)
        return Keys.getAddress(ecKeyPair)
    }

    // Complete process: mnemonic -> private key -> address
    fun mnemonicToEthereumWallet(mnemonic: String, passphrase: String = "", index: Int = 0): EthereumWallet {
        try {
            val seed = mnemonicToSeed(mnemonic, passphrase)
            val masterKey = HDKeyDerivation.createMasterPrivateKey(seed)
            val privateKey = deriveEthereumPrivateKeyAtIndex(masterKey, index)
            val address = privateKeyToAddress(privateKey)

            return EthereumWallet(
                mnemonic = mnemonic,
                privateKey = Numeric.toHexString(privateKey),
                address = "0x$address",
                index = index
            )

        } catch (e: Exception) {
            throw RuntimeException("Wallet generation failed: ${e.message}", e)
        }
    }

    // Complete process: mnemonic -> private key -> address for Solana
    fun mnemonicToSolanaWallet(mnemonic: String, passphrase: String = "", index: Int = 0): SolanaWallet {
        try {
            val seed = mnemonicToSeed(mnemonic, passphrase)
            val masterKey = HDKeyDerivation.createMasterPrivateKey(seed)
            val privateKey = deriveSolPrivateKeyAtIndex(masterKey, index)
            val address = Keypair.fromSecretKey(privateKey).publicKey.toBase58()

            return SolanaWallet(
                mnemonic = mnemonic,
                privateKey = Numeric.toHexString(privateKey),
                address = address,
                index = index
            )

        } catch (e: Exception) {
            throw RuntimeException("Solana wallet generation failed: ${e.message}", e)
        }
    }
}

data class EthereumWallet(
    val mnemonic: String,
    val privateKey: String,
    val address: String,
    val index: Int = 0
)

data class SolanaWallet(
    val mnemonic: String,
    val privateKey: String,
    val address: String,
    val index: Int = 0
)
