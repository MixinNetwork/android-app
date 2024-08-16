package one.mixin.android.vo

import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import one.mixin.android.R

data class ParticipantItem(
    val conversationId: String,
    val role: String,
    val userId: String,
    val createdAt: String?,
    val identityNumber: String,
    var relationship: String,
    val biography: String,
    val fullName: String?,
    val avatarUrl: String?,
    val phone: String?,
    val isVerified: Boolean?,
    val userCreatedAt: String?,
    var muteUntil: String?,
    val hasPin: Boolean? = null,
    var appId: String? = null,
    var isScam: Boolean? = null,
    val membership: Membership?,
) {
    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<ParticipantItem>() {
                override fun areItemsTheSame(
                    oldItem: ParticipantItem,
                    newItem: ParticipantItem,
                ) =
                    oldItem.userId == newItem.userId

                override fun areContentsTheSame(
                    oldItem: ParticipantItem,
                    newItem: ParticipantItem,
                ) =
                    oldItem == newItem
            }
    }

    fun toUser() =
        User(
            userId,
            identityNumber,
            relationship,
            biography,
            fullName,
            avatarUrl,
            phone,
            isVerified,
            userCreatedAt,
            muteUntil,
            hasPin,
            appId,
            isScam,
        )

    fun isMembership(): Boolean {
        return membership?.isMembership() == true
    }
}
