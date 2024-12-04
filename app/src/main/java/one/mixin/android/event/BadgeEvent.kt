package one.mixin.android.event

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.TipSigner

@Parcelize
data class BadgeEvent(val badge: String) : Parcelable
