package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class SearchMessageItem(
    val conversationId: String,
    val conversationCategory: String?,
    val conversationName: String?,
    val messageCount: Int,
    val userId: String,
    val userFullName: String?,
    val userAvatarUrl: String?,
    val conversationAvatarUrl: String?
) : Parcelable
