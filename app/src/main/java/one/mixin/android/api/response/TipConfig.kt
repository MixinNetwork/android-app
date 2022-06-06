package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class TipConfig(
    @SerializedName("commitments")
    val commitments: ArrayList<String>,
    @SerializedName("signers")
    val signers: ArrayList<TipSiger>,
)

data class TipSiger(
    val identity: String,
    val index: Int,
    val api: String,
)
