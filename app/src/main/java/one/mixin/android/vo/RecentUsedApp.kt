package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo

data class RecentUsedApp(
    @ColumnInfo(name = "full_name")
    val fullName: String?,
    @ColumnInfo(name = "app_id")
    val appId: String,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecentUsedApp>() {
            override fun areItemsTheSame(p0: RecentUsedApp, p1: RecentUsedApp): Boolean {
                return p0.appId == p1.appId
            }

            override fun areContentsTheSame(p0: RecentUsedApp, p1: RecentUsedApp): Boolean {
                return p0 == p1
            }
        }
    }
}
