package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.response.web3.QuoteResponse

data class SwapRequest(
    @SerializedName("userPublicKey") val userPublicKey: String,
    @SerializedName("quoteResponse") val quoteResponse: QuoteResponse,
    @SerializedName("wrapAndUnwrapSol") val wrapAndUnwrapSol: Boolean? = null,
    @SerializedName("useSharedAccounts") val useSharedAccounts: Boolean? = null,
    @SerializedName("feeAccount") val feeAccount: String? = null,
    @SerializedName("trackingAccount") val trackingAccount: String? = null,
    @SerializedName("computeUnitPriceMicroLamports") val computeUnitPriceMicroLamports: Long? = null,
    @SerializedName("prioritizationFeeLamports") val prioritizationFeeLamports: Long? = null,
    @SerializedName("asLegacyTransaction") val asLegacyTransaction: Boolean? = null,
    @SerializedName("useTokenLedger") val useTokenLedger: Boolean? = null,
    @SerializedName("destinationTokenAccount") val destinationTokenAccount: String? = null,
    @SerializedName("dynamicComputeUnitLimit") val dynamicComputeUnitLimit: Boolean? = null,
    @SerializedName("skipUserAccountsRpcCalls") val skipUserAccountsRpcCalls: Boolean? = null
)
