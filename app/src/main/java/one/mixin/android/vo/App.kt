package one.mixin.android.vo

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

interface BotInterface {
    fun getBotId(): String
}

@SuppressLint("ParcelCreator")
@Parcelize
@TypeConverters(ArrayConverters::class)
@Entity(tableName = "apps")
data class App(
    @PrimaryKey
    @SerializedName("app_id")
    @ColumnInfo(name = "app_id")
    val appId: String,
    @SerializedName("app_number")
    @ColumnInfo(name = "app_number")
    val appNumber: String,
    @SerializedName("home_uri")
    @ColumnInfo(name = "home_uri")
    val homeUri: String,
    @SerializedName("redirect_uri")
    @ColumnInfo(name = "redirect_uri")
    val redirectUri: String,
    @SerializedName("name")
    @ColumnInfo(name = "name")
    val name: String,
    @SerializedName("icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @SerializedName("category")
    @ColumnInfo(name = "category")
    val category: String?,
    @SerializedName("description")
    @ColumnInfo(name = "description")
    val description: String,
    @SerializedName("app_secret")
    @ColumnInfo(name = "app_secret")
    val appSecret: String,
    @SerializedName("capabilities")
    @ColumnInfo(name = "capabilities")
    val capabilities: ArrayList<String>?,
    @SerializedName("creator_id")
    @ColumnInfo(name = "creator_id")
    val creatorId: String,
    @SerializedName("resource_patterns")
    @ColumnInfo(name = "resource_patterns")
    val resourcePatterns: ArrayList<String>?,
    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String?
) : Parcelable, BotInterface {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<App>() {
            override fun areItemsTheSame(p0: App, p1: App): Boolean {
                return p0.appId == p1.appId
            }

            override fun areContentsTheSame(p0: App, p1: App): Boolean {
                return p0 == p1
            }
        }
    }

    override fun getBotId() = appId
}

enum class AppCap { GROUP, CONTACT, IMMERSIVE, ENCRYPTED }

fun String.matchResourcePattern(resourcePatterns: List<String>?): Boolean {
    val uri = Uri.parse(this).run { "$scheme://$host" }
    return resourcePatterns?.map { pattern -> Uri.parse(pattern).run { "$scheme://$host" } }
        ?.find { pattern -> uri.equals(pattern, true) } != null
}
