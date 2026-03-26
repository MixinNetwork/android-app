package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class SubmitGaslessTxRequest(
    @SerializedName("chain_id")
    val chainId: String,
    val payload: String,
    @SerializedName("user_op_signature")
    val userOpSignature: String,
    @SerializedName("eip7702_auth_signature")
    val eip7702AuthSignature: String,
)