package one.mixin.android.api.response.web3

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class GaslessTxResponse(
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("payload")
    val payload: JsonElement,
)

data class EthGaslessTxPayload(
    @SerializedName("userOperation")
    val userOperation: UserOperationJson,
    @SerializedName("signing")
    val signing: EthGaslessSignRequests,
)

data class UserOperationJson(
    @SerializedName("sender")
    val sender: String,
    @SerializedName("nonce")
    val nonce: String,
    @SerializedName("initCode")
    val initCode: String,
    @SerializedName("callData")
    val callData: String,
    @SerializedName("callGasLimit")
    val callGasLimit: String,
    @SerializedName("verificationGasLimit")
    val verificationGasLimit: String,
    @SerializedName("preVerificationGas")
    val preVerificationGas: String,
    @SerializedName("maxFeePerGas")
    val maxFeePerGas: String,
    @SerializedName("maxPriorityFeePerGas")
    val maxPriorityFeePerGas: String,
    @SerializedName("paymasterAndData")
    val paymasterAndData: String,
    @SerializedName("signature")
    val signature: String,
)

data class EthGaslessSignRequests(
    @SerializedName("userOperation")
    val userOperation: UserOpSignRequest,
    @SerializedName("eip7702Auth")
    val eip7702Auth: EIP7702SignRequest? = null,
)

data class UserOpSignRequest(
    @SerializedName("signType")
    val signType: String,
    @SerializedName("message")
    val message: String,
)

data class EIP7702SignRequest(
    @SerializedName("signType")
    val signType: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("chainId")
    val chainId: String?,
    @SerializedName("address")
    val address: String?,
    @SerializedName("nonce")
    val nonce: String?,
)

val EIP7702SignRequest.shouldSign: Boolean
    get() = !message.isNullOrBlank()
