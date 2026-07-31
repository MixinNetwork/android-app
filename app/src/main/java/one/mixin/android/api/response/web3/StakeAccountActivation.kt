package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class StakeAccountActivation(
    @SerializedName("pubkey")
    val pubkey: String,
    @SerializedName("active")
    val active: Long,
    @SerializedName("inactive")
    val inactive: Long,
    @SerializedName("state")
    val state: String,
): Parcelable

@Suppress("EnumEntryName")
enum class StakeState {
    active, inactive, activating, deactivating;
}

fun String.isActiveState(): Boolean = this == StakeState.active.name || this == StakeState.activating.name
fun String.isDeactivatingState(): Boolean = this == StakeState.deactivating.name