package one.mixin.android.vo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import kotlinx.parcelize.Parcelize
import one.mixin.android.R

@SuppressLint("ParcelCreator")
@Parcelize
data class Scope(val name: String, val desc: String) : Parcelable {
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
        Scope.SCOPES[0] -> R.string.auth_public_profile
        Scope.SCOPES[1] -> R.string.phone_number
        Scope.SCOPES[2] -> R.string.auth_messages_represent
        Scope.SCOPES[3] -> R.string.auth_permission_contacts_read
        Scope.SCOPES[4] -> R.string.auth_assets
        Scope.SCOPES[5] -> R.string.auth_snapshot_read
        Scope.SCOPES[6] -> R.string.auth_app_read
        Scope.SCOPES[7] -> R.string.auth_apps_write
        Scope.SCOPES[8] -> R.string.auth_circles_read
        Scope.SCOPES[9] -> R.string.auth_circles_write
        Scope.SCOPES[10] -> R.string.auth_collectibles_read
        else -> R.string.auth_public_profile
    }
    return ctx.getString(id)
}
