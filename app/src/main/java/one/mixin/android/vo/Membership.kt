package one.mixin.android.vo

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.threeten.bp.Instant

@Parcelize
@Serializable
data class Membership(
    @SerializedName("plan")
    @SerialName("plan")
    val plan: Plan,
    @SerializedName("expired_at")
    @SerialName("expired_at")
    val expiredAt: String,
) : Parcelable {
    fun isMembership(): Boolean {
        return plan in listOf(Plan.BASIC, Plan.STANDARD, Plan.PREMIUM) && Instant.now().isBefore(Instant.parse(expiredAt))
    }
}

enum class Plan(val value: String) {
    @SerializedName("none")
    None("none"),

    @SerializedName("basic")
    BASIC("basic"),

    @SerializedName("standard")
    STANDARD("standard"),

    @SerializedName("premium")
    PREMIUM("premium");
}
