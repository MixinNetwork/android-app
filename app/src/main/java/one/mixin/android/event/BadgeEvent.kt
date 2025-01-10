package one.mixin.android.event

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BadgeEvent(val badge: String) : Parcelable
