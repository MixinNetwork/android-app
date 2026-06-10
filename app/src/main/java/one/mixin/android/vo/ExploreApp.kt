package one.mixin.android.vo

import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName

class ExploreApp(
    @SerializedName("app_id")
    @SerialName("app_id")
    @ColumnInfo(name = "app_id")
    val appId: String,
    @SerializedName("app_number")
    @SerialName("app_number")
    @ColumnInfo(name = "app_number")
    val appNumber: String,
    @SerializedName("home_uri")
    @SerialName("home_uri")
    @ColumnInfo(name = "home_uri")
    val homeUri: String,
    @SerializedName("name")
    @SerialName("name")
    @ColumnInfo(name = "name")
    val name: String,
    @SerializedName("icon_url")
    @SerialName("icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean?,
    @ColumnInfo(name = "membership")
    val membership: Membership?
) : BotInterface {
    override fun getBotId() = appId

    fun isMembership(): Boolean {
        return membership?.isMembership() == true
    }

    fun isProsperity(): Boolean {
        return membership?.isProsperity() == true
    }
}
