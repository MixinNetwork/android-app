package one.mixin.android.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import blockchain.Blockchain
import one.mixin.android.Constants
import one.mixin.android.Constants.Tip.ENCRYPTED_WEB3_KEY
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.util.decodeBase58
import one.mixin.android.util.encodeToBase58String
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.JsSigner
import org.bitcoinj.crypto.MnemonicCode
import org.sol4k.Base58
import org.sol4k.Keypair.Companion.fromSecretKey
import org.web3j.utils.Numeric
import timber.log.Timber
import java.security.MessageDigest

object CryptoWalletHelper {

    fun getSecureStorage(context: Context): SharedPreferences? {
        return runCatching {
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
    }

    fun hasPrivateKey(context: Context, walletId: String): Boolean {
        return getSecureStorage(context)?.contains(walletId) ?: false
    }

    fun extractIndexFromPath(path: String): Int? {
        return try {
            when {
                // Ethereum path: m/44'/60'/0'/0/{index}
                path.startsWith("m/44'/60'/") -> {
                    path.removePrefix("m/44'/60'/0'/0/").toIntOrNull()
                }
                // Solana path: m/44'/501'/{index}'/0'
                path.startsWith("m/44'/501'/") -> {
                    path.removePrefix("m/44'/501'/")
                        .removeSuffix("'/0'")
                        .toIntOrNull()
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract index from path: $path")
            null
        }
    }

    fun mnemonicToEthereumWallet(mnemonic: String, passphrase: String = "", index: Int = 0): CryptoWallet {
        try {
            val path = Bip44Path.ethereumPathString(index)
            val privateKey = EthKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
                ?: throw IllegalArgumentException("Private key generation failed")
            val address = EthKeyGenerator.privateKeyToAddress(privateKey)
            val addressFromGo = Blockchain.generateEvmAddressFromMnemonic(mnemonic, path)
            assert(addressFromGo.equals(address)) { "Address mismatch: $addressFromGo != $address" }
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
            val path = Bip44Path.solanaPathString(index)
            val privateKey = SolanaKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
            val keyPair = newKeyPairFromSeed(privateKey)
            val address = keyPair.publicKey.encodeToBase58String()
            val addressFromGo = Blockchain.generateSolanaAddressFromMnemonic(mnemonic, path)
            assert(addressFromGo.equals(address)) { "Address mismatch: $addressFromGo != $address" }

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

    fun privateKeyToAddress(privateKey: String, chainId: String): String {
        return if (chainId == Constants.ChainId.SOLANA_CHAIN_ID) {
            val privateKeyBytes = Base58.decode(privateKey)
            val keyPair = fromSecretKey(privateKeyBytes)
            keyPair.publicKey.toBase58()
        } else {
            val privateKeyBytes = Numeric.hexStringToByteArray(privateKey)
            EthKeyGenerator.privateKeyToAddress(privateKeyBytes)
        }
    }

    fun mnemonicToAddress(
        mnemonic: String,
        chainId: String,
        passphrase: String = "",
        index: Int = 0
    ): String {
        return if (chainId == Constants.ChainId.SOLANA_CHAIN_ID) {
            val keyPair = SolanaKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
            newKeyPairFromSeed(keyPair).publicKey.encodeToBase58String()
        } else {
            val privateKey = EthKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
                ?: throw IllegalArgumentException("Private key generation failed")
            EthKeyGenerator.privateKeyToAddress(privateKey)
        }
    }

    fun encryptPrivateKeyWithSpendKey(spendKey: ByteArray, privateKey: String): String {
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = sha256Digest.digest(spendKey)
        val privateKeyBytes = privateKey.toByteArray()
        return aesGcmEncrypt(privateKeyBytes, aesKeyBytes).base64RawURLEncode()
    }

    fun encryptMnemonicWithSpendKey(spendKey: ByteArray, mnemonicWords: List<String>): String {
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = sha256Digest.digest(spendKey)
        val originalEntropy = toEntropy(mnemonicWords)
        return aesGcmEncrypt(originalEntropy, aesKeyBytes).base64RawURLEncode()
    }

    fun decryptMnemonicWithSpendKey(
        spendKey: ByteArray,
        base64EncryptedData: String
    ): List<String> {
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = sha256Digest.digest(spendKey)
        val decodedEncryptedData = base64EncryptedData.base64RawURLDecode()
        val decryptedEntropy = aesGcmDecrypt(decodedEncryptedData, aesKeyBytes)
        return MnemonicCode.INSTANCE.toMnemonic(decryptedEntropy)
    }

    fun getWeb3Mnemonic(context: Context, spendKey: ByteArray, walletId: String): String? {
        return try {
            val encryptedPrefs = getSecureStorage(context)
            val encryptedString = encryptedPrefs?.getString(walletId, null)
            if (encryptedString == null) {
                return null
            }
            decryptMnemonicWithSpendKey(spendKey, encryptedString).joinToString(" ")
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt mnemonic for walletId: $walletId")
            null
        }
    }

    fun getWeb3PrivateKey(
        context: Context,
        spendKey: ByteArray,
        walletId: String,
        chainId: String
    ): ByteArray? {
        return try {
            val encryptedPrefs = getSecureStorage(context)
            val encryptedString = encryptedPrefs?.getString(walletId, null) ?: return null
            val sha256Digest = MessageDigest.getInstance("SHA-256")
            val aesKeyBytes = sha256Digest.digest(spendKey)
            val encryptedBytes = encryptedString.base64RawURLDecode()
            val privateKeyStr = String(aesGcmDecrypt(encryptedBytes, aesKeyBytes))
            if (chainId == Constants.ChainId.SOLANA_CHAIN_ID) {
                privateKeyStr.decodeBase58()
            } else {
                Numeric.hexStringToByteArray(privateKeyStr)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt private key for walletId: $walletId")
            null
        }
    }

    fun getWeb3PrivateKey(context: Context, spendKey: ByteArray, chainId: String): ByteArray? {
        return try {
            val currentWalletId = JsSigner.currentWalletId
            val currentCategory = JsSigner.currentWalletCategory

            when {
                currentCategory == WalletCategory.CLASSIC.value || currentWalletId.isEmpty() -> {
                    val derivationIndex = extractIndexFromPath(JsSigner.path) ?: 0
                    Timber.d("currentWalletId: ${JsSigner.currentWalletId}, currentWalletCategory: ${JsSigner.currentWalletCategory}, evmAddress: ${JsSigner.evmAddress}, solanaAddress: ${JsSigner.solanaAddress} derivationIndex: $derivationIndex")
                    tipPrivToPrivateKey(spendKey, chainId, derivationIndex)
                }

                currentCategory == WalletCategory.IMPORTED_PRIVATE_KEY.value -> {
                    getWeb3PrivateKey(context, spendKey, currentWalletId, chainId)
                }

                else -> { // Mnemonic-derived wallet
                    val mnemonic = getWeb3Mnemonic(context, spendKey, currentWalletId)
                        ?: return null
                    val derivationIndex = requireNotNull(extractIndexFromPath(JsSigner.path))

                    if (chainId == Constants.ChainId.SOLANA_CHAIN_ID) {
                        SolanaKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, index = derivationIndex)
                    } else {
                        EthKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, index = derivationIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get web3 private key for chainId: $chainId")
            null
        }
    }

    fun saveWeb3PrivateKey(context: Context, walletId: String, encryptedString: String) {
        val secureStorage = getSecureStorage(context)
        secureStorage?.putString(walletId, encryptedString)
    }

    fun removePrivate(context: Context, walletId: String) {
        val encryptedPrefs = getSecureStorage(context)
        encryptedPrefs?.remove(walletId)
    }

    fun clear(context: Context) {
        context.deleteSharedPreferences(ENCRYPTED_WEB3_KEY)
    }
}
