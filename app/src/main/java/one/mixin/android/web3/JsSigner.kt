package one.mixin.android.web3

import android.util.LruCache
import one.mixin.android.Constants
import one.mixin.android.Constants.Account.ChainAddress.EVM_ADDRESS
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.toHex
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.TipGas
import one.mixin.android.tip.wc.internal.WCEthereumTransaction
import one.mixin.android.tip.wc.internal.WalletConnectException
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.RawTransaction
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

    fun getWeb3j(chain: Chain): Web3j {
        val exists = web3jPool[chain]
        return if (exists == null) {
            val web3j = Web3j.build(HttpService(chain.rpcUrl))
            web3jPool.put(chain, web3j)
            web3j
        } else {
            exists
        }
    }

    var address = ""
        private set
    var currentChain = Chain.Polygon //  todo
        private set

    suspend fun init() {
        address = PropertyHelper.findValueByKey(EVM_ADDRESS, "")
    }

    fun switchChain(switchChain: SwitchChain): Result<String> {
        return when (switchChain.chainId) {
            Chain.Ethereum.hexReference-> {
                Result.success(Chain.Ethereum.name)
            }
            Chain.Polygon.hexReference -> {
                Result.success(Chain.Polygon.name)
            }
            Chain.BinanceSmartChain.hexReference -> {
                Result.success(Chain.BinanceSmartChain.name)
            }
            else -> {
                Result.success("")
            }
        }
    }

    fun sendTransaction(
        chain: Chain, signedTransactionData: String
    ): String? {
        val tx = getWeb3j(chain).ethSendRawTransaction(signedTransactionData).send()
        if (tx.hasError()) {
            val msg = "error code: ${tx.error.code}, message: ${tx.error.message}"
            Timber.d("$TAG transactionHash is null, $msg")
            // todo
            throw WalletConnectException(tx.error.code, tx.error.message)
        }
        val transactionHash = tx.transactionHash
        Timber.d("$TAG sendTransaction $transactionHash")
        return transactionHash
    }

    fun ethSignTransaction(
        priv: ByteArray,
        chain: Chain,
        transaction: WCEthereumTransaction,
        tipGas: TipGas,
    ): String {
        val value = transaction.value ?: "0x0"

        val keyPair = ECKeyPair.create(priv)
        val credential = Credentials.create(keyPair)
        val transactionCount =
            getWeb3j(chain).ethGetTransactionCount(credential.address, DefaultBlockParameterName.LATEST).send()
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
            chain.chainReference.toLong(),
            nonce,
            gasLimit,
            transaction.to,
            v,
            transaction.data ?: "",
            maxPriorityFeePerGas,
            maxFeePerGas,
        )

        val signedMessage = TransactionEncoder.signMessage(rawTransaction, chain.chainReference.toLong(), credential)
        val hexMessage = Numeric.toHexString(signedMessage)
        Timber.d("$TAG signTransaction $hexMessage")
        return hexMessage
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