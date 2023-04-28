package one.mixin.android.ui.transfer.vo

import com.google.gson.JsonObject
import kotlinx.serialization.SerialName

data class TransferData(
    @SerialName("type")
    val type: String,
    @SerialName("data")
    val data: JsonObject,
)
