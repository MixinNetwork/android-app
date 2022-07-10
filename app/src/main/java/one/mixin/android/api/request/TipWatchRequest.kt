package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class TipWatchRequest(
    @SerializedName("watcher")
    val watcher: String? = null,
    @SerializedName("action")
    val action: String = "WATCH",
)
