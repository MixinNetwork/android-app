package one.mixin.android.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName

@Parcelize
data class SearchBot(
    @SerializedName("user_id")
    @SerialName("user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,
    @SerializedName("identity_number")
    @SerialName("identity_number")
    @ColumnInfo(name = "identity_number")
    val identityNumber: String,
    @SerializedName("relationship")
    @SerialName("relationship")
    @ColumnInfo(name = "relationship")
    var relationship: String,
    @SerializedName("biography")
    @SerialName("biography")
    @ColumnInfo(name = "biography")
    val biography: String,
    @SerializedName("full_name")
    @SerialName("full_name")
    @ColumnInfo(name = "full_name")
    val fullName: String?,
    @SerializedName("avatar_url")
    @SerialName("avatar_url")
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @SerializedName("phone")
    @SerialName("phone")
    @ColumnInfo(name = "phone")
    val phone: String?,
    @SerializedName("is_verified")
    @SerialName("is_verified")
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean?,
    @SerializedName("created_at")
    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String?,
    @SerializedName("mute_until")
    @SerialName("mute_until")
    @ColumnInfo(name = "mute_until")
    var muteUntil: String?,
    @SerializedName("has_pin")
    @SerialName("has_pin")
    @ColumnInfo(name = "has_pin")
    val hasPin: Boolean? = null,
    @SerializedName("app_id")
    @SerialName("app_id")
    @ColumnInfo(name = "app_id")
    var appId: String? = null,
    @SerializedName("is_scam")
    @SerialName("is_scam")
    @ColumnInfo(name = "is_scam")
    var isScam: Boolean? = null,
    @SerializedName("is_deactivated")
    @SerialName("is_deactivated")
    @ColumnInfo("is_deactivated")
    val isDeactivated: Boolean? = null,
    @SerializedName("membership")
    @SerialName("membership")
    @ColumnInfo("membership")
    val membership: Membership? = null,
) : Parcelable {
    fun isBot(): Boolean {
        return appId != null
    }

    fun isMembership(): Boolean {
        return membership?.isMembership() == true
    }

    fun isProsperity(): Boolean {
        return membership?.isProsperity() == true
    }

    fun toUser(): User {
        return User(
            userId = userId,
            identityNumber = identityNumber,
            relationship = relationship,
            biography = biography,
            fullName = fullName,
            avatarUrl = avatarUrl,
            phone = phone,
            isVerified = isVerified,
            createdAt = createdAt,
            muteUntil = muteUntil,
            hasPin = hasPin,
            appId = appId,
            isScam = isScam,
            isDeactivated = isDeactivated,
            membership = membership
        )
    }
}
