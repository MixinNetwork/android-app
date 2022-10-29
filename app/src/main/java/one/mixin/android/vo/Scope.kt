package one.mixin.android.vo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.recyclerview.widget.DiffUtil
import kotlinx.parcelize.Parcelize
import one.mixin.android.R

@SuppressLint("ParcelCreator")
@Parcelize
data class Scope(val source: String, val name: String, val desc: String) : Parcelable {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Scope>() {
            override fun areItemsTheSame(oldItem: Scope, newItem: Scope) = oldItem == newItem

            override fun areContentsTheSame(oldItem: Scope, newItem: Scope) = oldItem == newItem
        }

        val SCOPES = arrayListOf(
            "PROFILE:READ",
            "PHONE:READ",
            "MESSAGES:REPRESENT",
            "CONTACTS:READ",
            "ASSETS:READ",
            "SNAPSHOTS:READ",
            "APPS:READ",
            "APPS:WRITE",
            "CIRCLES:READ",
            "CIRCLES:WRITE",
            "COLLECTIBLES:READ"
        )

        fun generateScopeFromString(ctx: Context, scope: String): Scope {
            val (name, desc) = when (scope) {
                SCOPES[0] -> Pair(
                    ctx.getString(R.string.Read_your_public_profile),
                    ctx.getString(R.string.Read_your_public_profile_desc)
                )
                SCOPES[1] -> Pair(
                    ctx.getString(R.string.Read_your_phone_number),
                    ctx.getString(R.string.Read_your_phone_number_desc)
                )
                SCOPES[2] -> Pair(
                    ctx.getString(R.string.Represent_send_messages),
                    ctx.getString(R.string.Represent_send_messages_desc)
                )
                SCOPES[3] -> Pair(
                    ctx.getString(R.string.Read_your_contacts),
                    ctx.getString(R.string.Read_your_contacts_desc)
                )
                SCOPES [4] -> Pair(
                    ctx.getString(R.string.Read_your_asset),
                    ctx.getString(R.string.Read_your_asset_desc)
                )
                SCOPES [5] -> Pair(
                    ctx.getString(R.string.Read_your_snapshots),
                    ctx.getString(R.string.Read_your_snapshots_desc)
                )
                SCOPES [6] -> Pair(
                    ctx.getString(R.string.Read_your_apps),
                    ctx.getString(R.string.Read_your_apps_desc)
                )
                SCOPES [7] -> Pair(
                    ctx.getString(R.string.Manage_your_apps),
                    ctx.getString(R.string.Manage_your_apps_desc)
                )
                SCOPES [8] -> Pair(
                    ctx.getString(R.string.Read_your_circles),
                    ctx.getString(R.string.Read_your_circles_desc)
                )
                SCOPES [9] -> Pair(
                    ctx.getString(R.string.Manage_your_circles),
                    ctx.getString(R.string.Manage_your_circles_desc)
                )
                SCOPES [10] -> Pair(
                    ctx.getString(R.string.Read_your_NFTs),
                    ctx.getString(R.string.Read_your_NFTs_desc)
                )
                else -> Pair(ctx.getString(R.string.OTHER), ctx.getString(R.string.OTHER))
            }
            return Scope(scope, name, desc)
        }
    }
}

fun groupScope(scopes: List<Scope>): ArrayMap<Int, MutableList<Scope>> {
    return scopes.groupByTo(
        arrayMapOf()
    ) { it.groupId() }
}

@DrawableRes
fun Scope.groupId(): Int = when (source) {
    Scope.SCOPES[4], Scope.SCOPES[5], Scope.SCOPES[10] -> R.drawable.ic_auth_wallet
    Scope.SCOPES[6], Scope.SCOPES[7] -> R.drawable.ic_auth_apps
    Scope.SCOPES[8], Scope.SCOPES[9] -> R.drawable.ic_auth_circles
    else -> R.drawable.ic_auth_others
}

fun getScopeGroupName(@DrawableRes id: Int) = when (id) {
    R.drawable.ic_auth_wallet -> "Wallet"
    R.drawable.ic_auth_apps -> "Apps"
    R.drawable.ic_auth_circles -> "Circles"
    else -> "Others"
}

fun scopeGroupName(id: Int) = when (id) {
    R.drawable.ic_auth_wallet -> R.string.Wallet
    R.drawable.ic_auth_apps -> R.string.Apps
    R.drawable.ic_auth_circles -> R.string.Circles
    else -> R.string.OTHER
}
