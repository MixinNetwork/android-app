package one.mixin.android.tip.wc.internal

import one.mixin.android.Constants.DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS
import one.mixin.android.api.request.web3.EstimateFeeResponse
import org.web3j.protocol.core.methods.response.EthEstimateGas
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class TipGas(
    val assetId: String,
    val gasLimit: BigInteger,
    val maxFeePerGas: BigInteger,
    val maxPriorityFeePerGas: BigInteger,
) {
    fun selectMaxFeePerGas(maxFeePerGas: BigInteger): BigInteger {
        return this.maxFeePerGas.max(maxFeePerGas)
    }

    fun displayValue(maxFee: String?): BigDecimal? {
        val maxFeePerGas = maxFee?.let { Numeric.decodeQuantity(it) } ?: BigInteger.ZERO
        val gas = selectMaxFeePerGas(maxFeePerGas)
        return Convert.fromWei(gas.run { BigDecimal(this) }.multiply(gasLimit.run { BigDecimal(this) }), Convert.Unit.ETHER)
    }

    fun displayGas(maxFee: String?): BigDecimal? {
        val maxFeePerGas = maxFee?.let { Numeric.decodeQuantity(it) } ?: BigInteger.ZERO
        val gas = selectMaxFeePerGas(maxFeePerGas)
        return Convert.fromWei(gas.run { BigDecimal(this) }, Convert.Unit.GWEI).setScale(2, RoundingMode.UP)
    }
}

fun buildTipGas(assetId: String, response: EstimateFeeResponse): TipGas {
    return TipGas(assetId, response.gasLimit!!.toBigInteger(), response.maxFeePerGas!!.toBigInteger(), response.maxPriorityFeePerGas!!.toBigInteger())
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