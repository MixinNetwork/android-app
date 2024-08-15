package one.mixin.android.vo

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName

class AppMessenger(
    @SerializedName("version")
    @SerialName("version")
    val version: String,
    @SerializedName("release_url")
    @SerialName("release_url")
    val releaseUrl: String,
)