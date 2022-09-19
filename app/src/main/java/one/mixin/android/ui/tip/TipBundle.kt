package one.mixin.android.ui.tip

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.TipSigner
import one.mixin.android.event.TipEvent

@Parcelize
enum class TipType : Parcelable {
    Create, Change, Upgrade
}

sealed class TipStep : Parcelable
@Parcelize internal object TryConnecting : TipStep()
@Parcelize internal data class RetryConnect(val shouldWatch: Boolean) : TipStep()
@Parcelize internal object ReadyStart : TipStep()
@Parcelize internal data class RetryProcess(val reason: String) : TipStep()
@Parcelize internal sealed class Processing : TipStep() {
    @Parcelize internal object Creating : Processing()
    @Parcelize internal data class SyncingNode(val step: Int, val total: Int) : Processing()
    @Parcelize internal object Updating : Processing()
}

@Parcelize
data class TipBundle(
    val tipType: TipType,
    val deviceId: String,
    var tipStep: TipStep,
    var pin: String? = null,
    var oldPin: String? = null,
    var tipEvent: TipEvent? = null,
) : Parcelable {
    fun forChange() = tipType == TipType.Change

    fun forCreate() = tipType == TipType.Create

    fun forRecover() = tipEvent != null

    fun updateTipEvent(failedSigners: List<TipSigner>?, nodeCounter: Int) {
        tipEvent = TipEvent(nodeCounter, failedSigners)
    }
}

internal fun Intent.getTipBundle(): TipBundle = requireNotNull(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(TipFragment.ARGS_TIP_BUNDLE, TipBundle::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(TipFragment.ARGS_TIP_BUNDLE)
    }
) { "required TipBundle can not be null" }

internal fun Bundle.getTipBundle(): TipBundle = requireNotNull(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(TipFragment.ARGS_TIP_BUNDLE, TipBundle::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(TipFragment.ARGS_TIP_BUNDLE)
    }
) { "required TipBundle can not be null" }
