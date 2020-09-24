package one.mixin.android.webrtc

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SelectItem(val conversationId: String?, val userId: String?) : Parcelable
