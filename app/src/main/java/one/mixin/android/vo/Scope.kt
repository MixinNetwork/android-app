package one.mixin.android.vo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import kotlinx.parcelize.Parcelize
import one.mixin.android.R

@SuppressLint("ParcelCreator")
@Parcelize
data class Scope(val name: String, val desc: String) : Parcelable {

    constructor(ctx: Context, scope: String) : this(
        scope,
        when (scope) {
            Scope.SCOPES[0] -> ctx.getString(R.string.auth_profile_content)
            Scope.SCOPES[1] -> ctx.getString(R.string.auth_profile_content) //  Todo relpace
            Scope.SCOPES[2] -> ctx.getString(R.string.auth_messages_represent_description)
            Scope.SCOPES[3] -> ctx.getString(R.string.access_your_contacts_list)
            Scope.SCOPES[4] -> ctx.getString(R.string.auth_assets_more) //  Todo relpace
            Scope.SCOPES[5] -> ctx.getString(R.string.access_your_snapshots)
            Scope.SCOPES[6] -> ctx.getString(R.string.access_your_bots_list)
            Scope.SCOPES[7] -> ctx.getString(R.string.manage_all_your_bots)
            Scope.SCOPES[8] -> ctx.getString(R.string.access_your_circle_list)
            Scope.SCOPES[9] -> ctx.getString(R.string.manage_all_your_circles)
            Scope.SCOPES[10] -> ctx.getString(R.string.access_your_collectibles)
            else -> ctx.getString(R.string.OTHER)
        }
    )

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
    }
}

fun Scope.convertName(ctx: Context): String {
    val id = when (name) {
        Scope.SCOPES[0] -> R.string.Public_profile
        Scope.SCOPES[1] -> R.string.Phone_Number
        Scope.SCOPES[2] -> R.string.Represent_Messages
        Scope.SCOPES[3] -> R.string.Read_Contacts
        Scope.SCOPES[4] -> R.string.Read_Assets
        Scope.SCOPES[5] -> R.string.Read_Snapshots
        Scope.SCOPES[6] -> R.string.Read_Bots
        Scope.SCOPES[7] -> R.string.Manage_Bots
        Scope.SCOPES[8] -> R.string.Read_Circles
        Scope.SCOPES[9] -> R.string.Write_Circles
        Scope.SCOPES[10] -> R.string.Read_Collectibles
        else -> R.string.Public_profile
    }
    return ctx.getString(id)
}

fun group(scopes: List<Scope>): Map<Int, List<Scope>> {
    return scopes.groupBy(
        { it.groupId() }, { it }
    )
}

@DrawableRes
fun Scope.groupId(): Int = when (name) {
    Scope.SCOPES[4], Scope.SCOPES[5], Scope.SCOPES[10] -> R.drawable.ic_auth_wallet
    Scope.SCOPES[6], Scope.SCOPES[7] -> R.drawable.ic_auth_apps
    Scope.SCOPES[8], Scope.SCOPES[9] -> R.drawable.ic_auth_circles
    else -> R.drawable.ic_auth_others
}

fun scopeGroupName(id: Int) = when (id) {
    R.drawable.ic_auth_wallet -> R.string.Wallet
    R.drawable.ic_auth_apps -> R.string.Apps
    R.drawable.ic_auth_circles -> R.string.Circles
    else -> R.string.OTHER
}
