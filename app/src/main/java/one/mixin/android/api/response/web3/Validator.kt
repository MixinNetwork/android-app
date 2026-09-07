package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Validator(
    @SerializedName("nodePubkey")
    val nodePubkey: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("details")
    val details: String,
    @SerializedName("keybaseUsername")
    val keybaseUsername: String,
    @SerializedName("website")
    val website: String,
    @SerializedName("iconUrl")
    val iconUrl: String,
    @SerializedName("votePubkey")
    val votePubkey: String,
    @SerializedName("activatedStake")
    val activatedStake: Long,
    @SerializedName("commission")
    val commission: Int,
    @SerializedName("lastVote")
    val lastVote: Long,
    @SerializedName("rootSlot")
    val rootSlot: Long
) : Parcelable