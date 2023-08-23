package one.mixin.android.vo

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
    val number: String,
    val scheme: String,
    @SerialName("instrument_id")
    val instrumentId: String,
)
