package one.mixin.android.event

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.TipSigner

@Parcelize
data class TipEvent(val nodeCounter: Int, val failedSigners: List<TipSigner>? = null) : Parcelable {

    override fun toString(): String {
        val failedSignerIndices = failedSigners?.joinToString { signer -> "${signer.index}" }
        return "node max counter: $nodeCounter, failed signer indices: [$failedSignerIndices]"
    }
}
