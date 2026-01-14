package one.mixin.android.web3.js

import okio.Buffer
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.Constants.Account.ChainAddress.SOLANA_ADDRESS
import one.mixin.android.Constants.ChainId.SOLANA_CHAIN_ID
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.Web3Address
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
import org.bitcoinj.base.Coin
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionInput
import org.bitcoinj.core.TransactionWitness
import org.bitcoinj.crypto.ECKey
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
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
import org.web3j.protocol.core.Response
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import java.nio.ByteBuffer
import org.sol4k.Constants as ConstantsSolana

object Web3Signer {
    sealed class JsSignerNetwork(val name: String) {
        data object Ethereum : JsSignerNetwork("ethereum")

        data object Solana : JsSignerNetwork("solana")
    }

    private const val TAG = "Web3Signer"

    private val sp by lazy {
        MixinApplication.appContext.defaultSharedPreferences
    }

    private object Keys {
        const val ADDRESS = "signer_address"
        const val EVM_ADDRESS = "signer_evm_address"
        const val SOLANA_ADDRESS = "signer_solana_address"
        const val PATH = "signer_path"
        const val CURRENT_WALLET_CATEGORY = "signer_current_wallet_category"
        const val CLASSIC_WALLET_ID = "signer_classic_wallet_id"
        const val SELECTED_WEB3_WALLET_ID = "selected_web3_wallet_id"
        const val CURRENT_CHAIN = "signer_current_chain"
    }

    var address: String = ""
        private set
    var evmAddress: String = ""
        private set
    var solanaAddress: String = ""
        private set
    var path: String = ""
        private set
    var currentWalletId: String = ""
        private set
    var currentWalletCategory: String = ""
        private set
    var classicWalletId: String = ""
        private set
    var currentChain: Chain = Chain.Ethereum
        private set

    // now only ETH and SOL
    var currentNetwork = JsSignerNetwork.Ethereum.name
        private set

    init {
        load()
    }

    private fun load() {
        address = sp.getString(Keys.ADDRESS, "") ?: ""
        evmAddress = sp.getString(Keys.EVM_ADDRESS, "") ?: ""
        solanaAddress = sp.getString(Keys.SOLANA_ADDRESS, "") ?: ""
        path = sp.getString(Keys.PATH, "") ?: ""
        currentWalletId = sp.getString(Keys.SELECTED_WEB3_WALLET_ID, "") ?: ""
        currentWalletCategory = sp.getString(Keys.CURRENT_WALLET_CATEGORY, WalletCategory.CLASSIC.value)
            ?: WalletCategory.CLASSIC.value
        classicWalletId = sp.getString(Keys.CLASSIC_WALLET_ID, "") ?: ""
        currentChain = findChainByHex(sp.getString(Keys.CURRENT_CHAIN, Chain.Ethereum.hexReference))
            ?: Chain.Ethereum
        currentNetwork = if (currentChain == Chain.Solana) JsSignerNetwork.Solana.name else JsSignerNetwork.Ethereum.name
    }

    private fun persist() {
        sp.putString(Keys.ADDRESS, address)
        sp.putString(Keys.EVM_ADDRESS, evmAddress)
        sp.putString(Keys.SOLANA_ADDRESS, solanaAddress)
        sp.putString(Keys.PATH, path)
        sp.putString(Keys.SELECTED_WEB3_WALLET_ID, currentWalletId)
        sp.putString(Keys.CURRENT_WALLET_CATEGORY, currentWalletCategory)
        sp.putString(Keys.CLASSIC_WALLET_ID, classicWalletId)
        sp.putString(Keys.CURRENT_CHAIN, currentChain.hexReference)
    }

    private fun findChainByHex(hex: String?): Chain? {
        return when (hex) {
            Chain.Ethereum.hexReference -> Chain.Ethereum
            Chain.Base.hexReference -> Chain.Base
            Chain.Blast.hexReference -> Chain.Blast
            Chain.Arbitrum.hexReference -> Chain.Arbitrum
            Chain.Optimism.hexReference -> Chain.Optimism
            Chain.Avalanche.hexReference -> Chain.Avalanche
            Chain.Polygon.hexReference -> Chain.Polygon
            Chain.BinanceSmartChain.hexReference -> Chain.BinanceSmartChain
            Chain.Solana.hexReference -> Chain.Solana
            else -> null
        }
    }

    fun updateAddress(
        network: String,
        address: String,
    ) {
        if (network == JsSignerNetwork.Solana.name) {
            solanaAddress = address
        } else {
            evmAddress = address
        }
        persist()
    }

    fun useEvm() {
        address = evmAddress
        if (!evmChainList.contains(currentChain)) {
            currentChain = Chain.Ethereum
        }
        currentNetwork = JsSignerNetwork.Ethereum.name
        persist()
    }

    fun useSolana() {
        address = solanaAddress
        currentChain = Chain.Solana
        currentNetwork = JsSignerNetwork.Solana.name
        persist()
    }

    suspend fun init(classicQuery: () -> String?, queryAddress: (String) -> List<Web3Address>, queryWallet: (String) -> WalletItem?) {
        classicWalletId = PropertyHelper.findValueByKey(Keys.CLASSIC_WALLET_ID, classicQuery() ?: "")
        currentWalletId = PropertyHelper.findValueByKey(
            Keys.SELECTED_WEB3_WALLET_ID,
            classicWalletId
        )
        currentWalletCategory = PropertyHelper.findValueByKey(Keys.CURRENT_WALLET_CATEGORY, queryWallet(currentWalletId)?.category ?: WalletCategory.CLASSIC.value)
        updateAddressesAndPaths(currentWalletId, queryAddress)
        persist()
    }

    suspend fun setWallet(walletId: String, category: String, queryAddress: (String) -> List<Web3Address>) {
        if (category == WalletCategory.WATCH_ADDRESS.value) return
        currentWalletId = walletId
        PropertyHelper.updateKeyValue(Keys.SELECTED_WEB3_WALLET_ID, walletId)
        currentWalletCategory = category
        updateAddressesAndPaths(walletId, queryAddress)
        persist()
    }

    private suspend fun updateAddressesAndPaths(
        walletId: String,
        queryAddress: (String) -> List<Web3Address>,
    ) {
        if (walletId.isNotBlank()) {
            val addresses = queryAddress(walletId)
            path = addresses.firstOrNull()?.path ?: ""
            evmAddress =
                addresses.firstOrNull { it.chainId in Constants.Web3ChainIds }?.destination
                    ?: ""
            solanaAddress =
                addresses.firstOrNull { it.chainId == SOLANA_CHAIN_ID }?.destination
                    ?: ""
            address = evmAddress
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
                persist()
                Result.success(Chain.Ethereum.name)
            }
            Chain.Base.hexReference -> {
                currentChain = Chain.Base
                persist()
                Result.success(Chain.Base.name)
            }
            Chain.Blast.hexReference -> {
                currentChain = Chain.Blast
                persist()
                Result.success(Chain.Blast.name)
            }
            Chain.Arbitrum.hexReference -> {
                currentChain = Chain.Arbitrum
                persist()
                Result.success(Chain.Arbitrum.name)
            }
            Chain.Optimism.hexReference -> {
                currentChain = Chain.Optimism
                persist()
                Result.success(Chain.Optimism.name)
            }
            Chain.Avalanche.hexReference -> {
                currentChain = Chain.Avalanche
                persist()
                Result.success(Chain.Avalanche.name)
            }
            Chain.Polygon.hexReference -> {
                currentChain = Chain.Polygon
                persist()
                Result.success(Chain.Polygon.name)
            }
            Chain.BinanceSmartChain.hexReference -> {
                currentChain = Chain.BinanceSmartChain
                persist()
                Result.success(Chain.BinanceSmartChain.name)
            }
            Chain.Solana.hexReference -> {
                currentChain = Chain.Solana
                currentNetwork = JsSignerNetwork.Solana.name
                persist()
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

    fun signEthMessage(
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

    fun signBTCTransaction(priv: ByteArray, rawHex: String, amountSats: Long): String {
        val key: ECKey = ECKey.fromPrivate(priv, true)
        val rawTxBytes: ByteArray = rawHex.hexStringToByteArray()
        val transaction: Transaction = Transaction.read(ByteBuffer.wrap(rawTxBytes))
        if (transaction.inputs.isEmpty()) {
            throw IllegalArgumentException("Empty transaction inputs")
        }
        transaction.inputs.forEachIndexed { inputIndex: Int, input: TransactionInput ->
            val value: Coin = Coin.valueOf(amountSats)
            if (value.isZero) {
                throw IllegalArgumentException("Invalid utxo amount on input $inputIndex")
            }
            val scriptCode: Script = ScriptBuilder.createP2PKHOutputScript(key)
            val signature: TransactionSignature = transaction.calculateWitnessSignature(
                inputIndex,
                key,
                scriptCode,
                value,
                Transaction.SigHash.ALL,
                false,
            )
            val witness = TransactionWitness.of(
                signature.encodeToBitcoin(),
                key.pubKey,
            )
            val newInput = input.withScriptBytes(byteArrayOf()).withWitness(witness)
            transaction.inputs[inputIndex] = newInput
        }
        return transaction.serialize().toHex()
    }

    fun signSolanaMessage(
        secret: ByteArray,
        message: String,
    ): String {
        val m =
            try {
                message.decodeBase58()
            } catch (e: Exception) {
                message.removePrefix("0x").hexStringToByteArray()
            }
        return signSolanaMessage(secret, m)
    }

    fun signSolanaMessage(
        secret: ByteArray,
        message: ByteArray,
    ): String {
        val keyPair = Keypair.fromSecretKey(secret)
        val sig = keyPair.sign(message)
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
