package one.mixin.android.event

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.TipSigner

@Parcelize
data class TipEvent(val nodeCounter: Int, val failedSigners: List<TipSigner>? = null) : Parcelable
