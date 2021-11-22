package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import kotlin.contracts.contract

interface BotInterface {
    fun getBotId(): String
}

@SuppressLint("ParcelCreator")
@Parcelize
@TypeConverters(ArrayConverters::class)
@Entity(tableName = "apps")
@JsonClass(generateAdapter = true)
data class App(
    @PrimaryKey
    @Json(name = "app_id")
    @ColumnInfo(name = "app_id")
    val appId: String,
    @Json(name = "app_number")
    @ColumnInfo(name = "app_number")
    val appNumber: String,
    @Json(name = "home_uri")
    @ColumnInfo(name = "home_uri")
    val homeUri: String,
    @Json(name = "redirect_uri")
    @ColumnInfo(name = "redirect_uri")
    val redirectUri: String,
    @Json(name = "name")
    @ColumnInfo(name = "name")
    val name: String,
    @Json(name = "icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @Json(name = "category")
    @ColumnInfo(name = "category")
    val category: String?,
    @Json(name = "description")
    @ColumnInfo(name = "description")
    val description: String,
    @Json(name = "app_secret")
    @ColumnInfo(name = "app_secret")
    val appSecret: String,
    @Json(name = "capabilities")
    @ColumnInfo(name = "capabilities")
    val capabilities: ArrayList<String>?,
    @Json(name = "creator_id")
    @ColumnInfo(name = "creator_id")
    val creatorId: String,
    @Json(name = "resource_patterns")
    @ColumnInfo(name = "resource_patterns")
    val resourcePatterns: ArrayList<String>?,
    @Json(name = "updated_at")
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

fun App?.matchResourcePattern(url: String): Boolean {
    contract {
        returns(true) implies (this@matchResourcePattern != null)
    }
    return this?.resourcePatterns?.find { "$url/".startsWith(it, ignoreCase = true) } != null
}
