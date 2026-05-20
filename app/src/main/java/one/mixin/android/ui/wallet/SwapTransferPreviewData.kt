package one.mixin.android.ui.wallet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.vo.User
import one.mixin.android.web3.js.JsSignMessage

@Parcelize
data class SwapTransferPreviewData(
    val senderAddress: String,
    val receiver: User? = null,
    val web3Transaction: JsSignMessage,
    val gaslessPrepareResponseJson: String? = null,
    val feeAmount: String,
    val feeSymbol: String? = null,
    val feeUsd: String = "0",
    val gasPrice: String? = null,
    val tipGasLimit: String? = null,
    val tipGasMaxFeePerGas: String? = null,
    val tipGasMaxPriorityFeePerGas: String? = null,
) : Parcelable
