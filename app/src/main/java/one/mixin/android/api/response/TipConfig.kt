package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class TipConfig(
    @SerializedName("commitments")
    val commitments: ArrayList<String>,
    @SerializedName("signers")
    val signers: ArrayList<TipSigner>
)

@Parcelize
data class TipSigner(
    val identity: String,
    val index: Int,
    val api: String
) : Parcelable
