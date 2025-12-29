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

    override fun toString(): String {
        return if (Instant.now().isBefore(Instant.parse(expiredAt))) {
            plan.value
        } else {
            Plan.None.value
        }
    }
}

@DrawableRes
fun Membership?.membershipIcon(force: Boolean = false): Int = when {
    this == null -> View.NO_ID
    plan == Plan.ADVANCE -> R.drawable.ic_membership_advance
    plan == Plan.ELITE -> R.drawable.ic_membership_elite
    force && plan == Plan.PROSPERITY -> R.drawable.ic_membership_prosperity
    else -> View.NO_ID
}

enum class Plan(val value: String) {
    @SerializedName("none")
    @SerialName("none")
    None("none"),

    @SerializedName("advance")
    @SerialName("advance")
    ADVANCE("advance"),

    @SerializedName("elite")
    @SerialName("elite")
    ELITE("elite"),

    @SerializedName("prosperity")
    @SerialName("prosperity")
    PROSPERITY("prosperity");
}
