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
    @SerializedName("unit_price") val unitPrice: String?,
    @SerializedName("unit_limit") val unitLimit: String?
)