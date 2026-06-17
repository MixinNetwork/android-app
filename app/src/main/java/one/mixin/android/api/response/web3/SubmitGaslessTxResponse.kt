package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class SubmitGaslessTxResponse(
    @SerializedName("sponsor_tx_id")
    val sponsorTxId: String,
)
