package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class SearchMessageItem(
    val messageId: String,
    val conversationId: String,
    val conversationCategory: String?,
    val conversationName: String?,
    val messageCount: Int,
    val type: String,
    val userId: String,
    val userFullName: String?,
    val userAvatarUrl: String?,
    val conversationAvatarUrl: String?
) : Parcelable
