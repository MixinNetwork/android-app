package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class TipGas(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("safe_gas_price")
    val safeGasPrice: String,
    @SerializedName("propose_gas_price")
    val proposeGasPrice: String,
    @SerializedName("fast_gas_price")
    val fastGasPrice: String,
    @SerializedName("gas_limit")
    val gasLimit: String,
)

enum class GasPriceType {
    Safe, Propose, Fast;

    fun calcGas(tipGas: TipGas): BigDecimal =
        when (this) {
            Safe -> tipGas.safeGasPrice
            Propose -> tipGas.proposeGasPrice
            Fast -> tipGas.fastGasPrice
        }.toBigDecimal() * tipGas.gasLimit.toBigDecimal()

    fun getGasPrice(tipGas: TipGas): String =
        when (this) {
            Safe -> tipGas.safeGasPrice
            Propose -> tipGas.proposeGasPrice
            Fast -> tipGas.fastGasPrice
        }
}
