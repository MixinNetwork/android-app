package one.mixin.android.tip.wc.internal

import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class TipGas(
    val assetId: String,
    val gasPrice: BigInteger,
    val gasLimit: BigInteger,
    val ethMaxPriorityFeePerGas: BigInteger,
) {
    constructor(
        assetId: String,
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        ethMaxPriorityFeePerGas: BigInteger,
        tx: WCEthereumTransaction,
    ) : this(
        assetId,
        gasPrice.max(tx.gasPrice?.run { Numeric.decodeQuantity(this) } ?: BigInteger.ZERO),
        gasLimit.max(tx.gasLimit?.run { Numeric.decodeQuantity(this) } ?: BigInteger.ZERO).run {
            if (this == BigInteger.ZERO) {
                this
            } else {
                this.plus(this.divide(BigInteger.valueOf(2)))
            }
        },
        ethMaxPriorityFeePerGas.max(tx.maxPriorityFeePerGas?.run { Numeric.decodeQuantity(this) } ?: BigInteger.ZERO),
    )

    fun maxFeePerGas(maxFeePerGas: BigInteger): BigInteger {
        return gasPrice.max(maxFeePerGas).run {
            plus(this.divide(BigInteger.valueOf(5)))
        }
    }
}

fun TipGas.displayValue(maxFee: String?): BigDecimal? {
    val maxFeePerGas = maxFee?.let { Numeric.decodeQuantity(it) } ?: BigInteger.ZERO
    val gas = maxFeePerGas(maxFeePerGas)
    return Convert.fromWei(gas.run { BigDecimal(this) }.multiply(gasLimit.run { BigDecimal(this) }), Convert.Unit.ETHER)
}

fun TipGas.displayGas(maxFee: String?): BigDecimal? {
    val maxFeePerGas = maxFee?.let { Numeric.decodeQuantity(it) } ?: BigInteger.ZERO
    val gas = maxFeePerGas(maxFeePerGas)
    return Convert.fromWei(gas.run { BigDecimal(this) }, Convert.Unit.GWEI).setScale(2, RoundingMode.UP)
}