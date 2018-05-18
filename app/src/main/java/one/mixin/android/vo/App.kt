package one.mixin.android.vo

import android.annotation.SuppressLint
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@TypeConverters(ArrayConverters::class)
@Entity(tableName = "apps")
class App(
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
    val icon_url: String,
    @SerializedName("description")
    @ColumnInfo(name = "description")
    val description: String,
    @SerializedName("app_secret")
    @ColumnInfo(name = "app_secret")
    val appSecret: String,
    @SerializedName("capabilites")
    @ColumnInfo(name = "capabilites")
    val capabilites: ArrayList<String>?,
    @SerializedName("creator_id")
    @ColumnInfo(name = "creator_id")
    val creatorId: String

) : Parcelable

enum class AppCap { GROUP, CONTACT }