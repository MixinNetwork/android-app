package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName
import one.mixin.android.api.response.web3.QuoteResponse

data class SwapRequest(
    @SerializedName("user_public_key") val userPublicKey: String,
    @SerializedName("quote_response") val quoteResponse: QuoteResponse,
    @SerializedName("wrap_and_unwrap_sol") val wrapAndUnwrapSol: Boolean? = null,
    @SerializedName("use_shared_accounts") val useSharedAccounts: Boolean? = null,
    @SerializedName("fee_account") val feeAccount: String? = null,
    @SerializedName("tracking_account") val trackingAccount: String? = null,
    @SerializedName("compute_unit_price_micro_lamports") val computeUnitPriceMicroLamports: Long? = null,
    @SerializedName("prioritization_fee_lamports") val prioritizationFeeLamports: Long? = null,
    @SerializedName("as_legacy_transaction") val asLegacyTransaction: Boolean? = null,
    @SerializedName("use_token_ledger") val useTokenLedger: Boolean? = null,
    @SerializedName("destination_token_account") val destinationTokenAccount: String? = null,
    @SerializedName("dynamic_compute_unit_limit") val dynamicComputeUnitLimit: Boolean? = null,
    @SerializedName("skip_user_accounts_rpc_calls") val skipUserAccountsRpcCalls: Boolean? = null
)