package one.mixin.android.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import blockchain.Blockchain
import one.mixin.android.Constants
import one.mixin.android.Constants.Tip.ENCRYPTED_WEB3_KEY
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.remove
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.util.encodeToBase58String
import one.mixin.android.web3.js.JsSigner
import org.bitcoinj.crypto.MnemonicCode
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.utils.Numeric
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoWalletHelper {

    fun extractIndexFromPath(path: String): Int? {
        return try {
            val parts = path.removePrefix("m/").split("/")

            when {
                parts.size >= 5 && parts[1] == "60'" -> {
                    parts[4].toIntOrNull()
                }
                parts.size >= 4 && parts[1] == "501'" -> {
                    parts[2].removeSuffix("'").toIntOrNull()
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract index from path: $path")
            null
        }
    }

    fun isEthereumPath(path: String): Boolean {
        return path.contains("/60'/")
    }

    fun isSolanaPath(path: String): Boolean {
        return path.contains("/501'/")
    }

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

    suspend fun getWeb3Mnemonic(context: Context, spendKey: ByteArray, walletId: String): String? {
        return try {
            val encryptedPrefs = runCatching {
                EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_WEB3_KEY,
                    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }.onFailure {
                context.deleteSharedPreferences(ENCRYPTED_WEB3_KEY)
            }.getOrNull()

            val encryptedString = encryptedPrefs?.getString(walletId, null)
            if (encryptedString == null) {
                return null
            }
            CryptoWalletHelper.decryptMnemonicWithSpendKey(spendKey, encryptedString).joinToString(" ")
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt mnemonic for walletId: $walletId")
            null
        }
    }

    suspend fun getWeb3PrivateKey(context: Context, spendKey: ByteArray, chainId: String): ByteArray? {
        return try {
            val walletId = JsSigner.currentWalletId
            if (walletId == JsSigner.classicWalletId || walletId.isEmpty()) {
                return tipPrivToPrivateKey(spendKey, chainId)
            }
            val index = requireNotNull(extractIndexFromPath(JsSigner.path))
            val decryptedMnemonic = getWeb3Mnemonic(context, spendKey, walletId) ?: return null

            val privateKey = if (chainId == Constants.ChainId.SOLANA_CHAIN_ID) {
                CryptoWalletHelper.mnemonicToSolanaWallet(decryptedMnemonic, index = index).privateKey
            } else {
                CryptoWalletHelper.mnemonicToEthereumWallet(decryptedMnemonic, index = index).privateKey
            }
            privateKey.let {
                Numeric.hexStringToByteArray(it)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get web3 private key for chainId: $chainId")
            null
        }
    }

    fun removePrivate(context: Context, walletId: String) {
        val encryptedPrefs = runCatching {
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_WEB3_KEY,
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.onFailure {
            context.deleteSharedPreferences(ENCRYPTED_WEB3_KEY)
        }.getOrNull()
        encryptedPrefs?.remove(walletId)
    }
}
