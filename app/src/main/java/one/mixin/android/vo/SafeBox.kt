package one.mixin.android.vo

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import one.mixin.android.session.Session
@Serializable
data class SafeBox(
    val cards: List<Card>,
    val name: String = Session.getAccountId()!!,
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
