package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class TipConfig(
    @SerializedName("commitments")
    val commitments: ArrayList<String>,
    @SerializedName("signers")
    val signers: ArrayList<TipSigner>,
)

data class TipSigner(
    val identity: String,
    val index: Int,
    val api: String,
)
