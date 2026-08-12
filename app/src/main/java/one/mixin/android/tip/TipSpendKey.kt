package one.mixin.android.tip

import android.content.Context

suspend fun Tip.getSpendKeyFromPin(
    context: Context,
    pin: String,
): ByteArray {
    val tipPriv = getOrRecoverTipPriv(context, pin).getOrThrow()
    return getSpendPrivFromEncryptedSalt(
        getMnemonicFromEncryptedPreferences(context),
        getEncryptedSalt(context),
        pin,
        tipPriv,
    )
}
