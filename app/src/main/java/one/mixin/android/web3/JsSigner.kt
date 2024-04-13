package one.mixin.android.web3

import android.util.LruCache
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.tip.wc.internal.WalletConnectException
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.Response
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger

object JsSigner {

    private const val TAG = "JsSigner"

    private var web3jPool = LruCache<Chain, Web3j>(3)

    private fun getWeb3j(chain: Chain): Web3j {
        val exists = web3jPool[chain]
        return if (exists == null) {
            val web3j = Web3j.build(HttpService(chain.rpcUrl))
            web3jPool.put(chain, web3j)
            web3j
        } else {
            exists
        }
    }

    lateinit var address: String
        private set

    fun updateAddress(address: String) {
        this.address = address
    }
    var currentChain: Chain = Chain.Ethereum
        private set

    // now only ETH
    var currentNetwork = "ethereum"

    suspend fun init() {
        address = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
    }

    fun switchChain(switchChain: SwitchChain): Result<String> {
        return when (switchChain.chainId) {
            Chain.Ethereum.hexReference-> {
                currentChain = Chain.Ethereum
                Result.success(Chain.Ethereum.name)
            }
            Chain.Polygon.hexReference -> {
                currentChain = Chain.Polygon
                Result.success(Chain.Polygon.name)
            }
            Chain.BinanceSmartChain.hexReference -> {
                currentChain = Chain.BinanceSmartChain
                Result.success(Chain.BinanceSmartChain.name)
            }
            else -> {
                Result.failure(IllegalArgumentException("No support"))
            }
        }
    }

    fun sendTransaction(
        signedTransactionData: String
    ): String? {
        val tx = getWeb3j(currentChain).ethSendRawTransaction(signedTransactionData).send()
        if (tx.hasError()) {
            val msg = "error code: ${tx.error.code}, message: ${tx.error.message}"
            Timber.d("$TAG transactionHash is null, $msg")
            throw WalletConnectException(tx.error.code, tx.error.message)
        }
        val transactionHash = tx.transactionHash
        Timber.d("$TAG sendTransaction $transactionHash")
        return transactionHash
    }

    fun ethSignTransaction(
        priv: ByteArray,
        transaction: WCEthereumTransaction,
        tipGas: TipGas,
    ): String {
        val value = transaction.value ?: "0x0"

        val keyPair = ECKeyPair.create(priv)
        val credential = Credentials.create(keyPair)
        val transactionCount =
            getWeb3j(currentChain).ethGetTransactionCount(credential.address, DefaultBlockParameterName.LATEST).send()
        if (transactionCount.hasError()) {
            throwError(transactionCount.error)
        }
        val nonce = transactionCount.transactionCount
        val v = Numeric.toBigInt(value)

        val maxPriorityFeePerGas = tipGas.ethMaxPriorityFeePerGas
        val maxFeePerGas = tipGas.maxFeePerGas(transaction.maxFeePerGas?.let { Numeric.toBigInt(it) } ?: BigInteger.ZERO)
        val gasLimit = tipGas.gasLimit
        Timber.e(
            "$TAG dapp gas: ${transaction.gas?.let { Numeric.toBigInt(it) }} gasLimit: ${transaction.gasLimit?.let { Numeric.toBigInt(it) }} maxFeePerGas: ${transaction.maxFeePerGas?.let { Numeric.toBigInt(it) }} maxPriorityFeePerGas: ${
                transaction.maxPriorityFeePerGas?.let {
                    Numeric.toBigInt(
                        it
                    )
                }
            } "
        )
        Timber.e("$TAG nonce: $nonce, value $v wei, gasLimit: $gasLimit maxFeePerGas: $maxFeePerGas maxPriorityFeePerGas: $maxPriorityFeePerGas")
        val rawTransaction = RawTransaction.createTransaction(
            currentChain.chainReference.toLong(),
            nonce,
            gasLimit,
            transaction.to,
            v,
            transaction.data ?: "",
            maxPriorityFeePerGas,
            maxFeePerGas,
        )

        val signedMessage = TransactionEncoder.signMessage(rawTransaction, currentChain.chainReference.toLong(), credential)
        val hexMessage = Numeric.toHexString(signedMessage)
        Timber.d("$TAG signTransaction $hexMessage")
        return hexMessage
    }

    fun signMessage(
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