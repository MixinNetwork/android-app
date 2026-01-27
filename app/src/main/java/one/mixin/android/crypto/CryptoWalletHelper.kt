package one.mixin.android.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import blockchain.Blockchain
import one.mixin.android.Constants
import one.mixin.android.Constants.Tip.ENCRYPTED_WEB3_KEY
import one.mixin.android.MixinApplication
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.base64RawURLEncode
import one.mixin.android.extension.putString
import one.mixin.android.extension.remove
import one.mixin.android.tip.bip44.Bip44Path
import one.mixin.android.tip.tipPrivToPrivateKey
import one.mixin.android.util.decodeBase58
import one.mixin.android.util.encodeToBase58String
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.Web3Signer
import org.bitcoinj.base.BitcoinNetwork
import org.bitcoinj.base.ScriptType
import org.bitcoinj.crypto.DumpedPrivateKey
import org.bitcoinj.crypto.ECKey
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.params.MainNetParams
import org.sol4k.Base58
import org.sol4k.Keypair.Companion.fromSecretKey
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
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

    private val secureStorage by lazy {
        getSecureStorage(MixinApplication.appContext)
    }

    fun hasPrivateKey(context: Context, walletId: String): Boolean {
        return secureStorage?.contains(walletId) ?: false
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
                // Bitcoin SegWit path: m/84'/0'/0'/0/{index}
                path.startsWith("m/84'/0'/") -> {
                    path.removePrefix("m/84'/0'/0'/0/").toIntOrNull()
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract index from path: $path")
            null
        }
    }

    fun mnemonicToBitcoinSegwitWallet(mnemonic: String, passphrase: String = "", index: Int = 0): CryptoWallet {
        try {
            val path: String = Bip44Path.bitcoinSegwitPathString(index)
            val privateKeyBytes: ByteArray = BitcoinKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
            val address: String = BitcoinKeyGenerator.privateKeyToAddress(privateKeyBytes)
            return CryptoWallet(
                mnemonic = mnemonic,
                privateKey = Numeric.toHexString(privateKeyBytes),
                address = address,
                path = path,
            )
        } catch (e: Exception) {
            throw RuntimeException("Bitcoin SegWit wallet generation failed: ${e.message}", e)
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
        return when (chainId) {
            Constants.ChainId.SOLANA_CHAIN_ID -> {
                val privateKeyBytes: ByteArray = Base58.decode(privateKey)
                val keyPair = fromSecretKey(privateKeyBytes)
                keyPair.publicKey.toBase58()
            }

            Constants.ChainId.BITCOIN_CHAIN_ID -> {
                val ecKey: ECKey = if (isBitcoinWifPrivateKey(privateKey)) {
                    DumpedPrivateKey.fromBase58(BitcoinNetwork.MAINNET, privateKey).key
                } else {
                    val privateKeyBytes: ByteArray = Numeric.hexStringToByteArray(privateKey)
                    ECKey.fromPrivate(BigInteger(1, privateKeyBytes), true)
                }
                val address = ecKey.toAddress(ScriptType.P2WPKH, BitcoinNetwork.MAINNET)
                address.toString()
            }

            in Constants.Web3EvmChainIds -> {
                val privateKeyBytes: ByteArray = Numeric.hexStringToByteArray(privateKey)
                EthKeyGenerator.privateKeyToAddress(privateKeyBytes)
            }

            else -> {
                throw IllegalArgumentException("Unsupported chainId: $chainId")
            }
        }
    }

    fun mnemonicToAddress(
        mnemonic: String,
        chainId: String,
        passphrase: String = "",
        index: Int = 0
    ): String {
        return when (chainId) {
            Constants.ChainId.SOLANA_CHAIN_ID -> {
                val privateKey: ByteArray = SolanaKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
                newKeyPairFromSeed(privateKey).publicKey.encodeToBase58String()
            }
            Constants.ChainId.BITCOIN_CHAIN_ID -> {
                BitcoinKeyGenerator.mnemonicToAddress(mnemonic, passphrase, index)
            }
            in Constants.Web3EvmChainIds -> {
                val privateKey: ByteArray =
                    EthKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, passphrase, index)
                        ?: throw IllegalArgumentException("Private key generation failed")
                EthKeyGenerator.privateKeyToAddress(privateKey)
            }
            else -> {
                throw IllegalArgumentException("Unsupported chainId: $chainId")
            }
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
            val encryptedString = encryptedPrefs?.getString(walletId, null) ?: return null
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
            when (chainId) {
                Constants.ChainId.SOLANA_CHAIN_ID -> {
                    privateKeyStr.decodeBase58()
                }
                Constants.ChainId.BITCOIN_CHAIN_ID -> {
                    if (isBitcoinWifPrivateKey(privateKeyStr)) {
                        DumpedPrivateKey.fromBase58(BitcoinNetwork.MAINNET, privateKeyStr).key.privKeyBytes
                    } else {
                        Numeric.hexStringToByteArray(privateKeyStr)
                    }
                }
                in Constants.Web3EvmChainIds -> {
                    Numeric.hexStringToByteArray(privateKeyStr)
                }
                else -> {
                    throw IllegalArgumentException("Unsupported chainId: $chainId")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt private key for walletId: $walletId")
            null
        }
    }

    private fun isBitcoinWifPrivateKey(privateKey: String): Boolean {
        if (privateKey.length !in 51..52) return false
        return privateKey.startsWith("5") || privateKey.startsWith("K") || privateKey.startsWith("L")
    }

    fun getWeb3PrivateKey(context: Context, spendKey: ByteArray, chainId: String): ByteArray? {
        return try {
            val currentWalletId = Web3Signer.currentWalletId
            val currentCategory = Web3Signer.currentWalletCategory

            when {
                currentCategory == WalletCategory.CLASSIC.value || currentWalletId.isEmpty() -> {
                    val derivationIndex = extractIndexFromPath(Web3Signer.path) ?: 0
                    Timber.d("currentWalletId: ${Web3Signer.currentWalletId}, currentWalletCategory: ${Web3Signer.currentWalletCategory}, evmAddress: ${Web3Signer.evmAddress}, solanaAddress: ${Web3Signer.solanaAddress} derivationIndex: $derivationIndex")
                    tipPrivToPrivateKey(spendKey, chainId, derivationIndex)
                }

                currentCategory == WalletCategory.IMPORTED_PRIVATE_KEY.value -> {
                    getWeb3PrivateKey(context, spendKey, currentWalletId, chainId)
                }

                else -> { // Mnemonic-derived wallet
                    val mnemonic = getWeb3Mnemonic(context, spendKey, currentWalletId)
                        ?: return null
                    val derivationIndex = requireNotNull(extractIndexFromPath(Web3Signer.path))

                    when (chainId) {
                        Constants.ChainId.SOLANA_CHAIN_ID -> {
                            SolanaKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, index = derivationIndex)
                        }
                        Constants.ChainId.BITCOIN_CHAIN_ID -> {
                            BitcoinKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, "", derivationIndex)
                        }
                        else -> {
                            EthKeyGenerator.getPrivateKeyFromMnemonic(mnemonic, index = derivationIndex)
                        }
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
