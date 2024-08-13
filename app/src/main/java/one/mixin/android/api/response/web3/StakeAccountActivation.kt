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

@Suppress("EnumEntryName")
enum class StakeState {
    active, inactive, activating, deactivating;
}

fun String.isActiveState(): Boolean = this == StakeState.active.name || this == StakeState.activating.name
fun String.isDeactivatingState(): Boolean = this == StakeState.deactivating.name