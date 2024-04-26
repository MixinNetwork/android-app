package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppMetadata(
    val name: String,
    @SerializedName("icon_url") val iconUrl: String,
    @SerializedName("contract_address") val contractAddress: String,
    @SerializedName("method_id") val methodId: String,
    @SerializedName("method_name") val methodName: String
) : Parcelable
