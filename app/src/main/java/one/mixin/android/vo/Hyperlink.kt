package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hyperlinks")
data class Hyperlink(
    @PrimaryKey
    @ColumnInfo(name = "hyperlink")
    val hyperlink: String,
    @ColumnInfo(name = "site_name")
    val siteName: String,
    @ColumnInfo(name = "site_title")
    val siteTitle: String,
    @ColumnInfo(name = "site_description")
    val siteDescription: String?,
    @ColumnInfo(name = "site_image")
    val siteImage: String?,
)
