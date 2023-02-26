package one.mixin.android.tip.wc

import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import org.web3j.protocol.Web3j
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
        V1, V2
    }

    var chain: Chain = Chain.Polygon
        protected set
    protected var web3j = Web3j.build(HttpService(chain.rpcServers[0]))

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
                val error = ethGasPrice.error
                val msg = "error code: ${error.code}, message: ${error.message}"
                Timber.d("$TAG ethGasPrice error $msg")
                throw WalletConnectException(error.code, error.message)
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
                val error = ethEstimateGas.error
                val msg = "error code: ${error.code}, message: ${error.message}"
                Timber.d("$TAG ethEstimateGas error $msg")
                throw WalletConnectException(error.code, error.message)
            }
            ethEstimateGas.amountUsed
        }
        return gas * gasPrice
    }
}
