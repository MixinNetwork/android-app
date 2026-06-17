package one.mixin.android.ui.tip

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.TipSigner
import one.mixin.android.event.TipEvent
import one.mixin.android.extension.getParcelableCompat
import one.mixin.android.extension.getParcelableExtraCompat

@Parcelize
enum class TipType : Parcelable {
    Create,
    Change,
    Upgrade,
}

sealed class TipStep : Parcelable

@Parcelize internal data object TryConnecting : TipStep()

@Parcelize internal data class RetryConnect(val shouldWatch: Boolean, val reason: String) : TipStep()

@Parcelize internal data object ReadyStart : TipStep()

@Parcelize internal data class RetryProcess(val reason: String) : TipStep()

@Parcelize internal sealed class Processing : TipStep() {
    @Parcelize internal data object Creating : Processing()

    @Parcelize internal data class SyncingNode(val step: Int, val total: Int) : Processing()

    @Parcelize internal data object Updating : Processing()

    @Parcelize internal data object Registering : Processing()
}

@Parcelize internal data class RetryRegister(val tipPriv: ByteArray?, val reason: String) : TipStep()

@Parcelize internal data class LegacyPIN(val message: String) : TipStep()

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

    fun updateTipEvent(
        failedSigners: List<TipSigner>?,
        nodeCounter: Int,
    ) {
        tipEvent = TipEvent(nodeCounter, failedSigners)
    }
}

internal fun Intent.getTipBundle(): TipBundle =
    requireNotNull(
        getParcelableExtraCompat(TipFragment.ARGS_TIP_BUNDLE, TipBundle::class.java),
    ) { "required TipBundle can not be null" }

internal fun Bundle.getTipBundle(): TipBundle =
    requireNotNull(
        getParcelableCompat(TipFragment.ARGS_TIP_BUNDLE, TipBundle::class.java),
    ) { "required TipBundle can not be null" }
