package one.mixin.android.api.response.web3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StakeAccountActivation(
    val pubkey: String,
    val active: Long,
    val inactive: Long,
    val state: String,
): Parcelable