package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger

data class EstimateFeeResponse(
    @SerializedName("chain_id") val chainId: String,
    @SerializedName("gas_limit") val gasLimit: String?,
    @SerializedName("max_fee_per_gas") val maxFeePerGas: String?,
    @SerializedName("max_priority_fee_per_gas") val maxPriorityFeePerGas: String?,
    @SerializedName("unit_price") val price: String?,
    @SerializedName("unit_limit") val limit: String?
) {


    // fun displayValue(maxFee: String?): BigDecimal? {
    //     val maxFeePerGas = maxFee?.let { Numeric.decodeQuantity(it) } ?: BigInteger.ZERO
    //     val gas = maxFeePerGas(maxFeePerGas)
    //     return Convert.fromWei(gas.run { BigDecimal(this) }.multiply(gasLimit.run { BigDecimal(this) }), Convert.Unit.ETHER)
    // }
    //
    // fun maxFeePerGas(maxFeePerGas: BigInteger): BigInteger {
    //     return (baseGas.add(maxPriorityFeePerGas)).max(maxFeePerGas)
    // }

}
