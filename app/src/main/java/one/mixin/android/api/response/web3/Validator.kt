package one.mixin.android.api.response.web3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Validator(
    val nodePubkey: String,
    val name: String,
    val details: String,
    val keybaseUsername: String,
    val website: String,
    val iconUrl: String,
    val votePubkey: String,
    val activatedStake: Long,
    val commission: Int,
    val lastVote: Long,
    val rootSlot: Long
) : Parcelable