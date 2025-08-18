package one.mixin.android.web3.js

import android.util.LruCache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.hexStringToByteArray
import one.mixin.android.extension.putString
import one.mixin.android.extension.toHex
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.WalletConnectV2
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.tip.wc.internal.evmChainList
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.decodeBase58
import one.mixin.android.util.encodeToBase58String
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.Web3Exception
import org.sol4k.Keypair
import org.sol4kt.SignInAccount
import org.sol4kt.SignInInput
import org.sol4kt.SignInOutput
import org.sol4kt.VersionedTransactionCompat
import org.sol4kt.exception.MaliciousInstructionException
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.Response
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import org.sol4k.Constants as ConstantsSolana

object JsSigner {
    sealed class JsSignerNetwork(val name: String) {
        data object Ethereum : JsSignerNetwork("ethereum")

        data object Solana : JsSignerNetwork("solana")
    }

    private const val TAG = "JsSigner"

    private val sp by lazy {
        MixinApplication.appContext.defaultSharedPreferences
    }

    private var web3jPool = LruCache<Chain, Web3j>(3)

    private fun getWeb3j(chain: Chain): Web3j {
        val exists = web3jPool[chain]
        return if (exists == null) {
            val web3j = Web3j.build(HttpService(chain.rpcUrl, buildOkHttpClient()))
            web3jPool.put(chain, web3j)
            web3j
        } else {
            exists
        }
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(15, TimeUnit.SECONDS)
        builder.writeTimeout(15, TimeUnit.SECONDS)
        builder.readTimeout(15, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
        }
        return builder.build()
    }

    object Keys {
        const val ADDRESS = "signer_address"
        const val EVM_ADDRESS = "signer_evm_address"
        const val SOLANA_ADDRESS = "signer_solana_address"
        const val PATH = "signer_path"
        const val CURRENT_WALLET_CATEGORY = "signer_current_wallet_category"
        const val CLASSIC_WALLET_ID = "signer_classic_wallet_id"

        const val SELECTED_WEB3_WALLET_ID = "selected_web3_wallet_id"
    }

    var address: String
        private set
    var evmAddress: String
        private set
    var solanaAddress: String
        private set
    var path: String
        private set
    var currentWalletId: String
        private set
    var currentWalletCategory: String
        private set
    var classicWalletId: String
        private set

    init {
        address = sp.getString(Keys.ADDRESS, "") ?: ""
        evmAddress = sp.getString(Keys.EVM_ADDRESS, "") ?: ""
        solanaAddress = sp.getString(Keys.SOLANA_ADDRESS, "") ?: ""
        path = sp.getString(Keys.PATH, "") ?: ""
        currentWalletId = sp.getString(Keys.SELECTED_WEB3_WALLET_ID, "") ?: ""
        currentWalletCategory = sp.getString(Keys.CURRENT_WALLET_CATEGORY, WalletCategory.CLASSIC.value) ?: WalletCategory.CLASSIC.value
        classicWalletId = sp.getString(Keys.CLASSIC_WALLET_ID, "") ?: ""
    }

    fun updateAddress(
        network: String,
        address: String,
    ) {
        if (network == JsSignerNetwork.Solana.name) {
            solanaAddress = address
            sp.putString(Keys.SOLANA_ADDRESS, address)
        } else {
            evmAddress = address
            sp.putString(Keys.EVM_ADDRESS, address)
        }
    }

    fun useEvm() {
        address = evmAddress
        sp.putString(Keys.ADDRESS, address)
        if (!evmChainList.contains(currentChain)) {
            currentChain = Chain.Ethereum
        }
        currentNetwork = JsSignerNetwork.Ethereum.name
    }

    fun useSolana() {
        address = solanaAddress
        sp.putString(Keys.ADDRESS, address)
        currentChain = Chain.Solana
        currentNetwork = JsSignerNetwork.Solana.name
    }

    var currentChain: Chain = Chain.Ethereum
        private set

    // now only ETH and SOL
    var currentNetwork = JsSignerNetwork.Ethereum.name

    suspend fun init(classicQuery: () -> String?, queryAddress: (String) -> List<Web3Address>, queryWallet: (String) -> Web3Wallet?) {
        classicWalletId = PropertyHelper.findValueByKey(Keys.CLASSIC_WALLET_ID, classicQuery() ?: "")
        currentWalletId = PropertyHelper.findValueByKey(
            Keys.SELECTED_WEB3_WALLET_ID,
            classicWalletId
        )
        currentWalletCategory = PropertyHelper.findValueByKey(Keys.CURRENT_WALLET_CATEGORY, queryWallet(currentWalletId)?.category ?: WalletCategory.CLASSIC.value)
        address = PropertyHelper.findValueByKey(Keys.ADDRESS, "")
        evmAddress = PropertyHelper.findValueByKey(Keys.EVM_ADDRESS, "")
        solanaAddress = PropertyHelper.findValueByKey(Keys.SOLANA_ADDRESS, "")
        path = PropertyHelper.findValueByKey(Keys.PATH, "")
        updateAddressesAndPaths(currentWalletId, queryAddress)
    }

    suspend fun setWallet(walletId: String, category: String, queryAddress: (String) -> List<Web3Address>) {
        if (category == WalletCategory.WATCH_ADDRESS.value) return
        currentWalletId = walletId
        sp.putString(Keys.SELECTED_WEB3_WALLET_ID, walletId)
        PropertyHelper.updateKeyValue(Keys.SELECTED_WEB3_WALLET_ID, walletId)
        currentWalletCategory = category
        sp.putString(Keys.CURRENT_WALLET_CATEGORY, category)
        updateAddressesAndPaths(walletId, queryAddress)
    }

    private suspend fun updateAddressesAndPaths(
        walletId: String,
        queryAddress: (String) -> List<Web3Address>
    ) {
        if (walletId.isNotBlank()) {
            val addresses = queryAddress(walletId)
            path = addresses.firstOrNull()?.path ?: ""
            sp.putString(Keys.PATH, path)
            evmAddress =
                addresses.firstOrNull { it.chainId != SOLANA_CHAIN_ID }?.destination
                    ?: ""
            sp.putString(Keys.EVM_ADDRESS, evmAddress)
            solanaAddress =
                addresses.firstOrNull { it.chainId == SOLANA_CHAIN_ID }?.destination
                    ?: ""
            sp.putString(Keys.SOLANA_ADDRESS, solanaAddress)
            address = evmAddress
            sp.putString(Keys.ADDRESS, address)
        } else {
            evmAddress = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
            solanaAddress = PropertyHelper.findValueByKey(SOLANA_ADDRESS, "")
            address = evmAddress
            path = ""
        }

        if (WalletConnect.isEnabled()) {
            if (currentChain.assetId == SOLANA_CHAIN_ID) {
                WalletConnectV2.switchAccount(solanaAddress)
            } else {
                WalletConnectV2.switchAccount(evmAddress)
            }
        }
    }

    fun switchChain(switchChain: SwitchChain): Result<String> {
        currentNetwork = JsSignerNetwork.Ethereum.name
        return when (switchChain.chainId) {
            Chain.Ethereum.hexReference -> {
                currentChain = Chain.Ethereum
                Result.success(Chain.Ethereum.name)
            }
            Chain.Base.hexReference -> {
                currentChain = Chain.Base
                Result.success(Chain.Base.name)
            }
            Chain.Blast.hexReference -> {
                currentChain = Chain.Blast
                Result.success(Chain.Blast.name)
            }
            Chain.Arbitrum.hexReference -> {
                currentChain = Chain.Arbitrum
                Result.success(Chain.Arbitrum.name)
            }
            Chain.Optimism.hexReference -> {
                currentChain = Chain.Optimism
                Result.success(Chain.Optimism.name)
            }
            Chain.Polygon.hexReference -> {
                currentChain = Chain.Polygon
                Result.success(Chain.Polygon.name)
            }
            Chain.BinanceSmartChain.hexReference -> {
                currentChain = Chain.BinanceSmartChain
                Result.success(Chain.BinanceSmartChain.name)
            }
            Chain.Solana.hexReference -> {
                currentChain = Chain.Solana
                currentNetwork = JsSignerNetwork.Solana.name
                Result.success(Chain.Solana.name)
            }
            else -> {
                Result.failure(IllegalArgumentException("No support"))
            }
        }
    }

    suspend fun ethSignTransaction(
        priv: ByteArray,
        transaction: WCEthereumTransaction,
        tipGas: TipGas,
        chain: Chain?,
        getNonce: suspend (String) -> BigInteger,
    ): Pair<String, String> {
        val value = transaction.value ?: "0x0"
        val keyPair = ECKeyPair.create(priv)
        val credential = Credentials.create(keyPair)
        val nonce = transaction.nonce?.toBigIntegerOrNull() ?: getNonce(credential.address)
        val v = Numeric.decodeQuantity(value)

        val maxPriorityFeePerGas = tipGas.maxPriorityFeePerGas
        val maxFeePerGas = tipGas.selectMaxFeePerGas(transaction.maxFeePerGas?.let { Numeric.decodeQuantity(it) } ?: BigInteger.ZERO)
        val gasLimit = tipGas.gasLimit
        Timber.e(
            "$TAG dapp gas: ${transaction.gas?.let { Numeric.decodeQuantity(it) }} gasLimit: ${transaction.gasLimit?.let { Numeric.decodeQuantity(it) }} maxFeePerGas: ${transaction.maxFeePerGas?.let { Numeric.decodeQuantity(it) }} maxPriorityFeePerGas: ${
                transaction.maxPriorityFeePerGas?.let {
                    Numeric.decodeQuantity(
                        it,
                    )
                }
            } ",
        )
        Timber.e("$TAG nonce: $nonce, value $v wei, gasLimit: $gasLimit maxFeePerGas: $maxFeePerGas maxPriorityFeePerGas: $maxPriorityFeePerGas")
        val rawTransaction =
            RawTransaction.createTransaction(
                (chain ?: currentChain).chainReference.toLong(),
                nonce,
                gasLimit,
                transaction.to,
                v,
                transaction.data ?: "",
                maxPriorityFeePerGas,
                maxFeePerGas,
            )
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, (chain ?: currentChain).chainReference.toLong(), credential)
        val hexMessage = Numeric.toHexString(signedMessage)
        Timber.d("$TAG signTransaction $hexMessage")
        return Pair(hexMessage, credential.address)
    }

    suspend fun ethPreviewTransaction(
        address: String,
        transaction: WCEthereumTransaction,
        tipGas: TipGas,
        chain: Chain?,
        getNonce: suspend (String) -> BigInteger,
    ): String {
        val value = transaction.value ?: "0x0"
        val nonce = transaction.nonce?.toBigIntegerOrNull() ?: getNonce(address)
        val v = Numeric.decodeQuantity(value)

        val maxPriorityFeePerGas = tipGas.maxPriorityFeePerGas
        val maxFeePerGas = tipGas.selectMaxFeePerGas(transaction.maxFeePerGas?.let { Numeric.decodeQuantity(it) } ?: BigInteger.ZERO)
        val gasLimit = tipGas.gasLimit

        val rawTransaction =
            RawTransaction.createTransaction(
                (chain ?: currentChain).chainReference.toLong(),
                nonce,
                gasLimit,
                transaction.to,
                v,
                transaction.data ?: "",
                maxPriorityFeePerGas,
                maxFeePerGas,
            )
        return Numeric.toHexString(TransactionEncoder.encode(rawTransaction))
    }

    fun signMessage(
        priv: ByteArray,
        message: String,
        type: Int,
    ): String {
        return if (currentChain == Chain.Solana) {
            signSolanaMessage(priv, message)
        } else {
            signEthMessage(priv, message, type)
        }
    }

    private fun signEthMessage(
        priv: ByteArray,
        message: String,
        type: Int,
    ): String {
        val keyPair = ECKeyPair.create(priv)
        val signature =
            if (type == JsSignMessage.TYPE_TYPED_MESSAGE) {
                val encoder = StructuredDataEncoder(message)
                Sign.signMessage(encoder.hashStructuredData(), keyPair, false)
            } else {
                Sign.signPrefixedMessage(Numeric.hexStringToByteArray(message), keyPair)
            }
        val b = ByteArray(65)
        System.arraycopy(signature.r, 0, b, 0, 32)
        System.arraycopy(signature.s, 0, b, 32, 32)
        System.arraycopy(signature.v, 0, b, 64, 1)
        return Numeric.toHexString(b)
    }

    private fun signSolanaMessage(
        priv: ByteArray,
        message: String,
    ): String {
        val holder = Keypair.fromSecretKey(priv)
        val m =
            try {
                message.decodeBase58()
            } catch (e: Exception) {
                message.removePrefix("0x").hexStringToByteArray()
            }
        val sig = holder.sign(m)
        return sig.toHex()
    }

    suspend fun signSolanaTransaction(
        priv: ByteArray,
        tx: VersionedTransactionCompat,
        getBlockhash: suspend () -> String,
    ): VersionedTransactionCompat {
        val holder = Keypair.fromSecretKey(priv)
        // use latest blockhash should not break other signatures
        if (tx.onlyOneSigner() &&
            (tx.message.recentBlockhash.isBlank() ||
                    tx.message.recentBlockhash == holder.publicKey.toBase58())) { // inner transfer use address as temp blockhash
            tx.message.recentBlockhash = getBlockhash()
        }
        tx.sign(holder)
        return tx
    }

    fun solanaSignIn(
        priv: ByteArray,
        signInInput: SignInInput,
    ): String {
        val signInMessage = signInInput.toMessage().toByteArray()
        val holder = Keypair.fromSecretKey(priv)
        val sig = holder.sign(signInMessage)
        val signInOutput =
            SignInOutput(
                account = SignInAccount(holder.publicKey.toBase58()),
                signedMessage = signInMessage.encodeToBase58String(),
                signature = sig.encodeToBase58String(),
            )
        return GsonHelper.customGson.toJson(signInOutput).toHex()
    }

    private fun throwError(
        error: Response.Error,
        msgAction: ((String) -> Unit)? = null,
    ) {
        val msg = "error code: ${error.code}, message: ${error.message}"
        Timber.d("${WalletConnect.TAG} error $msg")
        msgAction?.invoke(msg)
        throw Web3Exception(error.code, error.message)
    }

    fun reset() {
        currentChain = Chain.Ethereum
    }
}

fun VersionedTransactionCompat.throwIfAnyMaliciousInstruction() {
    val accounts = message.accounts
    for (i in message.instructions) {
        val program = accounts[i.programIdIndex]
        if (program != ConstantsSolana.TOKEN_PROGRAM_ID) {
            continue
        }
        val d = Buffer()
        d.write(i.data)
        val instruction = d.readByte().toInt()
        // instructionSetAuthority
        if (instruction == 6) {
            throw MaliciousInstructionException(MixinApplication.get().getString(R.string.malicious_instruction_token_set_authority))
        }
    }
}
