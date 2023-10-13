package one.mixin.android.webrtc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectItem(val conversationId: String?, val userId: String?) : Parcelable
