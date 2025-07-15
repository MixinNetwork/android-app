package one.mixin.android.crypto

import blockchain.Blockchain
import one.mixin.android.util.encodeToBase58String
import org.web3j.utils.Numeric

object CryptoWalletHelper {

    fun mnemonicToEthereumWallet(mnemonic: String, passphrase: String = "", index: Int = 0): CryptoWallet {
        try {
            val path = "m/44'/60'/0'/0/$index"
            val privateKey = EthKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
                ?: throw IllegalArgumentException("Private key generation failed")
            val address = EthKeyGenerator.privateKeyToAddress(privateKey)
            val addressFromGo = Blockchain.generateEvmAddressFromMnemonic(mnemonic, path)
            assert(addressFromGo.equals(address, ignoreCase = true)) { "Address mismatch: $addressFromGo != $address" }
            return CryptoWallet(
                mnemonic = mnemonic,
                privateKey = Numeric.toHexString(privateKey),
                address = address,
                path = path
            )
        } catch (e: Exception) {
            throw RuntimeException("Ethereum wallet generation failed: ${e.message}", e)
        }
    }

    fun mnemonicToSolanaWallet(mnemonic: String, passphrase: String = "", index: Int = 0): CryptoWallet {
        try {
            val path = "m/44'/501'/$index'/0'"
            val privateKey = SolanaKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
            val keyPair = newKeyPairFromSeed(privateKey)
            val address = keyPair.publicKey.encodeToBase58String()
            val addressFromGo = Blockchain.generateSolanaAddressFromMnemonic(mnemonic, path)
            assert(addressFromGo.equals(address, ignoreCase = true)) { "Address mismatch: $addressFromGo != $address" }

            return CryptoWallet(
                mnemonic = mnemonic,
                privateKey = Numeric.toHexString(privateKey),
                address = address,
                path = path
            )
        } catch (e: Exception) {
            throw RuntimeException("Solana wallet generation failed: ${e.message}", e)
        }
    }
}
