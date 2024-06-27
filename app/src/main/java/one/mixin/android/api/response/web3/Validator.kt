package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Validator(
    val name: String,
    val icon: String?,
    @SerializedName("estimated_apy")
    val estimatedApy: String,
    val commission: String,
    @SerializedName("total_stake")
    val totalStake: String,
    @SerializedName("vote_pubkey")
    val votePubkey: String,
) : Parcelable