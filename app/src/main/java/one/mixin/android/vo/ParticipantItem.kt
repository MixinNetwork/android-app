package one.mixin.android.vo

import androidx.room.Entity

@Entity
data class ParticipantItem(
    val userId: String,
    val sessionId: String,
    val deviceId: Int
)