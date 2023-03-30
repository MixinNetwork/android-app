package one.mixin.android.tip.wc

import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.walletconnect.web3.wallet.client.Wallet
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.TimeUnit

abstract class WalletConnect {
    companion object {
        const val TAG = "WalletConnect"

        internal const val web3jTimeout = 3L
        internal const val defaultGasLimit = "250000"
    }

    enum class Version {
        V1, V2, TIP
    }

    enum class RequestType {
        SessionProposal, SessionRequest, SwitchNetwork,
    }

    sealed class WCSignData<T>(
        open val requestId: Long,
        open val signMessage: T,
    ) {
        data class V1SignData<T>(
            override val requestId: Long,
            override val signMessage: T,
        ) : WCSignData<T>(requestId, signMessage)

        data class V2SignData<T>(
            override val requestId: Long,
            override val signMessage: T,
            val sessionRequest: Wallet.Model.SessionRequest,
        ) : WCSignData<T>(requestId, signMessage)

        data class TIPSignData(
            override val signMessage: String,
        ) : WCSignData<String>(0L, signMessage)
    }

    var chain: Chain = Chain.Polygon
        protected set
    protected var web3j = Web3j.build(HttpService(chain.rpcServers[0]))

    open var currentSignData: WCSignData<*>? = null

    fun getHumanReadableTransactionInfo(wct: WCEthereumTransaction): String {
        val result = StringBuilder()
        val estimateGas = getEstimateGas(wct)
        val amount = Numeric.toBigInt(wct.value)
        result.append("Estimated gas fee: ${Convert.fromWei(estimateGas.toBigDecimal(), Convert.Unit.ETHER).toPlainString()} ${chain.symbol}\n\n")
            .append("Amount + gas fee: ${Convert.fromWei((estimateGas + amount).toBigDecimal(), Convert.Unit.ETHER).toPlainString()} ${chain.symbol}\n\n")
        wct.maxFeePerGas?.let { result.append("maxFeePerGas: ${Convert.fromWei(Numeric.toBigInt(it).toBigDecimal(), Convert.Unit.GWEI).toPlainString()} GWEI\n") }
        wct.maxPriorityFeePerGas?.let {
            result.append(
                "maxPriorityFeePerGas: ${
                    Convert.fromWei(
                        Numeric.toBigInt(it).toBigDecimal(),
                        Convert.Unit.GWEI,
                    ).toPlainString()} GWEI\n\n",
            )
        }
        return result.append("HEX Data\n")
            .append(wct.data)
            .toString()
    }

    fun getEstimateGas(wct: WCEthereumTransaction): BigInteger {
        val gasPrice = if (wct.maxFeePerGas != null) {
            Numeric.toBigInt(wct.maxFeePerGas)
        } else {
            val ethGasPrice = web3j.ethGasPrice().sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
            if (ethGasPrice.hasError()) {
                throwError(ethGasPrice.error)
            }
            ethGasPrice.gasPrice
        }
        val gas = if (wct.gas != null) {
            Numeric.toBigInt(wct.gas)
        } else {
            val tx = Transaction.createFunctionCallTransaction(
                wct.from,
                null,
                gasPrice,
                Numeric.toBigInt(wct.gasLimit ?: defaultGasLimit),
                wct.to,
                Numeric.toBigInt(wct.value),
                wct.data,
            )
            val ethEstimateGas =
                web3j.ethEstimateGas(tx).sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
            if (ethEstimateGas.hasError()) {
                throwError(ethEstimateGas.error)
            }
            ethEstimateGas.amountUsed
        }
        return gas * gasPrice
    }

    fun getBaseFeePerGas(): BigInteger {
        val number = web3j.ethBlockNumber().sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
        if (number.hasError()) {
            throwError(number.error)
        }
        val block = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(number.blockNumber), true)
            .sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
        if (block.hasError()) {
            throwError(block.error)
        }
        return block.block.baseFeePerGas
    }

    fun getMaxFeePerGasAndMaxPriorityFeePerGas(): Pair<BigInteger, BigInteger> {
        val number = web3j.ethBlockNumber().sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
        if (number.hasError()) {
            throwError(number.error)
        }
        val txResp = web3j.ethGetTransactionByBlockNumberAndIndex(DefaultBlockParameter.valueOf(number.blockNumber), BigInteger.ONE)
            .sendAsync().get(web3jTimeout, TimeUnit.SECONDS)
        if (txResp.hasError()) {
            throwError(txResp.error)
        }
        val tx = txResp.transaction.get()
        return Pair(tx.maxFeePerGas, tx.maxPriorityFeePerGas)
    }

    fun signMessage(priv: ByteArray, message: WCEthereumSignMessage): String {
        val keyPair = ECKeyPair.create(priv)
        val signature = if (message.type == WCEthereumSignMessage.WCSignType.TYPED_MESSAGE) {
            val encoder = StructuredDataEncoder(message.data)
            Sign.signMessage(encoder.hashStructuredData(), keyPair, false)
        } else {
            Sign.signPrefixedMessage(Numeric.hexStringToByteArray(message.data), keyPair)
        }
        val b = ByteArray(65)
        System.arraycopy(signature.r, 0, b, 0, 32)
        System.arraycopy(signature.s, 0, b, 32, 32)
        System.arraycopy(signature.v, 0, b, 64, 1)
        return Numeric.toHexString(b)
    }

    protected fun throwError(error: Response.Error, msgAction: ((String) -> Unit)? = null) {
        val msg = "error code: ${error.code}, message: ${error.message}"
        Timber.d("$TAG error $msg")
        msgAction?.invoke(msg)
        throw WalletConnectException(error.code, error.message)
    }
}
