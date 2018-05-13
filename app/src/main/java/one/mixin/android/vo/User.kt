package one.mixin.android.vo

import android.annotation.SuppressLint
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "users",
    indices = [(Index(value = arrayOf("full_name")))])
data class User(
    @PrimaryKey
    @SerializedName("user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,
    @SerializedName("identity_number")
    @ColumnInfo(name = "identity_number")
    val identityNumber: String,
    /**
     * @see UserRelationship
     */
    @SerializedName("relationship")
    @ColumnInfo(name = "relationship")
    var relationship: String,
    @SerializedName("full_name")
    @ColumnInfo(name = "full_name")
    val fullName: String?,
    @SerializedName("avatar_url")
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @ColumnInfo(name = "phone")
    val phone: String?,
    @SerializedName("is_verified")
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean?,
    @SerializedName("create_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String?,
    @SerializedName("mute_until")
    @ColumnInfo(name = "mute_until")
    var muteUntil: String?,
    @SerializedName("has_pin")
    @ColumnInfo(name = "has_pin")
    val hasPin: Boolean? = null,
    @SerializedName("app_id")
    @ColumnInfo(name = "app_id")
    var appId: String? = null
) : Parcelable {
    @SerializedName("app")
    @Ignore
    var app: App? = null

    fun isBot(): Boolean {
        return appId != null
    }
}

const val SYSTEM_USER = "00000000-0000-0000-0000-000000000000"

fun createSystemUser(): User {
    return User(SYSTEM_USER, "0", "", "0", null, null, false, null, null)
}
