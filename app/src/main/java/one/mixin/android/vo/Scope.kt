package one.mixin.android.vo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
                    ctx.getString(R.string.Allow_bot_access_profile)
                )
                SCOPES[1] -> Pair(
                    ctx.getString(R.string.Read_your_phone_number),
                    ctx.getString(R.string.Allow_bot_access_number)
                )
                SCOPES[2] -> Pair(
                    ctx.getString(R.string.Represent_send_messages),
                    ctx.getString(R.string.Allow_bot_send_messages)
                )
                SCOPES[3] -> Pair(
                    ctx.getString(R.string.Read_your_contacts),
                    ctx.getString(R.string.Read_your_contacts)
                )
                SCOPES [4] -> Pair(
                    ctx.getString(R.string.Read_your_assets),
                    ctx.getString(R.string.Allow_bot_access_asset)
                )
                SCOPES [5] -> Pair(
                    ctx.getString(R.string.Read_your_snapshots),
                    ctx.getString(R.string.Allow_bot_access_snapshots)
                )
                SCOPES [6] -> Pair(
                    ctx.getString(R.string.Read_your_apps),
                    ctx.getString(R.string.Allow_bot_access_bots)
                )
                SCOPES [7] -> Pair(
                    ctx.getString(R.string.Manage_your_apps),
                    ctx.getString(R.string.Allow_bot_manage_bots)
                )
                SCOPES [8] -> Pair(
                    ctx.getString(R.string.Read_your_circles),
                    ctx.getString(R.string.Allow_bot_access_circles)
                )
                SCOPES [9] -> Pair(
                    ctx.getString(R.string.Manage_your_circles),
                    ctx.getString(R.string.Allow_bot_manage_circles)
                )
                SCOPES [10] -> Pair(
                    ctx.getString(R.string.Read_your_NFTs),
                    ctx.getString(R.string.Allow_bot_access_nfts)
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

@StringRes
fun getScopeGroupName(@DrawableRes id: Int) = when (id) {
    R.drawable.ic_auth_wallet -> R.string.Wallet
    R.drawable.ic_auth_apps -> R.string.bots_title
    R.drawable.ic_auth_circles -> R.string.Circles
    else -> R.string.Others
}
