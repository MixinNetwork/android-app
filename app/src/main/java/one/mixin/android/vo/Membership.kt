package one.mixin.android.vo

import android.os.Parcelable
import android.view.View
import androidx.annotation.DrawableRes
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import one.mixin.android.R
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
        return plan in listOf(Plan.ADVANCE, Plan.ELITE, Plan.PROSPERITY) && Instant.now().isBefore(Instant.parse(expiredAt))
    }

    fun isProsperity(): Boolean {
        return plan == Plan.PROSPERITY && Instant.now().isBefore(Instant.parse(expiredAt))
    }
}

@DrawableRes
fun Membership?.membershipIcon() = when {
    this == null -> View.NO_ID
    plan == Plan.ADVANCE -> R.drawable.ic_membership_advance
    plan == Plan.ELITE -> R.drawable.ic_membership_elite
    // PROSPERITY is animation icon
    else -> null
}

enum class Plan(val value: String) {
    @SerializedName("none")
    None("none"),

    @SerializedName("advance")
    ADVANCE("advance"),

    @SerializedName("elite")
    ELITE("elite"),

    @SerializedName("prosperity")
    PROSPERITY("prosperity");
}
