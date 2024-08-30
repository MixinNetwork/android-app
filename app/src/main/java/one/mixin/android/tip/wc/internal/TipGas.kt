package one.mixin.android.tip.wc.internal

import androidx.annotation.WorkerThread
import one.mixin.android.Constants.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS
import one.mixin.android.tip.wc.WalletConnectV2
import org.web3j.exceptions.MessageDecodingException
import org.web3j.protocol.core.methods.response.EthEstimateGas
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class TipGas(
    val assetId: String,
    val baseGas: BigInteger,
    val gasLimit: BigInteger,
    val maxPriorityFeePerGas: BigInteger,
) {
    constructor(
        assetId: String,
        baseGas: BigInteger,
        gasLimit: BigInteger,
        maxPriorityFeePerGas: BigInteger,
        tx: WCEthereumTransaction,
    ) : this(
        assetId,
        baseGas.max(tx.gasPrice?.run { Numeric.decodeQuantity(this) } ?: BigInteger.ZERO).run {
            this.plus(this.divide(BigInteger.valueOf(10)))  // more 10% base gas
        },
        gasLimit.max(tx.gasLimit?.run { Numeric.decodeQuantity(this) } ?: BigInteger.ZERO).run {
            if (this == BigInteger.ZERO) {
                this
            } else {
                this.plus(this.divide(BigInteger.valueOf(2)))
            }
        },
        maxPriorityFeePerGas.max(tx.maxPriorityFeePerGas?.run { Numeric.decodeQuantity(this) } ?: BigInteger.ZERO).run {
            this.plus(this.divide(BigInteger.valueOf(5)))  // more 20% priority fee
        },
    )

    fun maxFeePerGas(maxFeePerGas: BigInteger): BigInteger {
        return (baseGas.add(maxPriorityFeePerGas)).max(maxFeePerGas)
    }

    fun displayValue(maxFee: String?): BigDecimal? {
        val maxFeePerGas = maxFee?.let { Numeric.decodeQuantity(it) } ?: BigInteger.ZERO
        val gas = maxFeePerGas(maxFeePerGas)
        return Convert.fromWei(gas.run { BigDecimal(this) }.multiply(gasLimit.run { BigDecimal(this) }), Convert.Unit.ETHER)
    }

    fun displayGas(maxFee: String?): BigDecimal? {
        val maxFeePerGas = maxFee?.let { Numeric.decodeQuantity(it) } ?: BigInteger.ZERO
        val gas = maxFeePerGas(maxFeePerGas)
        return Convert.fromWei(gas.run { BigDecimal(this) }, Convert.Unit.GWEI).setScale(2, RoundingMode.UP)
    }
}

@WorkerThread
fun buildTipGas(assetId: String, chain: Chain, tx: WCEthereumTransaction): TipGas? {
    val baseGas = WalletConnectV2.ethBlock(chain)?.run {
        this.block.baseFeePerGas
    } ?: return null
    val gasLimit = WalletConnectV2.ethEstimateGas(chain, tx.toTransaction())?.run {
        val defaultLimit = if (chain.chainReference == Chain.Ethereum.chainReference) BigInteger.valueOf(4712380L) else null
        convertToGasLimit(this, defaultLimit)
    } ?: return null
    val maxPriorityFeePerGas =  WalletConnectV2.ethMaxPriorityFeePerGas(chain)?.run {
        try {
            this.maxPriorityFeePerGas
        } catch (e: MessageDecodingException) {
            result?.run { Numeric.decodeQuantity(this) }
        }
    } ?: return null
    Timber.d("@@@ baseGas $baseGas, gasLimit $gasLimit, maxPriorityFeePerGas $maxPriorityFeePerGas")
    return TipGas(assetId, baseGas, gasLimit, maxPriorityFeePerGas, tx)
}

private fun convertToGasLimit(
    estimate: EthEstimateGas,
    defaultLimit: BigInteger?,
): BigInteger? {
    return if (estimate.hasError()) {
        // out of gas
        if (estimate.error.code == -32000) {
            defaultLimit
        } else {
            BigInteger.ZERO
        }
    } else if (estimate.amountUsed > BigInteger.ZERO) {
        estimate.amountUsed
    } else if (defaultLimit == null || defaultLimit == BigInteger.ZERO) {
        BigInteger(DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS)
    } else {
        defaultLimit
    }
}