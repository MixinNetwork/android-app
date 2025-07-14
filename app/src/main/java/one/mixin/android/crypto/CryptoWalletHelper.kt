package one.mixin.android.crypto

import blockchain.Blockchain
import one.mixin.android.util.encodeToBase58String
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric

object CryptoWalletHelper {

    private const val ETH_DERIVATION_PATH = "m/44'/60'/0'/0/0"
    private const val SOL_DERIVATION_PATH = "m/44'/501'/0'/0'"

    // Complete process: mnemonic -> private key -> address
    fun mnemonicToEthereumWallet(mnemonic: String, passphrase: String = "", index: Int = 0): EthereumWallet {
        try {
            val privateKey = EthKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index) ?: throw IllegalArgumentException()
            val address = EthKeyGenerator.privateKeyToAddress(privateKey)
            val addressFromGo = Blockchain.generateEvmAddressFromMnemonic(mnemonic, "m/44'/60'/0'/0/$index")
            assert(addressFromGo == address) { "Address mismatch: $addressFromGo != $address" }
            return EthereumWallet(
                mnemonic = mnemonic,
                privateKey = Numeric.toHexString(privateKey),
                address = address,
                index = index
            )

        } catch (e: Exception) {
            throw RuntimeException("Wallet generation failed: ${e.message}", e)
        }
    }

    // Complete process: mnemonic -> private key -> address for Solana
    fun mnemonicToSolanaWallet(mnemonic: String, passphrase: String = "", index: Int = 0): SolanaWallet {
        try {
            val privateKey = SolanaKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
            val keyPair = newKeyPairFromSeed(privateKey)
            val address = keyPair.publicKey.encodeToBase58String()
            val addressFromGo = Blockchain.generateSolanaAddressFromMnemonic(mnemonic, "m/44'/501'/$index'/0'")
            assert(addressFromGo.lowercase() == address.lowercase()) { "Address mismatch: $addressFromGo != $address" }

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
