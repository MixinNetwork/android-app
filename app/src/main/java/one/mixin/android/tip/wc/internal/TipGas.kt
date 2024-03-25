package one.mixin.android.tip.wc.internal

import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger

data class TipGas(
    val assetId: String,
    val gasPrice: BigInteger,
    val gasLimit: BigInteger,
    val ethMaxPriorityFeePerGas: BigInteger?,
) {
    constructor(
        assetId: String,
        gasPrice: BigInteger,
        estimateGas: BigInteger,
        ethMaxPriorityFeePerGas: BigInteger?,
        tx: WCEthereumTransaction,
    ) : this(assetId, gasPrice.max(tx.gasPrice?.run { Numeric.toBigInt(this) } ?: BigInteger.ZERO), estimateGas.max(tx.gasLimit?.run { Numeric.toBigInt(this) } ?: BigInteger.ZERO), ethMaxPriorityFeePerGas?.max(tx.maxPriorityFeePerGas?.run { Numeric.toBigInt(this) } ?: BigInteger.ZERO))

    constructor(
        assetId: String,
        gasPrice: BigInteger,
        estimateGas: BigInteger,
        tx: WCEthereumTransaction,
    ) : this(assetId, gasPrice.max(tx.gasPrice?.run { Numeric.toBigInt(this) } ?: BigInteger.ZERO), estimateGas.max(tx.gasLimit?.run { Numeric.toBigInt(this) } ?: BigInteger.ZERO), null)
}

fun TipGas.displayValue(): BigDecimal? {
    return Convert.fromWei(gasPrice.run { BigDecimal(this) }.multiply(gasLimit.run { BigDecimal(this) }), Convert.Unit.ETHER)
}
