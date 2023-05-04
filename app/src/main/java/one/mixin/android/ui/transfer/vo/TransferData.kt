package one.mixin.android.ui.transfer.vo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransferData<T>(
    @SerialName("type")
    val type: String,
    @SerialName("data")
    val data: T,
)
