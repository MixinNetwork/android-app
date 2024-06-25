package one.mixin.android.vo

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import one.mixin.android.R
import one.mixin.android.session.Session

@Serializable
data class SafeBox(
    val cards: List<Card>,
    val name: String = Session.getAccountId() ?: "",
)

@Serializable
class Card(
    @SerialName("last4")
    @SerializedName("last4")
    val number: String,
    @SerialName("scheme")
    @SerializedName("scheme")
    val scheme: String,
    @SerialName("instrument_id")
    @SerializedName("instrument_id")
    val instrumentId: String,
    @SerialName("card_type")
    @SerializedName("card_type")
    val cardType: String,
    @SerialName("user_id")
    @SerializedName("user_id")
    val userId: String,
)

fun cardIcon(scheme: String?) =
    when {
        scheme.equals("amex", true) -> R.drawable.ic_amex
        scheme.equals("jcb") -> R.drawable.ic_jcb
        scheme.equals("mastercard") -> R.drawable.ic_mastercard
        else -> R.drawable.ic_visa
    }
