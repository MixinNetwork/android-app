package one.mixin.android.api.response.web3

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class GaslessTxResponse(
    @SerializedName("chain_id")
    val chainId: String,
    val payload: JsonElement,
)

data class EthGaslessTxPayload(
    val userOperation: UserOperationJson,
    val signing: EthGaslessSignRequests,
)

data class UserOperationJson(
    val sender: String,
    val nonce: String,
    val initCode: String,
    val callData: String,
    val callGasLimit: String,
    val verificationGasLimit: String,
    val preVerificationGas: String,
    val maxFeePerGas: String,
    val maxPriorityFeePerGas: String,
    val paymasterAndData: String,
    val signature: String,
)

data class EthGaslessSignRequests(
    val userOperation: UserOpSignRequest,
    val eip7702Auth: EIP7702SignRequest? = null,
)

data class UserOpSignRequest(
    val signType: String,
    val message: String,
)

data class EIP7702SignRequest(
    val signType: String?,
    val message: String?,
    @SerializedName("chainId")
    val chainId: String?,
    val address: String?,
    val nonce: String?,
)

val EIP7702SignRequest.shouldSign: Boolean
    get() = !message.isNullOrBlank()
