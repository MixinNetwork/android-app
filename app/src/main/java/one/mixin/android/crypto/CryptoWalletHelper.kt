package one.mixin.android.crypto

import blockchain.Blockchain
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.util.encodeToBase58String
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.utils.Numeric
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

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

    fun encryptMnemonicWithSpendKey(spendKey: ByteArray, mnemonicWords: List<String>): String {
        val encryptionKeyPair = Bip32ECKeyPair.generateKeyPair(spendKey)
        val encryptionKeySource = encryptionKeyPair.privateKey.toByteArray()
        val sha256 = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = sha256.digest(encryptionKeySource)
        val secretKey = SecretKeySpec(aesKeyBytes, "AES")

        val originalEntropy = toEntropy(mnemonicWords)
        val initializationVector = ByteArray(16)
        SecureRandom().nextBytes(initializationVector)
        val ivParameterSpec = IvParameterSpec(initializationVector)

        val encryptionCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        encryptionCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        val encryptedEntropy = encryptionCipher.doFinal(originalEntropy)

        val encryptedDataWithIv = initializationVector + encryptedEntropy
        return encryptedDataWithIv.base64RawURLEncode()
    }

    fun decryptMnemonicWithSpendKey(
        spendKey: ByteArray,
        base64EncryptedData: String
    ): List<String> {
        val encryptionKeyPair = Bip32ECKeyPair.generateKeyPair(spendKey)
        val encryptionKeySource = encryptionKeyPair.privateKey.toByteArray()
        val sha256 = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = sha256.digest(encryptionKeySource)
        val secretKey = SecretKeySpec(aesKeyBytes, "AES")

        val decodedEncryptedData = base64EncryptedData.base64RawURLDecode()
        val extractedIv = decodedEncryptedData.copyOfRange(0, 16)
        val extractedEncryptedEntropy =
            decodedEncryptedData.copyOfRange(16, decodedEncryptedData.size)

        val decryptionCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        decryptionCipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(extractedIv))
        val decryptedEntropy = decryptionCipher.doFinal(extractedEncryptedEntropy)
        return MnemonicCode.INSTANCE.toMnemonic(decryptedEntropy)
    }
}
