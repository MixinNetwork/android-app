package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import android.view.View
import androidx.core.view.isVisible
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class ChatMinimal(
    val category: String,
    val conversationId: String,
    val groupIconUrl: String?,
    val groupName: String?,
    val ownerIdentityNumber: String,
    val userId: String,
    val fullName: String?,
    val avatarUrl: String?,
    val isVerified: Boolean?,
    val appId: String?
) : Parcelable

fun ChatMinimal.showVerifiedOrBot(verifiedView: View, botView: View) {
    when {
        isVerified == true -> {
            verifiedView.isVisible = true
            botView.isVisible = false
        }
        appId != null -> {
            verifiedView.isVisible = false
            botView.isVisible = true
        }
        else -> {
            verifiedView.isVisible = false
            botView.isVisible = false
        }
    }
}
